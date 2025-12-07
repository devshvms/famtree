package in.shvms.famt_be.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import java.util.UUID;

@Node("Lineage")
@Data
@NoArgsConstructor
public class Lineage {

    @Id @GeneratedValue
    private UUID id;

    @Property("tenantId")
    @NonNull
    private String tenantId;

    @Property("name")
    @NonNull
    private String name;
}