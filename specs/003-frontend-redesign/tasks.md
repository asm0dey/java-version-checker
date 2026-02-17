# Tasks: Frontend Redesign - Multi-tab Version Selection

**Input**: Design documents from `/specs/003-frontend-redesign/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, requirements.md (checklist)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish the core tabbed layout and navigation logic

- [x] T001 Define tab navigation structure (Horizontal Pills) in `src/main/resources/templates/JavaVersionResource/index.html`
- [x] T002 Create 3 content containers (`#quick-tab`, `#list-tab`, `#audit-tab`) in `src/main/resources/templates/JavaVersionResource/index.html`
- [x] T003 Implement CSS for tab navigation and active state in `src/main/resources/META-INF/resources/css/theme.css`
- [x] T004 Implement `switchTab(tabId)` function for visibility toggling in `src/main/resources/templates/JavaVersionResource/index.html` (or separate script)

## Phase 2: Foundational (Migration & Common Components)

**Purpose**: Move existing functionality and add shared UI elements

- [x] T005 [P] Relocate existing file upload form and JS to `#audit-tab` in `src/main/resources/templates/JavaVersionResource/index.html`
- [x] T006 [P] Relocate "Step-by-Step Guide" and its toggle JS to `#audit-tab` in `src/main/resources/templates/JavaVersionResource/index.html`
- [x] T007 Add optional Email input field to the `#audit-tab` form in `src/main/resources/templates/JavaVersionResource/index.html`

## Phase 3: Quick Selection (User Story 1 - P1)

**Purpose**: Implement the searchable Java version dropdown

- [x] T008 [US1] Create searchable combobox UI component (input + dropdown) in `#quick-tab` in `src/main/resources/templates/JavaVersionResource/index.html`
- [x] T009 [US1] Inject `java_versions.txt` data into index.html via Qute template engine
- [x] T010 [US1] Implement JS filtering logic for the combobox with 100ms response goal
- [x] T011 [US1] Add "Analyze" button for Quick Selection in `#quick-tab`

## Phase 4: List of Versions (User Story 2 - P1)

**Purpose**: Implement bulk version input and drag-and-drop

- [x] T012 [US2] Add bulk version entry `<textarea>` to `#list-tab` in `src/main/resources/templates/JavaVersionResource/index.html`
- [x] T013 [US2] Implement Drag-and-Drop JS to populate textarea from `.txt` files in `#list-tab`
- [x] T014 [US2] Add "Analyze" button for List of Versions in `#list-tab`
- [x] T015 [US2] Implement basic frontend validation (trimming) before submission

## Phase 5: Polish & Validation

**Purpose**: Final UI refinements and acceptance testing

- [x] T016 Verify mobile responsiveness of the 3-tab layout and combobox
- [x] T017 [P] Ensure keyboard accessibility (tabbing) for all new UI elements
- [x] T018 Conduct manual validation of all 3 User Stories against `spec.md` acceptance scenarios

## Phase 6: Enhancement & Bug Fixes

**Purpose**: Address issues found during implementation and enhance UX

- [x] T019 Fix dark mode text visibility issues in combobox dropdown
- [x] T020 Update version grouping to consolidate all versions by major version (e.g., all 8u* versions under Java 8)
- [x] T021 Implement deletable chips inside combobox for selected versions (matching java_chooser.png design)
- [x] T022 Fix Qute template expression error with inline onclick handlers
- [x] T023 Prevent combobox dropdown from closing on version selection
- [x] T024 Update light mode design to match reference images (upload_selector.png, java_chooser.png)
- [x] T025 Add fine print disclaimer about Oracle Java versions to Quick Selection and List of Versions tabs

## Dependencies & Execution Order

1. **Phase 1** is the blocker for all other phases as it provides the containers.
2. **Phase 2** ensures legacy parity within the new layout.
3. **Phase 3 & 4** can be implemented in parallel after Phase 1 is complete.
4. **Phase 5** requires all previous phases to be complete.

## Implementation Strategy

- **MVP**: Complete Phase 1 and 2 to maintain current feature set in the new layout.
- **Incremental**: Add Phase 3 (Quick Selection) as the next high-value increment.
- **Full Release**: Complete Phase 4 and 5 for the final redesigned experience.
