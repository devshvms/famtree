package in.shvms.famt_be.repo;

import in.shvms.famt_be.entity.Location;
import in.shvms.famt_be.entity.LocationType;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;  

@Repository
public interface LocationRepo extends Neo4jRepository<Location, String> {
    Optional<Location> findByLocationNameAndLocationType(String locationName, LocationType locationType);
}
