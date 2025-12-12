package in.shvms.famt_be.controller;

import in.shvms.famt_be.config.SecurityContextHelper;
import in.shvms.famt_be.dto.LocationDto;
import in.shvms.famt_be.entity.Location;
import in.shvms.famt_be.entity.LocationType;
import in.shvms.famt_be.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "Locations", description = "Location management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class LocationController {

    private final LocationService locationService;
    private final SecurityContextHelper securityContext;

    /**
     * Create a new location
     */
    @PostMapping
    @Operation(summary = "Create location", description = "Create a new location (STATE, DISTRICT, TOWN, or VILLAGE)")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'STANDARD_USER')")
    public ResponseEntity<?> createLocation(@RequestBody LocationDto locationDto) {
        try {
            String tenantId = securityContext.getCurrentTenantId();
            String userId = securityContext.getCurrentUserId();
            
            Location createdLocation = locationService.createLocation(tenantId, userId, locationDto);
            return new ResponseEntity<>(createdLocation, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create location: " + e.getMessage()));
        }
    }

    /**
     * Get location by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get location by ID", description = "Retrieve a specific location by its ID")
    public ResponseEntity<?> getLocationById(@PathVariable String id) {
        try {
            return locationService.getLocationById(id)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all locations of a specific type
     */
    @GetMapping("/type/{locationType}")
    @Operation(summary = "Get locations by type", description = "Get all locations of a specific type (COUNTRY, STATE, etc.)")
    public ResponseEntity<?> getLocationsByType(@PathVariable LocationType locationType) {
        try {
            List<Location> locations = locationService.getLocationsByType(locationType);
            return ResponseEntity.ok(locations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all countries
     */
    @GetMapping("/countries")
    @Operation(summary = "Get all countries", description = "Retrieve all pre-populated countries")
    public ResponseEntity<?> getAllCountries() {
        try {
            List<Location> countries = locationService.getAllCountries();
            return ResponseEntity.ok(countries);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get Earth location
     */
    @GetMapping("/earth")
    @Operation(summary = "Get Earth", description = "Get the root Earth location")
    public ResponseEntity<?> getEarthLocation() {
        try {
            Location earth = locationService.getEarthLocation();
            return ResponseEntity.ok(earth);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get children of a location
     */
    @GetMapping("/{id}/children")
    @Operation(summary = "Get child locations", description = "Get all direct children of a location")
    public ResponseEntity<?> getChildrenOfLocation(@PathVariable String id) {
        try {
            List<Location> children = locationService.getChildrenOfLocation(id);
            return ResponseEntity.ok(children);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get ancestors of a location
     */
    @GetMapping("/{id}/ancestors")
    @Operation(summary = "Get ancestor locations", description = "Get all ancestors (parent, grandparent, etc.) of a location")
    public ResponseEntity<?> getAncestorsOfLocation(@PathVariable String id) {
        try {
            List<Location> ancestors = locationService.getAncestorsOfLocation(id);
            return ResponseEntity.ok(ancestors);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get descendants of a location
     */
    @GetMapping("/{id}/descendants")
    @Operation(summary = "Get descendant locations", description = "Get all descendants (children, grandchildren, etc.) of a location")
    public ResponseEntity<?> getDescendantsOfLocation(@PathVariable String id) {
        try {
            List<Location> descendants = locationService.getDescendantsOfLocation(id);
            return ResponseEntity.ok(descendants);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get hierarchy path for a location
     */
    @GetMapping("/{id}/path")
    @Operation(summary = "Get location path", description = "Get full hierarchy path (e.g., 'Earth > India > Karnataka > Bangalore')")
    public ResponseEntity<?> getLocationPath(@PathVariable String id) {
        try {
            String path = locationService.getLocationHierarchyPath(id);
            return ResponseEntity.ok(Map.of("locationId", id, "path", path));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Search locations by name and type
     */
    @GetMapping("/search")
    @Operation(summary = "Search locations", description = "Search locations by name pattern and type")
    public ResponseEntity<?> searchLocations(
            @RequestParam LocationType locationType,
            @RequestParam(required = false) String searchTerm) {
        try {
            List<Location> locations = locationService.searchLocations(locationType, searchTerm);
            return ResponseEntity.ok(locations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update location (name only)
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update location", description = "Update location name (type and parent cannot be changed)")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<?> updateLocation(
            @PathVariable String id,
            @RequestBody LocationDto locationDto) {
        try {
            String tenantId = securityContext.getCurrentTenantId();
            String userId = securityContext.getCurrentUserId();
            
            Location updatedLocation = locationService.updateLocation(tenantId, userId, id, locationDto);
            return ResponseEntity.ok(updatedLocation);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update location: " + e.getMessage()));
        }
    }

    /**
     * Delete location
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete location", description = "Delete a location (must not have children)")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<?> deleteLocation(@PathVariable String id) {
        try {
            String tenantId = securityContext.getCurrentTenantId();
            String userId = securityContext.getCurrentUserId();
            
            locationService.deleteLocation(tenantId, userId, id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete location: " + e.getMessage()));
        }
    }

    /**
     * Get location statistics
     */
    @GetMapping("/stats")
    @Operation(summary = "Get location statistics", description = "Get counts of locations by type")
    public ResponseEntity<?> getLocationStatistics() {
        try {
            Map<String, Integer> stats = new HashMap<>();
            for (LocationType type : LocationType.values()) {
                stats.put(type.name(), locationService.getLocationsByType(type).size());
            }
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}