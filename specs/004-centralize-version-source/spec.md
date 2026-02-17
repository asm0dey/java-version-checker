# Feature Specification: Centralize Java Versions Data Source

**Feature Branch**: `004-centralize-version-source`
**Created**: 2026-02-17
**Status**: Draft
**Input**: User description: "For backend and for frontend the list of java versions should be supplied from the same source: src/main/resources/java_versions.txt"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consistent Version Data Across Application (Priority: P1)

As an application maintainer, I need the backend and frontend to display identical Java version information so that users receive consistent analysis results regardless of which interface they use.

**Why this priority**: Data consistency is critical for maintaining trust in the application. If the frontend and backend report different version categorizations or recommendations, users will lose confidence in the tool's reliability. This aligns with the constitution's principle of "Accuracy in Version Detection" and "Clear Risk Communication."

**Independent Test**: Can be fully tested by configuring a test version entry in the centralized data source, then verifying both frontend and backend display identical version categorization, license requirements, and risk assessment for that version.

**Acceptance Scenarios**:

1. **Given** a Java version entry in the central data source, **When** the backend analyzes a properties file containing that version, **Then** the backend returns the exact version categorization and license information as defined in the central source
2. **Given** the same Java version entry in the central data source, **When** the frontend displays version selection or analysis results, **Then** the frontend shows identical categorization, license information, and recommendations as the backend
3. **Given** an update to the central data source (e.g., marking a version as end-of-life), **When** the application is restarted, **Then** both frontend and backend immediately reflect the updated information without code changes

---

### User Story 2 - Single-Point Version Data Maintenance (Priority: P2)

As a system administrator, I need to update Java version information in a single location so that I can quickly respond to new Java releases, licensing changes, or end-of-life announcements without modifying multiple files or components.

**Why this priority**: Maintenance efficiency directly impacts how quickly the application can respond to important updates like security announcements or licensing policy changes. This supports the constitution's requirement for "License Rule Currency."

**Independent Test**: Can be fully tested by adding a new Java version entry to the central data source and verifying it becomes immediately available in both backend analysis and frontend displays without any code changes required.

**Acceptance Scenarios**:

1. **Given** a new Java version is released, **When** an administrator adds the version entry to the central data source file and restarts the application, **Then** both backend and frontend recognize and correctly categorize the new version
2. **Given** a vendor changes licensing terms for a specific version, **When** the administrator updates the license information in the central data source and restarts the application, **Then** all subsequent analysis and displays reflect the updated license requirements
3. **Given** a version reaches end-of-life status, **When** the administrator updates the status in the central data source and restarts the application, **Then** the system immediately begins warning users about that version in both interfaces

---

### User Story 3 - Validated Version Data Format (Priority: P3)

As a developer or administrator, I need the central version data source to have a clearly defined, validated format so that data entry errors are caught early and don't cause runtime failures or inconsistent behavior.

**Why this priority**: While less critical than consistency and maintainability, format validation prevents operational issues and reduces debugging time. This supports the constitution's "Defensive Data Handling" principle.

**Independent Test**: Can be fully tested by attempting to load a malformed version data file and verifying the system reports clear validation errors with specific guidance on what needs to be corrected.

**Acceptance Scenarios**:

1. **Given** the central data source file contains invalid formatting, **When** the application starts or reloads configuration, **Then** the system reports clear validation errors identifying the specific lines or entries with problems
2. **Given** a version entry is missing required fields, **When** the file is loaded, **Then** the system rejects the incomplete entry and provides explicit guidance on required fields
3. **Given** version entries contain conflicting information (e.g., overlapping version ranges), **When** the file is validated, **Then** the system detects and reports the conflicts

---

### Edge Cases

- What happens when the central data source file is missing or inaccessible at startup?
- How does the system handle concurrent reads from backend and frontend during a configuration reload?
- What occurs if the data source file is corrupted or contains unparseable content?
- How are partial updates handled if the file is modified while being read?
- What is the behavior when a Java version is detected that has no corresponding entry in the central data source?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST read Java version information from a single centralized data source file
- **FR-002**: Backend analysis logic MUST retrieve all version categorization, licensing, and lifecycle data exclusively from the central data source
- **FR-003**: Frontend display logic MUST retrieve all version categorization, licensing, and lifecycle data exclusively from the central data source
- **FR-004**: System MUST validate the structure and completeness of the central data source file at application startup
- **FR-005**: System SHALL load version data once at startup; updates to the central data source require application restart (hot-reload deferred for simplicity)
- **FR-006**: System MUST detect and report errors in the central data source format with sufficient detail for correction
- **FR-007**: System MUST gracefully handle missing or inaccessible central data source files by reporting the issue and providing fallback behavior or preventing incomplete operation
- **FR-008**: Central data source MUST include version identification patterns, categorization rules, license requirement flags, and lifecycle status for each Java version
- **FR-009**: System MUST prevent partial or inconsistent data states where backend and frontend read different versions of the data source
- **FR-010**: Updates to the central data source MUST be atomic from the perspective of consumers (no partial reads)

### Key Entities

- **Java Version Entry**: Represents a specific Java version or version range with associated metadata including:
  - Version identifier and matching patterns
  - Categorization (lifecycle stage)
  - License requirements (commercial vs free)
  - End-of-life dates
  - Vendor information
  - Recommendation level

- **Central Data Source**: The single source of truth for all Java version information:
  - Structured format (text-based, human-readable)
  - Located at predictable path (src/main/resources/java_versions.txt as specified)
  - Versioned or timestamped for tracking changes
  - Validated on load

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All version categorizations displayed in frontend match backend analysis results with 100% consistency
- **SC-002**: Adding a new Java version entry requires modification of exactly one file with no code changes
- **SC-003**: License policy updates can be deployed within 5 minutes of decision (file edit and reload/restart only)
- **SC-004**: Zero inconsistencies between frontend and backend version data in production monitoring
- **SC-005**: System startup fails safely with clear error message if central data source is invalid, preventing operation with inconsistent data
- **SC-006**: All automated tests pass using the same central data source file as production, ensuring test coverage of actual data

## Clarifications

### Session 2026-02-17

- Q: Should the system support hot-reload of version data without restart, or is restart acceptable? -> A: Restart required (Option B) - Simplicity over complexity for infrequent updates (quarterly Java releases). File changes are rare, deployment includes restart anyway, can add hot-reload later if needed.
