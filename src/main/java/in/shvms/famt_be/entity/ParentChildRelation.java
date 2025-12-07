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
public class ParentChildRelation {

    // Internal ID for the relationship entity
    @RelationshipId
    private Long id; 

    // Start node of the relationship (The Parent)
    @TargetNode
    @NonNull
    private Person parent; 

    // --- RELATIONSHIP PROPERTIES (Metadata) ---
    
    // Specifies if the relationship is Biological, Adoptive, Step, Guardian, etc.
    @Property("relationshipType") 
    @NonNull
    private ParentChildType relationshipType; // Assuming ParentChildType is an Enum (BIOLOGICAL, ADOPTIVE, etc.)

    // Confidence score (0.0 to 1.0) based on source documentation
    @Property("confidenceScore") 
    private Double confidenceScore; 

    // Optional: Date the parent-child relationship was established (e.g., Adoption Date)
    @Property("startDate") 
    private LocalDate startDate; 
}