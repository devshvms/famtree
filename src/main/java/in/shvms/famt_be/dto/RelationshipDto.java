package in.shvms.famt_be.dto;

import in.shvms.famt_be.entity.ParentChildType;
import in.shvms.famt_be.entity.SpousalStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTOs for relationship creation and updates
 */
public class RelationshipDto {
    
    /**
     * DTO for Parent-Child Relationship
     */
    public record ParentChildRelationRequest(
            @NotNull(message = "Parent ID is required")
            UUID parentId,
            
            @NotNull(message = "Child ID is required")
            UUID childId,
            
            @NotNull(message = "Relationship type is required")
            ParentChildType relationshipType,
            
            LocalDate startDate,
            
            @Min(value = 0, message = "Confidence score must be between 0 and 1")
            @Max(value = 1, message = "Confidence score must be between 0 and 1")
            Double confidenceScore // Default to 1.0 if null
    ) {
        public void validate() {
            if (parentId == null) {
                throw new IllegalArgumentException("Parent ID is required");
            }
            if (childId == null) {
                throw new IllegalArgumentException("Child ID is required");
            }
            if (parentId.equals(childId)) {
                throw new IllegalArgumentException("Parent and child cannot be the same person");
            }
            if (relationshipType == null) {
                throw new IllegalArgumentException("Relationship type is required");
            }
            if (confidenceScore != null && (confidenceScore < 0 || confidenceScore > 1)) {
                throw new IllegalArgumentException("Confidence score must be between 0 and 1");
            }
        }
    }
    
    /**
     * DTO for Spousal Relationship
     */
    public record SpousalRelationRequest(
            @NotNull(message = "Person 1 ID is required")
            UUID person1Id,
            
            @NotNull(message = "Person 2 ID is required")
            UUID person2Id,
            
            @NotNull(message = "Spousal status is required")
            SpousalStatus status,
            
            LocalDate startDate,
            LocalDate endDate,
            
            String partnershipType // Marriage, Civil Union, Domestic Partnership, etc.
    ) {
        public void validate() {
            if (person1Id == null) {
                throw new IllegalArgumentException("Person 1 ID is required");
            }
            if (person2Id == null) {
                throw new IllegalArgumentException("Person 2 ID is required");
            }
            if (person1Id.equals(person2Id)) {
                throw new IllegalArgumentException("Person cannot be married to themselves");
            }
            if (status == null) {
                throw new IllegalArgumentException("Spousal status is required");
            }
            if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
                throw new IllegalArgumentException("End date cannot be before start date");
            }
        }
    }
    
    /**
     * DTO for Friend Relationship
     */
    public record FriendRelationRequest(
            @NotNull(message = "Person 1 ID is required")
            UUID person1Id,
            
            @NotNull(message = "Person 2 ID is required")
            UUID person2Id
    ) {
        public void validate() {
            if (person1Id == null) {
                throw new IllegalArgumentException("Person 1 ID is required");
            }
            if (person2Id == null) {
                throw new IllegalArgumentException("Person 2 ID is required");
            }
            if (person1Id.equals(person2Id)) {
                throw new IllegalArgumentException("Person cannot be friends with themselves");
            }
        }
    }
}