package in.shvms.famt_be.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collection = "humans")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class Human {
    @Id
    String id;

    @JsonProperty("fName")
    @JsonAlias({"firstName", "fname", "first_name"})
    String fName;

    @JsonProperty("lName")
    @JsonAlias({"lastName", "lname", "last_name", "surname"})
    String lName;
    String petName;

    @NonNull
    Gender gender;

    LocalDateTime dateOfBirth;
    Location locationOfBirth;

    Location currentLocation;

    Human biologicalFather;
    Human biologicalMother;

    List<Human> children;

    List<Human> spouses;

    Map<Integer, List<Human>> friendCircle;
}