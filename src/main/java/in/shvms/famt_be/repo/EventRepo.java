package in.shvms.famt_be.repo;

import in.shvms.famt_be.entity.Event;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepo extends Neo4jRepository<Event, UUID> {

    Optional<Event> findByTenantIdAndId(String tenantId, UUID id);

    List<Event> findAllByTenantId(String tenantId);

    List<Event> findByTenantIdAndEventType(String tenantId, String eventType);
}