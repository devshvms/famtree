package in.shvms.famt_be.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * DTO for Lineage creation and updates
 */
public record LineageDto(
        UUID id, // Only for responses
        
        @NotBlank(message = "Lineage name is required")
        String name,
        
        String description, // Optional description of the lineage
        
        // Statistics (only in response)
        Integer memberCount
) {
    
    /**
     * Validation method
     */
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Lineage name is required");
        }
    }
    
    /**
     * Create a DTO for response (with statistics)
     */
    public static LineageDto forResponse(UUID id, String name, String description, 
                                        Integer memberCount) {
        return new LineageDto(id, name, description, memberCount);
    }
    
    /**
     * Create a DTO for request (without statistics)
     */
    public static LineageDto forRequest(String name, String description) {
        return new LineageDto(null, name, description, null);
    }
}