package in.shvms.famt_be.config;

import in.shvms.famt_be.entity.User;
import in.shvms.famt_be.entity.UserRole;
import in.shvms.famt_be.service.CustomUserDetailsService.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit test - no Docker, no Spring context. Keeps {@code mvn test} useful
 * on a machine with no container runtime.
 */
class JwtUtilTest {

    private static final String SECRET = "unit-test-signing-key-that-is-definitely-long-enough-for-hs256";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationTime", 86_400_000L);
    }

    private CustomUserDetails userDetails() {
        User user = new User(
                "user-123", "tenant-abc", "member@example.com", "$2a$10$hash",
                Set.of(UserRole.STANDARD_USER),
                true, true, false, null, null, null, null, 0, null);
        return new CustomUserDetails(user);
    }

    @Test
    void tokenCarriesTenantAndUserClaims() {
        String token = jwtUtil.generateToken(userDetails());

        assertThat(jwtUtil.extractUsername(token)).isEqualTo("member@example.com");
        assertThat(jwtUtil.extractTenantId(token)).isEqualTo("tenant-abc");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo("user-123");
    }

    @Test
    void freshTokenValidates() {
        String token = jwtUtil.generateToken(userDetails());

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.validateToken(token, userDetails())).isTrue();
    }

    @Test
    void tokenExpiryHonoursConfiguredLifetime() {
        String token = jwtUtil.generateToken(userDetails());

        assertThat(jwtUtil.extractExpiration(token)).isAfter(new Date());
    }

    @Test
    void alreadyExpiredTokenIsRejected() {
        ReflectionTestUtils.setField(jwtUtil, "expirationTime", -1_000L);
        String token = jwtUtil.generateToken(userDetails());

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String foreign = jwtUtil.generateToken(userDetails());

        JwtUtil other = new JwtUtil();
        ReflectionTestUtils.setField(other, "secret", "a-completely-different-signing-key-of-sufficient-length");
        ReflectionTestUtils.setField(other, "expirationTime", 86_400_000L);

        assertThat(other.validateToken(foreign)).isFalse();
    }

    @Test
    void garbageIsRejectedRatherThanThrowing() {
        assertThat(jwtUtil.validateToken("not-a-jwt")).isFalse();
    }
}
