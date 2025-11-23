package in.shvms.famt_be.controller;

import in.shvms.famt_be.dto.LocationDto;
import in.shvms.famt_be.entity.Location;
import in.shvms.famt_be.service.LocationService;
import lombok.AllArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/location")
@AllArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    public List<Location> getAll() {
        return locationService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Location> getById(@PathVariable String id) {
        return locationService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Location> create(@RequestBody LocationDto locationDto) {
        Location saved = locationService.save(locationDto);
        return ResponseEntity.created(URI.create("/api/locations/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Location> updatePut(@PathVariable String id, @RequestBody LocationDto locationDto) {
        return locationService.update(id, locationDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Location> updatePatch(@PathVariable String id, @RequestBody LocationDto locationDto) {
        return locationService.update(id, locationDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Location> delete(@PathVariable String id) {
        return locationService.deleteNodeById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
