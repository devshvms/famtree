package in.shvms.famt_be.repositories.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import in.shvms.famt_be.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);

    Optional<User> findByTenantIdAndId(String tenantId, String id);

    List<User> findAllByTenantId(String tenantId);
}
