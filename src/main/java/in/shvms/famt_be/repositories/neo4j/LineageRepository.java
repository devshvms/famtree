package in.shvms.famt_be.repositories.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import in.shvms.famt_be.entity.Lineage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LineageRepository extends Neo4jRepository<Lineage, UUID> {

    Optional<Lineage> findByTenantIdAndId(String tenantId, UUID id);

    List<Lineage> findAllByTenantId(String tenantId);
}
