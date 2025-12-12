package in.shvms.famt_be.repositories.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import in.shvms.famt_be.entity.Person;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonRepository extends Neo4jRepository<Person, UUID> {

    Optional<Person> findByTenantIdAndId(String tenantId, UUID personId);

    List<Person> findAllByTenantIdAndLineageId(String tenantId, UUID lineageId);

    List<Person> findByTenantIdAndFirstNameContaining(String tenantId, String name);

    List<Person> findAllByTenantId(String tenantId);
}
