package in.shvms.famt_be.repositories.mongo;

import in.shvms.famt_be.entities.Tenant;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TenantRepository extends MongoRepository<Tenant, String> {
}
