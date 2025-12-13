package in.shvms.famt_be.dto;

import in.shvms.famt_be.entity.Gender;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Detailed Person DTO with relationships for response
 * Used when fetching complete person information
 */
public record PersonDetailDto(
        UUID id,
        String firstName,
        String lastName,
        String maidenName,
        String petName,
        Gender gender,
        LocalDate dateOfBirth,
        LocalDate dateOfDeath,
        
        // Lineage information
        LineageInfo lineage,
        
        // Location information
        LocationInfo currentResidence,
        
        // Relationships (summary)
        List<ParentChildRelationDto> parents,
        List<ParentChildRelationDto> children,
        List<SpousalRelationDto> spouses,
        List<PersonSummaryDto> friends,
        
        // Events
        EventSummaryDto birthEvent,
        EventSummaryDto deathEvent,
        List<EventSummaryDto> lifeEvents,
        
        // Groups/Associations
        List<GroupSummaryDto> associations
) {
    
    /**
     * Nested record for lineage information
     */
    public record LineageInfo(
            UUID id,
            String name
    ) {}
    
    /**
     * Nested record for location information
     */
    public record LocationInfo(
            String id,
            String locationName,
            String locationType,
            String fullPath // e.g., "Earth > India > Karnataka > Bangalore"
    ) {}
    
    /**
     * Nested record for person summary (used in relationships)
     */
    public record PersonSummaryDto(
            UUID id,
            String firstName,
            String lastName,
            Gender gender,
            LocalDate dateOfBirth,
            LocalDate dateOfDeath
    ) {}
    
    /**
     * Nested record for parent-child relationship
     */
    public record ParentChildRelationDto(
            PersonSummaryDto person,
            String relationshipType, // BIOLOGICAL, ADOPTIVE, etc.
            LocalDate startDate,
            Double confidenceScore
    ) {}
    
    /**
     * Nested record for spousal relationship
     */
    public record SpousalRelationDto(
            PersonSummaryDto spouse,
            String status, // MARRIED, DIVORCED, etc.
            LocalDate startDate,
            LocalDate endDate,
            String partnershipType
    ) {}
    
    /**
     * Nested record for event summary
     */
    public record EventSummaryDto(
            UUID id,
            String eventType,
            LocalDate eventDate,
            String description,
            String locationName
    ) {}
    
    /**
     * Nested record for group summary
     */
    public record GroupSummaryDto(
            UUID id,
            String name
    ) {}
}