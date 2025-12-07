package in.shvms.famt_be.controller;

import in.shvms.famt_be.entity.ParentChildType;
import in.shvms.famt_be.entity.Person;
import in.shvms.famt_be.entity.SpousalStatus;
import in.shvms.famt_be.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/people")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    // Placeholder for userId until security is fully implemented
    private String getActingUserId() {
        return "testUser"; // Replace with actual user ID from security context
    }

    @PostMapping
    public ResponseEntity<Person> createPerson(
            @PathVariable String tenantId,
            @RequestBody Person person) {
        Person createdPerson = personService.createPerson(tenantId, getActingUserId(), person);
        return new ResponseEntity<>(createdPerson, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Person>> getAllPeople(@PathVariable String tenantId) {
        List<Person> people = personService.getAllPeople(tenantId);
        return ResponseEntity.ok(people);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Person> getPersonById(
            @PathVariable String tenantId,
            @PathVariable UUID id) {
        return personService.getPersonById(tenantId, id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Person> updatePerson(
            @PathVariable String tenantId,
            @PathVariable UUID id,
            @RequestBody Person person) {
        try {
            Person updatedPerson = personService.updatePerson(tenantId, getActingUserId(), id, person);
            return ResponseEntity.ok(updatedPerson);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePerson(
            @PathVariable String tenantId,
            @PathVariable UUID id) {
        try {
            personService.deletePerson(tenantId, getActingUserId(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/{parentId}/parent-child/{childId}")
    public ResponseEntity<Person> addParentChildRelation(
            @PathVariable String tenantId,
            @PathVariable UUID parentId,
            @PathVariable UUID childId,
            @RequestParam ParentChildType type,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) Double confidenceScore) {
        try {
            Person child = personService.addParentChildRelation(tenantId, getActingUserId(), parentId, childId, type, startDate, confidenceScore);
            return ResponseEntity.ok(child);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/{person1Id}/spousal/{person2Id}")
    public ResponseEntity<Person> addSpousalRelation(
            @PathVariable String tenantId,
            @PathVariable UUID person1Id,
            @PathVariable UUID person2Id,
            @RequestParam SpousalStatus status,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String partnershipType) {
        try {
            Person person = personService.addSpousalRelation(tenantId, getActingUserId(), person1Id, person2Id, status, startDate, endDate, partnershipType);
            return ResponseEntity.ok(person);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/{person1Id}/friend/{person2Id}")
    public ResponseEntity<Person> addFriendRelation(
            @PathVariable String tenantId,
            @PathVariable UUID person1Id,
            @PathVariable UUID person2Id) {
        try {
            Person person = personService.addFriendRelation(tenantId, getActingUserId(), person1Id, person2Id);
            return ResponseEntity.ok(person);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/search/by-name")
    public ResponseEntity<List<Person>> findPeopleByName(
            @PathVariable String tenantId,
            @RequestParam String name) {
        List<Person> people = personService.findPeopleByName(tenantId, name);
        return ResponseEntity.ok(people);
    }

    @GetMapping("/search/by-lineage/{lineageId}")
    public ResponseEntity<List<Person>> findPeopleByLineage(
            @PathVariable String tenantId,
            @PathVariable UUID lineageId) {
        List<Person> people = personService.findPeopleByLineage(tenantId, lineageId);
        return ResponseEntity.ok(people);
    }
}