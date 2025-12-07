package in.shvms.famt_be.controller;

import in.shvms.famt_be.entity.Event;
import in.shvms.famt_be.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // Placeholder for userId until security is fully implemented
    private String getActingUserId() {
        return "testUser"; // Replace with actual user ID from security context
    }

    @PostMapping
    public ResponseEntity<Event> createEvent(
            @PathVariable String tenantId,
            @RequestBody Event event) {
        Event createdEvent = eventService.createEvent(tenantId, getActingUserId(), event);
        return new ResponseEntity<>(createdEvent, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents(@PathVariable String tenantId) {
        List<Event> events = eventService.getAllEvents(tenantId);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(
            @PathVariable String tenantId,
            @PathVariable UUID id) {
        return eventService.getEventById(tenantId, id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(
            @PathVariable String tenantId,
            @PathVariable UUID id,
            @RequestBody Event event) {
        try {
            Event updatedEvent = eventService.updateEvent(tenantId, getActingUserId(), id, event);
            return ResponseEntity.ok(updatedEvent);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable String tenantId,
            @PathVariable UUID id) {
        try {
            eventService.deleteEvent(tenantId, getActingUserId(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/search/by-type")
    public ResponseEntity<List<Event>> getEventsByType(
            @PathVariable String tenantId,
            @RequestParam String eventType) {
        List<Event> events = eventService.getEventsByType(tenantId, eventType);
        return ResponseEntity.ok(events);
    }

    @PostMapping("/{eventId}/participants/{personId}")
    public ResponseEntity<Event> addParticipantToEvent(
            @PathVariable String tenantId,
            @PathVariable UUID eventId,
            @PathVariable UUID personId) {
        try {
            Event updatedEvent = eventService.addParticipantToEvent(tenantId, getActingUserId(), eventId, personId);
            return ResponseEntity.ok(updatedEvent);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}