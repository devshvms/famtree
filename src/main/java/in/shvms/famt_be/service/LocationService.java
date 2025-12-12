package in.shvms.famt_be.service;

import in.shvms.famt_be.dto.LocationDto;
import in.shvms.famt_be.entity.AuditLog;
import in.shvms.famt_be.entity.Location;
import in.shvms.famt_be.entity.LocationType;
import in.shvms.famt_be.repositories.mongo.AuditLogRepository;
import in.shvms.famt_be.repositories.neo4j.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final LocationRepository locationRepo;
    private final AuditLogRepository auditLogRepo;

    // Helper method for audit logging
    private void logAudit(String tenantId, String userId, String action, String entityType, String entityId, Map<String, Object> details) {
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
    }

    /**
     * Create a new location
     */
    public Location createLocation(String tenantId, String userId, LocationDto locationDto) {
        log.info("Creating location: {}", locationDto);
        
        // Validate location type
        if (locationDto.locationType() == LocationType.COUNTRY || locationDto.locationType() == LocationType.PLANET) {
            throw new IllegalArgumentException("Cannot create Planet or Country locations. They are pre-populated.");
        }
        
        // Validate parent location is provided
        if (locationDto.parentLocationId() == null || locationDto.parentLocationId().trim().isEmpty()) {
            throw new IllegalArgumentException("Parent location ID is required.");
        }
        
        // Validate parent location exists and is valid
        Location parentLocation = getValidatedParentLocation(locationDto.parentLocationId(), locationDto.locationType());
        
        // Check for duplicate location name under same parent
        Optional<Location> existingLocation = locationRepo.findByLocationNameAndParentLocationId(
                locationDto.locationName(), 
                locationDto.parentLocationId()
        );
        if (existingLocation.isPresent()) {
            throw new IllegalArgumentException(String.format(
                    "Location '%s' already exists under parent '%s'", 
                    locationDto.locationName(), 
                    parentLocation.getLocationName()
            ));
        }
        
        // Create new location
        Location location = new Location(locationDto.locationName(), locationDto.locationType());
        location.setParentLocation(parentLocation);
        location.setId(generateLocationId(location));
        location.setChildLocationIds(new HashSet<>());
        
        // Save location
        Location savedLocation = locationRepo.save(location);
        
        // Update parent to include this child
        addChildToParentLocation(savedLocation);
        
        logAudit(tenantId, userId, "CREATE", "Location", savedLocation.getId(), 
                Map.of("location", savedLocation, "parentId", parentLocation.getId()));
        
        log.info("Location created successfully: {}", savedLocation.getId());
        return savedLocation;
    }

    /**
     * Get location by ID
     */
    public Optional<Location> getLocationById(String id) {
        return locationRepo.findById(id);
    }

    /**
     * Get all locations of a specific type
     */
    public List<Location> getLocationsByType(LocationType locationType) {
        return locationRepo.findAllByLocationType(locationType);
    }

    /**
     * Get all countries (convenience method)
     */
    public List<Location> getAllCountries() {
        return locationRepo.findAllByLocationType(LocationType.COUNTRY);
    }

    /**
     * Get Earth location
     */
    public Location getEarthLocation() {
        return locationRepo.findByLocationNameAndLocationType("Earth", LocationType.PLANET)
                .orElseThrow(() -> new RuntimeException("Earth location not found. Please initialize the database."));
    }

    /**
     * Get direct children of a location
     */
    public List<Location> getChildrenOfLocation(String locationId) {
        return locationRepo.findDirectChildren(locationId);
    }

    /**
     * Get all ancestors of a location (parent, grandparent, etc.)
     */
    public List<Location> getAncestorsOfLocation(String locationId) {
        return locationRepo.findAllAncestors(locationId);
    }

    /**
     * Get all descendants of a location (children, grandchildren, etc.)
     */
    public List<Location> getDescendantsOfLocation(String locationId) {
        return locationRepo.findAllDescendants(locationId);
    }

    /**
     * Search locations by name pattern and type
     */
    public List<Location> searchLocations(LocationType locationType, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getLocationsByType(locationType);
        }
        return locationRepo.searchByTypeAndName(locationType, searchTerm.toLowerCase());
    }

    /**
     * Update location (name only, type and parent cannot be changed)
     */
    public Location updateLocation(String tenantId, String userId, String id, LocationDto locationDto) {
        log.info("Updating location: {} with data: {}", id, locationDto);
        
        Location location = locationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found: " + id));
        
        // Prevent updating Planet or Country
        if (location.getLocationType() == LocationType.COUNTRY || location.getLocationType() == LocationType.PLANET) {
            throw new IllegalArgumentException("Cannot update Planet or Country locations.");
        }
        
        Map<String, Object> oldValues = Map.of(
                "oldLocationName", location.getLocationName(),
                "oldId", location.getId()
        );
        
        // Only allow name updates
        if (!location.getLocationName().equals(locationDto.locationName())) {
            // Check for duplicate name under same parent
            Optional<Location> existingLocation = locationRepo.findByLocationNameAndParentLocationId(
                    locationDto.locationName(), 
                    location.getParentLocation().getId()
            );
            if (existingLocation.isPresent() && !existingLocation.get().getId().equals(id)) {
                throw new IllegalArgumentException(String.format(
                        "Location '%s' already exists under parent '%s'", 
                        locationDto.locationName(), 
                        location.getParentLocation().getLocationName()
                ));
            }
            
            location.setLocationName(locationDto.locationName());
            
            // Regenerate ID based on new name
            String oldId = location.getId();
            String newId = generateLocationId(location);
            
            if (!oldId.equals(newId)) {
                // Need to handle ID change carefully
                // Remove from old parent's children list
                removeChildFromParentLocation(location, oldId);
                
                // Update ID
                location.setId(newId);
                
                // Delete old node
                locationRepo.deleteById(oldId);
                
                // Save with new ID
                Location savedLocation = locationRepo.save(location);
                
                // Add to parent with new ID
                addChildToParentLocation(savedLocation);
                
                logAudit(tenantId, userId, "UPDATE", "Location", newId, 
                        Map.of("oldValues", oldValues, "newValues", Map.of("locationName", savedLocation.getLocationName(), "newId", newId)));
                
                log.info("Location updated with ID change: {} -> {}", oldId, newId);
                return savedLocation;
            }
        }
        
        Location savedLocation = locationRepo.save(location);
        logAudit(tenantId, userId, "UPDATE", "Location", savedLocation.getId(), 
                Map.of("oldValues", oldValues, "newValues", Map.of("locationName", savedLocation.getLocationName())));
        
        log.info("Location updated successfully: {}", savedLocation.getId());
        return savedLocation;
    }

    /**
     * Delete location
     */
    public void deleteLocation(String tenantId, String userId, String id) {
        log.info("Deleting location: {}", id);
        
        Location location = locationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found: " + id));
        
        // Prevent deleting Planet or Country
        if (location.getLocationType() == LocationType.COUNTRY || location.getLocationType() == LocationType.PLANET) {
            throw new IllegalArgumentException("Cannot delete Planet or Country locations.");
        }
        
        // Check if location has children
        if (location.getChildLocationIds() != null && !location.getChildLocationIds().isEmpty()) {
            throw new IllegalStateException(String.format(
                    "Cannot delete location '%s' as it has %d child locations. Delete children first.", 
                    location.getLocationName(), 
                    location.getChildLocationIds().size()
            ));
        }
        
        // Remove from parent's children list
        removeChildFromParentLocation(location, id);
        
        // Delete the location
        locationRepo.deleteById(id);
        
        logAudit(tenantId, userId, "DELETE", "Location", id, Map.of("deletedLocation", location));
        log.info("Location deleted successfully: {}", id);
    }

    /**
     * Get full hierarchy path for a location (e.g., "Earth > India > Karnataka > Bangalore > Koramangala")
     */
    public String getLocationHierarchyPath(String locationId) {
        Location location = locationRepo.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Location not found: " + locationId));
        
        List<String> path = new ArrayList<>();
        path.add(location.getLocationName());
        
        Location current = location;
        while (current.getParentLocation() != null) {
            current = current.getParentLocation();
            path.add(0, current.getLocationName());
        }
        
        return String.join(" > ", path);
    }

    // ========== Private Helper Methods ==========

    /**
     * Generate location ID based on type, parent, and name
     * Format: {type_prefix}_{parent_abbreviated}_{name_abbreviated}
     * Example: vil_banga_koram for Village Koramangala under Bangalore
     */
    private String generateLocationId(Location location) {
        String typePrefix = location.getLocationType().name().substring(0, 3).toLowerCase();
        
        String parentAbbr = abbreviateName(location.getParentLocation().getLocationName());
        String nameAbbr = abbreviateName(location.getLocationName());
        
        return typePrefix + "_" + parentAbbr + "_" + nameAbbr;
    }

    /**
     * Abbreviate a location name to first 5 characters of each word
     */
    private String abbreviateName(String name) {
        return Arrays.stream(name.toLowerCase().trim().split("\\s+"))
                .map(word -> word.substring(0, Math.min(word.length(), 5)))
                .collect(Collectors.joining());
    }

    /**
     * Validate parent location exists and is appropriate for child type
     */
    private Location getValidatedParentLocation(String parentId, LocationType childType) {
        Location parentLocation = locationRepo.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Parent location not found: " + parentId));
        
        if (!isValidParentChildRelationship(parentLocation.getLocationType(), childType)) {
            throw new IllegalArgumentException(String.format(
                    "Invalid parent-child relationship: %s cannot be parent of %s", 
                    parentLocation.getLocationType(), 
                    childType
            ));
        }
        
        return parentLocation;
    }

    /**
     * Validate parent-child relationship based on location hierarchy
     */
    private boolean isValidParentChildRelationship(LocationType parentType, LocationType childType) {
        return switch (childType) {
            case STATE -> parentType == LocationType.COUNTRY;
            case DISTRICT -> parentType == LocationType.STATE;
            case TOWN -> parentType == LocationType.DISTRICT;
            case VILLAGE -> parentType == LocationType.TOWN;
            default -> false;
        };
    }

    /**
     * Add child location ID to parent's childLocationIds set
     */
    private void addChildToParentLocation(Location child) {
        Location parent = child.getParentLocation();
        locationRepo.findById(parent.getId()).ifPresent(p -> {
            if (p.getChildLocationIds() == null) {
                p.setChildLocationIds(new HashSet<>());
            }
            p.getChildLocationIds().add(child.getId());
            locationRepo.save(p);
            log.debug("Added child {} to parent {}", child.getId(), p.getId());
        });
    }

    /**
     * Remove child location ID from parent's childLocationIds set
     */
    private void removeChildFromParentLocation(Location child, String childId) {
        Location parent = child.getParentLocation();
        locationRepo.findById(parent.getId()).ifPresent(p -> {
            if (p.getChildLocationIds() != null) {
                p.getChildLocationIds().remove(childId);
                locationRepo.save(p);
                log.debug("Removed child {} from parent {}", childId, p.getId());
            }
        });
    }
}