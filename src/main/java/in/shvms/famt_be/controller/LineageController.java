package in.shvms.famt_be.controller;

import in.shvms.famt_be.config.SecurityContextHelper;
import in.shvms.famt_be.dto.LineageDto;
import in.shvms.famt_be.entity.Lineage;
import in.shvms.famt_be.service.LineageService;
import in.shvms.famt_be.util.EntityMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lineage Management Controller using DTOs
 */
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/lineages")
@RequiredArgsConstructor
@Tag(name = "Lineages", description = "Lineage/Family management with DTOs")
@SecurityRequirement(name = "bearerAuth")
public class LineageController {

    private final LineageService lineageService;
    private final EntityMapper entityMapper;
    private final SecurityContextHelper securityContext;

    private void validateTenantAccess(String tenantId) {
        String currentTenantId = securityContext.getCurrentTenantId();
        if (!tenantId.equals(currentTenantId)) {
            throw new SecurityException("Access denied: tenant ID mismatch");
        }
    }

    @PostMapping
    @Operation(summary = "Create lineage", description = "Create a new lineage/family using DTO")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'STANDARD_USER')")
    public ResponseEntity<?> createLineage(
            @PathVariable String tenantId,
            @Valid @RequestBody LineageDto lineageDto) {
        try {
            validateTenantAccess(tenantId);
            String userId = securityContext.getCurrentUserId();
            
            // Validate DTO
            lineageDto.validate();
            
            // Convert DTO to entity
            Lineage lineage = entityMapper.toLineageEntity(lineageDto, tenantId);
            
            // Create lineage
            Lineage createdLineage = lineageService.createLineage(tenantId, userId, lineage);
            
            // Convert back to DTO with member count
            LineageDto responseDto = entityMapper.toLineageDto(createdLineage);
            
            return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create lineage: " + e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "Get all lineages", description = "Get all lineages in tenant with member counts")
    public ResponseEntity<?> getAllLineages(@PathVariable String tenantId) {
        try {
            validateTenantAccess(tenantId);
            
            List<Lineage> lineages = lineageService.getAllLineages(tenantId);
            List<LineageDto> responseDtos = lineages.stream()
                    .map(entityMapper::toLineageDto)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(responseDtos);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get lineage by ID", description = "Get lineage with member count")
    public ResponseEntity<?> getLineageById(
            @PathVariable String tenantId,
            @PathVariable UUID id) {
        try {
            validateTenantAccess(tenantId);
            
            return lineageService.getLineageById(tenantId, id)
                    .map(entityMapper::toLineageDto)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update lineage", description = "Update lineage information")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'STANDARD_USER')")
    public ResponseEntity<?> updateLineage(
            @PathVariable String tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody LineageDto lineageDto) {
        try {
            validateTenantAccess(tenantId);
            String userId = securityContext.getCurrentUserId();
            
            // Validate DTO
            lineageDto.validate();
            
            // Convert DTO to entity
            Lineage lineage = entityMapper.toLineageEntity(lineageDto, tenantId);
            
            // Update lineage
            Lineage updatedLineage = lineageService.updateLineage(tenantId, userId, id, lineage);
            
            // Convert back to DTO
            LineageDto responseDto = entityMapper.toLineageDto(updatedLineage);
            
            return ResponseEntity.ok(responseDto);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete lineage", description = "Delete a lineage (must not have members)")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<?> deleteLineage(
            @PathVariable String tenantId,
            @PathVariable UUID id) {
        try {
            validateTenantAccess(tenantId);
            String userId = securityContext.getCurrentUserId();
            
            lineageService.deleteLineage(tenantId, userId, id);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}