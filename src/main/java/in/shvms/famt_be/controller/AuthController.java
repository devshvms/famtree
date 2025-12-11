package in.shvms.famt_be.controller;

import in.shvms.famt_be.config.JwtUtil;
import in.shvms.famt_be.config.SecurityContextHelper;
import in.shvms.famt_be.dto.*;
import in.shvms.famt_be.dto.requests.CreateStandardUserRequest;
import in.shvms.famt_be.dto.requests.FirstLoginRequest;
import in.shvms.famt_be.dto.requests.OTPVerificationRequest;
import in.shvms.famt_be.dto.requests.ResendOTPRequest;
import in.shvms.famt_be.dto.requests.SetPasswordRequest;
import in.shvms.famt_be.entity.Tenant;
import in.shvms.famt_be.entity.User;
import in.shvms.famt_be.entity.UserRole;
import in.shvms.famt_be.service.CustomUserDetailsService.CustomUserDetails;
import in.shvms.famt_be.service.TenantService;
import in.shvms.famt_be.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and tenant management endpoints")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TenantService tenantService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final SecurityContextHelper securityContext;

    /**
     * Regular login for TENANT_ADMIN and activated STANDARD_USER
     */
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and get JWT token")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(), 
                            authRequest.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String jwt = jwtUtil.generateToken(userDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("username", userDetails.getUsername());
            
            if (userDetails instanceof CustomUserDetails) {
                CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
                response.put("tenantId", customUserDetails.getTenantId());
                response.put("userId", customUserDetails.getUserId());
                response.put("roles", customUserDetails.getAuthorities());
            }

            return ResponseEntity.ok(response);

        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "User account is disabled or not activated"));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }

    /**
     * First-time login with PIN (STANDARD_USER only)
     * This initiates the email verification process
     */
    @PostMapping("/first-login")
    @Operation(summary = "First time login", description = "Login with temporary PIN to start activation process")
    public ResponseEntity<?> firstTimeLogin(@RequestBody FirstLoginRequest request) {
        try {
            Map<String, Object> response = userService.initiateFirstTimeLogin(
                    request.getEmail(), 
                    request.getPin()
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "First login failed: " + e.getMessage()));
        }
    }

    /**
     * Verify OTP sent to email
     */
    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP", description = "Verify the OTP sent to email during first login")
    public ResponseEntity<?> verifyOTP(@RequestBody OTPVerificationRequest request) {
        try {
            Map<String, Object> response = userService.verifyOTP(
                    request.getEmail(), 
                    request.getOtp()
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "OTP verification failed: " + e.getMessage()));
        }
    }

    /**
     * Resend OTP if expired or not received
     */
    @PostMapping("/resend-otp")
    @Operation(summary = "Resend OTP", description = "Request a new OTP")
    public ResponseEntity<?> resendOTP(@RequestBody ResendOTPRequest request) {
        try {
            Map<String, Object> response = userService.resendOTP(request.getEmail());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Resend OTP failed: " + e.getMessage()));
        }
    }

    /**
     * Set new password after OTP verification
     */
    @PostMapping("/set-password")
    @Operation(summary = "Set password", description = "Set new password after email verification")
    public ResponseEntity<?> setPassword(@RequestBody SetPasswordRequest request) {
        try {
            Map<String, Object> response = userService.setNewPassword(
                    request.getEmail(), 
                    request.getNewPassword()
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Password setup failed: " + e.getMessage()));
        }
    }

    /**
     * Create a new tenant (existing)
     */
    @PostMapping("/tenants")
    @Operation(summary = "Create tenant", description = "Create a new tenant (family group)")
    public ResponseEntity<?> createTenant(@RequestBody TenantCreationRequest request) {
        try {
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Tenant name is required"));
            }

            Tenant tenant = tenantService.createTenant(request.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(tenant);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create tenant: " + e.getMessage()));
        }
    }

    /**
     * Register first admin user for a tenant (existing)
     */
    @PostMapping("/register-admin")
    @Operation(summary = "Register admin", description = "Create first admin user for a tenant")
    public ResponseEntity<?> registerAdmin(@RequestBody UserCreationRequest request) {
        try {
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Username is required"));
            }
            if (request.getPassword() == null || request.getPassword().length() < 8) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Password must be at least 8 characters"));
            }
            if (request.getTenantId() == null || request.getTenantId().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Tenant ID is required"));
            }

            User adminUser = userService.createUser(
                    request.getTenantId(),
                    "system",
                    request.getUsername(),
                    request.getPassword(),
                    Set.of(UserRole.TENANT_ADMIN)
            );

            adminUser.setPassword(null);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(adminUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to register admin: " + e.getMessage()));
        }
    }

    /**
     * TENANT_ADMIN creates a STANDARD_USER with random PIN (NEW)
     */
    @PostMapping("/create-standard-user")
    @Operation(summary = "Create standard user", description = "Admin creates standard user with random PIN")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ResponseEntity<?> createStandardUser(@RequestBody CreateStandardUserRequest request) {
        try {
            String tenantId = securityContext.getCurrentTenantId();
            String actingUserId = securityContext.getCurrentUserId();
            
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email is required"));
            }
            
            if (request.getDisplayName() == null || request.getDisplayName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Display name is required"));
            }
            
            User newUser = userService.createStandardUser(
                    tenantId,
                    actingUserId,
                    request.getEmail(),
                    request.getDisplayName()
            );
            
            // Remove password from response
            newUser.setPassword(null);
            newUser.setVerificationToken(null);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "User created successfully. Welcome email with temporary PIN sent.",
                    "user", newUser
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create user: " + e.getMessage()));
        }
    }

    /**
     * Validate JWT token (existing)
     */
    @GetMapping("/validate")
    @Operation(summary = "Validate token", description = "Check if JWT token is valid")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("valid", false, "error", "Invalid authorization header"));
            }

            String token = authHeader.substring(7);
            boolean isValid = jwtUtil.validateToken(token);

            if (isValid) {
                String username = jwtUtil.extractUsername(token);
                String tenantId = jwtUtil.extractTenantId(token);
                
                return ResponseEntity.ok(Map.of(
                        "valid", true,
                        "username", username,
                        "tenantId", tenantId
                ));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("valid", false, "error", "Token is invalid or expired"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", e.getMessage()));
        }
    }
}