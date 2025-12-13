package in.shvms.famt_be.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for Event creation and updates
 */
public record EventDto(
        UUID id, // Only for responses
        
        @NotBlank(message = "Event type is required")
        String eventType, // Birth, Death, Marriage, Graduation, etc.
        
        @Past(message = "Event date should be in the past")
        LocalDate eventDate,
        
        String description,
        
        // Reference IDs instead of nested entities
        String locationId,
        List<UUID> participantIds
) {
    
    /**
     * Validation method
     */
    public void validate() {
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new IllegalArgumentException("Event type is required");
        }
        
        if (eventDate != null && eventDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Event date cannot be in the future");
        }
    }
    
    /**
     * Create a DTO for response
     */
    public static EventDto forResponse(UUID id, String eventType, LocalDate eventDate,
                                      String description, String locationId, 
                                      List<UUID> participantIds) {
        return new EventDto(id, eventType, eventDate, description, locationId, participantIds);
    }
    
    /**
     * Create a DTO for request
     */
    public static EventDto forRequest(String eventType, LocalDate eventDate,
                                     String description, String locationId, 
                                     List<UUID> participantIds) {
        return new EventDto(null, eventType, eventDate, description, locationId, participantIds);
    }
}

/**
 * Detailed Event DTO with full information
 */
record EventDetailDto(
        UUID id,
        String eventType,
        LocalDate eventDate,
        String description,
        
        LocationInfo location,
        List<PersonSummaryDto> participants
) {
    
    record LocationInfo(
            String id,
            String locationName,
            String locationType
    ) {}
    
    record PersonSummaryDto(
            UUID id,
            String firstName,
            String lastName
    ) {}
}