# Research: Centralize Java Versions Data Source

**Feature**: 004-centralize-version-source
**Date**: 2026-02-17
**Status**: Complete

## Overview

Research to determine the best technical approach for centralizing Java version data from `src/main/resources/java_versions.txt` to serve both backend and frontend consistently.

## Current State Analysis

### Backend
- Reads `java_versions.txt` at runtime for version categorization logic
- File contains simple newline-delimited version strings (523 versions)
- Used by `JavaVersionService.determineVersionAge()` for traffic-light categorization
- File is already the source of truth for backend version logic

### Frontend
- Has hardcoded JavaScript array of 523 version strings (line 897 of index.html)
- Array is manually maintained and must match backend file
- Used for combobox dropdown and version selection UI
- Risk: Array can drift out of sync with backend file

### Problem
- **Duplication**: Same data exists in two places (file + JavaScript)
- **Maintenance burden**: Updates require changing two locations
- **Consistency risk**: No guarantee frontend and backend stay synchronized
- **No validation**: Frontend array changes aren't validated against backend logic

## Research Questions & Decisions

### 1. How should the frontend consume version data from backend?

**Options Evaluated**:

A. **REST API endpoint returning JSON** ⭐ CHOSEN
   - Pros: Dynamic, standard pattern, easy to test, cache-friendly
   - Cons: Requires network call (mitigated by caching)
   - Implementation: `GET /api/versions` returns JSON array

B. **Embed data in page template during server render**
   - Pros: No extra HTTP request, works without JavaScript
   - Cons: Increases initial page size, harder to cache separately
   - Not chosen: Frontend uses AJAX pattern for other features

C. **Serve as static JavaScript file generated at startup**
   - Pros: Separate cacheable resource
   - Cons: Adds build complexity, non-standard for Quarkus
   - Not chosen: Overengineered for this use case

**Decision**: Option A (REST API)
**Rationale**:
- Aligns with existing architecture (REST endpoints in JavaVersionResource)
- Frontend already uses fetch() for form submission
- Easy to add browser caching headers
- Testable with RestAssured
- Minimal code changes required

### 2. What format should the API response use?

**Options Evaluated**:

A. **Simple string array** ⭐ CHOSEN
   ```json
   ["1.0", "1.0.1", ..., "25.0.2"]
   ```
   - Pros: Minimal bandwidth, matches file format, frontend already expects strings
   - Cons: No metadata (not needed for this feature)

B. **Enriched objects with metadata**
   ```json
   [{"version": "1.0", "eol": "1998-01-01", "license": "free"}, ...]
   ```
   - Pros: Could add EOL dates, license info
   - Cons: File doesn't contain this data, would require separate maintenance
   - Not chosen: Out of scope for this feature

**Decision**: Option A (simple string array)
**Rationale**:
- File format is simple newline-delimited strings
- Frontend only needs version strings for dropdown
- Metadata (license, EOL) is derived at analysis time, not selection time
- Keep it simple per constitution principle

### 3. How to handle file reload without restart?

**Options Evaluated**:

A. **No reload - restart required** ⭐ CHOSEN
   - Pros: Simplest implementation, matches current behavior
   - Cons: Requires restart to pick up changes
   - Mitigation: File changes are infrequent (new Java releases ~quarterly)

B. **Watch file for changes**
   - Pros: Automatic detection
   - Cons: Requires file watching library, complexity for rare use case
   - Not chosen: Overkill for infrequent updates

C. **Admin endpoint to trigger reload**
   - Pros: Manual control, no automatic watching
   - Cons: Requires authentication/authorization, more code
   - Not chosen: Adds security considerations

**Decision**: Option A (no reload, restart required)
**Rationale**:
- Simplest implementation
- File updates are rare (new Java releases every 3-6 months)
- Deployment process already includes restart
- Can add hot-reload later if needed

### 4. Should we enhance java_versions.txt format?

**Options Evaluated**:

A. **Keep as simple newline-delimited list** ⭐ CHOSEN
   - Pros: Human-readable, easy to edit, no parsing complexity
   - Cons: No metadata in file
   - Current format: One version per line, no headers

B. **Add CSV columns for metadata**
   ```
   version,eol_date,license
   1.0,1998-01-01,free
   ```
   - Pros: Could store EOL dates, license rules
   - Cons: Harder to edit manually, requires CSV parsing
   - Not chosen: Constitution requires human-readable format

C. **Use JSON/YAML for structured data**
   - Pros: Flexible structure
   - Cons: Harder to edit, requires parser, violates human-readable constraint
   - Not chosen: Over-engineered

**Decision**: Option A (keep simple format)
**Rationale**:
- Constitution requires "human-readable, text-based" format
- Current format works well
- Metadata (EOL, license) is derived from version string + date logic
- Easy to add new versions manually
- No breaking changes to file format

## Technical Decisions Summary

| Decision | Choice | Justification |
|----------|--------|---------------|
| Frontend consumption | REST API endpoint | Standard pattern, testable, cacheable |
| API response format | JSON string array | Simple, sufficient, low bandwidth |
| File reload strategy | Restart required | Simple, matches current behavior, rare updates |
| File format | Keep as-is | Human-readable, constitution compliant |

## Implementation Approach

### Phase 1: Backend Changes
1. Create `VersionListService` class
   - `@ApplicationScoped` CDI bean
   - Load `java_versions.txt` at startup via `@PostConstruct`
   - Cache list in memory
   - Provide `getVersions()` method returning `List<String>`
   - Include validation: skip empty lines, trim whitespace

2. Add API endpoint to `JavaVersionResource`
   - `@GET @Path("/api/versions") @Produces(MediaType.APPLICATION_JSON)`
   - Inject `VersionListService`
   - Return version list as JSON array
   - Add `@Cache-Control` header (1 hour)

3. Add tests
   - `VersionListServiceTest`: Test file loading, validation, error handling
   - `JavaVersionResourceTest`: Test API endpoint, response format, caching headers

### Phase 2: Frontend Changes
1. Remove hardcoded `javaVersions` array from index.html
2. Add `loadVersions()` function to fetch from `/api/versions`
3. Call `loadVersions()` on page load
4. Handle loading state (show spinner while fetching)
5. Handle errors (fallback message if API fails)
6. Populate dropdown after successful fetch

### Phase 3: Validation
1. Integration test: Verify frontend and backend serve identical version lists
2. Manual test: Update `java_versions.txt`, restart, verify both interfaces show update
3. Performance test: Verify API response time <100ms

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| API call fails on page load | Frontend unusable | Add error handling, show fallback message |
| Network latency delays dropdown | Poor UX | Add loading indicator, consider in-page embedding for future optimization |
| File missing at startup | App fails to start | Validation at startup with clear error message |
| Malformed file content | Runtime errors | Robust parsing with validation, skip invalid lines |

## Tessl Tiles

### Installed Tiles

| Technology | Tile | Type | Version |
|------------|------|------|---------|
| Quarkus REST | tessl/maven-io-quarkus--quarkus-rest | Documentation | 3.15.0 |
| Quarkus JUnit5 | tessl/maven-io-quarkus--quarkus-junit5 | Documentation | 3.26.0 |
| JUnit Jupiter | tessl/maven-org-junit-jupiter--junit-jupiter | Documentation | 5.12.0 |

### Available Documentation

Tiles provide API documentation and best practices for:
- Quarkus REST endpoint patterns and annotations
- CDI injection in Quarkus applications
- JUnit 5 test patterns and assertions
- Quarkus test extensions (@QuarkusTest, @InjectMock)

### Technologies Without Tiles

- Qute templates: No dedicated tile (simple template engine, minimal API)
- RestAssured: Available but not installed (testing framework, well-documented)

## Dependencies

- No new external dependencies required
- Uses existing Quarkus REST and CDI features
- Frontend uses native `fetch()` API (no library needed)

## Performance Considerations

- File read once at startup: <1ms for 10KB file
- In-memory cache: O(1) access, ~4KB memory overhead
- JSON serialization: <1ms for 523 strings
- Network transfer: ~6KB gzipped, <50ms on typical connections
- Browser caching: Reduce subsequent loads to 0ms

## Alternatives Considered and Rejected

1. **GraphQL endpoint**: Over-engineered for simple array response
2. **WebSocket real-time updates**: Unnecessary complexity, versions change infrequently
3. **Shared database**: File-based solution is simpler, no DB needed for static data
4. **Git submodule for version list**: Adds deployment complexity
5. **Package version list as separate JAR**: Overengineered

## Open Questions

None - all research questions resolved.

## Next Steps

1. Create `data-model.md` with detailed class designs
2. Create `contracts/` with OpenAPI specification
3. Run `/iikit-06-tasks` to generate implementation task breakdown
