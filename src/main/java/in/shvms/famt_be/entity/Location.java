package in.shvms.famt_be.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;

import java.util.Set;

@Node("Location")
@Data
@NoArgsConstructor
public class Location {
    
    @Id
    private String id;

    @NonNull
    @Property("locationName")
    private String locationName;

    @NonNull
    @Property("locationType")
    private LocationType locationType;

    @Relationship(type = "PARENT", direction = Direction.OUTGOING)
    private Location parentLocation;

    // Store child location IDs for easier traversal
    @Property("childLocationIds")
    private Set<String> childLocationIds;

    public Location(String locationName, LocationType locationType) {
        this.locationName = locationName;
        this.locationType = locationType;
        validateLocationType(locationType);
    }

    private void validateLocationType(LocationType locationType) {
        if (locationType == LocationType.COUNTRY || locationType == LocationType.PLANET) {
            throw new IllegalArgumentException("Planets and countries are pre-populated and cannot be created via constructor.");
        }
    }
}