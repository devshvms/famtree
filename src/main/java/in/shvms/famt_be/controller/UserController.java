package in.shvms.famt_be.controller;

import in.shvms.famt_be.entity.User;
import in.shvms.famt_be.entity.UserRole;
import in.shvms.famt_be.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Placeholder for userId until security is fully implemented
    private String getActingUserId() {
        return "system"; // Replace with actual user ID from security context
    }

    @PostMapping("/register")
    public ResponseEntity<User> registerUser(
            @RequestBody Map<String, Object> payload) {
        try {
            String username = (String) payload.get("username");
            String password = (String) payload.get("password");
            String tenantId = (String) payload.get("tenantId");
            
            @SuppressWarnings("unchecked") // Cast is safe if roles are consistently passed as Strings
            Set<UserRole> roles = ((List<String>) payload.get("roles")).stream()
                    .map(UserRole::valueOf)
                    .collect(Collectors.toSet());

            User newUser = userService.createUser(tenantId, getActingUserId(), username, password, roles);
            return new ResponseEntity<>(newUser, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/authenticate")
    public ResponseEntity<String> authenticateUser(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username == null || password == null) {
            return new ResponseEntity<>("Username and password are required", HttpStatus.BAD_REQUEST);
        }

        if (userService.authenticateUser(username, password)) {
            // In a real application, generate and return a JWT token here
            // For now, returning a placeholder string
            return ResponseEntity.ok("Authentication successful! JWT Token Placeholder");
        } else {
            return new ResponseEntity<>("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/tenants/{tenantId}")
    public ResponseEntity<List<User>> getAllUsers(@PathVariable String tenantId) {
        List<User> users = userService.getAllUsers(tenantId);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/tenants/{tenantId}/{id}")
    public ResponseEntity<User> getUserById(
            @PathVariable String tenantId,
            @PathVariable String id) {
        return userService.getUserById(tenantId, id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/tenants/{tenantId}/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable String tenantId,
            @PathVariable String id,
            @RequestBody Map<String, Object> payload) {
        try {
            @SuppressWarnings("unchecked") // Cast is safe if roles are consistently passed as Strings
            Set<UserRole> newRoles = ((List<String>) payload.get("roles")).stream()
                    .map(UserRole::valueOf)
                    .collect(Collectors.toSet());
            Boolean isActive = (Boolean) payload.get("isActive");

            User updatedUser = userService.updateUser(tenantId, getActingUserId(), id, newRoles, isActive);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/tenants/{tenantId}/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable String tenantId,
            @PathVariable String id) {
        try {
            userService.deleteUser(tenantId, getActingUserId(), id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}