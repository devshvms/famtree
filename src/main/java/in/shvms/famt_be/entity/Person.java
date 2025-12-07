package in.shvms.famt_be.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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

@Node("Person") // Standard node label
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class Person {

    // --- 1. CORE IDENTIFIERS AND TENANCY ---
    @Id @GeneratedValue
    private UUID id; // Using UUID for robust distributed IDs
    
    // Crucial for Tenant Isolation: Every node must be linked to its tenant.
    @Property("tenantId")
    @NonNull
    private String tenantId; 

    // Crucial for Lineage Isolation: Links the person to a specific family tree within the tenant.
    @Relationship(type = "MEMBER_OF", direction = Direction.OUTGOING)
    private Lineage lineage;

    // --- 2. BASIC PROPERTIES ---
    @Property("firstName") // Use clear, consistent property names (fName -> firstName)
    @JsonProperty("firstName")
    @JsonAlias({"fName", "fname", "first_name"})
    private String firstName;

    @Property("lastName")
    @JsonProperty("lastName")
    @JsonAlias({"lName", "lname", "last_name", "surname"})
    private String lastName;
    
    // Add granularity properties like titles or maiden name.
    private String maidenName;
    private String petName;
    
    @NonNull
    private Gender gender; // Assuming Gender is an Enum

    // Use LocalDate for birth/death, not LocalDateTime, unless time-of-birth is crucial.
    @Property("dateOfBirth")
    private LocalDate dateOfBirth; 
    
    @Property("dateOfDeath")
    private LocalDate dateOfDeath;

    // --- 3. LOCATION & EVENT RELATIONSHIPS (Granular) ---
    // Link to an Event node, which then links to the Location.
    @Relationship(type = "BORN_IN_EVENT", direction = Direction.OUTGOING)
    private Event birthEvent;

    @Relationship(type = "DEATH_EVENT", direction = Direction.OUTGOING)
    private Event deathEvent;

    @Relationship(type = "LIFE_EVENT", direction = Direction.OUTGOING)
    private Set<Event> lifeEvents;

    @Relationship(type = "RESIDES_AT", direction = Direction.OUTGOING)
    private Location currentResidence; // Changed LIVES_IN to be more specific.

    // --- 4. FAMILY RELATIONSHIPS (Highly Granular) ---
    
    // Instead of simple Human links, use an SDN-specific Relationship Entity.
    // This allows you to store properties *on the relationship*, like Adoption Date,
    // confidence score, and relationship status.
    
    @Relationship(type = "PARENT_CHILD", direction = Direction.INCOMING)
    // ParentChildRelation is a new class that contains 'since' or 'type' (Biological/Adoptive) properties.
    private Set<ParentChildRelation> childrenRelations; 

    @Relationship(type = "SPOUSAL", direction = Direction.OUTGOING)
    // SpousalRelation is a new class that contains 'startDate', 'endDate', and 'status' (Married/Divorced) properties.
    private Set<SpousalRelation> spouseRelations;


    // --- 5. NON-FAMILY RELATIONSHIPS ---
    // FRIEND_OF is typically simpler. Use a Set<Person> for simplicity unless the Map is specifically required.
    // The Map<Integer, List<Human>> structure is unusual for simple graph relationships unless the Integer represents a 'friendship score' property on the relationship itself.
    @Relationship(type = "KNOWS", direction = Direction.OUTGOING)
    private Set<Person> friends; 
    
    // Optional: Add a relationship for professional/group connections
    @Relationship(type = "ASSOCIATED_WITH", direction = Direction.OUTGOING)
    private Set<Group> associations;
}