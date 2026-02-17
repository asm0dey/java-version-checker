# Data Model: Centralize Java Versions Data Source

**Feature**: 004-centralize-version-source
**Date**: 2026-02-17

## Overview

Data structures and service designs for centralizing Java version data from `src/main/resources/java_versions.txt` to serve both backend and frontend.

## Core Entities

### VersionListService

**Purpose**: Manage lifecycle of Java version list data - load from file, cache in memory, serve to consumers.

**Scope**: Application-scoped CDI bean (singleton)

**Responsibilities**:
- Load version list from `src/main/resources/java_versions.txt` at startup
- Parse and validate file content
- Cache version list in memory for fast access
- Provide thread-safe read access to version list
- Handle file reading errors gracefully

**Class Structure**:

```java
package com.github.asm0dey;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class VersionListService {

    private List<String> versions;

    @PostConstruct
    void loadVersions() {
        // Load at application startup
    }

    public List<String> getVersions() {
        // Return immutable copy
    }

    private List<String> readVersionsFromFile() throws IOException {
        // Read and parse java_versions.txt
    }
}
```

**Attributes**:

| Attribute | Type | Description |
|-----------|------|-------------|
| versions | `List<String>` | Immutable cached list of version strings |

**Methods**:

| Method | Return Type | Parameters | Description |
|--------|-------------|------------|-------------|
| loadVersions() | void | none | `@PostConstruct` method - loads file at startup |
| getVersions() | `List<String>` | none | Returns immutable copy of version list |
| readVersionsFromFile() | `List<String>` | none | Private helper - reads and parses file |

**Lifecycle**:
1. Bean created at application startup (CDI container initialization)
2. `@PostConstruct loadVersions()` called automatically
3. File loaded once, cached for application lifetime
4. Multiple consumers can call `getVersions()` concurrently (thread-safe)
5. Bean destroyed at application shutdown

**Error Handling**:

| Error Condition | Behavior |
|----------------|----------|
| File not found | Throw runtime exception with clear message - FAIL FAST |
| Empty file | Throw runtime exception - invalid state |
| IOException during read | Throw runtime exception - FAIL FAST |
| Malformed line | Skip line, log warning, continue (per Defensive Data Handling) |

**Validation Rules**:
- Skip empty lines
- Trim leading/trailing whitespace
- Reject lines with only whitespace
- Minimum 1 version required (empty list is invalid)
- No duplicate detection (duplicates allowed in file)

**Thread Safety**:
- `versions` field is set once in `@PostConstruct`
- `getVersions()` returns immutable copy via `Collections.unmodifiableList()`
- No mutable state after initialization
- Safe for concurrent reads without synchronization

### Version List File Format

**File**: `src/main/resources/java_versions.txt`

**Format**: Newline-delimited plain text, one version string per line

**Example**:
```
1.0
1.0.1
1.0.2
...
25
25.0.1
25.0.2
```

**Constraints**:
- UTF-8 encoding
- Unix (LF) or Windows (CRLF) line endings accepted
- No header row
- No comments
- Version strings as-is (no quotes, no escaping)
- Maximum line length: 100 characters (reasonable version string limit)
- Maximum file size: 1MB (thousands of versions)

**Update Process**:
1. Edit file manually or via script
2. Commit to version control
3. Deploy updated application
4. Restart application (new versions loaded at startup)

## API Contract

### GET /api/versions

**Purpose**: Provide version list to frontend for dropdown population

**Request**:
```http
GET /api/versions HTTP/1.1
Host: localhost:8080
Accept: application/json
```

**Response (Success)**:
```http
HTTP/1.1 200 OK
Content-Type: application/json
Cache-Control: public, max-age=3600
Content-Length: 6234

["1.0","1.0.1","1.0.2",...,"25","25.0.1","25.0.2"]
```

**Response Format**: JSON array of strings
- Each element is a version string
- Array preserves file order (important for frontend grouping logic)
- No pagination (list is small, <10KB)
- No filtering (frontend handles search)

**Response Headers**:
- `Content-Type: application/json` - Standard JSON response
- `Cache-Control: public, max-age=3600` - Cache for 1 hour (versions change infrequently)
- `Content-Length` - Size of response body

**Error Response (Internal Error)**:
```http
HTTP/1.1 500 Internal Server Error
Content-Type: application/json

{"error": "Failed to load version list"}
```

**Performance Characteristics**:
- Response time: <10ms (in-memory access)
- Response size: ~6KB uncompressed, ~2KB gzipped
- Concurrent requests: No limit (read-only operation)

**Caching Strategy**:
- Browser cache: 1 hour (versions update infrequently)
- CDN cache: Not applicable (internal tool)
- Server cache: Permanent (loaded at startup)

### Integration with JavaVersionResource

**Existing Class**: `com.github.asm0dey.JavaVersionResource`

**Changes**:
1. Inject `VersionListService`
2. Add new endpoint method for `/api/versions`

**Modified Class Structure**:
```java
@Path("/")
public class JavaVersionResource {

    @Inject
    VersionListService versionListService;  // NEW

    // Existing methods unchanged
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() { ... }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance upload(@FormParam("file") FileUpload fileUpload) { ... }

    // NEW METHOD
    @GET
    @Path("/api/versions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getVersions() {
        return versionListService.getVersions();
    }
}
```

## Frontend Data Flow

### Current State (Hardcoded)

```javascript
// Line 897 in index.html
const javaVersions = ['1.0', '1.0.1', ..., '25.0.2'];
```

### New State (API-driven)

```javascript
let javaVersions = [];  // Empty initially

async function loadVersions() {
    try {
        const response = await fetch('/api/versions');
        if (!response.ok) throw new Error('Failed to load versions');
        javaVersions = await response.json();
        renderDropdown();  // Populate UI
    } catch (error) {
        console.error('Error loading versions:', error);
        showErrorMessage('Failed to load Java versions. Please refresh the page.');
    }
}

// Call on page load
document.addEventListener('DOMContentLoaded', loadVersions);
```

**Loading States**:
1. **Initial**: Show loading spinner, disable version selection
2. **Success**: Hide spinner, populate dropdown, enable selection
3. **Error**: Hide spinner, show error message, suggest refresh

**Error Handling**:
- Network error: Show user-friendly message, log to console
- Invalid JSON: Show error message, log details
- Empty array: Show "No versions available" message

## Data Validation

### Backend Validation (VersionListService)

**File Loading**:
- ✅ File exists at classpath resource path
- ✅ File is readable
- ✅ File contains at least one non-empty line
- ⚠️ Skip lines that are empty or whitespace-only
- ⚠️ Trim whitespace from each line
- ❌ Fail fast if file is missing (throw exception)
- ❌ Fail fast if all lines are invalid (throw exception)

**Version String Validation**:
- Accept any non-empty string after trimming
- No format validation (versions have varied formats historically)
- No duplicate checking (allow duplicates if present in file)
- No sorting (preserve file order)

### Frontend Validation

**API Response**:
- ✅ Response is valid JSON
- ✅ Response is an array
- ✅ Array contains strings
- ⚠️ Empty array: Show "No versions" message
- ❌ Invalid JSON: Show error, log details

**No validation of version string format** (backend is source of truth)

## Testing Strategy

### Unit Tests (Backend)

**VersionListServiceTest**:

```java
@QuarkusTest
class VersionListServiceTest {

    @Inject
    VersionListService service;

    @Test
    void shouldLoadVersionsFromFile() {
        List<String> versions = service.getVersions();
        assertNotNull(versions);
        assertFalse(versions.isEmpty());
        assertTrue(versions.contains("1.0"));
        assertTrue(versions.contains("25"));
    }

    @Test
    void shouldReturnImmutableList() {
        List<String> versions = service.getVersions();
        assertThrows(UnsupportedOperationException.class,
            () -> versions.add("999"));
    }

    @Test
    void shouldHandleEmptyLines() {
        // Test that empty lines are skipped
        // (requires test resource file)
    }
}
```

**JavaVersionResourceTest** (addition):

```java
@QuarkusTest
class JavaVersionResourceTest {

    @Test
    void testGetVersionsEndpoint() {
        given()
            .when().get("/api/versions")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("$", hasSize(greaterThan(500)))
            .body("[0]", equalTo("1.0"));
    }

    @Test
    void testVersionsEndpointCaching() {
        given()
            .when().get("/api/versions")
            .then()
            .header("Cache-Control", containsString("max-age"));
    }
}
```

### Integration Tests

**Frontend-Backend Consistency Test**:
1. Load versions from API
2. Parse java_versions.txt directly in test
3. Assert arrays are identical (order, content)

### Manual Test Cases

| Test Case | Steps | Expected Result |
|-----------|-------|----------------|
| Initial page load | Navigate to / | Version dropdown populates with 500+ versions |
| Version selection | Select "17.0.1" from dropdown | Version appears as chip, can be removed |
| API response | Call `/api/versions` directly | Returns JSON array of strings |
| File update | Edit java_versions.txt, restart | New version appears in dropdown |
| Network error simulation | Block /api/versions with DevTools | Error message shown, page remains usable |

## Migration Path

**No data migration required** - file format unchanged

**Deployment Steps**:
1. Deploy new backend with `VersionListService` and `/api/versions` endpoint
2. Deploy new frontend with API fetch logic
3. Verify both interfaces show same versions
4. Monitor logs for file loading errors

**Rollback Plan**:
1. Revert frontend to hardcoded array
2. Backend changes are backward compatible (no breaking changes)

## Performance Benchmarks

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| Startup time | +0ms | Application logs |
| API response time | <10ms | RestAssured test with timing |
| Memory overhead | <10KB | Heap dump analysis |
| Concurrent requests | 100 req/s | Load test with Apache Bench |

## Security Considerations

- **No authentication required**: Version list is public information
- **No user input**: API returns static data, no injection risk
- **No sensitive data**: Version strings are public knowledge
- **Rate limiting**: Not required (lightweight endpoint)
- **CORS**: Not needed (same-origin frontend)

## Maintenance

**Adding new Java version**:
1. Edit `src/main/resources/java_versions.txt`
2. Add new version string on new line
3. Commit and deploy
4. Restart application

**Removing deprecated version**:
1. Remove line from `java_versions.txt`
2. Commit and deploy
3. Restart application

**No code changes required for version list updates**

## Dependencies

**New Classes**:
- `com.github.asm0dey.VersionListService` (new)

**Modified Classes**:
- `com.github.asm0dey.JavaVersionResource` (add endpoint method)
- `src/main/resources/templates/JavaVersionResource/index.html` (fetch from API)

**No new external dependencies**

## Compliance with Constitution

✅ **Accuracy in Version Detection**: Single source of truth ensures consistency
✅ **License Compliance Transparency**: No changes to license logic
✅ **Defensive Data Handling**: Validation, error handling, fail-fast on critical errors
✅ **Clear Risk Communication**: No changes to risk categorization
✅ **Comprehensive Test Coverage**: Unit tests, integration tests, API tests added

---

**Next**: Create API contract specification in `contracts/`
