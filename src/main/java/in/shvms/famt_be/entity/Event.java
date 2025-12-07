package in.shvms.famt_be.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;

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

    // Crucial for Tenant Isolation
    @Property("tenantId")
    @NonNull
    private String tenantId; 

    // Type of event (e.g., Birth, Death, Marriage, Divorce, Adoption, Census)
    @Property("eventType")
    @NonNull
    private String eventType; 

    // The date the event occurred
    @Property("eventDate")
    private LocalDate eventDate; 
    
    // An optional descriptive title or notes
    private String description;
    
    // --- RELATIONSHIPS ---
    
    // Links the event to the location where it happened
    @Relationship(type = "OCCURRED_AT", direction = Direction.OUTGOING)
    private Location location;

    // Links the event back to the Person(s) involved 
    // (This is usually defined on the Person side, but helpful for direct lookups)
    @Relationship(type = "PARTICIPATED_IN", direction = Direction.INCOMING)
    private Set<Person> participants; 
}