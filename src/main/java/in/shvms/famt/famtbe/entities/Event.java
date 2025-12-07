package in.shvms.famt.famtbe.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Node("Event")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id @GeneratedValue
    private UUID id;

    @Property("tenantId")
    @NonNull
    private String tenantId;

    @Property("eventType")
    @NonNull
    private String eventType;

    @Property("eventDate")
    private LocalDate eventDate;

    private String description;

    @Relationship(type = "OCCURRED_AT", direction = Relationship.Direction.OUTGOING)
    private Location location;

    @Relationship(type = "PARTICIPATED_IN", direction = Relationship.Direction.INCOMING)
    private Set<Person> participants;
}
