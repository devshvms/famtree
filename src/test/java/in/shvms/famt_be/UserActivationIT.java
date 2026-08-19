package in.shvms.famt_be;

import in.shvms.famt_be.entity.User;
import in.shvms.famt_be.repositories.mongo.UserRepository;
import in.shvms.famt_be.service.EmailService;
import in.shvms.famt_be.service.UserService;
import in.shvms.famt_be.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

/**
 * Walks the documented standard-user onboarding journey against real MongoDB:
 * admin creates the user, a PIN is mailed, PIN unlocks an OTP, OTP unlocks
 * password setup, and only then does the account go active.
 *
 * <p>{@link EmailService} is spied so the PIN and OTP can be captured without
 * an SMTP server; everything else is the real code path.
 */
class UserActivationIT extends AbstractIntegrationTest {

    private static final String TENANT = "tenant-activation-it";
    private static final String ADMIN = "admin-it";
    private static final String EMAIL = "new.member@example.com";

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepo;

    @MockitoSpyBean
    private EmailService emailService;

    @BeforeEach
    void reset() {
        userRepo.findByUsername(EMAIL).ifPresent(userRepo::delete);
        doNothing().when(emailService).sendWelcomeEmail(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        doNothing().when(emailService).sendOTPEmail(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        doNothing().when(emailService).sendPasswordResetConfirmation(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    private String createUserAndCapturePin() {
        userService.createStandardUser(TENANT, ADMIN, EMAIL, "New Member");

        ArgumentCaptor<String> pin = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendWelcomeEmail(
                org.mockito.ArgumentMatchers.eq(EMAIL),
                org.mockito.ArgumentMatchers.eq("New Member"),
                pin.capture());
        return pin.getValue();
    }

    private String startLoginAndCaptureOtp(String pin) {
        userService.initiateFirstTimeLogin(EMAIL, pin);

        ArgumentCaptor<String> otp = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOTPEmail(
                org.mockito.ArgumentMatchers.eq(EMAIL),
                otp.capture(),
                org.mockito.ArgumentMatchers.eq(EMAIL));
        return otp.getValue();
    }

    @Test
    void newUserStartsInactiveWithAHashedPin() {
        String pin = createUserAndCapturePin();

        assertThat(pin).hasSize(6).containsOnlyDigits();

        User stored = userRepo.findByUsername(EMAIL).orElseThrow();
        assertThat(stored.isActive()).isFalse();
        assertThat(stored.isEmailVerified()).isFalse();
        assertThat(stored.isFirstTimeLogin()).isTrue();
        assertThat(stored.getPassword()).isNotEqualTo(pin).startsWith("$2");
    }

    @Test
    void completesTheFullActivationJourney() {
        String pin = createUserAndCapturePin();
        String otp = startLoginAndCaptureOtp(pin);

        assertThat(otp).hasSize(6).containsOnlyDigits();

        userService.verifyOTP(EMAIL, otp);
        assertThat(userRepo.findByUsername(EMAIL).orElseThrow().isEmailVerified()).isTrue();

        userService.setNewPassword(EMAIL, "MySecure123!Password");

        User activated = userRepo.findByUsername(EMAIL).orElseThrow();
        assertThat(activated.isActive()).isTrue();
        assertThat(activated.isFirstTimeLogin()).isFalse();
        assertThat(activated.getPasswordLastChangedAt()).isNotNull();
    }

    @Test
    void rejectsAWrongPin() {
        createUserAndCapturePin();

        assertThatThrownBy(() -> userService.initiateFirstTimeLogin(EMAIL, "000000"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refusesPasswordSetupBeforeTheEmailIsVerified() {
        String pin = createUserAndCapturePin();
        startLoginAndCaptureOtp(pin);

        assertThatThrownBy(() -> userService.setNewPassword(EMAIL, "MySecure123!Password"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("verify your email");
    }

    @Test
    void enforcesPasswordStrength() {
        String pin = createUserAndCapturePin();
        String otp = startLoginAndCaptureOtp(pin);
        userService.verifyOTP(EMAIL, otp);

        assertThatThrownBy(() -> userService.setNewPassword(EMAIL, "weakpass"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDuplicateEmail() {
        createUserAndCapturePin();

        assertThatThrownBy(() -> userService.createStandardUser(TENANT, ADMIN, EMAIL, "Impostor"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }
}
