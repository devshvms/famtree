package in.shvms.famt.famtbe.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import java.util.UUID;

@Node("Group")
@Data
@NoArgsConstructor
public class Group {

    @Id
    @GeneratedValue
    private UUID id;

    @Property("tenantId")
    @NonNull
    private String tenantId;

    private String name;
}
