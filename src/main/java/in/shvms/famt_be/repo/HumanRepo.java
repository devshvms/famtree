package in.shvms.famt_be.repo;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import in.shvms.famt_be.entity.Human;

public interface HumanRepo extends Neo4jRepository<Human, String> {
}
