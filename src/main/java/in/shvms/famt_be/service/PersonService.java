package in.shvms.famt_be.service;

import in.shvms.famt_be.entity.*;
import in.shvms.famt_be.repositories.mongo.AuditLogRepository;
import in.shvms.famt_be.repositories.neo4j.PersonRepository;
import in.shvms.famt_be.repositories.neo4j.LineageRepository;
import in.shvms.famt_be.repositories.neo4j.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced PersonService with comprehensive relationship management and audit logging
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PersonService {

    private final PersonRepository personRepo;
    private final LineageRepository lineageRepo;
    private final LocationRepository locationRepo;
    private final AuditLogRepository auditLogRepo;

    // ========== AUDIT LOGGING ==========

    private void logAudit(String tenantId, String userId, String action, String entityType, 
                         String entityId, Map<String, Object> details) {
        try {
            AuditLog auditLog = new AuditLog(
                    null,
                    tenantId,
                    LocalDateTime.now(),
                    userId,
                    action,
                    entityType,
                    entityId,
                    details,
                    null
            );
            auditLogRepo.save(auditLog);
            log.debug("Audit logged: {} {} for entity {}", action, entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to log audit for {} {} {}", action, entityType, entityId, e);
        }
    }

    // ========== PERSON CRUD OPERATIONS ==========

    @Transactional
    public Person createPerson(String tenantId, String userId, Person person) {
        log.info("Creating person in tenant: {}", tenantId);
        
        // Set tenant ID
        person.setTenantId(tenantId);
        
        // Initialize collections to prevent null pointer exceptions
        if (person.getSpouseRelations() == null) {
            person.setSpouseRelations(new HashSet<>());
        }
        if (person.getChildrenRelations() == null) {
            person.setChildrenRelations(new HashSet<>());
        }
        if (person.getFriends() == null) {
            person.setFriends(new HashSet<>());
        }
        if (person.getLifeEvents() == null) {
            person.setLifeEvents(new HashSet<>());
        }
        if (person.getAssociations() == null) {
            person.setAssociations(new HashSet<>());
        }
        
        // Validate and resolve lineage if provided
        if (person.getLineage() != null && person.getLineage().getId() != null) {
            Lineage lineage = lineageRepo.findByTenantIdAndId(tenantId, person.getLineage().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Lineage not found: " + person.getLineage().getId()));
            person.setLineage(lineage);
        }
        
        // Validate and resolve current residence if provided
        if (person.getCurrentResidence() != null && person.getCurrentResidence().getId() != null) {
            Location location = locationRepo.findById(person.getCurrentResidence().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Location not found: " + person.getCurrentResidence().getId()));
            person.setCurrentResidence(location);
        }
        
        // Validate dates
        validatePersonDates(person);
        
        Person savedPerson = personRepo.save(person);
        
        logAudit(tenantId, userId, "CREATE", "Person", savedPerson.getId().toString(), 
                Map.of(
                    "firstName", Optional.ofNullable(savedPerson.getFirstName()).orElse(""),
                    "lastName", Optional.ofNullable(savedPerson.getLastName()).orElse(""),
                    "gender", savedPerson.getGender().name(),
                    "lineageId", Optional.ofNullable(savedPerson.getLineage())
                            .map(l -> l.getId().toString()).orElse("none")
                ));
        
        log.info("Person created successfully: {}", savedPerson.getId());
        return savedPerson;
    }

    public Optional<Person> getPersonById(String tenantId, UUID id) {
        return personRepo.findByTenantIdAndId(tenantId, id);
    }

    public List<Person> getAllPeople(String tenantId) {
        return personRepo.findAllByTenantId(tenantId);
    }

    @Transactional
    public Person updatePerson(String tenantId, String userId, UUID id, Person updatedPerson) {
        log.info("Updating person: {} in tenant: {}", id, tenantId);
        
        return personRepo.findByTenantIdAndId(tenantId, id).map(person -> {
            // Capture old values for audit
            Map<String, Object> oldValues = capturePersonSnapshot(person);
            
            // Update basic fields
            person.setFirstName(updatedPerson.getFirstName());
            person.setLastName(updatedPerson.getLastName());
            person.setMaidenName(updatedPerson.getMaidenName());
            person.setPetName(updatedPerson.getPetName());
            person.setGender(updatedPerson.getGender());
            person.setDateOfBirth(updatedPerson.getDateOfBirth());
            person.setDateOfDeath(updatedPerson.getDateOfDeath());
            
            // Update lineage if changed
            if (updatedPerson.getLineage() != null && updatedPerson.getLineage().getId() != null) {
                Lineage lineage = lineageRepo.findByTenantIdAndId(tenantId, updatedPerson.getLineage().getId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Lineage not found: " + updatedPerson.getLineage().getId()));
                person.setLineage(lineage);
            }
            
            // Update current residence if changed
            if (updatedPerson.getCurrentResidence() != null && 
                updatedPerson.getCurrentResidence().getId() != null) {
                Location location = locationRepo.findById(updatedPerson.getCurrentResidence().getId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Location not found: " + updatedPerson.getCurrentResidence().getId()));
                person.setCurrentResidence(location);
            }
            
            // Validate dates
            validatePersonDates(person);
            
            Person savedPerson = personRepo.save(person);
            
            // Capture new values for audit
            Map<String, Object> newValues = capturePersonSnapshot(savedPerson);
            
            logAudit(tenantId, userId, "UPDATE", "Person", savedPerson.getId().toString(), 
                    Map.of("oldValues", oldValues, "newValues", newValues));
            
            log.info("Person updated successfully: {}", savedPerson.getId());
            return savedPerson;
        }).orElseThrow(() -> new RuntimeException("Person not found: " + id));
    }

    @Transactional
    public void deletePerson(String tenantId, String userId, UUID id) {
        log.info("Deleting person: {} in tenant: {}", id, tenantId);
        
        Person personToDelete = personRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new RuntimeException("Person not found: " + id));
        
        // Capture details before deletion for audit
        Map<String, Object> deletionDetails = Map.of(
                "personId", personToDelete.getId().toString(),
                "firstName", Optional.ofNullable(personToDelete.getFirstName()).orElse(""),
                "lastName", Optional.ofNullable(personToDelete.getLastName()).orElse(""),
                "hadSpouses", personToDelete.getSpouseRelations() != null ? 
                        personToDelete.getSpouseRelations().size() : 0,
                "hadChildren", personToDelete.getChildrenRelations() != null ? 
                        personToDelete.getChildrenRelations().size() : 0
        );
        
        // Remove this person from all relationships
        removePersonFromAllRelationships(tenantId, personToDelete);
        
        // Delete the person
        personRepo.delete(personToDelete);
        
        logAudit(tenantId, userId, "DELETE", "Person", id.toString(), deletionDetails);
        log.info("Person deleted successfully: {}", id);
    }

    // ========== PARENT-CHILD RELATIONSHIP MANAGEMENT ==========

    @Transactional
    public Person addParentChildRelation(String tenantId, String userId, UUID parentId, 
                                        UUID childId, ParentChildType type, 
                                        LocalDate startDate, Double confidenceScore) {
        log.info("Adding parent-child relation: parent={}, child={}, type={}", parentId, childId, type);
        
        Person parent = personRepo.findByTenantIdAndId(tenantId, parentId)
                .orElseThrow(() -> new RuntimeException("Parent not found: " + parentId));
        Person child = personRepo.findByTenantIdAndId(tenantId, childId)
                .orElseThrow(() -> new RuntimeException("Child not found: " + childId));
        
        // Validate relationship
        validateParentChildRelation(parent, child);
        
        // Check for duplicate relation
        if (child.getChildrenRelations() != null) {
            boolean alreadyExists = child.getChildrenRelations().stream()
                    .anyMatch(r -> r.getParent().getId().equals(parentId));
            if (alreadyExists) {
                throw new IllegalStateException(
                        "Parent-child relationship already exists between " + parentId + " and " + childId);
            }
        } else {
            child.setChildrenRelations(new HashSet<>());
        }
        
        // Create the relation
        ParentChildRelation relation = new ParentChildRelation(
                null, 
                parent, 
                type, 
                confidenceScore != null ? confidenceScore : 1.0, 
                startDate
        );
        child.getChildrenRelations().add(relation);
        
        Person savedChild = personRepo.save(child);
        
        logAudit(tenantId, userId, "ADD_PARENT_CHILD_RELATION", "Person", childId.toString(), 
                Map.of(
                    "parentId", parentId.toString(),
                    "childId", childId.toString(),
                    "relationshipType", type.name(),
                    "startDate", Optional.ofNullable(startDate).map(LocalDate::toString).orElse("N/A"),
                    "confidenceScore", confidenceScore != null ? confidenceScore : 1.0
                ));
        
        log.info("Parent-child relation added successfully");
        return savedChild;
    }

    @Transactional
    public Person removeParentChildRelation(String tenantId, String userId, UUID parentId, UUID childId) {
        log.info("Removing parent-child relation: parent={}, child={}", parentId, childId);
        
        Person child = personRepo.findByTenantIdAndId(tenantId, childId)
                .orElseThrow(() -> new RuntimeException("Child not found: " + childId));
        
        if (child.getChildrenRelations() != null) {
            boolean removed = child.getChildrenRelations().removeIf(
                    r -> r.getParent().getId().equals(parentId)
            );
            
            if (removed) {
                Person savedChild = personRepo.save(child);
                
                logAudit(tenantId, userId, "REMOVE_PARENT_CHILD_RELATION", "Person", childId.toString(), 
                        Map.of("parentId", parentId.toString(), "childId", childId.toString()));
                
                log.info("Parent-child relation removed successfully");
                return savedChild;
            }
        }
        
        throw new RuntimeException("Parent-child relationship not found");
    }

    @Transactional
    public Person updateParentChildRelation(String tenantId, String userId, UUID parentId, 
                                           UUID childId, ParentChildType newType, 
                                           LocalDate newStartDate, Double newConfidenceScore) {
        log.info("Updating parent-child relation: parent={}, child={}", parentId, childId);
        
        Person child = personRepo.findByTenantIdAndId(tenantId, childId)
                .orElseThrow(() -> new RuntimeException("Child not found: " + childId));
        
        if (child.getChildrenRelations() == null) {
            throw new RuntimeException("No parent-child relationships found for child: " + childId);
        }
        
        ParentChildRelation relation = child.getChildrenRelations().stream()
                .filter(r -> r.getParent().getId().equals(parentId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Parent-child relationship not found"));
        
        // Capture old values
        Map<String, Object> oldValues = Map.of(
                "type", relation.getRelationshipType().name(),
                "startDate", Optional.ofNullable(relation.getStartDate())
                        .map(LocalDate::toString).orElse("N/A"),
                "confidenceScore", relation.getConfidenceScore()
        );
        
        // Update relation
        relation.setRelationshipType(newType);
        relation.setStartDate(newStartDate);
        relation.setConfidenceScore(newConfidenceScore);
        
        Person savedChild = personRepo.save(child);
        
        Map<String, Object> newValues = Map.of(
                "type", newType.name(),
                "startDate", Optional.ofNullable(newStartDate).map(LocalDate::toString).orElse("N/A"),
                "confidenceScore", newConfidenceScore
        );
        
        logAudit(tenantId, userId, "UPDATE_PARENT_CHILD_RELATION", "Person", childId.toString(), 
                Map.of("parentId", parentId.toString(), "oldValues", oldValues, "newValues", newValues));
        
        log.info("Parent-child relation updated successfully");
        return savedChild;
    }

    // ========== SPOUSAL RELATIONSHIP MANAGEMENT ==========

    @Transactional
    public Person addSpousalRelation(String tenantId, String userId, UUID person1Id, 
                                    UUID person2Id, SpousalStatus status, 
                                    LocalDate startDate, LocalDate endDate, 
                                    String partnershipType) {
        log.info("Adding spousal relation: person1={}, person2={}, status={}", 
                person1Id, person2Id, status);
        
        Person person1 = personRepo.findByTenantIdAndId(tenantId, person1Id)
                .orElseThrow(() -> new RuntimeException("Person 1 not found: " + person1Id));
        Person person2 = personRepo.findByTenantIdAndId(tenantId, person2Id)
                .orElseThrow(() -> new RuntimeException("Person 2 not found: " + person2Id));
        
        // Validate relationship
        validateSpousalRelation(person1, person2, startDate, endDate);
        
        // Check for duplicate relation
        if (person1.getSpouseRelations() != null) {
            boolean alreadyExists = person1.getSpouseRelations().stream()
                    .anyMatch(r -> r.getSpouse().getId().equals(person2Id));
            if (alreadyExists) {
                throw new IllegalStateException(
                        "Spousal relationship already exists between " + person1Id + " and " + person2Id);
            }
        } else {
            person1.setSpouseRelations(new HashSet<>());
        }
        
        if (person2.getSpouseRelations() == null) {
            person2.setSpouseRelations(new HashSet<>());
        }
        
        // Create bidirectional relationship
        SpousalRelation relation1 = new SpousalRelation(
                null, person2, startDate, endDate, status, partnershipType
        );
        SpousalRelation relation2 = new SpousalRelation(
                null, person1, startDate, endDate, status, partnershipType
        );
        
        person1.getSpouseRelations().add(relation1);
        person2.getSpouseRelations().add(relation2);
        
        personRepo.save(person1);
        Person savedPerson2 = personRepo.save(person2);
        
        logAudit(tenantId, userId, "ADD_SPOUSAL_RELATION", "Person", person1Id.toString(), 
                Map.of(
                    "person1Id", person1Id.toString(),
                    "person2Id", person2Id.toString(),
                    "status", status.name(),
                    "startDate", Optional.ofNullable(startDate).map(LocalDate::toString).orElse("N/A"),
                    "endDate", Optional.ofNullable(endDate).map(LocalDate::toString).orElse("N/A"),
                    "partnershipType", Optional.ofNullable(partnershipType).orElse("N/A")
                ));
        
        log.info("Spousal relation added successfully");
        return savedPerson2;
    }

    @Transactional
    public Person removeSpousalRelation(String tenantId, String userId, UUID person1Id, UUID person2Id) {
        log.info("Removing spousal relation: person1={}, person2={}", person1Id, person2Id);
        
        Person person1 = personRepo.findByTenantIdAndId(tenantId, person1Id)
                .orElseThrow(() -> new RuntimeException("Person 1 not found: " + person1Id));
        Person person2 = personRepo.findByTenantIdAndId(tenantId, person2Id)
                .orElseThrow(() -> new RuntimeException("Person 2 not found: " + person2Id));
        
        boolean removed1 = false, removed2 = false;
        
        if (person1.getSpouseRelations() != null) {
            removed1 = person1.getSpouseRelations().removeIf(r -> r.getSpouse().getId().equals(person2Id));
        }
        
        if (person2.getSpouseRelations() != null) {
            removed2 = person2.getSpouseRelations().removeIf(r -> r.getSpouse().getId().equals(person1Id));
        }
        
        if (removed1 || removed2) {
            personRepo.save(person1);
            Person savedPerson2 = personRepo.save(person2);
            
            logAudit(tenantId, userId, "REMOVE_SPOUSAL_RELATION", "Person", person1Id.toString(), 
                    Map.of("person1Id", person1Id.toString(), "person2Id", person2Id.toString()));
            
            log.info("Spousal relation removed successfully");
            return savedPerson2;
        }
        
        throw new RuntimeException("Spousal relationship not found");
    }

    @Transactional
    public Person updateSpousalRelation(String tenantId, String userId, UUID person1Id, 
                                       UUID person2Id, SpousalStatus newStatus, 
                                       LocalDate newStartDate, LocalDate newEndDate, 
                                       String newPartnershipType) {
        log.info("Updating spousal relation: person1={}, person2={}", person1Id, person2Id);
        
        Person person1 = personRepo.findByTenantIdAndId(tenantId, person1Id)
                .orElseThrow(() -> new RuntimeException("Person 1 not found: " + person1Id));
        Person person2 = personRepo.findByTenantIdAndId(tenantId, person2Id)
                .orElseThrow(() -> new RuntimeException("Person 2 not found: " + person2Id));
        
        // Find and update relations
        SpousalRelation relation1 = person1.getSpouseRelations().stream()
                .filter(r -> r.getSpouse().getId().equals(person2Id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Spousal relationship not found"));
        
        SpousalRelation relation2 = person2.getSpouseRelations().stream()
                .filter(r -> r.getSpouse().getId().equals(person1Id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Spousal relationship not found"));
        
        // Capture old values
        Map<String, Object> oldValues = Map.of(
                "status", relation1.getStatus().name(),
                "startDate", Optional.ofNullable(relation1.getStartDate())
                        .map(LocalDate::toString).orElse("N/A"),
                "endDate", Optional.ofNullable(relation1.getEndDate())
                        .map(LocalDate::toString).orElse("N/A"),
                "partnershipType", Optional.ofNullable(relation1.getPartnershipType()).orElse("N/A")
        );
        
        // Validate dates
        validateSpousalRelation(person1, person2, newStartDate, newEndDate);
        
        // Update both relations
        relation1.setStatus(newStatus);
        relation1.setStartDate(newStartDate);
        relation1.setEndDate(newEndDate);
        relation1.setPartnershipType(newPartnershipType);
        
        relation2.setStatus(newStatus);
        relation2.setStartDate(newStartDate);
        relation2.setEndDate(newEndDate);
        relation2.setPartnershipType(newPartnershipType);
        
        personRepo.save(person1);
        Person savedPerson2 = personRepo.save(person2);
        
        Map<String, Object> newValues = Map.of(
                "status", newStatus.name(),
                "startDate", Optional.ofNullable(newStartDate).map(LocalDate::toString).orElse("N/A"),
                "endDate", Optional.ofNullable(newEndDate).map(LocalDate::toString).orElse("N/A"),
                "partnershipType", Optional.ofNullable(newPartnershipType).orElse("N/A")
        );
        
        logAudit(tenantId, userId, "UPDATE_SPOUSAL_RELATION", "Person", person1Id.toString(), 
                Map.of("person1Id", person1Id.toString(), "person2Id", person2Id.toString(),
                       "oldValues", oldValues, "newValues", newValues));
        
        log.info("Spousal relation updated successfully");
        return savedPerson2;
    }

    // ========== FRIEND RELATIONSHIP MANAGEMENT ==========

    @Transactional
    public Person addFriendRelation(String tenantId, String userId, UUID person1Id, UUID person2Id) {
        log.info("Adding friend relation: person1={}, person2={}", person1Id, person2Id);
        
        Person person1 = personRepo.findByTenantIdAndId(tenantId, person1Id)
                .orElseThrow(() -> new RuntimeException("Person 1 not found: " + person1Id));
        Person person2 = personRepo.findByTenantIdAndId(tenantId, person2Id)
                .orElseThrow(() -> new RuntimeException("Person 2 not found: " + person2Id));
        
        if (person1Id.equals(person2Id)) {
            throw new IllegalArgumentException("Person cannot be friends with themselves");
        }
        
        if (person1.getFriends() == null) {
            person1.setFriends(new HashSet<>());
        }
        if (person2.getFriends() == null) {
            person2.setFriends(new HashSet<>());
        }
        
        // Check for duplicate
        if (person1.getFriends().stream().anyMatch(f -> f.getId().equals(person2Id))) {
            throw new IllegalStateException("Friend relationship already exists");
        }
        
        person1.getFriends().add(person2);
        person2.getFriends().add(person1);
        
        personRepo.save(person1);
        Person savedPerson2 = personRepo.save(person2);
        
        logAudit(tenantId, userId, "ADD_FRIEND_RELATION", "Person", person1Id.toString(), 
                Map.of("person1Id", person1Id.toString(), "person2Id", person2Id.toString()));
        
        log.info("Friend relation added successfully");
        return savedPerson2;
    }

    @Transactional
    public Person removeFriendRelation(String tenantId, String userId, UUID person1Id, UUID person2Id) {
        log.info("Removing friend relation: person1={}, person2={}", person1Id, person2Id);
        
        Person person1 = personRepo.findByTenantIdAndId(tenantId, person1Id)
                .orElseThrow(() -> new RuntimeException("Person 1 not found: " + person1Id));
        Person person2 = personRepo.findByTenantIdAndId(tenantId, person2Id)
                .orElseThrow(() -> new RuntimeException("Person 2 not found: " + person2Id));
        
        boolean removed1 = false, removed2 = false;
        
        if (person1.getFriends() != null) {
            removed1 = person1.getFriends().removeIf(f -> f.getId().equals(person2Id));
        }
        
        if (person2.getFriends() != null) {
            removed2 = person2.getFriends().removeIf(f -> f.getId().equals(person1Id));
        }
        
        if (removed1 || removed2) {
            personRepo.save(person1);
            Person savedPerson2 = personRepo.save(person2);
            
            logAudit(tenantId, userId, "REMOVE_FRIEND_RELATION", "Person", person1Id.toString(), 
                    Map.of("person1Id", person1Id.toString(), "person2Id", person2Id.toString()));
            
            log.info("Friend relation removed successfully");
            return savedPerson2;
        }
        
        throw new RuntimeException("Friend relationship not found");
    }

    // ========== SEARCH AND FILTER ==========

    public List<Person> findPeopleByName(String tenantId, String name) {
        return personRepo.findByTenantIdAndFirstNameContaining(tenantId, name);
    }

    public List<Person> findPeopleByLineage(String tenantId, UUID lineageId) {
        return personRepo.findAllByTenantIdAndLineageId(tenantId, lineageId);
    }

    // ========== RELATIONSHIP QUERIES ==========

    public List<Person> getChildren(String tenantId, UUID personId) {
        Person person = personRepo.findByTenantIdAndId(tenantId, personId)
                .orElseThrow(() -> new RuntimeException("Person not found: " + personId));
        
        // Find all people where this person is in their childrenRelations
        return personRepo.findAllByTenantId(tenantId).stream()
                .filter(p -> p.getChildrenRelations() != null && 
                           p.getChildrenRelations().stream()
                                   .anyMatch(r -> r.getParent().getId().equals(personId)))
                .collect(Collectors.toList());
    }

    public List<Person> getParents(String tenantId, UUID personId) {
        Person person = personRepo.findByTenantIdAndId(tenantId, personId)
                .orElseThrow(() -> new RuntimeException("Person not found: " + personId));
        
        if (person.getChildrenRelations() == null) {
            return Collections.emptyList();
        }
        
        return person.getChildrenRelations().stream()
                .map(ParentChildRelation::getParent)
                .collect(Collectors.toList());
    }

    public List<Person> getSpouses(String tenantId, UUID personId) {
        Person person = personRepo.findByTenantIdAndId(tenantId, personId)
                .orElseThrow(() -> new RuntimeException("Person not found: " + personId));
        
        if (person.getSpouseRelations() == null) {
            return Collections.emptyList();
        }
        
        return person.getSpouseRelations().stream()
                .map(SpousalRelation::getSpouse)
                .collect(Collectors.toList());
    }

    public List<Person> getSiblings(String tenantId, UUID personId) {
        List<Person> parents = getParents(tenantId, personId);
        
        if (parents.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Get all children of all parents, excluding the person themselves
        return parents.stream()
                .flatMap(parent -> getChildren(tenantId, parent.getId()).stream())
                .filter(sibling -> !sibling.getId().equals(personId))
                .distinct()
                .collect(Collectors.toList());
    }

    // ========== VALIDATION HELPERS ==========

    private void validatePersonDates(Person person) {
        if (person.getDateOfBirth() != null && person.getDateOfDeath() != null) {
            if (person.getDateOfDeath().isBefore(person.getDateOfBirth())) {
                throw new IllegalArgumentException("Date of death cannot be before date of birth");
            }
        }
        
        if (person.getDateOfBirth() != null && person.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth cannot be in the future");
        }
    }

    private void validateParentChildRelation(Person parent, Person child) {
        if (parent.getId().equals(child.getId())) {
            throw new IllegalArgumentException("Person cannot be their own parent");
        }
        
        // Check if dates are logical (parent should be born before child)
        if (parent.getDateOfBirth() != null && child.getDateOfBirth() != null) {
            if (!parent.getDateOfBirth().isBefore(child.getDateOfBirth())) {
                throw new IllegalArgumentException(
                        "Parent must be born before child");
            }
        }
    }

    private void validateSpousalRelation(Person person1, Person person2, 
                                        LocalDate startDate, LocalDate endDate) {
        if (person1.getId().equals(person2.getId())) {
            throw new IllegalArgumentException("Person cannot be married to themselves");
        }
        
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
    }

    // ========== HELPER METHODS ==========

    private Map<String, Object> capturePersonSnapshot(Person person) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("firstName", Optional.ofNullable(person.getFirstName()).orElse(""));
        snapshot.put("lastName", Optional.ofNullable(person.getLastName()).orElse(""));
        snapshot.put("maidenName", Optional.ofNullable(person.getMaidenName()).orElse(""));
        snapshot.put("petName", Optional.ofNullable(person.getPetName()).orElse(""));
        snapshot.put("gender", person.getGender().name());
        snapshot.put("dateOfBirth", Optional.ofNullable(person.getDateOfBirth())
                .map(LocalDate::toString).orElse("N/A"));
        snapshot.put("dateOfDeath", Optional.ofNullable(person.getDateOfDeath())
                .map(LocalDate::toString).orElse("N/A"));
        snapshot.put("lineageId", Optional.ofNullable(person.getLineage())
                .map(l -> l.getId().toString()).orElse("none"));
        snapshot.put("currentResidenceId", Optional.ofNullable(person.getCurrentResidence())
                .map(Location::getId).orElse("none"));
        return snapshot;
    }

    private void removePersonFromAllRelationships(String tenantId, Person person) {
        // Remove from all spousal relationships
        if (person.getSpouseRelations() != null) {
            for (SpousalRelation relation : new HashSet<>(person.getSpouseRelations())) {
                Person spouse = relation.getSpouse();
                if (spouse.getSpouseRelations() != null) {
                    spouse.getSpouseRelations().removeIf(r -> r.getSpouse().getId().equals(person.getId()));
                    personRepo.save(spouse);
                }
            }
        }
        
        // Remove from all parent-child relationships
        if (person.getChildrenRelations() != null) {
            // This person is a child, so parents need to be notified
            // But in Neo4j, deleting the relationship automatically handles this
        }
        
        // Remove as parent from children
        List<Person> children = getChildren(tenantId, person.getId());
        for (Person child : children) {
            if (child.getChildrenRelations() != null) {
                child.getChildrenRelations().removeIf(r -> r.getParent().getId().equals(person.getId()));
                personRepo.save(child);
            }
        }
        
        // Remove from friend relationships
        if (person.getFriends() != null) {
            for (Person friend : new HashSet<>(person.getFriends())) {
                if (friend.getFriends() != null) {
                    friend.getFriends().removeIf(f -> f.getId().equals(person.getId()));
                    personRepo.save(friend);
                }
            }
        }
        
        log.info("Removed person {} from all relationships", person.getId());
    }
}