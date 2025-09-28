package in.shvms.famt_be.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class Human {
    @Id
    @NonNull
    String id;

    String fName;
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