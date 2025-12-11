package in.shvms.famt_be.repositories.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import in.shvms.famt_be.entity.Event;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends Neo4jRepository<Event, UUID> {

    Optional<Event> findByTenantIdAndId(String tenantId, UUID eventId);

    List<Event> findByTenantIdAndEventType(String tenantId, String eventType);

    List<Event> findAllByTenantId(String tenantId);
}
