package in.shvms.famt_be.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "tenants")
public class Tenant {

    @Id
    private String id;
    private String name;

}
