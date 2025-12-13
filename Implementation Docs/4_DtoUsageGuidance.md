# DTO Usage Guide

## Overview

This application uses Data Transfer Objects (DTOs) to simplify API requests and responses. DTOs avoid the complexity of nested entities and prevent redundant `tenantId` fields in the request body.

## Key Benefits

1. **Simplified Input**: Use IDs instead of nested entities
2. **No Redundant tenantId**: TenantId comes from the URL path, not request body
3. **Type Safety**: Java records provide immutable DTOs
4. **Validation**: Built-in validation at DTO level
5. **Clean Separation**: DTOs for requests vs responses

---

## Person Management

### Creating a Person

**Endpoint**: `POST /api/v1/tenants/{tenantId}/people`

**Request Body** (PersonDto):
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "maidenName": null,
  "petName": "Johnny",
  "gender": "MALE",
  "dateOfBirth": "1980-05-15",
  "dateOfDeath": null,
  "lineageId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "currentResidenceId": "sta_india_karna",
  "birthEventId": null,
  "deathEventId": null
}
```

**Response** (PersonDto):
```json
{
  "id": "7b8d9e0f-1234-5678-90ab-cdef12345678",
  "firstName": "John",
  "lastName": "Doe",
  "maidenName": null,
  "petName": "Johnny",
  "gender": "MALE",
  "dateOfBirth": "1980-05-15",
  "dateOfDeath": null,
  "lineageId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "currentResidenceId": "sta_india_karna",
  "birthEventId": null,
  "deathEventId": null
}
```

### Getting Detailed Person Information

**Endpoint**: `GET /api/v1/tenants/{tenantId}/people/{id}`

**Response** (PersonDetailDto):
```json
{
  "id": "7b8d9e0f-1234-5678-90ab-cdef12345678",
  "firstName": "John",
  "lastName": "Doe",
  "maidenName": null,
  "petName": "Johnny",
  "gender": "MALE",
  "dateOfBirth": "1980-05-15",
  "dateOfDeath": null,
  "lineage": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "name": "Smith Family"
  },
  "currentResidence": {
    "id": "sta_india_karna",
    "locationName": "Karnataka",
    "locationType": "STATE",
    "fullPath": "Earth > India > Karnataka"
  },
  "parents": [
    {
      "person": {
        "id": "parent-id-1",
        "firstName": "Jane",
        "lastName": "Doe",
        "gender": "FEMALE",
        "dateOfBirth": "1955-03-20",
        "dateOfDeath": null
      },
      "relationshipType": "BIOLOGICAL",
      "startDate": "1980-05-15",
      "confidenceScore": 1.0
    }
  ],
  "children": [],
  "spouses": [],
  "friends": [],
  "birthEvent": null,
  "deathEvent": null,
  "lifeEvents": [],
  "associations": []
}
```

---

## Relationship Management

### Adding Parent-Child Relationship

**Endpoint**: `POST /api/v1/tenants/{tenantId}/people/relationships/parent-child`

**Request Body** (ParentChildRelationRequest):
```json
{
  "parentId": "parent-uuid",
  "childId": "child-uuid",
  "relationshipType": "BIOLOGICAL",
  "startDate": "1980-05-15",
  "confidenceScore": 1.0
}
```

**Relationship Types**:
- `BIOLOGICAL`
- `ADOPTIVE`
- `FOSTER`
- `GUARDIAN`

### Adding Spousal Relationship

**Endpoint**: `POST /api/v1/tenants/{tenantId}/people/relationships/spousal`

**Request Body** (SpousalRelationRequest):
```json
{
  "person1Id": "person1-uuid",
  "person2Id": "person2-uuid",
  "status": "MARRIED",
  "startDate": "2005-06-20",
  "endDate": null,
  "partnershipType": "Marriage"
}
```

**Spousal Status**:
- `MARRIED`
- `DIVORCED`
- `SEPARATED`
- `WIDOWED`

### Adding Friend Relationship

**Endpoint**: `POST /api/v1/tenants/{tenantId}/people/relationships/friend`

**Request Body** (FriendRelationRequest):
```json
{
  "person1Id": "person1-uuid",
  "person2Id": "person2-uuid"
}
```

---

## Event Management

### Creating an Event

**Endpoint**: `POST /api/v1/tenants/{tenantId}/events`

**Request Body** (EventDto):
```json
{
  "eventType": "Wedding",
  "eventDate": "2005-06-20",
  "description": "Wedding ceremony at St. Mary's Church",
  "locationId": "vil_banga_koram",
  "participantIds": [
    "person1-uuid",
    "person2-uuid"
  ]
}
```

**Response** (EventDto):
```json
{
  "id": "event-uuid",
  "eventType": "Wedding",
  "eventDate": "2005-06-20",
  "description": "Wedding ceremony at St. Mary's Church",
  "locationId": "vil_banga_koram",
  "participantIds": [
    "person1-uuid",
    "person2-uuid"
  ]
}
```

---

## Lineage Management

### Creating a Lineage

**Endpoint**: `POST /api/v1/tenants/{tenantId}/lineages`

**Request Body** (LineageDto):
```json
{
  "name": "Smith Family",
  "description": "The Smith family lineage originating from England"
}
```

**Response** (LineageDto):
```json
{
  "id": "lineage-uuid",
  "name": "Smith Family",
  "description": "The Smith family lineage originating from England",
  "memberCount": 15
}
```

---

## Location Management

### Creating a Location

**Endpoint**: `POST /api/v1/locations`

**Request Body** (LocationDto):
```json
{
  "locationName": "Koramangala",
  "locationType": "VILLAGE",
  "parentLocationId": "tow_banga"
}
```

**Location Types Hierarchy**:
1. `PLANET` (Earth - pre-populated)
2. `COUNTRY` (Pre-populated)
3. `STATE` (User-created)
4. `DISTRICT` (User-created)
5. `TOWN` (User-created)
6. `VILLAGE` (User-created)

---

## Important Notes

### TenantId Handling

❌ **OLD WAY** (Don't do this):
```json
{
  "tenantId": "my-tenant-id",
  "firstName": "John",
  "person": {
    "tenantId": "my-tenant-id",
    ...
  }
}
```

✅ **NEW WAY** (Correct):
```
POST /api/v1/tenants/my-tenant-id/people
{
  "firstName": "John",
  ...
}
```

The `tenantId` is extracted from the URL path and automatically applied to all entities.

### ID References

Instead of sending nested entities, send only their IDs:

❌ **OLD WAY**:
```json
{
  "lineage": {
    "id": "uuid",
    "tenantId": "tenant-id",
    "name": "Smith Family"
  }
}
```

✅ **NEW WAY**:
```json
{
  "lineageId": "uuid"
}
```

### Validation

All DTOs include built-in validation:

```java
// Example validation in PersonDto
personDto.validate(); // Throws IllegalArgumentException if invalid
```

Validation rules:
- Required fields must not be null/empty
- Dates must be logical (birth < death, no future dates)
- Relationship cycles prevented
- Confidence scores between 0 and 1

---

## EntityMapper Utility

The `EntityMapper` utility class handles all conversions between DTOs and Entities:

```java
@Autowired
private EntityMapper entityMapper;

// DTO to Entity
Person person = entityMapper.toPersonEntity(personDto, tenantId);

// Entity to DTO (simple)
PersonDto dto = entityMapper.toPersonDto(person);

// Entity to DTO (detailed)
PersonDetailDto detailDto = entityMapper.toPersonDetailDto(person);
```

---

## Complete API Flow Example

### Creating a Complete Family Tree

**Step 1: Create Lineage**
```bash
POST /api/v1/tenants/my-family/lineages
{
  "name": "Johnson Family"
}
# Response: { "id": "lineage-123", ... }
```

**Step 2: Create Grandparents**
```bash
POST /api/v1/tenants/my-family/people
{
  "firstName": "Robert",
  "lastName": "Johnson",
  "gender": "MALE",
  "dateOfBirth": "1940-01-15",
  "lineageId": "lineage-123"
}
# Response: { "id": "robert-id", ... }

POST /api/v1/tenants/my-family/people
{
  "firstName": "Mary",
  "lastName": "Johnson",
  "gender": "FEMALE",
  "dateOfBirth": "1942-05-20",
  "lineageId": "lineage-123"
}
# Response: { "id": "mary-id", ... }
```

**Step 3: Add Spousal Relationship**
```bash
POST /api/v1/tenants/my-family/people/relationships/spousal
{
  "person1Id": "robert-id",
  "person2Id": "mary-id",
  "status": "MARRIED",
  "startDate": "1960-06-15"
}
```

**Step 4: Create Child**
```bash
POST /api/v1/tenants/my-family/people
{
  "firstName": "John",
  "lastName": "Johnson",
  "gender": "MALE",
  "dateOfBirth": "1965-03-10",
  "lineageId": "lineage-123"
}
# Response: { "id": "john-id", ... }
```

**Step 5: Add Parent-Child Relationships**
```bash
POST /api/v1/tenants/my-family/people/relationships/parent-child
{
  "parentId": "robert-id",
  "childId": "john-id",
  "relationshipType": "BIOLOGICAL",
  "startDate": "1965-03-10"
}

POST /api/v1/tenants/my-family/people/relationships/parent-child
{
  "parentId": "mary-id",
  "childId": "john-id",
  "relationshipType": "BIOLOGICAL",
  "startDate": "1965-03-10"
}
```

**Step 6: Get Complete Family Tree**
```bash
GET /api/v1/tenants/my-family/people/john-id
# Returns PersonDetailDto with all relationships populated
```

---

## Error Handling

All endpoints return consistent error responses:

```json
{
  "error": "Descriptive error message"
}
```

**Common HTTP Status Codes**:
- `200 OK` - Success
- `201 Created` - Resource created successfully
- `204 No Content` - Deletion successful
- `400 Bad Request` - Validation error
- `403 Forbidden` - Tenant access denied
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

---

## Best Practices

1. **Always validate DTOs** before processing
2. **Use specific DTOs** for different operations (creation vs response)
3. **Reference by ID** instead of nesting entities
4. **Let the URL provide context** (tenantId from path)
5. **Use detailed DTOs** only when needed (GET by ID)
6. **Use simple DTOs** for list operations
7. **Validate relationships** before creating them
8. **Handle errors gracefully** with proper HTTP codes

---

## Testing Examples

### Using cURL

```bash
# Create a person
curl -X POST http://localhost:8080/api/v1/tenants/my-family/people \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "firstName": "Jane",
    "lastName": "Doe",
    "gender": "FEMALE",
    "dateOfBirth": "1990-05-15"
  }'

# Get person details
curl -X GET http://localhost:8080/api/v1/tenants/my-family/people/person-id \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Add parent-child relationship
curl -X POST http://localhost:8080/api/v1/tenants/my-family/people/relationships/parent-child \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "parentId": "parent-uuid",
    "childId": "child-uuid",
    "relationshipType": "BIOLOGICAL",
    "confidenceScore": 1.0
  }'
```

---

## Summary

The DTO approach provides:

- ✅ Simpler request bodies
- ✅ No redundant tenantId fields
- ✅ Type-safe API contracts
- ✅ Clear separation between request and response
- ✅ Built-in validation
- ✅ Easier testing and documentation
- ✅ Better maintainability