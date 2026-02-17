# Implementation Plan: Analysis Page with Results

**Branch**: `005-analysis-page` | **Date**: 2026-02-17 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/005-analysis-page/spec.md`

## Summary

Add an analysis results page that displays Java version licensing assessments with a blurred preview mechanism. When users navigate to the analysis page after uploading data, they see a form requesting email and company information overlaying blurred preview data. Upon form submission, the actual analysis results are revealed showing licensing status, end-of-life dates, and actionable recommendations sorted by urgency.

**Technical Approach**: Extend existing Quarkus REST API with new analysis endpoint, add new Qute template for analysis page with form and results table, implement client-side blur/reveal logic, and send form data to backend without persistence.

## Technical Context

**Language/Version**: Java 21
**Primary Dependencies**: Quarkus 3.18.x, Qute Templates, Jakarta REST, Jsonb
**Storage**: N/A (no new persistent storage required for this feature)
**Testing**: JUnit 5, REST Assured
**Target Platform**: Linux server (containerized with Quarkus)
**Project Type**: Web application (server-side rendering with Qute)
**Performance Goals**: Analysis results ready immediately on page load (pre-computed during upload), form submission response < 500ms
**Constraints**: Must reuse existing JavaVersionService analysis logic, no backend persistence of form data, must work with existing Qute template structure
**Scale/Scope**: Single-page addition with 2 new endpoints (analysis page GET, form submission POST), integration with existing upload flow

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Principle I: Accuracy in Version Detection
✓ **PASS** - Reusing existing JavaVersionService.parsePropertiesFile() and getDistinctVersions() logic. No changes to version detection.

### Principle II: License Compliance Transparency
✓ **PASS** - Analysis results will display existing JavaVersionInfo fields including license explanations. FR-013 requires detailed explanations for each license type.

### Principle III: Defensive Data Handling
✓ **PASS** - Form inputs will be validated (email format per FR-007). Backend endpoint receives form data but does not persist (FR-016), preventing data handling issues.

### Principle IV: Clear Risk Communication
✓ **PASS** - FR-018 mandates sorting by recommendation severity (Migration required > License required > Migration recommended > Keep as is). Results table includes all recommendation fields from existing analysis.

### Principle V: Comprehensive Test Coverage
⚠️ **REQUIRES ATTENTION** - New endpoints and form validation logic must have unit and integration tests. Test plan TBD in Phase 1.

**Status**: Ready to proceed. One test coverage requirement to address in implementation.

## Project Structure

### Documentation (this feature)

```text
specs/005-analysis-page/
  spec.md              # Feature specification
  plan.md              # This file
  research.md          # Phase 0 output (design decisions)
  data-model.md        # Phase 1 output (entities and contracts)
  quickstart.md        # Phase 1 output (testing scenarios)
  contracts/           # Phase 1 output (API specifications)
  checklists/          # Requirements quality checklist
    requirements.md
  tasks.md             # Phase 2 output (NOT created by /iikit-plan)
```

### Source Code (repository root)

```text
src/main/java/com/github/asm0dey/
  JavaVersionResource.java     # Add new GET /analysis and POST /analysis/submit endpoints
  JavaVersionService.java       # No changes needed (reuse existing logic)
  AnalysisFormData.java         # NEW: Form data DTO

src/main/resources/templates/JavaVersionResource/
  index.html                    # Existing (no changes needed)
  results.html                  # Existing (no changes needed)
  analysis.html                 # NEW: Analysis page with blur + form + results table

src/main/resources/META-INF/resources/
  js/
    theme.js                    # Existing
    analysis.js                 # NEW: Blur/reveal logic, form submission
  css/
    theme.css                   # May need minor additions for blur effect

src/test/java/com/github/asm0dey/
  AnalysisPageTest.java         # NEW: Integration tests for analysis endpoints
  AnalysisFormValidationTest.java  # NEW: Unit tests for form validation
```

**Structure Decision**: This is a web application using Quarkus with server-side rendering (Qute templates). The existing structure follows Quarkus conventions with JAX-RS resources in `src/main/java` and Qute templates in `src/main/resources/templates/{ResourceClass}/`. We'll extend the existing `JavaVersionResource` class with new endpoints for the analysis page.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations requiring justification. All constitutional principles are met.
