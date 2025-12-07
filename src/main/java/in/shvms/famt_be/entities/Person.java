package in.shvms.famt_be.entities;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

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
    private Event birthEvent;

    @Relationship(type = "DEATH_EVENT", direction = Relationship.Direction.OUTGOING)
    private Event deathEvent;

    @Relationship(type = "LIFE_EVENT", direction = Relationship.Direction.OUTGOING)
    private Set<Event> lifeEvents;

    @Relationship(type = "RESIDES_AT", direction = Relationship.Direction.OUTGOING)
    private Location currentResidence;

    @Relationship(type = "PARENT_CHILD", direction = Relationship.Direction.INCOMING)
    private Set<ParentChildRelation> childrenRelations;

    @Relationship(type = "SPOUSAL", direction = Relationship.Direction.OUTGOING)
    private Set<SpousalRelation> spouseRelations;

    @Relationship(type = "KNOWS", direction = Relationship.Direction.OUTGOING)
    private Set<Person> friends;

    @Relationship(type = "ASSOCIATED_WITH", direction = Relationship.Direction.OUTGOING)
    private Set<Group> associations;
}
