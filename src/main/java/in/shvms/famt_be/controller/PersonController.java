package in.shvms.famt_be.controller;

import in.shvms.famt_be.config.SecurityContextHelper;
import in.shvms.famt_be.dto.*;
import in.shvms.famt_be.entity.Person;
import in.shvms.famt_be.service.PersonService;
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
 * Enhanced Person Management Controller using DTOs
 */
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/people")
@RequiredArgsConstructor
@Tag(name = "People", description = "Person and relationship management with DTOs")
@SecurityRequirement(name = "bearerAuth")
public class PersonController {

    private final PersonService personService;
    private final EntityMapper entityMapper;
    private final SecurityContextHelper securityContext;

    private void validateTenantAccess(String tenantId) {
        String currentTenantId = securityContext.getCurrentTenantId();
        if (!tenantId.equals(currentTenantId)) {
            throw new SecurityException("Access denied: tenant ID mismatch");
        }
    }

    // ========== PERSON CRUD ==========

    @PostMapping
    @Operation(summary = "Create person", description = "Create a new person using simplified DTO")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'STANDARD_USER')")
    public ResponseEntity<?> createPerson(
            @PathVariable String tenantId,
            @Valid @RequestBody PersonDto personDto) {
        try {
            validateTenantAccess(tenantId);
            String userId = securityContext.getCurrentUserId();
            
            // Validate DTO
            personDto.validate();
            
            // Convert DTO to entity
            Person person = entityMapper.toPersonEntity(personDto, tenantId);
            
            // Create person
            Person createdPerson = personService.createPerson(tenantId, userId, person);
            
            // Convert back to DTO
            PersonDto responseDto = entityMapper.toPersonDto(createdPerson);
            
            return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create person: " + e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "Get all people", description = "Get all people as simple DTOs")
    public ResponseEntity<?> getAllPeople(@PathVariable String tenantId) {
        try {
            validateTenantAccess(tenantId);
            
            List<Person> people = personService.getAllPeople(tenantId);
            List<PersonDto> responseDtos = people.stream()
                    .map(entityMapper::toPersonDto)
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
    @Operation(summary = "Get person by ID", description = "Get detailed person information")
    public ResponseEntity<?> getPersonById(
            @PathVariable String tenantId,
            @PathVariable UUID id) {
        try {
            validateTenantAccess(tenantId);
            
            return personService.getPersonById(tenantId, id)
                    .map(entityMapper::toPersonDetailDto)
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
    @Operation(summary = "Update person", description = "Update person using DTO")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'STANDARD_USER')")
    public ResponseEntity<?> updatePerson(
            @PathVariable String tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody PersonDto personDto) {
        try {
            validateTenantAccess(tenantId);
            String userId = securityContext.getCurrentUserId();
            
            // Validate DTO
            personDto.validate();
            
            // Convert DTO to entity
            Person person = entityMapper.toPersonEntity(personDto, tenantId);
            
            // Update person
            Person updatedPerson = personService.updatePerson(tenantId, userId, id, person);
            
            // Convert back to DTO
            PersonDto responseDto = entityMapper.toPersonDto(updatedPerson);
            
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
    @Operation(summary = "Delete person", description = "Delete a person")
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
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ========== PARENT-CHILD RELATIONSHIPS ==========

    @PostMapping("/relationships/parent-child")
    @Operation(summary = "Add parent-child relationship", 
              description = "Create a parent-child relationship using DTO")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'STANDARD_USER')")
    public ResponseEntity<?> addParentChildRelation(
            @PathVariable String tenantId,
            @Valid @RequestBody RelationshipDto.ParentChildRelationRequest request) {
        try {
            validateTenantAccess(tenantId);
            String userId = securityContext.getCurrentUserId();
            
            // Validate request
            request.validate();
            
            Person child = personService.addParentChildRelation(
                    tenantId, userId, 
                    request.parentId(), 
                    request.childId(), 
                    request.relationshipType(), 
                    request.startDate(), 
                    request.confidenceScore() != null ? request.confidenceScore() : 1.0
            );
            
            PersonDto responseDto = entityMapper.toPersonDto(child);
            return ResponseEntity.ok(responseDto);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/relationships/parent-child")
    @Operation(summary = "Remove parent-child relationship")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'STANDARD_USER')")
    public ResponseEntity<?> removeParentChildRelation(
            @PathVariable String tenantId,
            @RequestParam UUID parentId,
            @RequestParam UUID childId) {
        try {
            validateTenantAccess(tenantId);
            String userId = securityContext.getCurrentUserId();
            
            Person child = personService.removeParentChildRelation(tenantId, userId, parentId, childId);
            PersonDto responseDto = entityMapper.toPersonDto(child);
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

    @GetMapping("/{id}/children")
    @Operation(summary = "Get children", description = "Get all children of a person")
    public ResponseEntity<?> getChildren(
            @PathVariable String tenantId,
            @PathVariable UUID id) {
        try {
            validateTenantAccess(tenantId);
            
            List<Person> children = personService.getChildren(tenantId, id);
            List<PersonDto> responseDtos = children.stream()
                    .map(entityMapper::toPersonDto)
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

    @GetMapping("/{id}/parents")
    @Operation(summary = "Get parents", description = "Get all parents of a person")
    public ResponseEntity<?> getParents(
            @PathVariable String tenantId,
            @PathVariable UUID id) {
        try {
            validateTenantAccess(tenantId);
            
            List<Person> parents = personService.getParents(tenantId, id);
            List<PersonDto> responseDtos = parents.stream()
                    .map(entityMapper::toPersonDto)
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

    // ========== SPOUSAL RELATIONSHIPS ==========

    @PostMapping("/relationships/spousal")
    @Operation(summary = "Add spousal relationship", description = "Create spousal relationship using DTO")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'STANDARD_USER')")
    public ResponseEntity<?> addSpousalRelation(
            @PathVariable String tenantId,
            @Valid @RequestBody RelationshipDto.SpousalRelationRequest request) {
        try {
            validateTenantAccess(tenantId);
            String userId = securityContext.getCurrentUserId();
            
            // Validate request
            request.validate();
            
            Person person = personService.addSpousalRelation(
                    tenantId, userId,
                    request.person1Id(),
                    request.person2Id(),
                    request.status(),
                    request.startDate(),
                    request.endDate(),
                    request.partnershipType()
            );
            
            PersonDto responseDto = entityMapper.toPersonDto(person);
            return ResponseEntity.ok(responseDto);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/relationships/spousal")
    @Operation(summary = "Remove spousal relationship")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'STANDARD_USER')")
    public ResponseEntity<?> removeSpousalRelation(
            @PathVariable String tenantId,
            @RequestParam UUID person1Id,
            @RequestParam UUID person2Id) {
        try {
            validateTenantAccess(tenantId);
            String userId = securityContext.getCurrentUserId();
            
            Person person = personService.removeSpousalRelation(tenantId, userId, person1Id, person2Id);
            PersonDto responseDto = entityMapper.toPersonDto(person);
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

    @GetMapping("/{id}/spouses")
    @Operation(summary = "Get spouses")
    public ResponseEntity<?> getSpouses(
            @PathVariable String tenantId,
            @PathVariable UUID id) {
        try {
            validateTenantAccess(tenantId);
            
            List<Person> spouses = personService.getSpouses(tenantId, id);
            List<PersonDto> responseDtos = spouses.stream()
                    .map(entityMapper::toPersonDto)
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

    // ========== FRIEND RELATIONSHIPS ==========

    @PostMapping("/relationships/friend")
    @Operation(summary = "Add friend relationship")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'STANDARD_USER')")
    public ResponseEntity<?> addFriendRelation(
            @PathVariable String tenantId,
            @Valid @RequestBody RelationshipDto.FriendRelationRequest request) {
        try {
            validateTenantAccess(tenantId);
            String userId = securityContext.getCurrentUserId();
            
            // Validate request
            request.validate();
            
            Person person = personService.addFriendRelation(
                    tenantId, userId, 
                    request.person1Id(), 
                    request.person2Id()
            );
            
            PersonDto responseDto = entityMapper.toPersonDto(person);
            return ResponseEntity.ok(responseDto);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ========== SEARCH ==========

    @GetMapping("/search")
    @Operation(summary = "Search people by name")
    public ResponseEntity<?> searchPeople(
            @PathVariable String tenantId,
            @RequestParam String name) {
        try {
            validateTenantAccess(tenantId);
            
            List<Person> people = personService.findPeopleByName(tenantId, name);
            List<PersonDto> responseDtos = people.stream()
                    .map(entityMapper::toPersonDto)
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

    @GetMapping("/{id}/siblings")
    @Operation(summary = "Get siblings")
    public ResponseEntity<?> getSiblings(
            @PathVariable String tenantId,
            @PathVariable UUID id) {
        try {
            validateTenantAccess(tenantId);
            
            List<Person> siblings = personService.getSiblings(tenantId, id);
            List<PersonDto> responseDtos = siblings.stream()
                    .map(entityMapper::toPersonDto)
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
}