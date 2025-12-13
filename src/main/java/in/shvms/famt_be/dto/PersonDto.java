package in.shvms.famt_be.dto;

import in.shvms.famt_be.entity.Gender;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for creating and updating Person entities
 * Simplified structure without nested tenant references
 */
public record PersonDto(
        UUID id, // Only for responses, null for creation
        
        @NotNull(message = "First name is required")
        String firstName,
        
        String lastName,
        String maidenName,
        String petName,
        
        @NotNull(message = "Gender is required")
        Gender gender,
        
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,
        
        @Past(message = "Date of death must be in the past")
        LocalDate dateOfDeath,
        
        // Reference IDs instead of nested entities
        UUID lineageId,
        String currentResidenceId,
        
        // Event IDs
        UUID birthEventId,
        UUID deathEventId
) {
    /**
     * Validation method
     */
    public void validate() {
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be empty");
        }
        
        if (gender == null) {
            throw new IllegalArgumentException("Gender is required");
        }
        
        // Validate date logic
        if (dateOfBirth != null && dateOfDeath != null) {
            if (dateOfDeath.isBefore(dateOfBirth)) {
                throw new IllegalArgumentException("Date of death cannot be before date of birth");
            }
        }
        
        if (dateOfBirth != null && dateOfBirth.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth cannot be in the future");
        }
        
        if (dateOfDeath != null && dateOfDeath.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of death cannot be in the future");
        }
    }
    
    /**
     * Create a DTO for response (includes ID)
     */
    public static PersonDto forResponse(UUID id, String firstName, String lastName, 
                                       String maidenName, String petName, Gender gender,
                                       LocalDate dateOfBirth, LocalDate dateOfDeath,
                                       UUID lineageId, String currentResidenceId,
                                       UUID birthEventId, UUID deathEventId) {
        return new PersonDto(id, firstName, lastName, maidenName, petName, gender,
                           dateOfBirth, dateOfDeath, lineageId, currentResidenceId,
                           birthEventId, deathEventId);
    }
    
    /**
     * Create a DTO for request (no ID)
     */
    public static PersonDto forRequest(String firstName, String lastName, 
                                      String maidenName, String petName, Gender gender,
                                      LocalDate dateOfBirth, LocalDate dateOfDeath,
                                      UUID lineageId, String currentResidenceId,
                                      UUID birthEventId, UUID deathEventId) {
        return new PersonDto(null, firstName, lastName, maidenName, petName, gender,
                           dateOfBirth, dateOfDeath, lineageId, currentResidenceId,
                           birthEventId, deathEventId);
    }
}