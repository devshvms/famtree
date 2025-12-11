package in.shvms.famt_be.service;

import in.shvms.famt_be.entity.AuditLog;
import in.shvms.famt_be.entity.User;
import in.shvms.famt_be.entity.UserRole;
import in.shvms.famt_be.repositories.mongo.AuditLogRepository;
import in.shvms.famt_be.repositories.mongo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepo;
    private final AuditLogRepository auditLogRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    private static final SecureRandom random = new SecureRandom();
    private static final int OTP_VALIDITY_MINUTES = 10;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int ACCOUNT_LOCK_MINUTES = 30;

    private void logAudit(String tenantId, String userId, String action, String entityType, String entityId, Map<String, Object> details) {
        AuditLog auditLog = new AuditLog(
                null,
                tenantId,
                LocalDateTime.now(),
                userId,
                action,
                entityType,
                entityId,
                details,
                null
        );
        auditLogRepo.save(auditLog);
    }

    /**
     * Generate a random 6-digit PIN
     */
    private String generateRandomPin() {
        return String.format("%06d", random.nextInt(1000000));
    }

    /**
     * Create TENANT_ADMIN user (existing functionality)
     */
    public User createUser(String tenantId, String actingUserId, String username, String password, Set<UserRole> roles) {
        if (userRepo.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        User newUser = new User(
                null,
                tenantId,
                username,
                passwordEncoder.encode(password),
                roles,
                true, // Admin is active immediately
                true, // Email verified
                false, // Not first time login
                null,
                null,
                null,
                null,
                0,
                null
        );
        
        User savedUser = userRepo.save(newUser);
        logAudit(tenantId, actingUserId, "CREATE", "User", savedUser.getId(), 
                Map.of("newUser", savedUser.getUsername(), "roles", savedUser.getRoles()));
        return savedUser;
    }

    /**
     * Create STANDARD_USER with random PIN (NEW)
     * Only TENANT_ADMIN can call this
     */
    public User createStandardUser(String tenantId, String actingUserId, String email, String displayName) {
        if (userRepo.findByUsername(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        // Generate random PIN
        String randomPin = generateRandomPin();
        
        User newUser = new User(
                null,
                tenantId,
                email, // username is email
                passwordEncoder.encode(randomPin), // Encode the PIN
                Set.of(UserRole.STANDARD_USER),
                false, // Not active until email verified and password set
                false, // Email not verified
                true, // First time login
                null, // No verification token yet
                null, // No token expiry yet
                null, // Never logged in
                null, // Password not changed yet
                0, // No failed attempts
                null // Not locked
        );
        
        User savedUser = userRepo.save(newUser);
        
        // Send welcome email with temporary PIN
        try {
            emailService.sendWelcomeEmail(email, displayName, randomPin);
        } catch (Exception e) {
            // If email fails, delete the user and throw exception
            userRepo.delete(savedUser);
            throw new RuntimeException("Failed to send welcome email. User creation rolled back.", e);
        }
        
        logAudit(tenantId, actingUserId, "CREATE_STANDARD_USER", "User", savedUser.getId(), 
                Map.of("newUser", savedUser.getUsername(), "roles", savedUser.getRoles()));
        
        return savedUser;
    }

    /**
     * First time login with PIN - generates OTP for email verification
     */
    public Map<String, Object> initiateFirstTimeLogin(String email, String pin) {
        User user = userRepo.findByUsername(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        
        // Check if account is locked
        if (user.getAccountLockedUntil() != null && 
            user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
            throw new SecurityException("Account is locked. Please try again later.");
        }
        
        // Verify PIN
        if (!passwordEncoder.matches(pin, user.getPassword())) {
            handleFailedLogin(user);
            throw new IllegalArgumentException("Invalid credentials");
        }
        
        // Check if this is truly first time login
        if (!user.isFirstTimeLogin()) {
            throw new IllegalStateException("This account has already been activated. Please use regular login.");
        }
        
        // Generate OTP
        String otp = emailService.generateOTP();
        user.setVerificationToken(passwordEncoder.encode(otp));
        user.setTokenExpiry(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));
        user.setFailedLoginAttempts(0); // Reset failed attempts on successful PIN verification
        userRepo.save(user);
        
        // Send OTP email
        emailService.sendOTPEmail(email, otp, email);
        
        logAudit(user.getTenantId(), user.getId(), "INITIATE_FIRST_LOGIN", "User", user.getId(), 
                Map.of("email", email));
        
        return Map.of(
                "message", "OTP sent to your email",
                "email", email,
                "expiresIn", OTP_VALIDITY_MINUTES + " minutes"
        );
    }

    /**
     * Verify OTP during first time login
     */
    public Map<String, Object> verifyOTP(String email, String otp) {
        User user = userRepo.findByUsername(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Verify token exists and not expired
        if (user.getVerificationToken() == null || user.getTokenExpiry() == null) {
            throw new IllegalStateException("No verification in progress. Please request a new OTP.");
        }
        
        if (LocalDateTime.now().isAfter(user.getTokenExpiry())) {
            throw new IllegalStateException("OTP has expired. Please request a new one.");
        }
        
        // Verify OTP
        if (!passwordEncoder.matches(otp, user.getVerificationToken())) {
            throw new IllegalArgumentException("Invalid OTP");
        }
        
        // Mark email as verified
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setTokenExpiry(null);
        userRepo.save(user);
        
        logAudit(user.getTenantId(), user.getId(), "EMAIL_VERIFIED", "User", user.getId(), 
                Map.of("email", email));
        
        return Map.of(
                "message", "Email verified successfully. Please set your password.",
                "email", email
        );
    }

    /**
     * Resend OTP if expired or not received
     */
    public Map<String, Object> resendOTP(String email) {
        User user = userRepo.findByUsername(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (!user.isFirstTimeLogin()) {
            throw new IllegalStateException("This account has already been activated.");
        }
        
        if (user.isEmailVerified()) {
            throw new IllegalStateException("Email already verified. Please set your password.");
        }
        
        // Generate new OTP
        String otp = emailService.generateOTP();
        user.setVerificationToken(passwordEncoder.encode(otp));
        user.setTokenExpiry(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));
        userRepo.save(user);
        
        // Send OTP email
        emailService.sendOTPEmail(email, otp, email);
        
        logAudit(user.getTenantId(), user.getId(), "RESEND_OTP", "User", user.getId(), 
                Map.of("email", email));
        
        return Map.of(
                "message", "New OTP sent to your email",
                "email", email,
                "expiresIn", OTP_VALIDITY_MINUTES + " minutes"
        );
    }

    /**
     * Set new password after email verification (first time login)
     */
    public Map<String, Object> setNewPassword(String email, String newPassword) {
        User user = userRepo.findByUsername(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (!user.isFirstTimeLogin()) {
            throw new IllegalStateException("This account has already been activated.");
        }
        
        if (!user.isEmailVerified()) {
            throw new IllegalStateException("Please verify your email first.");
        }
        
        // Validate password strength
        validatePasswordStrength(newPassword);
        
        // Set new password and activate account
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFirstTimeLogin(false);
        user.setActive(true);
        user.setPasswordLastChangedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());
        userRepo.save(user);
        
        // Send confirmation email
        emailService.sendPasswordResetConfirmation(email, email);
        
        logAudit(user.getTenantId(), user.getId(), "ACCOUNT_ACTIVATED", "User", user.getId(), 
                Map.of("email", email));
        
        return Map.of(
                "message", "Password set successfully. Your account is now active.",
                "email", email
        );
    }

    /**
     * Handle failed login attempts and account locking
     */
    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(ACCOUNT_LOCK_MINUTES));
            logAudit(user.getTenantId(), user.getId(), "ACCOUNT_LOCKED", "User", user.getId(), 
                    Map.of("reason", "Too many failed login attempts"));
        }
        
        userRepo.save(user);
    }

    /**
     * Validate password strength
     */
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);
        
        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            throw new IllegalArgumentException(
                "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
            );
        }
    }

    // Existing methods continue...
    
    public Optional<User> getUserById(String tenantId, String id) {
        return userRepo.findByTenantIdAndId(tenantId, id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    public List<User> getAllUsers(String tenantId) {
        return userRepo.findAllByTenantId(tenantId);
    }

    public User updateUser(String tenantId, String actingUserId, String id, Set<UserRole> newRoles, Boolean isActive) {
        return userRepo.findByTenantIdAndId(tenantId, id).map(user -> {
            Map<String, Object> oldUser = Map.of("oldRoles", user.getRoles(), "oldIsActive", user.isActive());
            user.setRoles(newRoles);
            user.setActive(isActive != null ? isActive : user.isActive());
            User savedUser = userRepo.save(user);
            logAudit(tenantId, actingUserId, "UPDATE", "User", savedUser.getId(), 
                    Map.of("oldValues", oldUser, "newValues", Map.of("roles", savedUser.getRoles(), "isActive", savedUser.isActive())));
            return savedUser;
        }).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void deleteUser(String tenantId, String actingUserId, String id) {
        User userToDelete = userRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepo.delete(userToDelete);
        logAudit(tenantId, actingUserId, "DELETE", "User", id, Map.of("deletedUser", userToDelete.getUsername()));
    }

    public boolean authenticateUser(String username, String password) {
        Optional<User> userOptional = userRepo.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            
            // Check if account is locked
            if (user.getAccountLockedUntil() != null && 
                user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
                return false;
            }
            
            // Check if account is active
            if (!user.isActive()) {
                return false;
            }
            
            boolean matches = passwordEncoder.matches(password, user.getPassword());
            
            if (matches) {
                // Reset failed attempts on successful login
                user.setFailedLoginAttempts(0);
                user.setLastLoginAt(LocalDateTime.now());
                userRepo.save(user);
            } else {
                handleFailedLogin(user);
            }
            
            return matches;
        }
        return false;
    }
}