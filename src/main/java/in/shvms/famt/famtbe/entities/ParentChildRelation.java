package in.shvms.famt.famtbe.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDate;

@RelationshipProperties
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParentChildRelation {

    @RelationshipId
    private Long id;

    @TargetNode
    @NonNull
    private Person parent;

    @Property("relationshipType")
    @NonNull
    private ParentChildType relationshipType;

    @Property("confidenceScore")
    private Double confidenceScore;

    @Property("startDate")
    private LocalDate startDate;
}
