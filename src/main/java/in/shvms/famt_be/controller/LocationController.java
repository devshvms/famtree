package in.shvms.famt_be.controller;

import in.shvms.famt_be.entity.Location;
import in.shvms.famt_be.entity.LocationType;
import in.shvms.famt_be.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    // Placeholder for userId until security is fully implemented
    private String getActingUserId() {
        return "testUser"; // Replace with actual user ID from security context
    }

    @PostMapping
    public ResponseEntity<Location> createLocation(
            @RequestParam String tenantId,
            @RequestBody Location location) {
        try {
            Location createdLocation = locationService.createLocation(tenantId, getActingUserId(), location);
            return new ResponseEntity<>(createdLocation, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Location> getLocationById(@PathVariable String id) {
        return locationService.getLocationById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/by-type")
    public ResponseEntity<List<Location>> getLocationsByType(@RequestParam LocationType locationType) {
        List<Location> locations = locationService.getLocationsByType(locationType);
        return ResponseEntity.ok(locations);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Location> updateLocation(
            @RequestParam String tenantId,
            @PathVariable String id,
            @RequestBody Location location) {
        try {
            Location updatedLocation = locationService.updateLocation(tenantId, getActingUserId(), id, location);
            return ResponseEntity.ok(updatedLocation);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(
            @RequestParam String tenantId,
            @PathVariable String id) {
        try {
            locationService.deleteLocation(tenantId, getActingUserId(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/child/{parentLocationId}")
    public ResponseEntity<Location> createChildLocation(
            @RequestParam String tenantId,
            @PathVariable String parentLocationId,
            @RequestBody Location location) {
        try {
            Location createdLocation = locationService.createChildLocation(
                    tenantId,
                    getActingUserId(),
                    location.getLocationName(),
                    location.getLocationType(),
                    parentLocationId
            );
            return new ResponseEntity<>(createdLocation, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }
}