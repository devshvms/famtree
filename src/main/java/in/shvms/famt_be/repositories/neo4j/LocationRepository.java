package in.shvms.famt_be.repositories.neo4j;

import in.shvms.famt_be.entities.Location;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends Neo4jRepository<Location, String> {
}
