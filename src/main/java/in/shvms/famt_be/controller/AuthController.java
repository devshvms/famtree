package in.shvms.famt_be.controller;

import in.shvms.famt_be.config.JwtUtil;
import in.shvms.famt_be.dto.AuthRequest;
import in.shvms.famt_be.dto.TenantCreationRequest;
import in.shvms.famt_be.dto.UserCreationRequest;
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

/**
 * Authentication and Tenant Management Controller
 * Handles login, tenant creation, and initial user setup
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and tenant management endpoints")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TenantService tenantService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * Admin/User Login endpoint
     * Authenticates user and returns JWT token with tenant and user information
     */
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and get JWT token")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(), 
                            authRequest.getPassword())
            );

            // Get user details
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            
            // Generate JWT token
            String jwt = jwtUtil.generateToken(userDetails);

            // Build response with additional user info
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
                    .body(Map.of("error", "User account is disabled"));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }

    /**
     * Create a new tenant (family group)
     * This endpoint should be protected or have additional validation in production
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
     * Register first admin user for a tenant
     * This creates the initial TENANT_ADMIN user for a new tenant
     */
    @PostMapping("/register-admin")
    @Operation(summary = "Register admin", description = "Create first admin user for a tenant")
    public ResponseEntity<?> registerAdmin(@RequestBody UserCreationRequest request) {
        try {
            // Validate input
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

            // Create admin user with TENANT_ADMIN role
            User adminUser = userService.createUser(
                    request.getTenantId(),
                    "system", // Acting user for initial setup
                    request.getUsername(),
                    request.getPassword(),
                    Set.of(UserRole.TENANT_ADMIN)
            );

            // Remove password from response
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
     * Validate JWT token
     * Useful for frontend to check if token is still valid
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