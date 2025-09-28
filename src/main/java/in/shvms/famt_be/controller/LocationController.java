package in.shvms.famt_be.controller;

import in.shvms.famt_be.entity.Location;
import in.shvms.famt_be.repo.LocationRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationRepo locationRepo;

    public LocationController(LocationRepo locationRepo) {
        this.locationRepo = locationRepo;
    }

    @GetMapping
    public List<Location> getAll() {
        return locationRepo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Location> getById(@PathVariable String id) {
        return locationRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Location> create(@RequestBody Location location) {
        if (location.getId() == null || location.getId().isBlank()) {
            location.setId(null);
        }
        Location saved = locationRepo.save(location);
        return ResponseEntity.created(URI.create("/api/locations/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Location> update(@PathVariable String id, @RequestBody Location location) {
        return locationRepo.findById(id)
                .map(existing -> {
                    location.setId(id);
                    return ResponseEntity.ok(locationRepo.save(location));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable String id) {
        return locationRepo.findById(id)
                .map(existing -> {
                    locationRepo.deleteById(id);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
