package in.shvms.famt_be.repo;

import in.shvms.famt_be.entity.Person;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonRepo extends Neo4jRepository<Person, UUID> {

    Optional<Person> findByTenantIdAndId(String tenantId, UUID id);

    List<Person> findAllByTenantId(String tenantId);

    @Query("MATCH (p:Person) WHERE p.tenantId = $tenantId AND (toLower(p.firstName) CONTAINS toLower($name) OR toLower(p.lastName) CONTAINS toLower($name)) RETURN p")
    List<Person> findByTenantIdAndNameContainingIgnoreCase(String tenantId, String name);

    @Query("MATCH (p:Person)-[:MEMBER_OF]->(l:Lineage) WHERE p.tenantId = $tenantId AND l.id = $lineageId RETURN p")
    List<Person> findAllByTenantIdAndLineageId(String tenantId, UUID lineageId);
}