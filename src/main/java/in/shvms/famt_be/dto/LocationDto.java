package in.shvms.famt_be.dto;

import in.shvms.famt_be.entity.LocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for Location creation and updates
 * Note: childLocationIds is not included as children are managed automatically
 */
public record LocationDto(
        @NotBlank(message = "Location name is required")
        String locationName,
        
        @NotNull(message = "Location type is required")
        LocationType locationType,
        
        // Required for creating new locations (except PLANET and COUNTRY)
        String parentLocationId
) {
    
    /**
     * Validation method
     */
    public void validate() {
        if (locationName == null || locationName.trim().isEmpty()) {
            throw new IllegalArgumentException("Location name cannot be empty");
        }
        
        if (locationType == null) {
            throw new IllegalArgumentException("Location type is required");
        }
        
        // Parent is required for all types except PLANET
        if (locationType != LocationType.PLANET && 
            locationType != LocationType.COUNTRY && 
            (parentLocationId == null || parentLocationId.trim().isEmpty())) {
            throw new IllegalArgumentException("Parent location ID is required for " + locationType);
        }
    }
}