package in.shvms.famt_be.repo;

import in.shvms.famt_be.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByTenantIdAndId(String tenantId, String id);

    List<User> findAllByTenantId(String tenantId);
}