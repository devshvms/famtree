package in.shvms.famt_be.controller;

import in.shvms.famt_be.entity.Lineage;
import in.shvms.famt_be.service.LineageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/lineages")
@RequiredArgsConstructor
public class LineageController {

    private final LineageService lineageService;

    // Placeholder for userId until security is fully implemented
    private String getActingUserId() {
        return "testUser"; // Replace with actual user ID from security context
    }

    @PostMapping
    public ResponseEntity<Lineage> createLineage(
            @PathVariable String tenantId,
            @RequestBody Lineage lineage) {
        Lineage createdLineage = lineageService.createLineage(tenantId, getActingUserId(), lineage);
        return new ResponseEntity<>(createdLineage, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Lineage>> getAllLineages(@PathVariable String tenantId) {
        List<Lineage> lineages = lineageService.getAllLineages(tenantId);
        return ResponseEntity.ok(lineages);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lineage> getLineageById(
            @PathVariable String tenantId,
            @PathVariable UUID id) {
        return lineageService.getLineageById(tenantId, id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Lineage> updateLineage(
            @PathVariable String tenantId,
            @PathVariable UUID id,
            @RequestBody Lineage lineage) {
        try {
            Lineage updatedLineage = lineageService.updateLineage(tenantId, getActingUserId(), id, lineage);
            return ResponseEntity.ok(updatedLineage);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLineage(
            @PathVariable String tenantId,
            @PathVariable UUID id) {
        try {
            lineageService.deleteLineage(tenantId, getActingUserId(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}