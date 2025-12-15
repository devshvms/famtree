# Family Tree Management API - Complete Documentation

## Table of Contents
1. [System Overview](#system-overview)
2. [Architecture](#architecture)
3. [User Journeys](#user-journeys)
4. [API Endpoints Reference](#api-endpoints-reference)
5. [Authentication & Authorization](#authentication--authorization)
6. [Data Models](#data-models)
7. [Error Handling](#error-handling)
8. [Best Practices](#best-practices)

---

## System Overview

### Purpose
The Family Tree Management System is a comprehensive multi-tenant SaaS application that enables families to create, manage, and explore their genealogical records. The system supports complex family relationships, location hierarchies, events, and collaborative family tree building.

### Key Features
- **Multi-Tenant Architecture**: Complete data isolation between family groups
- **Hierarchical Location System**: Global location hierarchy (Earth → Country → State → District → Town → Village)
- **Complex Relationship Management**: Parent-child, spousal, friend relationships with metadata
- **Event Management**: Track life events (births, deaths, marriages, etc.) with participants
- **Role-Based Access Control**: Tenant Admin, Standard User, and Guest Viewer roles
- **Audit Logging**: Complete audit trail of all data modifications
- **Two-Factor Authentication**: OTP-based email verification for new users
- **RESTful API**: Clean, versioned API with comprehensive error handling

### Technology Stack
- **Backend**: Spring Boot 3.4.12, Java 17
- **Databases**: 
  - MongoDB (tenant data, users, audit logs)
  - Neo4j (family tree relationships, graph data)
- **Security**: Spring Security, JWT tokens, BCrypt password hashing
- **API Documentation**: OpenAPI 3.0 (Swagger)
- **Email**: Spring Mail with SMTP

---

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     API Gateway / Load Balancer              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                   │
│                                                               │
│  ┌────────────────┐  ┌────────────────┐  ┌───────────────┐ │
│  │  Controllers   │  │   Services     │  │  Repositories │ │
│  │  - Auth        │  │  - User Mgmt   │  │  - MongoDB    │ │
│  │  - Person      │  │  - Lineage     │  │  - Neo4j      │ │
│  │  - Location    │  │  - Event       │  │               │ │
│  │  - Event       │  │  - Audit       │  │               │ │
│  └────────────────┘  └────────────────┘  └───────────────┘ │
│                                                               │
│  ┌────────────────────────────────────────────────────────┐ │
│  │          Security Layer (JWT + RBAC)                   │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                    │                    │
                    ▼                    ▼
         ┌──────────────────┐  ┌──────────────────┐
         │     MongoDB      │  │      Neo4j       │
         │  - Tenants       │  │  - People        │
         │  - Users         │  │  - Relationships │
         │  - Audit Logs    │  │  - Locations     │
         └──────────────────┘  └──────────────────┘
```

### Database Design

#### MongoDB Collections
1. **tenants**: Tenant metadata, settings, subscription info
2. **users**: User accounts, credentials, roles, activation status
3. **auditLogs**: Complete audit trail of all operations

#### Neo4j Graph Database
1. **Person Nodes**: Individual family members with properties
2. **Location Nodes**: Hierarchical location data
3. **Event Nodes**: Life events with participants
4. **Lineage Nodes**: Family line identifiers
5. **Relationships**: 
   - PARENT_CHILD (with type, confidence score)
   - SPOUSAL (with status, dates)
   - KNOWS (friends)
   - MEMBER_OF (lineage)
   - PARENT (location hierarchy)

---

## User Journeys

### Journey 1: Tenant Admin Registration & Setup

**Actors**: New Family Administrator

**Preconditions**: None

**Steps**:

1. **Create Tenant**
   ```
   POST /api/auth/tenants
   Body: { "name": "Smith Family" }
   Response: { "id": "tenant_123", "name": "Smith Family", ... }
   ```
   - System creates a new tenant entity in MongoDB
   - Generates unique tenant ID
   - Initializes default settings and limits
   - Status set to TRIAL initially

2. **Register Admin User**
   ```
   POST /api/auth/register-admin
   Body: {
     "username": "admin@smithfamily.com",
     "password": "SecurePass123!",
     "tenantId": "tenant_123"
   }
   Response: { "id": "user_456", "username": "admin@smithfamily.com", ... }
   ```
   - System validates password strength (8+ chars, upper, lower, digit, special)
   - Hashes password using BCrypt
   - Creates user with TENANT_ADMIN role
   - User is immediately active (no OTP required for first admin)
   - Stores in MongoDB with tenant association

3. **Login**
   ```
   POST /api/auth/login
   Body: {
     "username": "admin@smithfamily.com",
     "password": "SecurePass123!"
   }
   Response: {
     "token": "eyJhbGciOiJIUzI1NiIs...",
     "username": "admin@smithfamily.com",
     "tenantId": "tenant_123",
     "userId": "user_456",
     "roles": ["ROLE_TENANT_ADMIN"]
   }
   ```
   - Spring Security authenticates credentials
   - JWT token generated with 24-hour expiry
   - Token contains: username, tenantId, userId, roles
   - Client stores token for subsequent requests

4. **Setup Location Hierarchy**
   ```
   GET /api/v1/locations/countries
   Response: [ { "id": "IN", "locationName": "India", ... }, ... ]
   
   POST /api/v1/locations
   Body: {
     "locationName": "Karnataka",
     "locationType": "STATE",
     "parentLocationId": "IN"
   }
   ```
   - Admin can browse pre-populated countries
   - Create states, districts, towns, villages as needed
   - System validates parent-child hierarchy
   - Generates location IDs based on hierarchy

5. **Create First Lineage**
   ```
   POST /api/v1/tenants/tenant_123/lineages
   Body: {
     "name": "Smith Main Line",
     "description": "Primary Smith family lineage"
   }
   Response: { "id": "lineage_789", "name": "Smith Main Line", ... }
   ```
   - Creates lineage node in Neo4j
   - Associates with tenant
   - Audit log entry created

**Postconditions**: 
- Tenant fully configured
- Admin can now add family members
- Location hierarchy available for use

---

### Journey 2: Standard User Invitation & Activation

**Actors**: Tenant Admin (inviter), Standard User (invitee)

**Preconditions**: Admin is logged in

**Steps**:

1. **Admin Creates Standard User**
   ```
   POST /api/auth/create-standard-user
   Headers: { Authorization: "Bearer <admin_jwt>" }
   Body: {
     "email": "john.smith@example.com",
     "displayName": "John Smith"
   }
   Response: {
     "message": "User created. Welcome email sent.",
     "user": {
       "id": "user_789",
       "username": "john.smith@example.com",
       "isActive": false,
       "emailVerified": false,
       "firstTimeLogin": true
     }
   }
   ```
   - System generates random 6-digit PIN
   - Hashes PIN and stores as temporary password
   - Sets flags: isActive=false, emailVerified=false, firstTimeLogin=true
   - Sends welcome email with PIN to user
   - Email contains: display name, temporary PIN, activation instructions

2. **New User Receives Email**
   ```
   Email Subject: Welcome to Famtree Application
   Body:
   Hello John Smith,
   
   Your account has been created by your family administrator.
   
   Your login credentials:
   Email: john.smith@example.com
   Temporary PIN: 234567
   
   Please login to complete your account setup.
   ```

3. **First Time Login with PIN**
   ```
   POST /api/auth/first-login
   Body: {
     "email": "john.smith@example.com",
     "pin": "234567"
   }
   Response: {
     "message": "OTP sent to your email",
     "email": "john.smith@example.com",
     "expiresIn": "10 minutes"
   }
   ```
   - System verifies PIN matches hashed value
   - Checks account not locked (max 5 failed attempts)
   - Generates new 6-digit OTP
   - Hashes OTP and stores with 10-minute expiry
   - Sends OTP to user's email
   - Resets failed login counter

4. **User Receives OTP Email**
   ```
   Email Subject: Famtree Application - Email Verification OTP
   Body:
   Hello John Smith,
   
   Your email verification code is: 891234
   
   This code will expire in 10 minutes.
   ```

5. **Verify OTP**
   ```
   POST /api/auth/verify-otp
   Body: {
     "email": "john.smith@example.com",
     "otp": "891234"
   }
   Response: {
     "message": "Email verified successfully. Please set your password.",
     "email": "john.smith@example.com"
   }
   ```
   - System checks OTP not expired
   - Verifies hashed OTP matches
   - Sets emailVerified=true
   - Clears verification token and expiry
   - Throws error if OTP expired or invalid

6. **Set Permanent Password**
   ```
   POST /api/auth/set-password
   Body: {
     "email": "john.smith@example.com",
     "newPassword": "MySecure123!Password"
   }
   Response: {
     "message": "Password set successfully. Your account is now active.",
     "email": "john.smith@example.com"
   }
   ```
   - System validates password strength
   - Hashes new password (replaces PIN)
   - Sets: firstTimeLogin=false, isActive=true
   - Records passwordLastChangedAt timestamp
   - Sends confirmation email
   - Audit log entry created

7. **Regular Login**
   ```
   POST /api/auth/login
   Body: {
     "username": "john.smith@example.com",
     "password": "MySecure123!Password"
   }
   Response: {
     "token": "eyJhbGciOiJIUzI1NiIs...",
     "username": "john.smith@example.com",
     "tenantId": "tenant_123",
     "userId": "user_789",
     "roles": ["ROLE_STANDARD_USER"]
   }
   ```
   - User can now login normally with password
   - Receives JWT token for API access
   - Has STANDARD_USER permissions

**Postconditions**:
- User account fully activated
- User can add/edit family members
- User receives JWT for authenticated requests

---

### Journey 3: Building Family Tree

**Actors**: Standard User or Tenant Admin

**Preconditions**: User is logged in, lineage exists

**Steps**:

1. **Add First Person (Self)**
   ```
   POST /api/v1/tenants/tenant_123/people
   Headers: { Authorization: "Bearer <jwt>" }
   Body: {
     "firstName": "John",
     "lastName": "Smith",
     "gender": "MALE",
     "dateOfBirth": "1980-05-15",
     "lineageId": "lineage_789",
     "currentResidenceId": "dis_karna_banga"
   }
   Response: {
     "id": "person_001",
     "firstName": "John",
     "lastName": "Smith",
     ...
   }
   ```
   - Creates Person node in Neo4j
   - Associates with tenant and lineage
   - Sets current residence relationship
   - Validates dates (DoB not in future)
   - Audit log entry created

2. **Add Spouse**
   ```
   POST /api/v1/tenants/tenant_123/people
   Body: {
     "firstName": "Mary",
     "lastName": "Johnson",
     "maidenName": "Johnson",
     "gender": "FEMALE",
     "dateOfBirth": "1982-03-20",
     "lineageId": "lineage_789"
   }
   Response: { "id": "person_002", ... }
   ```
   - Creates second person
   - Ready to establish spousal relationship

3. **Create Spousal Relationship**
   ```
   POST /api/v1/tenants/tenant_123/people/relationships/spousal
   Body: {
     "person1Id": "person_001",
     "person2Id": "person_002",
     "status": "MARRIED",
     "startDate": "2005-06-15",
     "endDate": null,
     "partnershipType": "Marriage"
   }
   Response: { "id": "person_002", ... }
   ```
   - Creates bidirectional SPOUSAL relationship in Neo4j
   - Both persons get relationship in their spouseRelations set
   - Validates: persons not same, start before end date
   - Prevents duplicate relationships
   - Audit log entry: "ADD_SPOUSAL_RELATION"

4. **Add Children**
   ```
   POST /api/v1/tenants/tenant_123/people
   Body: {
     "firstName": "Sarah",
     "lastName": "Smith",
     "gender": "FEMALE",
     "dateOfBirth": "2008-11-22",
     "lineageId": "lineage_789"
   }
   Response: { "id": "person_003", ... }
   ```
   - Creates child person

5. **Establish Parent-Child Relationships**
   ```
   POST /api/v1/tenants/tenant_123/people/relationships/parent-child
   Body: {
     "parentId": "person_001",
     "childId": "person_003",
     "relationshipType": "BIOLOGICAL",
     "startDate": "2008-11-22",
     "confidenceScore": 1.0
   }
   
   POST /api/v1/tenants/tenant_123/people/relationships/parent-child
   Body: {
     "parentId": "person_002",
     "childId": "person_003",
     "relationshipType": "BIOLOGICAL",
     "startDate": "2008-11-22",
     "confidenceScore": 1.0
   }
   ```
   - Creates PARENT_CHILD relationships
   - Relationship stored on child node (incoming direction)
   - Validates parent born before child
   - Supports types: BIOLOGICAL, ADOPTIVE, FOSTER, GUARDIAN
   - Confidence score for uncertain relationships

6. **Add Parents (Going Back)**
   ```
   POST /api/v1/tenants/tenant_123/people
   Body: {
     "firstName": "Robert",
     "lastName": "Smith",
     "gender": "MALE",
     "dateOfBirth": "1950-08-10",
     "lineageId": "lineage_789"
   }
   Response: { "id": "person_004", ... }
   
   POST /api/v1/tenants/tenant_123/people/relationships/parent-child
   Body: {
     "parentId": "person_004",
     "childId": "person_001",
     "relationshipType": "BIOLOGICAL",
     "confidenceScore": 1.0
   }
   ```
   - Can build tree upward (ancestors) or downward (descendants)
   - No limit to generational depth

7. **Record Life Events**
   ```
   POST /api/v1/tenants/tenant_123/events
   Body: {
     "eventType": "Wedding",
     "eventDate": "2005-06-15",
     "description": "John and Mary's wedding at St. Mary's Church",
     "locationId": "dis_karna_banga",
     "participantIds": ["person_001", "person_002"]
   }
   Response: { "id": "event_001", ... }
   ```
   - Creates Event node in Neo4j
   - Links participants via PARTICIPATED_IN relationships
   - Links location via OCCURRED_AT relationship
   - Can be retrieved by person, date, type, or location

8. **Query Relationships**
   ```
   GET /api/v1/tenants/tenant_123/people/person_001/children
   Response: [ { "id": "person_003", "firstName": "Sarah", ... } ]
   
   GET /api/v1/tenants/tenant_123/people/person_003/parents
   Response: [
     { "id": "person_001", "firstName": "John", ... },
     { "id": "person_002", "firstName": "Mary", ... }
   ]
   
   GET /api/v1/tenants/tenant_123/people/person_003/siblings
   Response: [ ... ] // Other children of same parents
   ```
   - Efficiently queries graph relationships
   - Returns person DTOs with core information
   - Can retrieve extended details via person ID endpoint

**Postconditions**:
- Multi-generational family tree established
- Relationships properly mapped
- Events documented
- Data queryable via various endpoints

---

### Journey 4: Searching and Exploring Family Tree

**Actors**: Any authenticated user in tenant

**Preconditions**: Family tree data exists

**Steps**:

1. **Search People by Name**
   ```
   GET /api/v1/tenants/tenant_123/people/search?name=Smith
   Response: [
     { "id": "person_001", "firstName": "John", "lastName": "Smith", ... },
     { "id": "person_004", "firstName": "Robert", "lastName": "Smith", ... }
   ]
   ```
   - Searches firstName field (contains match)
   - Returns simple PersonDto list
   - Client can fetch detailed info for specific person

2. **Get Detailed Person Information**
   ```
   GET /api/v1/tenants/tenant_123/people/person_001
   Response: {
     "id": "person_001",
     "firstName": "John",
     "lastName": "Smith",
     "gender": "MALE",
     "dateOfBirth": "1980-05-15",
     "lineage": { "id": "lineage_789", "name": "Smith Main Line" },
     "currentResidence": {
       "id": "dis_karna_banga",
       "locationName": "Bangalore Urban",
       "locationType": "DISTRICT",
       "fullPath": "Earth > India > Karnataka > Bangalore Urban"
     },
     "parents": [
       {
         "person": { "id": "person_004", "firstName": "Robert", ... },
         "relationshipType": "BIOLOGICAL",
         "confidenceScore": 1.0
       }
     ],
     "children": [
       {
         "person": { "id": "person_003", "firstName": "Sarah", ... },
         "relationshipType": "BIOLOGICAL",
         "confidenceScore": 1.0
       }
     ],
     "spouses": [
       {
         "spouse": { "id": "person_002", "firstName": "Mary", ... },
         "status": "MARRIED",
         "startDate": "2005-06-15",
         "partnershipType": "Marriage"
       }
     ],
     "lifeEvents": [ ... ],
     "friends": [ ... ]
   }
   ```
   - Returns PersonDetailDto with all relationships
   - Includes location hierarchy path
   - Shows relationship metadata (types, dates, confidence)
   - Nested objects for easy UI rendering

3. **Browse Location Hierarchy**
   ```
   GET /api/v1/locations/IN
   Response: {
     "id": "IN",
     "locationName": "India",
     "locationType": "COUNTRY",
     "parentLocation": { "id": "E", "locationName": "Earth" }
   }
   
   GET /api/v1/locations/IN/children
   Response: [
     { "id": "sta_india_karna", "locationName": "Karnataka", ... },
     { "id": "sta_india_tamil", "locationName": "Tamil Nadu", ... },
     ...
   ]
   ```
   - Navigate location tree
   - Get all locations of specific type
   - Search locations by name and type

4. **Get All People in Lineage**
   ```
   GET /api/v1/tenants/tenant_123/lineages/lineage_789
   Response: {
     "id": "lineage_789",
     "name": "Smith Main Line",
     "memberCount": 15
   }
   ```
   - Shows lineage statistics
   - Can query all members via PersonRepository

5. **Find Events by Type**
   ```
   GET /api/v1/tenants/tenant_123/events/search/by-type?eventType=Wedding
   Response: [
     {
       "id": "event_001",
       "eventType": "Wedding",
       "eventDate": "2005-06-15",
       "description": "John and Mary's wedding",
       "locationId": "dis_karna_banga",
       "participantIds": ["person_001", "person_002"]
     }
   ]
   ```
   - Filter events by type
   - Can also filter by date range (future enhancement)

**Postconditions**:
- Users can explore family tree
- Can discover relationships
- Can search by various criteria

---

### Journey 5: Tenant Admin - User Management & Audit

**Actors**: Tenant Admin

**Preconditions**: Admin logged in, standard users exist

**Steps**:

1. **View All Users in Tenant**
   ```
   GET /api/v1/users/tenants/tenant_123
   Response: [
     {
       "id": "user_456",
       "username": "admin@smithfamily.com",
       "roles": ["TENANT_ADMIN"],
       "isActive": true
     },
     {
       "id": "user_789",
       "username": "john.smith@example.com",
       "roles": ["STANDARD_USER"],
       "isActive": true,
       "lastLoginAt": "2024-12-14T10:30:00"
     }
   ]
   ```
   - Lists all users in tenant
   - Shows activation status, roles, last login

2. **Update User Permissions**
   ```
   PUT /api/v1/users/tenants/tenant_123/user_789
   Body: {
     "roles": ["STANDARD_USER", "GUEST_VIEWER"],
     "isActive": true
   }
   Response: { "id": "user_789", "roles": [...], ... }
   ```
   - Change user roles
   - Deactivate/reactivate users
   - Audit log created

3. **Monitor System Usage** (Future Enhancement)
   ```
   GET /api/v1/tenants/tenant_123/usage
   Response: {
     "currentPeopleCount": 45,
     "currentUserCount": 5,
     "currentLineageCount": 2,
     "limits": {
       "maxPeople": 500,
       "maxUsers": 10
     }
   }
   ```
   - View tenant usage against limits
   - Track subscription status

4. **Review Audit Logs** (Internal Query)
   ```
   MongoDB Query: db.auditLogs.find({ tenantId: "tenant_123" })
   Results: [
     {
       "timestamp": "2024-12-14T09:15:00",
       "userId": "user_789",
       "action": "CREATE",
       "entityType": "Person",
       "entityId": "person_005",
       "details": { "firstName": "Jane", "lastName": "Doe" }
     },
     ...
   ]
   ```
   - Complete audit trail in MongoDB
   - Tracks: who, what, when, old/new values
   - Enables accountability and troubleshooting

**Postconditions**:
- Admin has full visibility
- Can manage user access
- Audit trail maintained

---

## API Endpoints Reference

### Authentication & Tenants

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/tenants` | None | Create new tenant |
| POST | `/api/auth/register-admin` | None | Register first admin for tenant |
| POST | `/api/auth/login` | None | Login and get JWT token |
| POST | `/api/auth/create-standard-user` | Admin | Create standard user with PIN |
| POST | `/api/auth/first-login` | None | First login with PIN, triggers OTP |
| POST | `/api/auth/verify-otp` | None | Verify OTP after first login |
| POST | `/api/auth/resend-otp` | None | Resend OTP if expired |
| POST | `/api/auth/set-password` | None | Set permanent password |
| GET | `/api/auth/validate` | Any | Validate JWT token |

### Locations

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/locations/earth` | Any | Get Earth root node |
| GET | `/api/v1/locations/countries` | Any | Get all countries |
| GET | `/api/v1/locations/type/{type}` | Any | Get locations by type |
| GET | `/api/v1/locations/{id}` | Any | Get specific location |
| GET | `/api/v1/locations/{id}/children` | Any | Get child locations |
| GET | `/api/v1/locations/{id}/ancestors` | Any | Get ancestor locations |
| GET | `/api/v1/locations/{id}/descendants` | Any | Get all descendants |
| GET | `/api/v1/locations/{id}/path` | Any | Get full hierarchy path |
| GET | `/api/v1/locations/search` | Any | Search by type and name |
| POST | `/api/v1/locations` | User | Create new location |
| PUT | `/api/v1/locations/{id}` | Admin | Update location name |
| DELETE | `/api/v1/locations/{id}` | Admin | Delete location (no children) |
| GET | `/api/v1/locations/stats` | Any | Get location statistics |

### Lineages

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/tenants/{tid}/lineages` | Any | Get all lineages |
| GET | `/api/v1/tenants/{tid}/lineages/{id}` | Any | Get lineage by ID |
| POST | `/api/v1/tenants/{tid}/lineages` | User | Create lineage |
| PUT | `/api/v1/tenants/{tid}/lineages/{id}` | User | Update lineage |
| DELETE | `/api/v1/tenants/{tid}/lineages/{id}` | Admin | Delete lineage |

### People

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/tenants/{tid}/people` | Any | Get all people |
| GET | `/api/v1/tenants/{tid}/people/{id}` | Any | Get person details |
| GET | `/api/v1/tenants/{tid}/people/search` | Any | Search by name |
| GET | `/api/v1/tenants/{tid}/people/{id}/children` | Any | Get children |
| GET | `/api/v1/tenants/{tid}/people/{id}/parents` | Any | Get parents |
| GET | `/api/v1/tenants/{tid}/people/{id}/spouses` | Any | Get spouses |
| GET | `/api/v1/tenants/{tid}/people/{id}/siblings` | Any | Get siblings |
| POST | `/api/v1/tenants/{tid}/people` | User | Create person |
| PUT | `/api/v1/tenants/{tid}/people/{id}` | User | Update person |
| DELETE | `/api/v1/tenants/{tid}/people/{id}` | Admin | Delete person |

### Relationships

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/v1/tenants/{tid}/people/relationships/parent-child` | User | Add parent-child relation |
| DELETE | `/api/v1/tenants/{tid}/people/relationships/parent-child` | User | Remove parent-child |
| POST | `/api/v1/tenants/{tid}/people/relationships/spousal` | User | Add spousal relation |
| DELETE | `/api/v1/tenants/{tid}/people/relationships/spousal` | User | Remove spousal |
| POST | `/api/v1/tenants/{tid}/people/relationships/friend` | User | Add friend relation |

### Events

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/tenants/{tid}/events` | Any | Get all events |
| GET | `/api/v1/tenants/{tid}/events/{id}` | Any | Get event by ID |
| GET | `/api/v1/tenants/{tid}/events/search/by-type` | Any | Get events by type |
| POST | `/api/v1/tenants/{tid}/events` | User | Create event |
| PUT | `/api/v1/tenants/{tid}/events/{id}` | User | Update event |
| DELETE | `/api/v1/tenants/{tid}/events/{id}` | Admin | Delete event |
| POST | `/api/v1/tenants/{tid}/events/{eid}/participants/{pid}` | User | Add participant |
| DELETE | `/api/v1/tenants/{tid}/events/{eid}/participants/{pid}` | User | Remove participant |

### Users

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/api/v1/users/tenants/{tid}` | Admin | Get all users |
| GET | `/api/v1/users/tenants/{tid}/{id}` | Admin | Get user by ID |
| PUT | `/api/v1/users/tenants/{tid}/{id}` | Admin | Update user |
| DELETE | `/api/v1/users/tenants/{tid}/{id}` | Admin | Delete user |

---

## Authentication & Authorization

### JWT Token Structure

**Header**:
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload**:
```json
{
  "sub": "admin@smithfamily.com",
  "tenantId": "tenant_123",
  "userId": "user_456",
  "roles": ["ROLE_TENANT_ADMIN"],
  "iat": 1702563600,
  "exp": 1702650000
}
```

**Usage**:
- Include in Authorization header: `Bearer <token>`
- Token expires after 24 hours
- Refresh requires new login

### Role-Based Access Control

#### TENANT_ADMIN
- **Permissions**:
  - All STANDARD_USER permissions
  - Create/manage standard users
  - Delete people, events, lineages
  - Update any user's roles
  - View audit logs
  - Export data