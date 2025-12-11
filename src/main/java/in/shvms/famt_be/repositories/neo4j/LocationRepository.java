package in.shvms.famt_be.repositories.neo4j;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import in.shvms.famt_be.entity.Location;
import in.shvms.famt_be.entity.LocationType;

@Repository
public interface LocationRepository extends Neo4jRepository<Location, String> {

    List<Location> findAllByLocationType(LocationType locationType);
}
