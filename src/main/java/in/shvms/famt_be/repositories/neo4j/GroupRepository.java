package in.shvms.famt_be.repositories.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import in.shvms.famt_be.entity.Group;

import java.util.UUID;

@Repository
public interface GroupRepository extends Neo4jRepository<Group, UUID> {
}
