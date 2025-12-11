package in.shvms.famt_be.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Node("humans")
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class Human {

    @Id @GeneratedValue
    String id;

    @JsonProperty("fName")
    @JsonAlias({"firstName", "fname", "first_name"})
    @Property("fName")
    String fName;

    @JsonProperty("lName")
    @JsonAlias({"lastName", "lname", "last_name", "surname"})
    @Property("lName")
    String lName;
    String petName;

    @NonNull
    Gender gender;

    @Property("dateOfBirth")
    LocalDateTime dateOfBirth;

    @Relationship(type = "BORN_IN")
    Location locationOfBirth;

    @Relationship(type = "LIVES_IN")
    Location livesIn;

    @Relationship(type = "FATHER_OF", direction = Direction.INCOMING)
    Human biologicalFather;
    @Relationship(type = "MOTHER_OF", direction = Direction.INCOMING)
    Human biologicalMother;

    

    @Relationship(type = "CHILD_OF", direction = Direction.OUTGOING)
    Set<Human> children;

    @Relationship(type = "MARRIED_TO", direction = Direction.OUTGOING)
    Set<Human> spouses;

    @Relationship(type = "FRIEND_OF", direction = Direction.OUTGOING)
    Map<Integer, List<Human>> friendCircle;
}