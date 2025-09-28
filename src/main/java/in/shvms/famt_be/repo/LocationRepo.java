package in.shvms.famt_be.repo;

import in.shvms.famt_be.entity.Location;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LocationRepo extends MongoRepository<Location, String> {
}
