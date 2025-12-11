package in.shvms.famt_be.repositories.mongo;

import in.shvms.famt_be.entity.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    // Add custom query methods here if needed
}
