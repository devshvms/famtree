package in.shvms.famt_be.repo;

import in.shvms.famt_be.entity.Human;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface HumanRepo extends MongoRepository<Human, String> {
}
