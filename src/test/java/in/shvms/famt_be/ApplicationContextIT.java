package in.shvms.famt_be;

import in.shvms.famt_be.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the Spring context wires up against real datastores. This is the
 * check the old {@code FamtBeApplicationTests.contextLoads()} was trying to
 * make, except it could never pass without hand-started databases.
 */
class ApplicationContextIT extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
        assertThat(context.containsBean("personService")).isTrue();
        assertThat(context.containsBean("locationService")).isTrue();
        assertThat(context.containsBean("securityFilterChain")).isTrue();
    }
}
