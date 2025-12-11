package in.shvms.famt_be.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Set;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private String id;

    @NonNull
    private String tenantId;

    @NonNull
    @Indexed(unique = true)
    private String username; // This will be email

    @NonNull
    private String password; // Initial random PIN, later user-set password

    @NonNull
    private Set<UserRole> roles;

    private boolean isActive = false; // False until email verified and password set

    // New fields for email verification and first-time login
    private boolean emailVerified = false;
    
    private boolean firstTimeLogin = true; // Flag for first-time login
    
    private String verificationToken; // OTP or verification token
    
    private LocalDateTime tokenExpiry; // OTP expiry time
    
    private LocalDateTime lastLoginAt;
    
    private LocalDateTime passwordLastChangedAt;
    
    private int failedLoginAttempts = 0;
    
    private LocalDateTime accountLockedUntil;
}