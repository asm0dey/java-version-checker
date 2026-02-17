# Implementation Plan: Frontend Redesign - Multi-tab Version Selection

**Branch**: `003-frontend-redesign` | **Date**: 2026-02-17 | **Spec**: `/specs/003-frontend-redesign/spec.md`
**Input**: Feature specification from `/specs/003-frontend-redesign/spec.md`

## Summary

The frontend will be redesigned to support three distinct version selection workflows: Quick Selection (searchable dropdown), List of Versions (bulk text entry), and Detailed Audit (archive upload). The implementation will use vanilla JavaScript and CSS variables to maintain consistency with the existing theme, while introducing a 3-tab navigation system.

## Technical Context

**Language/Version**: HTML5, ES6+ JavaScript, CSS3
**Primary Dependencies**: Quarkus Qute (templating)
**Storage**: N/A (Frontend only implementation)
**Testing**: Manual UI verification and Playwright (if configured)
**Target Platform**: Modern web browsers (Chrome, Firefox, Safari)
**Project Type**: Web Application
**Performance Goals**: Tab switching < 50ms, Combobox filtering < 100ms for ~500 items.
**Constraints**: Visual consistency with existing `theme.css`.
**Scale/Scope**: Single page redesign (index.html).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **Accuracy in Version Detection**: UI must clearly present version strings from `java_versions.txt`.
- [x] **License Compliance Transparency**: N/A for this frontend-only phase.
- [x] **Defensive Data Handling**: Frontend validation for email and empty inputs.
- [x] **Clear Risk Communication**: Tab navigation provides clear context for each analysis mode.
- [x] **Comprehensive Test Coverage**: Manual test plans defined in spec.

## Project Structure

### Documentation (this feature)

```text
specs/003-frontend-redesign/
  spec.md              # Feature specification
  plan.md              # This file
  research.md          # Technology research and design decisions
  contracts/           # Frontend-Backend interface (planned)
```

### Source Code (repository root)

```text
src/main/resources/
  templates/JavaVersionResource/
    index.html         # Main page to be redesigned
  META-INF/resources/
    css/theme.css      # Shared styles
    js/theme.js        # Shared scripts
```

## Implementation Phases

### Phase 1: Tabbed Infrastructure & Layout

- **Objective**: Establish the 3-tab navigation system.
- **Tasks**:
  1. Add tab navigation (Pills) to `index.html`.
  2. Create 3 content containers (`#quick-tab`, `#list-tab`, `#audit-tab`).
  3. Implement CSS for active tab state and visibility toggling.
  4. Add JS `switchTab(tabId)` function to manage state and visibility.

### Phase 2: Detailed Audit & Guide Migration

- **Objective**: Move existing functionality into the "Detailed Audit" tab.
- **Tasks**:
  1. Relocate current file upload form to `#audit-tab`.
  2. Relocate "Step-by-Step Guide" to `#audit-tab`.
  3. Add optional Email input field to the upload form.
  4. Ensure existing file upload JS still works within the new structure.

### Phase 3: Quick Selection Tab

- **Objective**: Implement searchable Java version dropdown.
- **Tasks**:
  1. Add searchable combobox UI components to `#quick-tab`.
  2. Inject `java_versions.txt` data into the page (e.g., as a JS array or hidden list).
  3. Implement JS filtering logic for the dropdown.
  4. Add "Analyze" button for this specific tab.

### Phase 4: List of Versions Tab

- **Objective**: Implement bulk version input.
- **Tasks**:
  1. Add large `<textarea>` for manual version entry to `#list-tab`.
  2. Implement Drag-and-Drop for `.txt` files to populate the textarea.
  3. Add basic frontend validation (trimming, non-empty).
  4. Add "Analyze" button for this specific tab.

### Phase 5: Final Polish & Validation

- **Objective**: Ensure responsiveness and usability.
- **Tasks**:
  1. Verify mobile responsiveness.
  2. Ensure keyboard accessibility for tabs and combobox.
  3. Final CSS tweaks for consistency.
  4. Conduct all manual tests from `spec.md`.

## Quality Score Report

```
+----------------------------------------------------+
|  SPEC QUALITY REPORT                               |
+----------------------------------------------------+
|  Requirements:     11 found (min: 3)         [Y]   |
|  Success Criteria: 4 found (min: 3)          [Y]   |
|  User Stories:     3 found (min: 1)          [Y]   |
|  Measurable:       4 criteria have metrics   [Y]   |
|  Clarifications:   0 unresolved              [Y]   |
|  Coverage:         100% requirements linked  [Y]   |
+----------------------------------------------------+
|  OVERALL SCORE: 10/10                              |
|  STATUS: READY                                     |
+----------------------------------------------------+
```
