package in.shvms.famt_be;

import in.shvms.famt_be.entity.Gender;
import in.shvms.famt_be.entity.ParentChildType;
import in.shvms.famt_be.entity.Person;
import in.shvms.famt_be.entity.SpousalStatus;
import in.shvms.famt_be.repositories.neo4j.PersonRepository;
import in.shvms.famt_be.service.PersonService;
import in.shvms.famt_be.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the graph model end to end: the parent-child and spousal
 * relationships are the heart of this application, and their Neo4j mapping
 * (incoming PARENT_CHILD edges whose target node is the parent) is subtle
 * enough that only a real database proves it works.
 */
class PersonGraphIT extends AbstractIntegrationTest {

    private static final String TENANT = "tenant-graph-it";
    private static final String ACTOR = "user-it";

    @Autowired
    private PersonService personService;

    @Autowired
    private PersonRepository personRepo;

    @BeforeEach
    void cleanTenant() {
        personRepo.findAllByTenantId(TENANT).forEach(personRepo::delete);
    }

    private Person newPerson(String firstName, Gender gender, LocalDate dob) {
        Person person = new Person();
        person.setTenantId(TENANT);
        person.setFirstName(firstName);
        person.setLastName("Testfamily");
        person.setGender(gender);
        person.setDateOfBirth(dob);
        return personService.createPerson(TENANT, ACTOR, person);
    }

    @Test
    void createsPersonAndReadsItBack() {
        Person saved = newPerson("Ada", Gender.FEMALE, LocalDate.of(1980, 5, 15));

        assertThat(saved.getId()).isNotNull();
        assertThat(personService.getPersonById(TENANT, saved.getId()))
                .isPresent()
                .get()
                .satisfies(p -> assertThat(p.getFirstName()).isEqualTo("Ada"));
    }

    @Test
    void isolatesPeopleByTenant() {
        Person mine = newPerson("Ada", Gender.FEMALE, LocalDate.of(1980, 5, 15));

        assertThat(personService.getPersonById("some-other-tenant", mine.getId())).isEmpty();
    }

    @Test
    void linksParentsChildrenAndSiblings() {
        Person father = newPerson("Robert", Gender.MALE, LocalDate.of(1950, 8, 10));
        Person mother = newPerson("Mary", Gender.FEMALE, LocalDate.of(1952, 3, 20));
        Person child = newPerson("Sarah", Gender.FEMALE, LocalDate.of(1980, 11, 22));
        Person sibling = newPerson("John", Gender.MALE, LocalDate.of(1983, 1, 5));

        for (Person kid : List.of(child, sibling)) {
            for (Person parent : List.of(father, mother)) {
                personService.addParentChildRelation(
                        TENANT, ACTOR, parent.getId(), kid.getId(),
                        ParentChildType.BIOLOGICAL, kid.getDateOfBirth(), 1.0);
            }
        }

        assertThat(personService.getParents(TENANT, child.getId()))
                .extracting(Person::getFirstName)
                .containsExactlyInAnyOrder("Robert", "Mary");

        assertThat(personService.getChildren(TENANT, father.getId()))
                .extracting(Person::getFirstName)
                .containsExactlyInAnyOrder("Sarah", "John");

        assertThat(personService.getSiblings(TENANT, child.getId()))
                .extracting(Person::getFirstName)
                .containsExactly("John");
    }

    @Test
    void rejectsParentBornAfterChild() {
        Person parent = newPerson("Younger", Gender.MALE, LocalDate.of(2000, 1, 1));
        Person child = newPerson("Older", Gender.FEMALE, LocalDate.of(1990, 1, 1));

        assertThatThrownBy(() -> personService.addParentChildRelation(
                TENANT, ACTOR, parent.getId(), child.getId(),
                ParentChildType.BIOLOGICAL, null, 1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parent must be born before child");
    }

    @Test
    void rejectsDuplicateParentChildRelation() {
        Person parent = newPerson("Robert", Gender.MALE, LocalDate.of(1950, 8, 10));
        Person child = newPerson("Sarah", Gender.FEMALE, LocalDate.of(1980, 11, 22));

        personService.addParentChildRelation(TENANT, ACTOR, parent.getId(), child.getId(),
                ParentChildType.BIOLOGICAL, null, 1.0);

        assertThatThrownBy(() -> personService.addParentChildRelation(
                TENANT, ACTOR, parent.getId(), child.getId(),
                ParentChildType.BIOLOGICAL, null, 1.0))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void spousalRelationIsVisibleFromBothSides() {
        Person one = newPerson("John", Gender.MALE, LocalDate.of(1980, 5, 15));
        Person two = newPerson("Mary", Gender.FEMALE, LocalDate.of(1982, 3, 20));

        personService.addSpousalRelation(TENANT, ACTOR, one.getId(), two.getId(),
                SpousalStatus.MARRIED, LocalDate.of(2005, 6, 15), null, "Marriage");

        assertThat(personService.getSpouses(TENANT, one.getId()))
                .extracting(Person::getFirstName).contains("Mary");
        assertThat(personService.getSpouses(TENANT, two.getId()))
                .extracting(Person::getFirstName).contains("John");
    }

    @Test
    void deletingAPersonDetachesThemFromRelatives() {
        Person parent = newPerson("Robert", Gender.MALE, LocalDate.of(1950, 8, 10));
        Person child = newPerson("Sarah", Gender.FEMALE, LocalDate.of(1980, 11, 22));
        personService.addParentChildRelation(TENANT, ACTOR, parent.getId(), child.getId(),
                ParentChildType.BIOLOGICAL, null, 1.0);

        UUID parentId = parent.getId();
        personService.deletePerson(TENANT, ACTOR, parentId);

        assertThat(personService.getPersonById(TENANT, parentId)).isEmpty();
        assertThat(personService.getParents(TENANT, child.getId())).isEmpty();
    }
}
