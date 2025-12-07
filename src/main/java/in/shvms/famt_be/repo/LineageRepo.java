package in.shvms.famt_be.repo;

import in.shvms.famt_be.entity.Lineage;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LineageRepo extends Neo4jRepository<Lineage, UUID> {

    Optional<Lineage> findByTenantIdAndId(String tenantId, UUID id);

    List<Lineage> findAllByTenantId(String tenantId);
}