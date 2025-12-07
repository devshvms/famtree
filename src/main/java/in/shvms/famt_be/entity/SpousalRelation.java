package in.shvms.famt_be.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import java.time.LocalDate;

@RelationshipProperties
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpousalRelation {

    // Internal ID for the relationship entity
    @RelationshipId
    private Long id; 

    // Target node of the relationship (The Spouse)
    @TargetNode
    @NonNull
    private Person spouse; 

    // --- RELATIONSHIP PROPERTIES (Metadata) ---
    
    // Date the marriage/partnership began
    @Property("startDate") 
    private LocalDate startDate; 

    // Date the partnership ended (e.g., Date of Divorce or Death of spouse)
    @Property("endDate") 
    private LocalDate endDate; 

    // Current status (e.g., Married, Divorced, Separated, Widowed)
    @Property("status") 
    @NonNull
    private SpousalStatus status; // Assuming SpousalStatus is an Enum
    
    // Type of partnership (e.g., Marriage, CommonLaw, RegisteredPartnership)
    @Property("partnershipType") 
    private String partnershipType; 
}