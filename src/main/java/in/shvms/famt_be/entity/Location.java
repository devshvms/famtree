package in.shvms.famt_be.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

 
@Document(collection = "locations")
@Data
@NoArgsConstructor
public class Location {
    @Id
    String id;
    String town;
    String district;
    String state;
    Country country;
}
