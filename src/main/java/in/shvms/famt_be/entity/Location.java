package in.shvms.famt_be.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;
import org.springframework.data.neo4j.core.schema.Property;
import io.micrometer.common.lang.NonNull;
 
@Node("Location")
@Data
@NoArgsConstructor
public class Location {
    @Id
    String id;

    @NonNull
    String locationName;
        
    @NonNull
    LocationType locationType;
    
    @NonNull
    @Relationship(type = "PARENT", direction = Direction.INCOMING)
    Location parentLocation;

    @Property("childLocationIds")
    Set<String> childLocationIds;

    public Location(String locationName, LocationType locationType) {
        this.locationName = locationName;
        this.locationType = locationType;
        // planets cannot have parent locations
        if (locationType == LocationType.COUNTRY || locationType == LocationType.PLANET) {
            throw new IllegalArgumentException("Planets and countries are pre populated.");
        } else {
            this.childLocationIds = new HashSet<>();
        }
        // villages and towns cannot have child locations
        if (locationType == LocationType.VILLAGE || locationType == LocationType.TOWN) {
            this.childLocationIds = null;
        }
    }
}
