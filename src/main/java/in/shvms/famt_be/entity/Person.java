package in.shvms.famt_be.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.NonNull;
import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * A family graph is cyclic - a spouse points back at their spouse, a child at
 * their parent, an event at its participants. Lombok's generated equals,
 * hashCode and toString walk every field, so the relationship collections are
 * excluded from them; without that, adding a mutual relationship to a HashSet
 * recurses until the stack dies. Identity stays based on the person's own
 * attributes.
 */
@Node("Person")
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class Person {

    @Id @GeneratedValue
    private UUID id;

    @Property("tenantId")
    @NonNull
    private String tenantId;

    @Relationship(type = "MEMBER_OF", direction = Relationship.Direction.OUTGOING)
    private Lineage lineage;

    @Property("firstName")
    @JsonProperty("firstName")
    @JsonAlias({"fName", "fname", "first_name"})
    private String firstName;

    @Property("lastName")
    @JsonProperty("lastName")
    @JsonAlias({"lName", "lname", "last_name", "surname"})
    private String lastName;

    private String maidenName;
    private String petName;

    @NonNull
    private Gender gender;

    @Property("dateOfBirth")
    private LocalDate dateOfBirth;

    @Property("dateOfDeath")
    private LocalDate dateOfDeath;

    @Relationship(type = "BORN_IN_EVENT", direction = Relationship.Direction.OUTGOING)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Event birthEvent;

    @Relationship(type = "DEATH_EVENT", direction = Relationship.Direction.OUTGOING)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Event deathEvent;

    @Relationship(type = "LIFE_EVENT", direction = Relationship.Direction.OUTGOING)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Event> lifeEvents;

    @Relationship(type = "RESIDES_AT", direction = Relationship.Direction.OUTGOING)
    private Location currentResidence;

    @Relationship(type = "PARENT_CHILD", direction = Relationship.Direction.INCOMING)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<ParentChildRelation> childrenRelations;

    @Relationship(type = "SPOUSAL", direction = Relationship.Direction.OUTGOING)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<SpousalRelation> spouseRelations;

    @Relationship(type = "KNOWS", direction = Relationship.Direction.OUTGOING)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Person> friends;

    @Relationship(type = "ASSOCIATED_WITH", direction = Relationship.Direction.OUTGOING)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Group> associations;
}
