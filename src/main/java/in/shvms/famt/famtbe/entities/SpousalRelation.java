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
