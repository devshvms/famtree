package in.shvms.famt.famtbe.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.UUID;

@Node("Lineage")
@Data
@NoArgsConstructor
public class Lineage {

    @Id
    @GeneratedValue
    private UUID id;
}
