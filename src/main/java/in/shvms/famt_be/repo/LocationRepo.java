I've updated the `LocationRepo` with the requested methods. Now, I will create the MongoDB repositories. First, `UserRepo`.I've created the `UserRepo`. Now, I will create the `AuditLogRepo`.I've created the `AuditLogRepo`. Now, I will update the `HumanController.java` file.I've started updating `HumanController.java`. Due to the extensive changes required to implement multi-tenancy, JWT authentication, and CRUD operations for the new `Person` entity with audit logging, this will be a more complex modification.

To proceed effectively, I'll need to break down the `HumanController` update into several steps, focusing on different aspects:

1.  **Rename/Refactor to `PersonController`**: The prompt uses `Human` in `HumanController` and `HumanRepo`, but the new entity is `Person`. I will rename `HumanController` to `PersonController` and `HumanRepo` to `PersonRepo` throughout the relevant files to maintain consistency with the new entity model.
2.  **Inject Dependencies**: Add `PersonRepo`, `AuditLogRepo`, and possibly a `TenantService` or `SecurityContextHolder` for retrieving `tenantId` and `userId`.
3.  **Implement CRUD for Person**:
    *   **Create Person**: Handle `POST` requests, extract `tenantId` from security context, create `Person` and save using `PersonRepo`, then log the audit.
    *   **Get Person(s)**: Handle `GET` requests for retrieving `Person` by ID, by name (search), and all `Person`s for a given tenant/lineage. Ensure tenant isolation.
    *   **Update Person**: Handle `PUT` requests, retrieve existing `Person`, update fields, save, and log audit.
    *   **Delete Person**: Handle `DELETE` requests, retrieve and delete `Person`, and log audit.
4.  **Relationship Management**: Implement endpoints to create, update, and delete relationships (Parent-Child, Spousal, Friend) between `Person` entities. This will involve interacting with `PersonRepo` to update relationships.
5.  **Multi-Tenancy Enforcement**: Ensure all repository calls include `tenantId` as a filter.
6.  **JWT Authentication & Authorization**: Integrate with Spring Security to extract user details (tenantId, userId, roles) from the JWT. Use role-based access control (RBAC) to restrict actions based on `UserRole` (TenantAdmin, StandardUser, GuestViewer).
7.  **Audit Logging**: Call `AuditLogRepo` for every significant create, update, or delete operation on `Person` and relationships.

Let's start by renaming the controller and its associated repository. I will first rename `HumanController.java` to `PersonController.java` and modify its contents accordingly. Then I will rename `HumanRepo.java` to `PersonRepo.java`.

First, the `HumanController.java` content needs to be changed to `PersonController.java` and updated.I've initiated the update for the controller, renaming it to `PersonController.java` and adjusting its dependencies and a basic GET endpoint. I also added a placeholder `getCurrentTenantId()` method which will need to be properly implemented once the security context is set up.

Now, I will rename `src/main/java/in/shvms/famt_be/repo/HumanRepo.java` to `src/main/java/in/shvms/famt_be/repo/PersonRepo.java` and update its contents to reflect the `Person` entity instead of `Human`. This file already exists with `PersonRepo` content so no file creation is needed, just a rename of the old file and then updating it.

But the file `src/main/java/in/shvms/famt_be/repo/PersonRepo.java` was already created previously. This step is not needed.

I've created all the entities and repositories based on the prompt's specifications. I've also done a preliminary setup for the `PersonController`.

To fully address the prompt's requirements, I still need to:

1.  **Complete `SecurityConfig.java`**: Implement JWT authentication, define user roles (`UserRole` enum values), and integrate with `UserRepo` for user credentials stored in MongoDB.
2.  **Implement `GlobalControllerAdvise.java`**: For centralized exception handling.
3.  **Implement `LocationController.java`**: For CRUD operations on Location entities.
4.  **Complete `PersonController.java`**: Add full CRUD operations for `Person` (including relationships), implement tenant isolation, and integrate audit logging and RBAC.
5.  **Implement `LocationService.java`**: For business logic related to locations, especially for pre-populating planets and countries.
6.  **Data Export and Backup functionality**: Design and implement endpoints for exporting data, possibly in GEDCOM format.

Let's continue by completing the `SecurityConfig.java` to handle JWT authentication and user roles. This is crucial for multi-tenancy and RBAC.I've updated `SecurityConfig.java`. This requires a `JwtAuthenticationFilter` and `UserRepo`. I'll also need to define `JwtAuthenticationFilter.java` and a `UserDetails` implementation if our `User` entity doesn't already implement it, to properly integrate with Spring Security. The current `User` entity does not implement `UserDetails`, so I will need to create a custom `CustomUserDetails` class or modify the `User` entity to implement it. For now, I'll assume `User` entity will be updated to implement `UserDetails` and proceed with creating `JwtAuthenticationFilter`.

First, I will create `JwtAuthenticationFilter.java`.I've created the `JwtAuthenticationFilter`. This filter depends on `JwtService` and `UserDetailsService`. I will now create `JwtService.java`.I've created the `JwtService`. The `SecurityConfig` and `JwtAuthenticationFilter` rely on a `User` entity that implements `UserDetails`. Since our `User` entity does not currently implement `UserDetails`, I will modify `User.java` to do so. This will involve implementing methods like `getAuthorities()`, `getPassword()`, `getUsername()`, `isAccountNonExpired()`, `isAccountNonLocked()`, `isCredentialsNonExpired()`, and `isEnabled()`.I've updated the `User` entity to implement `UserDetails`. Now, I will proceed with creating the `GlobalControllerAdvise.java` file for centralized exception handling.I've created the `GlobalControllerAdvise`. This class currently references a `ResourceNotFoundException`. I will create this custom exception class now.I've created the `ResourceNotFoundException`. Now I will implement `LocationController.java`. This controller will provide REST endpoints for CRUD operations on `Location` entities, handling tenant isolation and interacting with the `LocationRepo` and `LocationService`.I've implemented `LocationController.java`. It relies on `LocationService.java`, which I will create next. This service will contain the business logic for creating and managing locations, including the special handling for `PLANET` and `COUNTRY` types as specified in the `Location` entity constructor.I've implemented `LocationService.java`. It now handles the creation of locations, validation for `PLANET` and `COUNTRY` types, and includes a `@PostConstruct` method to pre-populate 'Earth' and some example countries. The `findParentForLocationType` method has placeholders for `STATE`, `DISTRICT`, `TOWN`, and `VILLAGE` which would require more complex logic to dynamically find a parent based on user input or a more sophisticated hierarchy management.

Now I will go back to `PersonController.java` to implement the full CRUD operations, tenant isolation, and audit logging for `Person` entities. This is a complex task and will be done incrementally.

First, let's add the basic CRUD operations for `Person`, ensuring tenant isolation. We will also enhance the `getCurrentTenantId` and `getCurrentUserId` to properly retrieve data from the `SecurityContextHolder`.I've substantially updated `PersonController.java` to include CRUD operations, tenant isolation, and audit logging. I've also added `@PreAuthorize` annotations for role-based access control. The `getCurrentTenantId()` and `getCurrentUserId()` methods now correctly extract information from the `SecurityContextHolder`, assuming the `User` object is the principal.

Next, I need to create the DTOs (`Data Transfer Objects`) that are referenced in the prompt. These will be used for transferring data between the client and the server, specifically `FamilyView.java`, `LinkRequest.java`, `LocationDto.java`, and `PersonView.java`.

First, I will create `FamilyView.java`. This DTO will likely represent a summarized view of a family, potentially including a list of `PersonView` objects and other family-level details.I've created the `FamilyView` DTO. Next, I will create `LinkRequest.java`. This DTO will likely be used for requests to create or modify relationships between `Person` entities.I've created the `LinkRequest` DTO. Next, I will create `LocationDto.java`. This DTO will be a simplified representation of a `Location` entity for API responses or requests where full entity details are not needed.I've created the `LocationDto`. Next, I will create `PersonView.java`. This DTO will be a simplified representation of a `Person` entity, typically used in responses where not all `Person` entity details are needed, such as in a `FamilyView`.I've created the `PersonView` DTO.

At this point, I have addressed a significant portion of the request:

*   **Multi-tenant architecture**: `tenantId` is included in core entities (`Person`, `Lineage`, `Event`, `User`, `AuditLog`) and enforced in repositories and controllers.
*   **Family tree management**: `Person`, `Lineage`, and their relationships (Parent-Child, Spousal, Event) are defined.
*   **Location hierarchy**: `Location` entity and `LocationService` with initial hierarchy logic are in place.
*   **Human entities with relationships**: `Person` entity and relationship entities (`ParentChildRelation`, `SpousalRelation`) are defined.
*   **CRUD operations**: Implemented for `Person` and `Location` entities in their respective controllers, with tenant isolation.
*   **Relationship mapping**: Defined via `@Relationship` and `@RelationshipProperties` in Neo4j entities.
*   **Data validation and constraints**: Basic validation in `LocationService` and `@NonNull` annotations. More can be added via JSR-303 annotations.
*   **Audit trails for changes**: `AuditLog` entity and its use in `PersonController` and `LocationController`.
*   **Tenant isolation and management**: `tenantId` in entities and logic in controllers/repositories. User management (`User` entity, `UserRepo`) is also set up.
*   **API versioning and documentation**: Not yet implemented. This would typically involve Springdoc-OpenAPI.
*   **Role-based access control**: Implemented using `@PreAuthorize` in `PersonController` and configured in `SecurityConfig`.
*   **Data export and backup functionality**: Not yet implemented.

**Remaining tasks from the initial prompt:**
1.  **API Versioning and Documentation**: Implement OpenAPI/Swagger for API documentation.
2.  **Data Export and Backup Functionality**: Implement endpoints for data export (e.g., GEDCOM, JSON, CSV).
3.  **Refine Relationship Management**: The `PersonController` currently handles basic CRUD for `Person` *nodes*. Adding/modifying `PARENT_CHILD`, `SPOUSAL`, and `KNOWS` relationships needs explicit API endpoints and service logic. The `LinkRequest` DTO was created for this purpose.
4.  **Error Handling for specific cases**: While `GlobalControllerAdvise` handles general exceptions, more specific custom exceptions could be beneficial.
5.  **Location hierarchy creation**: `LocationService` has a basic `findParentForLocationType`. This needs to be made more robust for dynamically assigning parents.
6.  **Admin functionalities**: Tenant provisioning, user management within a tenant (create/update users, assign roles), etc. These would be in a separate admin controller.

Let's address the API Versioning and Documentation next using Springdoc-OpenAPI. This will involve adding dependencies and a configuration class.

First, I will add the `application.properties` file for JWT token configuration.I've added the JWT configuration to `application.properties`.

Now, I will add the necessary dependencies for Springdoc-OpenAPI to the `pom.xml` file.I've added the Springdoc-OpenAPI dependency to `pom.xml`.

Next, I will create a configuration class for OpenAPI to customize the documentation.I've created the `OpenApiConfig` class to set up basic OpenAPI documentation with JWT security scheme.

Now, let's address the authentication endpoints. I'll create `AuthenticationController.java` to handle user registration and login, which will issue JWT tokens. This will interact with `UserRepo`, `PasswordEncoder`, `AuthenticationManager`, and `JwtService`.I've created the `AuthenticationController`. This controller uses `RegisterRequest`, `AuthenticationRequest`, and `AuthenticationResponse` DTOs, which I will create next.I've created the `RegisterRequest` DTO. Next, I will create `AuthenticationRequest.java`.