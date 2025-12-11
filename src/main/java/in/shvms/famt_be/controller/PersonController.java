package in.shvms.famt_be.controller;

import in.shvms.famt_be.config.SecurityContextHelper;
import in.shvms.famt_be.entity.ParentChildType;
import in.shvms.famt_be.entity.Person;
import in.shvms.famt_be.entity.SpousalStatus;
import in.shvms.famt_be.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Person Management Controller
 * All endpoints require authentication and tenant isolation
 */
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/people")
@RequiredArgsConstructor
@Tag(name = "People", description = "Person management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class PersonController {

    private final PersonService personService;
    private final SecurityContextHelper securityContext;

    /**
     * Validate that the path tenantId matches the authenticated user's tenantId
     */
    private void validateTenantAccess(String tenantId) {
        String currentTenantId = securityContext.getCurrentTenantId();
        if (!tenantId.equals(currentTenantId)) {
            throw new SecurityException("Access denied: tenant ID mismatch");
        }
    }

    @PostMapping
    @Operation(summary = "Create person", description = "Create a new person in the family tree")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'STANDARD_USER')")
    public ResponseEntity<?> createPerson(
            @PathVariable String tenantId,
            @RequestBody Person person) {
        try {
            validateTenantAccess(tenantId);
            String userId = securityContext.getCurrentUserId();
            
            Person createdPerson = personService.createPerson(tenantId, userId, person);
            return new ResponseEntity<>(createdPerson, HttpStatus.CREATED);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create person: " + e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "Get all people", description = "Get all people in tenant's family tree")
    public ResponseEntity<?> getAllPeople(@PathVariable String tenantId) {
        try {
            validateTenantAccess(tenantId);
            List<Person> people = personService.getAllPeople(tenantId);
            return ResponseEntity.ok(people);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get person by ID", description = "Get a specific person by their ID")
    public ResponseEntity<?> getPersonById(
            @PathVariable String tenantId,
            @PathVariable UUID id) {
        try {
            validateTenantAccess(tenantId);
            return personService.getPersonById(tenantId, id)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update person", description = "Update person information")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'STANDARD_USER')")
    public ResponseEntity<?> updatePerson(
            @PathVariable String tenantId,
            @PathVariable UUID id,
            @RequestBody Person person) {
        try {
            validateTenantAccess(tenantId);
            String userId = securityContext.getCurrentUserId();
            
            Person updatedPerson = personService.updatePerson(tenantId, userId, id, person);
            return ResponseEntity.ok(updatedPerson);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete person", description = "Delete a person from the family tree")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<?> deletePerson(
            @PathVariable String tenantId,
            @PathVariable UUID id) {
        try {
            validateTenantAccess(tenantId);
            String userId = securityContext.getCurrentUserId();
            
            personService.deletePerson(tenantId, userId, id);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{parentId}/parent-child/{childId}")
    @Operation(summary = "Add parent-child relationship", description = "Create a parent-child relationship")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'STANDARD_USER')")
    public ResponseEntity<?> addParentChildRelation(
            @PathVariable String tenantId,
            @PathVariable UUID parentId,
            @PathVariable UUID childId,
            @RequestParam ParentChildType type,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) Double confidenceScore) {
        try {
            validateTenantAccess(tenantId);
            String userId = securityContext.getCurrentUserId();
            
            Person child = personService.addParentChildRelation(
                    tenantId, userId, parentId, childId, type, startDate, confidenceScore);
            return ResponseEntity.ok(child);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{person1Id}/spousal/{person2Id}")
    @Operation(summary = "Add spousal relationship", description = "Create a spousal relationship")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'STANDARD_USER')")
    public ResponseEntity<?> addSpousalRelation(
            @PathVariable String tenantId,
            @PathVariable UUID person1Id,
            @PathVariable UUID person2Id,
            @RequestParam SpousalStatus status,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String partnershipType) {
        try {
            validateTenantAccess(tenantId);
            String userId = securityContext.getCurrentUserId();
            
            Person person = personService.addSpousalRelation(
                    tenantId, userId, person1Id, person2Id, status, 
                    startDate, endDate, partnershipType);
            return ResponseEntity.ok(person);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{person1Id}/friend/{person2Id}")
    @Operation(summary = "Add friend relationship", description = "Create a friend relationship")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'STANDARD_USER')")
    public ResponseEntity<?> addFriendRelation(
            @PathVariable String tenantId,
            @PathVariable UUID person1Id,
            @PathVariable UUID person2Id) {
        try {
            validateTenantAccess(tenantId);
            String userId = securityContext.getCurrentUserId();
            
            Person person = personService.addFriendRelation(tenantId, userId, person1Id, person2Id);
            return ResponseEntity.ok(person);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search/by-name")
    @Operation(summary = "Search people by name", description = "Find people by name")
    public ResponseEntity<?> findPeopleByName(
            @PathVariable String tenantId,
            @RequestParam String name) {
        try {
            validateTenantAccess(tenantId);
            List<Person> people = personService.findPeopleByName(tenantId, name);
            return ResponseEntity.ok(people);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        }
    }

    @GetMapping("/search/by-lineage/{lineageId}")
    @Operation(summary = "Search people by lineage", description = "Find people by lineage")
    public ResponseEntity<?> findPeopleByLineage(
            @PathVariable String tenantId,
            @PathVariable UUID lineageId) {
        try {
            validateTenantAccess(tenantId);
            List<Person> people = personService.findPeopleByLineage(tenantId, lineageId);
            return ResponseEntity.ok(people);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        }
    }
}