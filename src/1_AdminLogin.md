# Admin Login Implementation Guide

## Overview
This guide explains the complete admin login flow for your family tree multi-tenant application.

## Architecture Changes

### 1. **CustomUserDetailsService**
- Implements Spring Security's `UserDetailsService`
- Loads users from MongoDB
- Wraps User entity in `CustomUserDetails` for Spring Security
- Provides tenant and user ID access

### 2. **Enhanced JwtUtil**
- Generates JWT tokens with tenant ID and user ID claims
- Extracts tenant and user information from tokens
- Validates tokens with proper user details

### 3. **Enhanced JwtAuthenticationFilter**
- Validates JWT tokens on each request
- Sets Spring Security authentication context
- Proper error handling for expired/invalid tokens

### 4. **Enhanced AuthController**
- `/api/auth/login` - User authentication endpoint
- `/api/auth/tenants` - Create new tenant
- `/api/auth/register-admin` - Register first admin for tenant
- `/api/auth/validate` - Validate JWT token

### 5. **SecurityContextHelper**
- Utility to extract current user information
- Methods to get tenant ID, user ID, roles
- Used in controllers for tenant isolation

## Setup Instructions

### 1. Database Setup

**MongoDB Collections:**
```javascript
// Create indexes for better performance
db.users.createIndex({ "username": 1 }, { unique: true })
db.users.createIndex({ "tenantId": 1 })
db.tenants.createIndex({ "name": 1 })
```

### 2. Application Configuration

Update `application.properties`:
```properties
# MongoDB Configuration
spring.data.mongodb.uri=mongodb://localhost:27017/famt

# Neo4j Configuration
spring.neo4j.uri=bolt://localhost:7687
spring.neo4j.authentication.username=neo4j
spring.neo4j.authentication.password=password

# JWT Configuration
jwt.secret=your-very-long-secure-secret-key-change-this-in-production
jwt.expiration=86400000
```

**Important:** Change `jwt.secret` to a secure random string in production!

## API Usage Examples

### Step 1: Create a Tenant

```bash
curl -X POST http://localhost:8080/api/auth/tenants \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Smith Family"
  }'
```

Response:
```json
{
  "id": "65abc123def456...",
  "name": "Smith Family"
}
```

### Step 2: Register Admin User

```bash
curl -X POST http://localhost:8080/api/auth/register-admin \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin@smithfamily.com",
    "password": "SecurePass123!",
    "tenantId": "65abc123def456..."
  }'
```

Response:
```json
{
  "id": "user123...",
  "tenantId": "65abc123def456...",
  "username": "admin@smithfamily.com",
  "roles": ["TENANT_ADMIN"],
  "active": true
}
```

### Step 3: Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin@smithfamily.com",
    "password": "SecurePass123!"
  }'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin@smithfamily.com",
  "tenantId": "65abc123def456...",
  "userId": "user123...",
  "roles": [
    {
      "authority": "ROLE_TENANT_ADMIN"
    }
  ]
}
```

### Step 4: Use Token for Authenticated Requests

```bash
curl -X GET http://localhost:8080/api/v1/tenants/65abc123def456.../people \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

### Validate Token

```bash
curl -X GET http://localhost:8080/api/auth/validate \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

## Security Features

### 1. **Multi-Tenant Isolation**
- Each request validates tenantId from JWT matches path tenantId
- Users can only access data from their tenant
- Enforced in all controllers via `validateTenantAccess()`

### 2. **Role-Based Access Control**
- `TENANT_ADMIN` - Full access, can delete, export, view audit logs
- `STANDARD_USER` - Can create/modify family members
- `GUEST_VIEWER` - Read-only access

### 3. **Audit Logging**
- All create/update/delete operations are logged
- Logs include: timestamp, user, action, entity type, old/new values
- Stored in MongoDB `auditLogs` collection

## Controller Updates Required

Update all your controllers to use `SecurityContextHelper`:

```java
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/...")
@RequiredArgsConstructor
public class YourController {
    
    private final YourService service;
    private final SecurityContextHelper securityContext;
    
    private void validateTenantAccess(String tenantId) {
        String currentTenantId = securityContext.getCurrentTenantId();
        if (!tenantId.equals(currentTenantId)) {
            throw new SecurityException("Access denied: tenant ID mismatch");
        }
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'STANDARD_USER')")
    public ResponseEntity<?> create(@PathVariable String tenantId, ...) {
        validateTenantAccess(tenantId);
        String userId = securityContext.getCurrentUserId();
        // ... your logic
    }
}
```

## Testing Checklist

- [ ] Create tenant successfully
- [ ] Register admin user successfully
- [ ] Login with correct credentials
- [ ] Login fails with wrong password
- [ ] Login fails for disabled user
- [ ] JWT token contains correct tenant and user info
- [ ] Authenticated requests work with valid token
- [ ] Requests fail with invalid/expired token
- [ ] Users cannot access other tenants' data
- [ ] Role-based access control works
- [ ] Audit logs are created for operations

## Troubleshooting

### Issue: "User not found" during login
- Check if user exists in MongoDB `users` collection
- Verify username is correct

### Issue: "Access denied: tenant ID mismatch"
- JWT token's tenantId doesn't match URL path tenantId
- User trying to access another tenant's data

### Issue: "JWT signature does not match"
- JWT secret changed after token generation
- Token was tampered with

### Issue: "Token expired"
- Token lifetime exceeded (default 24 hours)
- Request new token via login

## Next Steps

1. **Implement all controllers** using the SecurityContextHelper pattern
2. **Add refresh token** functionality for better UX
3. **Implement password reset** flow
4. **Add email verification** for new users
5. **Rate limiting** for login attempts
6. **IP address tracking** in audit logs
7. **Export functionality** for audit logs (admin only)

## Security Best Practices

1. **Use HTTPS** in production
2. **Change JWT secret** to a strong random value
3. **Implement refresh tokens** to reduce token lifetime
4. **Add rate limiting** to prevent brute force
5. **Log failed login attempts**
6. **Implement account lockout** after failed attempts
7. **Use strong password policies**
8. **Regular security audits**