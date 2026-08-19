package in.shvms.famt_be.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests.
 *
 * <p>This application's behaviour lives in Cypher traversals and Mongo
 * documents, so tests run against real databases rather than mocks. The
 * containers are {@code static} and deliberately never stopped: Testcontainers'
 * Ryuk sidecar reaps them when the JVM exits, so every {@code *IT} in the module
 * shares one Neo4j and one Mongo instead of paying startup cost per class.
 *
 * <p>Requires a running Docker daemon. Extend this only from {@code *IT}
 * classes so {@code mvn test} stays runnable without Docker.
 */
@SpringBootTest
@Testcontainers
public abstract class AbstractIntegrationTest {

    protected static final String NEO4J_PASSWORD = "test-password";

    static final Neo4jContainer<?> NEO4J = new Neo4jContainer<>("neo4j:5-community")
            .withAdminPassword(NEO4J_PASSWORD)
            .withReuse(true);

    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7").withReuse(true);

    static {
        NEO4J.start();
        MONGO.start();
    }

    @DynamicPropertySource
    static void datastoreProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.neo4j.uri", NEO4J::getBoltUrl);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", () -> NEO4J_PASSWORD);
        registry.add("spring.data.mongodb.uri", () -> MONGO.getReplicaSetUrl("famt-test"));
    }
}
