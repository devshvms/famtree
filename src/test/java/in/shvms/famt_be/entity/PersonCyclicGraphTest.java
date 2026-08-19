package in.shvms.famt_be.entity;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Family graphs are cyclic: a spouse points back at their spouse, a child
 * points back at a parent. Lombok's generated equals/hashCode/toString walk
 * every field, so unless the back-references are excluded they recurse until
 * the stack dies.
 *
 * <p>Reproduces the StackOverflowError seen in PersonGraphIT without needing
 * a database.
 */
class PersonCyclicGraphTest {

    private Person person(String name) {
        Person p = new Person();
        p.setTenantId("t1");
        p.setFirstName(name);
        p.setGender(Gender.OTHER);
        p.setSpouseRelations(new HashSet<>());
        p.setChildrenRelations(new HashSet<>());
        p.setFriends(new HashSet<>());
        p.setLifeEvents(new HashSet<>());
        p.setAssociations(new HashSet<>());
        return p;
    }

    @Test
    void mutualSpousesCanBeHashed() {
        Person john = person("John");
        Person mary = person("Mary");

        // Exactly what PersonService.addSpousalRelation does.
        john.getSpouseRelations().add(new SpousalRelation(null, mary, null, null, SpousalStatus.MARRIED, "Marriage"));
        mary.getSpouseRelations().add(new SpousalRelation(null, john, null, null, SpousalStatus.MARRIED, "Marriage"));

        assertThatCode(john::hashCode).doesNotThrowAnyException();
        assertThatCode(john::toString).doesNotThrowAnyException();
        assertThatCode(() -> john.equals(mary)).doesNotThrowAnyException();
    }

    @Test
    void mutualFriendsCanBeHashed() {
        Person a = person("A");
        Person b = person("B");

        a.getFriends().add(b);
        b.getFriends().add(a);

        assertThatCode(a::hashCode).doesNotThrowAnyException();
        assertThatCode(a::toString).doesNotThrowAnyException();
    }

    @Test
    void parentChildBackReferenceCanBeHashed() {
        Person parent = person("Parent");
        Person child = person("Child");

        child.getChildrenRelations()
                .add(new ParentChildRelation(null, parent, ParentChildType.BIOLOGICAL, 1.0, null));
        // A parent knowing its child through an event participation closes the loop.
        Event birth = new Event();
        birth.setTenantId("t1");
        birth.setEventType("Birth");
        birth.setParticipants(new HashSet<>(java.util.List.of(parent)));
        parent.getLifeEvents().add(birth);

        assertThatCode(child::hashCode).doesNotThrowAnyException();
        assertThatCode(parent::hashCode).doesNotThrowAnyException();
    }

    @Test
    void peopleRemainDistinctInASet() {
        Person john = person("John");
        Person mary = person("Mary");

        assertThat(new HashSet<>(java.util.List.of(john, mary))).hasSize(2);
    }
}
