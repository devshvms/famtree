# Location Module — Key Changes and Features

## 1. Location Entity (`Location.java`)
- **Child tracking:** Added `childLocationIds` set to efficiently track children.
- **Type protection:** Validation to prevent direct creation of `PLANET` and `COUNTRY`.
- **Relationships:** Clean relationship mapping with parent location.

## 2. Enhanced Repository (`LocationRepository.java`)
- **Lookup by name/parent:** Custom queries for finding locations by name and parent.
- **Hierarchy traversal:** Methods to find ancestors and descendants.
- **Search:** By type and name pattern.
- **Children:** Direct children lookup queries.

## 3. Comprehensive Service (`LocationService.java`)
- **Create Location:** Validates hierarchy, prevents duplicates, generates unique IDs.
- **Get Location:** Fetch by ID, by type, or get all countries.
- **Hierarchical queries:** Children, ancestors, descendants, and full path.
- **Search:** By location type and name pattern.
- **Update:** Only name updates allowed (preserves integrity).
- **Delete:** Blocks deletion if node has children or is `PLANET`/`COUNTRY`.
- **Smart ID generation:** Format like `vil_banga_koram` (`type_parent_name`).

## 4. REST Controller (`LocationController.java`)
- **POST** `/api/v1/locations` — Create location
- **GET** `/api/v1/locations/{id}` — Get by ID
- **GET** `/api/v1/locations/type/{type}` — Get by type
- **GET** `/api/v1/locations/countries` — Get all countries
- **GET** `/api/v1/locations/earth` — Get Earth node
- **GET** `/api/v1/locations/{id}/children` — Get children
- **GET** `/api/v1/locations/{id}/ancestors` — Get ancestors
- **GET** `/api/v1/locations/{id}/descendants` — Get descendants
- **GET** `/api/v1/locations/{id}/path` — Get hierarchy path
- **GET** `/api/v1/locations/search` — Search locations
- **PUT** `/api/v1/locations/{id}` — Update location
- **DELETE** `/api/v1/locations/{id}` — Delete location
- **GET** `/api/v1/locations/stats` — Get statistics

## 5. Validation and Business Rules
- ✅ **Country creation:** Only `TENANT_ADMIN` can create; countries are pre-populated.
- ✅ **Hierarchy:** `STATE → DISTRICT → TOWN → VILLAGE`.
- ✅ **No duplicates:** Prevent duplicates under the same parent.
- ✅ **Deletion protection:** Cannot delete locations with children.
- ✅ **Type/parent immutability:** Cannot change location type or parent.
- ✅ **Relationships:** Automatic parent-child relationship management.
- ✅ **Auditing:** Comprehensive audit logging.

## 6. Database Initialization Script
- **Earth node:** Creates Earth node with ID `E`.
- **Countries:** Creates all 250+ countries with proper relationships.
- **Relationships:** Automatic `PARENT` relationships.