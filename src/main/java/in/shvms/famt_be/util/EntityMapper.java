package in.shvms.famt_be.util;

import in.shvms.famt_be.dto.*;
import in.shvms.famt_be.entity.*;
import in.shvms.famt_be.repositories.neo4j.EventRepository;
import in.shvms.famt_be.repositories.neo4j.LineageRepository;
import in.shvms.famt_be.repositories.neo4j.LocationRepository;
import in.shvms.famt_be.repositories.neo4j.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for mapping between DTOs and Entities
 */
@Component
@RequiredArgsConstructor
public class EntityMapper {

    private final LineageRepository lineageRepo;
    private final LocationRepository locationRepo;
    private final EventRepository eventRepo;
    private final PersonRepository personRepo;

    // ========== PERSON MAPPING ==========

    /**
     * Convert PersonDto to Person entity
     */
    public Person toPersonEntity(PersonDto dto, String tenantId) {
        Person person = new Person();
        person.setId(dto.id());
        person.setTenantId(tenantId);
        person.setFirstName(dto.firstName());
        person.setLastName(dto.lastName());
        person.setMaidenName(dto.maidenName());
        person.setPetName(dto.petName());
        person.setGender(dto.gender());
        person.setDateOfBirth(dto.dateOfBirth());
        person.setDateOfDeath(dto.dateOfDeath());

        // Resolve lineage
        if (dto.lineageId() != null) {
            lineageRepo.findByTenantIdAndId(tenantId, dto.lineageId())
                    .ifPresent(person::setLineage);
        }

        // Resolve current residence
        if (dto.currentResidenceId() != null) {
            locationRepo.findById(dto.currentResidenceId())
                    .ifPresent(person::setCurrentResidence);
        }

        // Resolve birth event
        if (dto.birthEventId() != null) {
            eventRepo.findByTenantIdAndId(tenantId, dto.birthEventId())
                    .ifPresent(person::setBirthEvent);
        }

        // Resolve death event
        if (dto.deathEventId() != null) {
            eventRepo.findByTenantIdAndId(tenantId, dto.deathEventId())
                    .ifPresent(person::setDeathEvent);
        }

        // Initialize collections
        person.setSpouseRelations(new HashSet<>());
        person.setChildrenRelations(new HashSet<>());
        person.setFriends(new HashSet<>());
        person.setLifeEvents(new HashSet<>());
        person.setAssociations(new HashSet<>());

        return person;
    }

    /**
     * Convert Person entity to PersonDto
     */
    public PersonDto toPersonDto(Person person) {
        return PersonDto.forResponse(
                person.getId(),
                person.getFirstName(),
                person.getLastName(),
                person.getMaidenName(),
                person.getPetName(),
                person.getGender(),
                person.getDateOfBirth(),
                person.getDateOfDeath(),
                person.getLineage() != null ? person.getLineage().getId() : null,
                person.getCurrentResidence() != null ? person.getCurrentResidence().getId() : null,
                person.getBirthEvent() != null ? person.getBirthEvent().getId() : null,
                person.getDeathEvent() != null ? person.getDeathEvent().getId() : null
        );
    }

    /**
     * Convert Person entity to detailed PersonDetailDto
     */
    public PersonDetailDto toPersonDetailDto(Person person) {
        // Lineage info
        PersonDetailDto.LineageInfo lineageInfo = null;
        if (person.getLineage() != null) {
            lineageInfo = new PersonDetailDto.LineageInfo(
                    person.getLineage().getId(),
                    person.getLineage().getName()
            );
        }

        // Location info
        PersonDetailDto.LocationInfo locationInfo = null;
        if (person.getCurrentResidence() != null) {
            Location loc = person.getCurrentResidence();
            String fullPath = buildLocationPath(loc);
            locationInfo = new PersonDetailDto.LocationInfo(
                    loc.getId(),
                    loc.getLocationName(),
                    loc.getLocationType().name(),
                    fullPath
            );
        }

        // Parents
        List<PersonDetailDto.ParentChildRelationDto> parents = new ArrayList<>();
        if (person.getChildrenRelations() != null) {
            parents = person.getChildrenRelations().stream()
                    .map(rel -> new PersonDetailDto.ParentChildRelationDto(
                            toPersonSummary(rel.getParent()),
                            rel.getRelationshipType().name(),
                            rel.getStartDate(),
                            rel.getConfidenceScore()
                    ))
                    .collect(Collectors.toList());
        }

        // Children (need to find persons where this person is parent)
        List<PersonDetailDto.ParentChildRelationDto> children = new ArrayList<>();
        List<Person> allPeople = personRepo.findAllByTenantId(person.getTenantId());
        for (Person p : allPeople) {
            if (p.getChildrenRelations() != null) {
                for (ParentChildRelation rel : p.getChildrenRelations()) {
                    if (rel.getParent().getId().equals(person.getId())) {
                        children.add(new PersonDetailDto.ParentChildRelationDto(
                                toPersonSummary(p),
                                rel.getRelationshipType().name(),
                                rel.getStartDate(),
                                rel.getConfidenceScore()
                        ));
                    }
                }
            }
        }

        // Spouses
        List<PersonDetailDto.SpousalRelationDto> spouses = new ArrayList<>();
        if (person.getSpouseRelations() != null) {
            spouses = person.getSpouseRelations().stream()
                    .map(rel -> new PersonDetailDto.SpousalRelationDto(
                            toPersonSummary(rel.getSpouse()),
                            rel.getStatus().name(),
                            rel.getStartDate(),
                            rel.getEndDate(),
                            rel.getPartnershipType()
                    ))
                    .collect(Collectors.toList());
        }

        // Friends
        List<PersonDetailDto.PersonSummaryDto> friends = new ArrayList<>();
        if (person.getFriends() != null) {
            friends = person.getFriends().stream()
                    .map(this::toPersonSummary)
                    .collect(Collectors.toList());
        }

        // Birth event
        PersonDetailDto.EventSummaryDto birthEvent = null;
        if (person.getBirthEvent() != null) {
            birthEvent = toEventSummary(person.getBirthEvent());
        }

        // Death event
        PersonDetailDto.EventSummaryDto deathEvent = null;
        if (person.getDeathEvent() != null) {
            deathEvent = toEventSummary(person.getDeathEvent());
        }

        // Life events
        List<PersonDetailDto.EventSummaryDto> lifeEvents = new ArrayList<>();
        if (person.getLifeEvents() != null) {
            lifeEvents = person.getLifeEvents().stream()
                    .map(this::toEventSummary)
                    .collect(Collectors.toList());
        }

        // Associations/Groups
        List<PersonDetailDto.GroupSummaryDto> associations = new ArrayList<>();
        if (person.getAssociations() != null) {
            associations = person.getAssociations().stream()
                    .map(group -> new PersonDetailDto.GroupSummaryDto(
                            group.getId(),
                            group.getName()
                    ))
                    .collect(Collectors.toList());
        }

        return new PersonDetailDto(
                person.getId(),
                person.getFirstName(),
                person.getLastName(),
                person.getMaidenName(),
                person.getPetName(),
                person.getGender(),
                person.getDateOfBirth(),
                person.getDateOfDeath(),
                lineageInfo,
                locationInfo,
                parents,
                children,
                spouses,
                friends,
                birthEvent,
                deathEvent,
                lifeEvents,
                associations
        );
    }

    // ========== EVENT MAPPING ==========

    /**
     * Convert EventDto to Event entity
     */
    public Event toEventEntity(EventDto dto, String tenantId) {
        Event event = new Event();
        event.setId(dto.id());
        event.setTenantId(tenantId);
        event.setEventType(dto.eventType());
        event.setEventDate(dto.eventDate());
        event.setDescription(dto.description());

        // Resolve location
        if (dto.locationId() != null) {
            locationRepo.findById(dto.locationId())
                    .ifPresent(event::setLocation);
        }

        // Resolve participants
        Set<Person> participants = new HashSet<>();
        if (dto.participantIds() != null) {
            for (UUID participantId : dto.participantIds()) {
                personRepo.findByTenantIdAndId(tenantId, participantId)
                        .ifPresent(participants::add);
            }
        }
        event.setParticipants(participants);

        return event;
    }

    /**
     * Convert Event entity to EventDto
     */
    public EventDto toEventDto(Event event) {
        List<UUID> participantIds = new ArrayList<>();
        if (event.getParticipants() != null) {
            participantIds = event.getParticipants().stream()
                    .map(Person::getId)
                    .collect(Collectors.toList());
        }

        return EventDto.forResponse(
                event.getId(),
                event.getEventType(),
                event.getEventDate(),
                event.getDescription(),
                event.getLocation() != null ? event.getLocation().getId() : null,
                participantIds
        );
    }

    // ========== LINEAGE MAPPING ==========

    /**
     * Convert LineageDto to Lineage entity
     */
    public Lineage toLineageEntity(LineageDto dto, String tenantId) {
        Lineage lineage = new Lineage();
        lineage.setId(dto.id());
        lineage.setTenantId(tenantId);
        lineage.setName(dto.name());
        return lineage;
    }

    /**
     * Convert Lineage entity to LineageDto
     */
    public LineageDto toLineageDto(Lineage lineage) {
        // Count members in this lineage
        int memberCount = personRepo.findAllByTenantIdAndLineageId(
                lineage.getTenantId(), 
                lineage.getId()
        ).size();

        return LineageDto.forResponse(
                lineage.getId(),
                lineage.getName(),
                null, // description if added to entity
                memberCount
        );
    }

    // ========== HELPER METHODS ==========

    /**
     * Convert Person to PersonSummaryDto
     */
    private PersonDetailDto.PersonSummaryDto toPersonSummary(Person person) {
        return new PersonDetailDto.PersonSummaryDto(
                person.getId(),
                person.getFirstName(),
                person.getLastName(),
                person.getGender(),
                person.getDateOfBirth(),
                person.getDateOfDeath()
        );
    }

    /**
     * Convert Event to EventSummaryDto
     */
    private PersonDetailDto.EventSummaryDto toEventSummary(Event event) {
        return new PersonDetailDto.EventSummaryDto(
                event.getId(),
                event.getEventType(),
                event.getEventDate(),
                event.getDescription(),
                event.getLocation() != null ? event.getLocation().getLocationName() : null
        );
    }

    /**
     * Build full location path (e.g., "Earth > India > Karnataka > Bangalore")
     */
    private String buildLocationPath(Location location) {
        List<String> path = new ArrayList<>();
        Location current = location;
        path.add(current.getLocationName());

        while (current.getParentLocation() != null) {
            current = current.getParentLocation();
            path.add(0, current.getLocationName());
        }

        return String.join(" > ", path);
    }
}