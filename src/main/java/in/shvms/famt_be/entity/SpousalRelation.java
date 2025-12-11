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

    @RelationshipId
    private Long id;

    @TargetNode
    @NonNull
    private Person spouse;

    @Property("startDate")
    private LocalDate startDate;

    @Property("endDate")
    private LocalDate endDate;

    @Property("status")
    @NonNull
    private SpousalStatus status;

    @Property("partnershipType")
    private String partnershipType;
}