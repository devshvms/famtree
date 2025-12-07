package in.shvms.famt_be.repo;

import in.shvms.famt_be.entity.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepo extends MongoRepository<AuditLog, String> {

    List<AuditLog> findAllByTenantId(String tenantId);

    List<AuditLog> findByTenantIdAndEntityType(String tenantId, String entityType);
}