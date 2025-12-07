package in.shvms.famt_be.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "auditLogs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    private String id;

    @NonNull
    @Indexed
    private String tenantId;

    @NonNull
    private LocalDateTime timestamp;

    @NonNull
    private String userId;

    @NonNull
    private String action; // e.g., 'CREATE', 'UPDATE', 'DELETE'

    @NonNull
    private String entityType; // e.g., 'Person', 'Location', 'Lineage'

    @NonNull
    private String entityId; // ID of the entity that was acted upon

    private Map<String, Object> details; // Stores old and new values, specific fields modified, etc.

    private String ipAddress; // IP address from where the action originated
}