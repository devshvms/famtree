package in.shvms.famt_be.repositories.neo4j;

import in.shvms.famt_be.entities.Event;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EventRepository extends Neo4jRepository<Event, UUID> {
}
