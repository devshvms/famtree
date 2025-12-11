package in.shvms.famt_be.repositories.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import in.shvms.famt_be.entity.Tenant;

public interface TenantRepository extends MongoRepository<Tenant, String> {
}
