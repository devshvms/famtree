package in.shvms.famt_be.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;

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

    public Location(String locationName, LocationType locationType) {
        this.locationName = locationName;
        this.locationType = locationType;
        if (locationType == LocationType.COUNTRY || locationType == LocationType.PLANET) {
            throw new IllegalArgumentException("Planets and countries are pre populated.");
        }
    }
}