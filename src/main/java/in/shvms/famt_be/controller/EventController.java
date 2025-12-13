package in.shvms.famt_be.controller;

import in.shvms.famt_be.config.SecurityContextHelper;
import in.shvms.famt_be.dto.EventDto;
import in.shvms.famt_be.entity.Event;
import in.shvms.famt_be.service.EventService;
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
 * Event Management Controller using DTOs
 */
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Event management with DTOs")
@SecurityRequirement(name = "bearerAuth")
public class EventController {

    private final EventService eventService;
    private final EntityMapper entityMapper;
    private final SecurityContextHelper securityContext;

    private void validateTenantAccess(String tenantId) {
        String currentTenantId = securityContext.getCurrentTenantId();
        if (!tenantId.equals(currentTenantId)) {
            throw new SecurityException("Access denied: tenant ID mismatch");
        }
    }

    @PostMapping
    @Operation(summary = "Create event", description = "Create a new event using DTO")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'STANDARD_USER')")
    public ResponseEntity<?> createEvent(
            @PathVariable String tenantId,
            @Valid @RequestBody EventDto eventDto) {
        try {
            validateTenantAccess(tenantId);
            String userId = securityContext.getCurrentUserId();
            
            // Validate DTO
            eventDto.validate();
            
            // Convert DTO to entity
            Event event = entityMapper.toEventEntity(eventDto, tenantId);
            
            // Create event
            Event createdEvent = eventService.createEvent(tenantId, userId, event);
            
            // Convert back to DTO
            EventDto responseDto = entityMapper.toEventDto(createdEvent);
            
            return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create event: " + e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "Get all events", description = "Get all events in tenant")
    public ResponseEntity<?> getAllEvents(@PathVariable String tenantId) {
        try {
            validateTenantAccess(tenantId);
            
            List<Event> events = eventService.getAllEvents(tenantId);
            List<EventDto> responseDtos = events.stream()
                    .map(entityMapper::toEventDto)
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
    @Operation(summary = "Get event by ID")
    public ResponseEntity<?> getEventById(
            @PathVariable String tenantId,
            @PathVariable UUID id) {
        try {
            validateTenantAccess(tenantId);
            
            return eventService.getEventById(tenantId, id)
                    .map(entityMapper::toEventDto)
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
    @Operation(summary = "Update event")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'STANDARD_USER')")
    public ResponseEntity<?> updateEvent(
            @PathVariable String tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody EventDto eventDto) {
        try {
            validateTenantAccess(tenantId);
            String userId = securityContext.getCurrentUserId();
            
            // Validate DTO
            eventDto.validate();
            
            // Convert DTO to entity
            Event event = entityMapper.toEventEntity(eventDto, tenantId);
            
            // Update event
            Event updatedEvent = eventService.updateEvent(tenantId, userId, id, event);
            
            // Convert back to DTO
            EventDto responseDto = entityMapper.toEventDto(updatedEvent);
            
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
    @Operation(summary = "Delete event")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<?> deleteEvent(
            @PathVariable String tenantId,
            @PathVariable UUID id) {
        try {
            validateTenantAccess(tenantId);
            String userId = securityContext.getCurrentUserId();
            
            eventService.deleteEvent(tenantId, userId, id);
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

    @GetMapping("/search/by-type")
    @Operation(summary = "Get events by type")
    public ResponseEntity<?> getEventsByType(
            @PathVariable String tenantId,
            @RequestParam String eventType) {
        try {
            validateTenantAccess(tenantId);
            
            List<Event> events = eventService.getEventsByType(tenantId, eventType);
            List<EventDto> responseDtos = events.stream()
                    .map(entityMapper::toEventDto)
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

    @PostMapping("/{eventId}/participants/{personId}")
    @Operation(summary = "Add participant to event")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'STANDARD_USER')")
    public ResponseEntity<?> addParticipantToEvent(
            @PathVariable String tenantId,
            @PathVariable UUID eventId,
            @PathVariable UUID personId) {
        try {
            validateTenantAccess(tenantId);
            String userId = securityContext.getCurrentUserId();
            
            Event updatedEvent = eventService.addParticipantToEvent(tenantId, userId, eventId, personId);
            EventDto responseDto = entityMapper.toEventDto(updatedEvent);
            
            return ResponseEntity.ok(responseDto);
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

    @DeleteMapping("/{eventId}/participants/{personId}")
    @Operation(summary = "Remove participant from event")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'STANDARD_USER')")
    public ResponseEntity<?> removeParticipantFromEvent(
            @PathVariable String tenantId,
            @PathVariable UUID eventId,
            @PathVariable UUID personId) {
        try {
            validateTenantAccess(tenantId);
            String userId = securityContext.getCurrentUserId();
            
            Event updatedEvent = eventService.removeParticipantFromEvent(tenantId, userId, eventId, personId);
            EventDto responseDto = entityMapper.toEventDto(updatedEvent);
            
            return ResponseEntity.ok(responseDto);
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