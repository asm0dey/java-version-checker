# Implementation Tasks: Centralize Java Versions Data Source

**Feature**: 004-centralize-version-source
**Branch**: `004-centralize-version-source`
**Created**: 2026-02-17
**Source**: [spec.md](spec.md) | [plan.md](plan.md) | [data-model.md](data-model.md) | [test-specs.md](tests/test-specs.md)

## Overview

Implement centralized Java version data source that serves both backend and frontend from single file (`src/main/resources/java_versions.txt`). Frontend will fetch versions via REST API instead of using hardcoded array.

**Total Tasks**: 25
**Test Coverage**: 32 test specifications (from `/iikit-05-testify`)
**User Stories**: 3 (P1, P2, P3)

## MVP Scope

**Minimum Viable Product** = User Story 1 (P1): Consistent Version Data
- Tasks: T001-T016
- Delivers: Core functionality for frontend-backend consistency
- Tests: TS-001, TS-002, TS-003, TS-010, TS-011, TS-014, TS-015, TS-021, TS-023, TS-031

---

## Phase 1: Setup & Infrastructure

**Purpose**: Project structure verification and test resource preparation

- [x] T001 Verify project structure matches plan.md expectations (src/main/java/com/github/asm0dey/, src/test/java/, src/main/resources/)
- [x] T002 Verify java_versions.txt exists at src/main/resources/java_versions.txt with 500+ version entries
- [x] T003 [P] Create test resource file src/test/resources/java_versions_test.txt with sample versions for unit testing
- [x] T004 [P] Review existing JavaVersionResource.java to understand current endpoint structure and CDI patterns

**Completion Criteria**: All source directories verified, test resources created, existing code reviewed

---

## Phase 2: Foundational - VersionListService (Blocks all user stories)

**Purpose**: Create core service that loads and serves version list - foundational for all stories

### Backend Service Implementation

- [x] T005 Create VersionListService class in src/main/java/com/github/asm0dey/VersionListService.java with @ApplicationScoped annotation
- [x] T006 Implement @PostConstruct loadVersions() method to read java_versions.txt from classpath
- [x] T007 Implement file parsing logic: skip empty lines, trim whitespace, validate minimum 1 version (satisfies TS-016, TS-017)
- [x] T008 Implement getVersions() method returning Collections.unmodifiableList() for thread safety (satisfies TS-015)
- [x] T009 Implement error handling: fail-fast on missing file with clear RuntimeException (satisfies TS-018)
- [x] T010 Implement error handling: fail-fast on empty file with clear RuntimeException (satisfies TS-019)
- [x] T011 Implement malformed line handling: skip line, log warning, continue loading (satisfies TS-020)

### Unit Tests for VersionListService

- [x] T012 [P] Create VersionListServiceTest class in src/test/java/com/github/asm0dey/VersionListServiceTest.java
- [x] T013 [P] Write test: shouldLoadVersionsFromFile() - verifies non-null, non-empty, contains known versions (satisfies TS-014)
- [x] T014 [P] Write test: shouldReturnImmutableList() - verifies UnsupportedOperationException on modification (satisfies TS-015)
- [x] T015 [P] Write test: shouldSkipEmptyLines() - verifies empty/whitespace lines are skipped (satisfies TS-016)
- [x] T016 [P] Write test: shouldTrimWhitespace() - verifies leading/trailing whitespace removed (satisfies TS-017)
- [x] T017 [P] Write test: shouldFailFastOnMissingFile() - verifies RuntimeException with clear message (satisfies TS-018)
- [x] T018 [P] Write test: shouldFailFastOnEmptyFile() - verifies RuntimeException for all-empty file (satisfies TS-019)

**Completion Criteria**: VersionListService fully implemented and tested, all service unit tests passing

**Parallel Batches**:
- Batch 1: [T003, T004] (no mutual dependencies)
- Batch 2: [T005-T011] (sequential - service implementation)
- Batch 3: [T012-T018] (parallel - independent unit tests)

---

## Phase 3: User Story 1 (P1) - Consistent Version Data Across Application

**User Story**: [spec.md:User Story 1](spec.md#user-story-1---consistent-version-data-across-application-priority-p1)
**Priority**: P1 (MVP)
**Blocks**: US2, US3
**Test Coverage**: TS-001, TS-002, TS-003, TS-010, TS-011, TS-013, TS-021, TS-023, TS-024, TS-025, TS-031

### Backend API Implementation

- [ ] T019 [US1] Add VersionListService injection to JavaVersionResource class (src/main/java/com/github/asm0dey/JavaVersionResource.java)
- [ ] T020 [US1] Implement GET /api/versions endpoint method returning List<String> with @Path, @GET, @Produces annotations (satisfies TS-010)
- [ ] T021 [US1] Add Cache-Control header "public, max-age=3600" to /api/versions endpoint (satisfies TS-012)

### Backend API Tests

- [ ] T022 [P] [US1] Add testGetVersionsEndpoint() to JavaVersionResourceTest - verify 200 OK, JSON response, 500+ versions (satisfies TS-010)
- [ ] T023 [P] [US1] Add testVersionsPreservesOrder() to JavaVersionResourceTest - verify array matches file order exactly (satisfies TS-011)
- [ ] T024 [P] [US1] Add testVersionsCacheHeaders() to JavaVersionResourceTest - verify Cache-Control header present (satisfies TS-012)
- [ ] T025 [P] [US1] Add testVersionsPerformance() to JavaVersionResourceTest - verify response time <100ms (satisfies TS-013)

### Frontend Implementation

- [ ] T026 [US1] Remove hardcoded javaVersions array from src/main/resources/templates/JavaVersionResource/index.html (line ~897)
- [ ] T027 [US1] Implement loadVersions() async function in index.html to fetch from /api/versions (satisfies TS-021)
- [ ] T028 [US1] Implement loading state UI: show spinner, disable version dropdown during fetch (satisfies TS-022)
- [ ] T029 [US1] Implement success state UI: hide spinner, populate dropdown, enable selection (satisfies TS-023)
- [ ] T030 [US1] Implement error state UI: hide spinner, show error message, log to console (satisfies TS-024)
- [ ] T031 [US1] Add DOMContentLoaded event listener to call loadVersions() on page load (satisfies TS-021)
- [ ] T032 [US1] Add response validation: check valid JSON, check array type, check string elements (satisfies TS-025)

### Integration Testing

- [ ] T033 [US1] Create integration test: verify frontend and backend serve identical version lists (satisfies TS-031)
- [ ] T034 [US1] Manual verification: open homepage, verify dropdown populates with 500+ versions from API
- [ ] T035 [US1] Manual verification: use browser DevTools Network tab to confirm /api/versions request and caching headers

**Completion Criteria**: API endpoint implemented and tested, frontend fetches dynamically, 100% consistency verified

**Parallel Batches**:
- Batch 1: [T019-T021] (sequential - backend API implementation)
- Batch 2: [T022-T025] (parallel - independent API tests)
- Batch 3: [T026-T032] (sequential - frontend implementation)
- Batch 4: [T033] (integration test after all implementation)

---

## Phase 4: User Story 2 (P2) - Single-Point Version Data Maintenance

**User Story**: [spec.md:User Story 2](spec.md#user-story-2---single-point-version-data-maintenance-priority-p2)
**Priority**: P2
**Depends on**: US1 (T001-T035)
**Test Coverage**: TS-004, TS-005, TS-006, TS-032

### Update Testing

- [ ] T036 [US2] Create integration test: add test version to java_versions.txt, restart app, verify both frontend and backend recognize it (satisfies TS-032, TS-004)
- [ ] T037 [US2] Create test scenario documentation in quickstart.md for single-file update workflow (satisfies TS-032)
- [ ] T038 [US2] Verify SC-002: adding new version requires exactly one file modification, zero code changes (satisfies TS-004)
- [ ] T039 [US2] Verify SC-003: measure deployment time from file edit to restart completion, confirm <5 minutes (satisfies TS-005, TS-006)

**Completion Criteria**: Single-point maintenance verified, update workflow documented and tested

**Parallel Batches**:
- Batch 1: [T036-T039] (can run in parallel with T040-T043)

---

## Phase 5: User Story 3 (P3) - Validated Version Data Format

**User Story**: [spec.md:User Story 3](spec.md#user-story-3---validated-version-data-format-priority-p3)
**Priority**: P3
**Depends on**: US1 (T001-T035)
**Test Coverage**: TS-007, TS-008, TS-009, TS-026, TS-027, TS-028, TS-029, TS-030

### Validation Enhancement & Testing

- [ ] T040 [P] [US3] Enhance VersionListService validation: add line number tracking for error messages (satisfies TS-007)
- [ ] T041 [P] [US3] Add validation test: shouldReportInvalidFormatWithLineNumbers() - verify specific line identification (satisfies TS-007)
- [ ] T042 [P] [US3] Add test: shouldHandleMissingFileAtStartup() - verify startup failure with clear error (satisfies TS-026)
- [ ] T043 [P] [US3] Add test: shouldHandleConcurrentReads() - verify thread-safe access during/after startup (satisfies TS-027)
- [ ] T044 [P] [US3] Add test: shouldHandleCorruptedFile() - verify fail-fast or skip-invalid-lines behavior (satisfies TS-028)

**Completion Criteria**: Enhanced validation with line numbers, all edge cases tested

**Parallel Batches**:
- Batch 1: [T040-T044] (mostly independent validation tests)

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, observability, performance verification

### Documentation

- [ ] T045 [P] Update quickstart.md with all test scenarios from Phase 3-5 (frontend-backend consistency, version updates, error handling)
- [ ] T046 [P] Update README.md (if exists) to document new architecture: single data source, API endpoint, restart requirement

### Performance & Monitoring

- [ ] T047 [P] Add logging to VersionListService: log version count on successful load, log warnings for skipped lines
- [ ] T048 [P] Manual performance test: measure API response time with Apache Bench or similar (target: <10ms, max: 100ms)
- [ ] T049 [P] Manual performance test: verify memory overhead <10KB using heap dump analysis

### Final Verification

- [ ] T050 Run full test suite: ./gradlew test - verify all unit and integration tests pass
- [ ] T051 Verify all 32 test specifications from test-specs.md are satisfied by implementation
- [ ] T052 Manual browser testing: verify homepage loads, dropdown populates, version selection works
- [ ] T053 Manual error testing: block /api/versions in DevTools, verify error message shown
- [ ] T054 Constitution compliance review: verify all 5 principles satisfied (Accuracy, Transparency, Defensive Handling, Clear Communication, Test Coverage)

**Completion Criteria**: All tests passing, documentation complete, performance targets met, constitution compliance verified

**Parallel Batches**:
- Batch 1: [T045-T049] (independent polish tasks)
- Batch 2: [T050-T054] (sequential final verification)

---

## Dependencies

### Blocking Dependencies

```
Phase 1 (Setup) [T001-T004]
    ↓
Phase 2 (Foundational) [T005-T018] ← BLOCKS ALL USER STORIES
    ↓
Phase 3 (US1 - P1) [T019-T035] ← MVP, BLOCKS US2 & US3
    ├→ Phase 4 (US2 - P2) [T036-T039]
    └→ Phase 5 (US3 - P3) [T040-T044]
        ↓
Phase 6 (Polish) [T045-T054]
```

### Cross-Phase Dependencies

- **T019-T035 (US1)** require **T005-T018 (VersionListService)** - foundational service must exist before API can use it
- **T036-T039 (US2)** require **T019-T035 (US1)** - update testing requires working API
- **T040-T044 (US3)** require **T005-T018 (VersionListService)** - validation testing requires service foundation
- **T050-T054 (Final)** require **ALL previous phases** - final verification needs complete implementation

### No Dependencies

These tasks can start immediately after their phase prerequisites:
- Phase 1: T003, T004 (parallel)
- Phase 2 Tests: T012-T018 (parallel after T005-T011)
- Phase 3 Tests: T022-T025 (parallel after T019-T021)
- Phase 5 Tests: T040-T044 (parallel with each other)
- Phase 6: T045-T049 (parallel with each other)

---

## Parallel Execution Strategy

### Maximum Parallelism by Phase

**Phase 1**: 2 parallel tasks (T003, T004)
**Phase 2**: 7 parallel tests (T012-T018) after service implementation (T005-T011)
**Phase 3**: 4 parallel API tests (T022-T025), frontend tasks mostly sequential due to interdependencies
**Phase 4**: 4 parallel tasks (T036-T039) - all independent test scenarios
**Phase 5**: 5 parallel tasks (T040-T044) - all independent validation tests
**Phase 6**: 5 parallel polish tasks (T045-T049), then sequential verification (T050-T054)

### Estimated Speedup

- Sequential execution: ~54 task units
- With parallelism: ~35 task units
- **Estimated speedup: 35% time reduction**

### Critical Path

Longest dependency chain (cannot be parallelized):
```
T001 → T002 → T005 → T006 → T007 → T008 → T009 → T019 → T020 → T021 → T026 → T027 → T028 → T029 → T033 → T050 → T051 → T052 → T054
```
**Critical path length**: 19 tasks

---

## Test Specification Traceability

| Task ID | Satisfies Test Specs | Type |
|---------|----------------------|------|
| T007 | TS-016, TS-017 | Validation |
| T008 | TS-015 | Validation |
| T009 | TS-018 | Validation |
| T010 | TS-019 | Validation |
| T011 | TS-020 | Validation |
| T013 | TS-014 | Unit Test |
| T014 | TS-015 | Unit Test |
| T015 | TS-016 | Unit Test |
| T016 | TS-017 | Unit Test |
| T017 | TS-018 | Unit Test |
| T018 | TS-019 | Unit Test |
| T020 | TS-010 | Contract Test |
| T021 | TS-012 | Contract Test |
| T022 | TS-010 | Contract Test |
| T023 | TS-011 | Contract Test |
| T024 | TS-012 | Contract Test |
| T025 | TS-013 | Contract Test |
| T027 | TS-021 | Acceptance Test |
| T028 | TS-022 | Acceptance Test |
| T029 | TS-023 | Acceptance Test |
| T030 | TS-024 | Acceptance Test |
| T031 | TS-021 | Acceptance Test |
| T032 | TS-025 | Acceptance Test |
| T033 | TS-031 | Integration Test |
| T036 | TS-032, TS-004 | Integration Test |
| T038 | TS-004 | Acceptance Test |
| T039 | TS-005, TS-006 | Acceptance Test |
| T040 | TS-007 | Validation Enhancement |
| T041 | TS-007 | Validation Test |
| T042 | TS-026 | Edge Case Test |
| T043 | TS-027 | Edge Case Test |
| T044 | TS-028 | Edge Case Test |

**Coverage**: 32/32 test specifications addressed (100%)

---

## Implementation Strategy

### MVP-First Approach

1. **Start with Phase 1-3** (Setup + Foundational + US1) = T001-T035
   - Delivers core value: frontend-backend consistency
   - Fully testable independently
   - Satisfies 18 test specifications

2. **Add Phase 4** (US2) = T036-T039
   - Validates single-point maintenance
   - Quick to implement (4 tasks, mostly testing)

3. **Add Phase 5** (US3) = T040-T044
   - Enhanced validation and edge cases
   - Lower priority, can defer if time-constrained

4. **Finish with Phase 6** (Polish) = T045-T054
   - Documentation and final verification
   - Required before merge

### Incremental Delivery Milestones

- **Milestone 1** (MVP): T001-T035 complete → Feature is functional, can deploy
- **Milestone 2** (Validated): T036-T039 complete → Single-point updates verified
- **Milestone 3** (Hardened): T040-T044 complete → Edge cases covered
- **Milestone 4** (Production-Ready): T045-T054 complete → Documented and verified

---

## Task Summary

| Phase | Task Range | Count | Type | Parallelizable |
|-------|------------|-------|------|----------------|
| 1: Setup | T001-T004 | 4 | Setup | 2 tasks |
| 2: Foundational | T005-T018 | 14 | Service + Tests | 7 tests |
| 3: US1 (P1) | T019-T035 | 17 | Feature + Tests | 4 API tests |
| 4: US2 (P2) | T036-T039 | 4 | Testing | All 4 |
| 5: US3 (P3) | T040-T044 | 5 | Validation | All 5 |
| 6: Polish | T045-T054 | 10 | Polish + Verification | 5 polish tasks |
| **Total** | T001-T054 | **54** | - | **27 tasks** (50%) |

---

## Notes

- **Task Format**: All tasks follow required format: `- [ ] [TaskID] [P?] [Story?] Description`
- **Story Labels**: [US1], [US2], [US3] used for user story phase tasks only
- **Parallelization**: [P] marker indicates tasks that can run in parallel (different files or no dependencies)
- **Test Specs**: 32 test specifications from `/iikit-05-testify` are fully integrated into task breakdown
- **TDD Support**: Tasks reference specific test IDs (TS-XXX) they should satisfy
- **Constitution**: All tasks designed to satisfy constitution principles (comprehensive testing, defensive handling, etc.)

---

**Next Steps**:
- `/iikit-07-analyze` - (Recommended) Validate cross-artifact consistency
- `/iikit-08-implement` - Execute implementation (requires 100% checklist completion)
