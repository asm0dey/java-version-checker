# Test Specifications: Centralize Java Versions Data Source

**Generated**: 2026-02-17
**Feature**: [spec.md](../spec.md) | **Plan**: [plan.md](../plan.md) | **Data Model**: [data-model.md](../data-model.md)

## TDD Assessment

**Determination**: optional
**Confidence**: high
**Evidence**: "Comprehensive Test Coverage - All version parsing, license detection, and categorization logic must be covered by automated tests" (Constitution §V)
**Reasoning**: Constitution requires comprehensive test coverage but does not mandate test-first development order. TDD is recommended but not constitutionally required.

---

<!--
DO NOT MODIFY TEST ASSERTIONS

These test specifications define the expected behavior derived from requirements.
During implementation:
- Fix code to pass tests, don't modify test assertions
- Structural changes (file organization, naming) are acceptable with justification
- Logic changes to assertions require explicit justification and re-review

If requirements change, re-run /iikit-05-testify to regenerate test specs.
-->

## From spec.md (Acceptance Tests)

### TS-001: Backend returns version data from central source

**Source**: spec.md:User Story 1:scenario-1
**Type**: acceptance
**Priority**: P1

**Given**: a Java version entry in the central data source
**When**: the backend analyzes a properties file containing that version
**Then**: the backend returns the exact version categorization and license information as defined in the central source

**Traceability**: FR-001, FR-002, US-001-scenario-1, SC-001

---

### TS-002: Frontend displays version data from central source

**Source**: spec.md:User Story 1:scenario-2
**Type**: acceptance
**Priority**: P1

**Given**: the same Java version entry in the central data source
**When**: the frontend displays version selection or analysis results
**Then**: the frontend shows identical categorization, license information, and recommendations as the backend

**Traceability**: FR-001, FR-003, US-001-scenario-2, SC-001

---

### TS-003: Version data updates reflect after restart

**Source**: spec.md:User Story 1:scenario-3
**Type**: acceptance
**Priority**: P1

**Given**: an update to the central data source (e.g., marking a version as end-of-life)
**When**: the application is restarted
**Then**: both frontend and backend immediately reflect the updated information without code changes

**Traceability**: FR-005, FR-009, US-001-scenario-3, SC-002

---

### TS-004: New version addition recognized after restart

**Source**: spec.md:User Story 2:scenario-1
**Type**: acceptance
**Priority**: P2

**Given**: a new Java version is released
**When**: an administrator adds the version entry to the central data source file and restarts the application
**Then**: both backend and frontend recognize and correctly categorize the new version

**Traceability**: FR-001, FR-008, US-002-scenario-1, SC-002

---

### TS-005: License policy updates apply after restart

**Source**: spec.md:User Story 2:scenario-2
**Type**: acceptance
**Priority**: P2

**Given**: a vendor changes licensing terms for a specific version
**When**: the administrator updates the license information in the central data source and restarts the application
**Then**: all subsequent analysis and displays reflect the updated license requirements

**Traceability**: FR-001, FR-008, US-002-scenario-2, SC-003

---

### TS-006: EOL status updates apply after restart

**Source**: spec.md:User Story 2:scenario-3
**Type**: acceptance
**Priority**: P2

**Given**: a version reaches end-of-life status
**When**: the administrator updates the status in the central data source and restarts the application
**Then**: the system immediately begins warning users about that version in both interfaces

**Traceability**: FR-001, FR-008, US-002-scenario-3, SC-001

---

### TS-007: Invalid file format reports validation errors

**Source**: spec.md:User Story 3:scenario-1
**Type**: acceptance
**Priority**: P3

**Given**: the central data source file contains invalid formatting
**When**: the application starts or reloads configuration
**Then**: the system reports clear validation errors identifying the specific lines or entries with problems

**Traceability**: FR-004, FR-006, US-003-scenario-1, SC-005

---

### TS-008: Missing required fields rejected with guidance

**Source**: spec.md:User Story 3:scenario-2
**Type**: acceptance
**Priority**: P3

**Given**: a version entry is missing required fields
**When**: the file is loaded
**Then**: the system rejects the incomplete entry and provides explicit guidance on required fields

**Traceability**: FR-004, FR-006, US-003-scenario-2

---

### TS-009: Conflicting entries detected and reported

**Source**: spec.md:User Story 3:scenario-3
**Type**: acceptance
**Priority**: P3

**Given**: version entries contain conflicting information (e.g., overlapping version ranges)
**When**: the file is validated
**Then**: the system detects and reports the conflicts

**Traceability**: FR-004, FR-006, US-003-scenario-3

---

## From plan.md (Contract Tests)

### TS-010: GET /api/versions returns JSON array

**Source**: plan.md:Phase 1:API Contract
**Type**: contract
**Priority**: P1

**Given**: API is available and version service is initialized
**When**: GET request is made to /api/versions with Accept: application/json
**Then**: Response returns HTTP 200 with Content-Type: application/json and body is a JSON array of strings

**Traceability**: FR-003, plan.md:API Contract

**Contract Details**:
- Endpoint: `GET /api/versions`
- Response status: `200 OK`
- Response headers: `Content-Type: application/json`, `Cache-Control: public, max-age=3600`
- Response body: JSON array of version strings (e.g., `["1.0", "1.0.1", ..., "25.0.2"]`)

---

### TS-011: GET /api/versions preserves file order

**Source**: plan.md:Phase 1:API Contract
**Type**: contract
**Priority**: P1

**Given**: java_versions.txt contains versions in a specific order
**When**: GET request is made to /api/versions
**Then**: Response array preserves the exact order from the file

**Traceability**: FR-001, data-model.md:API Contract

**Contract Details**:
- Array order must match file line order exactly
- No sorting or reordering allowed
- Frontend depends on this order for grouping logic

---

### TS-012: GET /api/versions returns cache headers

**Source**: plan.md:Phase 1:API Contract
**Type**: contract
**Priority**: P2

**Given**: API is available
**When**: GET request is made to /api/versions
**Then**: Response includes Cache-Control header with max-age=3600

**Traceability**: plan.md:API Contract, performance goals

**Contract Details**:
- Header: `Cache-Control: public, max-age=3600`
- Rationale: Versions change infrequently (quarterly)
- Reduces server load and improves frontend performance

---

### TS-013: GET /api/versions responds within 100ms

**Source**: plan.md:Technical Context:Performance Goals
**Type**: contract
**Priority**: P1

**Given**: API is available with version list loaded
**When**: GET request is made to /api/versions
**Then**: Response is returned within 100ms (target: <10ms)

**Traceability**: plan.md:Performance Goals, SC-001

**Performance Criteria**:
- Target: <10ms (in-memory access)
- Maximum acceptable: 100ms
- Test with concurrent requests: 100 req/s

---

## From data-model.md (Validation Tests)

### TS-014: VersionListService loads file at startup

**Source**: data-model.md:VersionListService:Lifecycle
**Type**: validation
**Priority**: P1

**Given**: application is starting up
**When**: CDI container initializes VersionListService bean
**Then**: @PostConstruct loadVersions() is called automatically and java_versions.txt is loaded into memory

**Traceability**: FR-004, FR-005, data-model.md:VersionListService

**Validation Criteria**:
- File read once at startup
- Cached in memory for application lifetime
- No file reads during request handling

---

### TS-015: VersionListService returns immutable list

**Source**: data-model.md:VersionListService:Thread Safety
**Type**: validation
**Priority**: P1

**Given**: VersionListService has loaded version list
**When**: getVersions() is called and caller attempts to modify returned list
**Then**: UnsupportedOperationException is thrown (list is immutable)

**Traceability**: FR-009, data-model.md:Thread Safety

**Validation Criteria**:
- `Collections.unmodifiableList()` used
- No mutable state exposed
- Thread-safe for concurrent reads

---

### TS-016: VersionListService skips empty lines

**Source**: data-model.md:VersionListService:Validation Rules
**Type**: validation
**Priority**: P2

**Given**: java_versions.txt contains empty lines or whitespace-only lines
**When**: file is loaded during startup
**Then**: empty lines are skipped and only non-empty trimmed lines are included in version list

**Traceability**: FR-004, data-model.md:Validation Rules

**Validation Criteria**:
- Skip completely empty lines
- Skip whitespace-only lines
- Trim leading/trailing whitespace from valid lines

---

### TS-017: VersionListService trims whitespace

**Source**: data-model.md:VersionListService:Validation Rules
**Type**: validation
**Priority**: P2

**Given**: java_versions.txt contains version strings with leading/trailing whitespace
**When**: file is loaded during startup
**Then**: whitespace is trimmed from each version string

**Traceability**: FR-004, data-model.md:Validation Rules

---

### TS-018: VersionListService fails fast on missing file

**Source**: data-model.md:VersionListService:Error Handling
**Type**: validation
**Priority**: P1

**Given**: java_versions.txt is missing from classpath
**When**: application starts and VersionListService attempts to load file
**Then**: RuntimeException is thrown with clear error message and application fails to start

**Traceability**: FR-007, SC-005, data-model.md:Error Handling

**Validation Criteria**:
- FAIL FAST - do not start with invalid state
- Clear error message identifying missing file
- No fallback behavior (prevent inconsistent operation)

---

### TS-019: VersionListService fails fast on empty file

**Source**: data-model.md:VersionListService:Error Handling
**Type**: validation
**Priority**: P1

**Given**: java_versions.txt exists but contains no non-empty lines
**When**: application starts and VersionListService attempts to load file
**Then**: RuntimeException is thrown indicating empty file is invalid state

**Traceability**: FR-004, FR-007, SC-005, data-model.md:Error Handling

**Validation Criteria**:
- Minimum 1 version required
- Empty list is invalid state
- Application must not start with empty version list

---

### TS-020: VersionListService logs warning on malformed line

**Source**: data-model.md:VersionListService:Error Handling
**Type**: validation
**Priority**: P3

**Given**: java_versions.txt contains a malformed line
**When**: file is loaded during startup
**Then**: malformed line is skipped, warning is logged, and file loading continues

**Traceability**: FR-006, data-model.md:Error Handling (Defensive Data Handling)

**Validation Criteria**:
- Skip line that cannot be parsed
- Log warning with line number and content
- Continue loading valid lines
- Fail only if ALL lines are invalid

---

### TS-021: Frontend loads versions on page load

**Source**: data-model.md:Frontend Data Flow:New State
**Type**: validation
**Priority**: P1

**Given**: user navigates to application homepage
**When**: DOMContentLoaded event fires
**Then**: loadVersions() function is called and fetches /api/versions

**Traceability**: FR-003, data-model.md:Frontend Data Flow

**Validation Criteria**:
- Fetch triggered automatically on page load
- No user interaction required
- Loading state shown during fetch

---

### TS-022: Frontend shows loading state during version fetch

**Source**: data-model.md:Frontend Data Flow:Loading States
**Type**: validation
**Priority**: P2

**Given**: frontend is fetching versions from API
**When**: fetch is in progress
**Then**: loading spinner is shown and version selection is disabled

**Traceability**: data-model.md:Loading States (UX requirement)

**Validation Criteria**:
- Loading spinner visible
- Version dropdown disabled
- Other UI remains interactive

---

### TS-023: Frontend populates dropdown on successful fetch

**Source**: data-model.md:Frontend Data Flow:Loading States
**Type**: validation
**Priority**: P1

**Given**: frontend successfully fetches versions from API
**When**: response is valid JSON array
**Then**: loading spinner is hidden, dropdown is populated with versions, and selection is enabled

**Traceability**: FR-003, SC-001, data-model.md:Loading States

---

### TS-024: Frontend shows error on failed fetch

**Source**: data-model.md:Frontend Data Flow:Loading States
**Type**: validation
**Priority**: P1

**Given**: frontend fetch to /api/versions fails
**When**: network error or non-2xx status code occurs
**Then**: loading spinner is hidden, error message is shown, and error is logged to console

**Traceability**: data-model.md:Error Handling

**Validation Criteria**:
- User-friendly error message displayed
- Detailed error logged to console
- Suggest refresh action to user
- Page remains usable (upload tab still works)

---

### TS-025: Frontend validates API response is JSON array

**Source**: data-model.md:Frontend Validation:API Response
**Type**: validation
**Priority**: P1

**Given**: frontend receives response from /api/versions
**When**: response is not valid JSON or not an array
**Then**: error is shown and logged with details

**Traceability**: data-model.md:Frontend Validation

**Validation Criteria**:
- Check response is valid JSON
- Check response is array type
- Check array elements are strings

---

## Edge Case Tests

### TS-026: Handle missing file at startup

**Source**: spec.md:Edge Cases:line-60
**Type**: edge-case
**Priority**: P1

**Given**: java_versions.txt is missing from classpath
**When**: application attempts to start
**Then**: application fails to start with clear error message indicating missing file

**Traceability**: FR-007, SC-005, Edge Case #1

---

### TS-027: Handle concurrent reads during startup

**Source**: spec.md:Edge Cases:line-61
**Type**: edge-case
**Priority**: P2

**Given**: multiple threads attempt to read version list during/after startup
**When**: VersionListService is accessed concurrently
**Then**: all threads receive consistent data without race conditions or exceptions

**Traceability**: FR-009, FR-010, Edge Case #2

**Validation Criteria**:
- Thread-safe read access
- No concurrent modification exceptions
- Immutable list prevents data corruption

---

### TS-028: Handle corrupted file content

**Source**: spec.md:Edge Cases:line-62
**Type**: edge-case
**Priority**: P1

**Given**: java_versions.txt contains unparseable or corrupted content
**When**: application starts and loads file
**Then**: application either fails fast with clear error OR skips invalid lines and loads valid ones (per malformed line handling)

**Traceability**: FR-006, FR-007, Edge Case #3

---

### TS-029: Handle file modification during read

**Source**: spec.md:Edge Cases:line-63
**Type**: edge-case
**Priority**: P3

**Given**: java_versions.txt is modified while being read during startup
**When**: file read is in progress
**Then**: read completes with either old or new content (no partial read) OR fails with clear error

**Traceability**: FR-010, Edge Case #4

**Note**: Since file is read once at startup and system requires restart for updates, this is low priority but should be handled gracefully.

---

### TS-030: Handle unknown version detection

**Source**: spec.md:Edge Cases:line-64
**Type**: edge-case
**Priority**: P3

**Given**: a Java version is detected in properties file that has no corresponding entry in central data source
**When**: backend processes the properties file
**Then**: system behavior is well-defined (log warning, use default categorization, or report as unknown)

**Traceability**: Edge Case #5

**Note**: Specific behavior to be defined during implementation based on existing JavaVersionService logic.

---

## Integration Tests

### TS-031: Frontend-backend consistency verification

**Source**: SC-001, requirement for 100% consistency
**Type**: integration
**Priority**: P1

**Given**: java_versions.txt contains a known set of versions
**When**: frontend fetches from /api/versions AND backend loads from file
**Then**: frontend receives identical version list as backend uses internally (order, content, count)

**Traceability**: FR-001, FR-009, SC-001

**Test Approach**:
1. Read java_versions.txt directly in test
2. Call GET /api/versions
3. Compare arrays for exact match (order and content)

---

### TS-032: Version update end-to-end test

**Source**: SC-002, SC-003, requirement for single-file updates
**Type**: integration
**Priority**: P2

**Given**: application is running with known version list
**When**: java_versions.txt is updated with new version and application is restarted
**Then**: both frontend dropdown and backend analysis recognize the new version

**Traceability**: FR-005, SC-002, SC-003

**Test Approach**:
1. Add test version to file
2. Restart application
3. Verify frontend dropdown includes new version
4. Verify backend API returns new version
5. Verify no code changes were required

---

## Summary

| Source | Count | Types |
|--------|-------|-------|
| spec.md | 9 | acceptance |
| plan.md | 4 | contract |
| data-model.md | 12 | validation |
| Edge Cases | 5 | edge-case |
| Integration | 2 | integration |
| **Total** | **32** | |

### Coverage by Priority

| Priority | Count | Percentage |
|----------|-------|------------|
| P1 | 18 | 56% |
| P2 | 8 | 25% |
| P3 | 6 | 19% |

### Coverage by Functional Requirement

| Requirement | Test IDs | Coverage |
|-------------|----------|----------|
| FR-001 | TS-001, TS-002, TS-004, TS-005, TS-006, TS-011, TS-031 | ✅ High |
| FR-002 | TS-001 | ✅ Covered |
| FR-003 | TS-002, TS-010, TS-021, TS-023 | ✅ High |
| FR-004 | TS-007, TS-008, TS-009, TS-014, TS-016, TS-019 | ✅ High |
| FR-005 | TS-003, TS-014, TS-032 | ✅ Covered |
| FR-006 | TS-007, TS-008, TS-009, TS-020, TS-028 | ✅ High |
| FR-007 | TS-018, TS-019, TS-026, TS-028 | ✅ High |
| FR-008 | TS-004, TS-005, TS-006 | ✅ Covered |
| FR-009 | TS-003, TS-015, TS-027, TS-031 | ✅ High |
| FR-010 | TS-029, TS-027 | ✅ Covered |

### Coverage by Success Criterion

| Criterion | Test IDs | Coverage |
|-----------|----------|----------|
| SC-001 | TS-001, TS-002, TS-006, TS-013, TS-023, TS-031 | ✅ High |
| SC-002 | TS-003, TS-004, TS-032 | ✅ Covered |
| SC-003 | TS-005, TS-032 | ✅ Covered |
| SC-004 | TS-031 (monitors consistency) | ✅ Covered |
| SC-005 | TS-007, TS-018, TS-019, TS-026 | ✅ High |
| SC-006 | (All tests use production data source) | ✅ Implicit |

---

**Assertion Integrity**: Test specifications locked. Any modification requires re-running `/iikit-05-testify`.
