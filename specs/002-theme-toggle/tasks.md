# Tasks: Theme Toggle

**Feature**: Theme Toggle  
**Branch**: `002-theme-toggle`  
**Generated**: 2026-02-08

## Phase 1: Setup

Initial project structure and asset preparation.

- [x] T001 Create theme CSS file at `src/main/resources/META-INF/resources/css/theme.css`
- [x] T002 Create theme JavaScript module at `src/main/resources/META-INF/resources/js/theme.js`

## Phase 2: Foundational - CSS Variable Migration

Migrate existing CSS to use CSS custom properties for theming support. Must complete before user stories.

- [x] T003 [P] Define CSS custom properties for light theme in `theme.css`
- [x] T004 [P] Define CSS custom properties for dark theme in `theme.css`
- [x] T005 Update `index.html` inline styles to use CSS variables
- [x] T006 Update `results.html` inline styles to use CSS variables

## Phase 3: User Story 1 - System Theme Detection

**Story**: System Theme Detection (P1) - Auto-detect and match system theme on first visit

**Acceptance Criteria**:
- First-time visitors see theme matching their system preference
- Interface displays correctly in both dark and light modes
- System theme changes are detected when in auto mode

### Tasks

- [x] T007 [US1] Implement system theme detection in `theme.js` using `matchMedia('(prefers-color-scheme: dark)')`
- [x] T008 [US1] Implement theme application logic (set `data-theme` attribute on `<html>`)
- [x] T009 [US1] Add event listener for system theme changes (when in auto mode)
- [x] T010 [US1] Add theme CSS/JS includes to `index.html` head section
- [x] T011 [US1] Add theme CSS/JS includes to `results.html` head section
- [ ] T012 [US1] Test system detection on first visit (dark mode system)
- [ ] T013 [US1] Test system detection on first visit (light mode system)

## Phase 4: User Story 2 - Manual Theme Selection

**Story**: Manual Theme Selection (P1) - Allow users to override with dark/light/auto selector

**Acceptance Criteria**:
- Theme selector accessible from all pages
- Manual selection immediately applies theme
- Auto mode re-enables system following

### Tasks

- [x] T014 [US2] Create theme selector UI component (dropdown with Auto/Dark/Light options)
- [x] T015 [US2] Add theme selector to `index.html` header area
- [x] T016 [US2] Add theme selector to `results.html` header area
- [x] T017 [US2] Implement manual theme selection handler in `theme.js`
- [x] T018 [US2] Update selector UI to reflect current active theme
- [ ] T019 [US2] Test manual switch from auto to dark
- [ ] T020 [US2] Test manual switch from dark to light
- [ ] T021 [US2] Test manual switch back to auto mode

## Phase 5: User Story 3 - Theme Persistence

**Story**: Theme Persistence (P2) - Save and restore user preference across sessions

**Acceptance Criteria**:
- Manual selections persist across browser sessions
- Invalid stored values fall back to auto
- Graceful degradation when storage unavailable

### Tasks

- [x] T022 [US3] Implement localStorage read on page load in `theme.js`
- [x] T023 [US3] Implement localStorage write on manual theme change
- [x] T024 [US3] Add input validation for stored theme preference (must be 'auto', 'dark', or 'light')
- [x] T025 [US3] Add try/catch for localStorage operations (graceful degradation)
- [ ] T026 [US3] Test persistence: select dark, close browser, reopen, verify dark mode
- [ ] T027 [US3] Test persistence: select light, close browser, reopen, verify light mode
- [ ] T028 [US3] Test invalid value handling: corrupt localStorage, verify falls back to auto
- [ ] T029 [US3] Test graceful degradation: disable localStorage, verify theme switching still works

## Phase 6: Polish & Cross-Cutting Concerns

Final validation, accessibility, and edge case handling.

- [x] T030 Add CSS transitions for smooth theme switching (200ms)
- [ ] T031 Test rapid theme switching (no flickering)
- [ ] T032 Verify accessibility: keyboard navigation for theme selector
- [ ] T033 Verify accessibility: screen reader labels for theme selector
- [ ] T034 Test across target browsers (Chrome, Firefox, Safari)
- [x] T035 Verify no Flash of Unstyled Content (FOUC) on page load
- [x] T036 Code review: ensure no hardcoded colors remain in templates
- [ ] T037 Update CLAUDE.md or documentation with theme feature notes

## Dependencies

```
T001, T002 (parallel)
    ↓
T003, T004 (parallel)
    ↓
T005, T006 (parallel, after T003/T004)
    ↓
T007 → T008 → T009 (sequential)
    ↓
T010, T011 (parallel, after T005/T006/T009)
    ↓
T012, T013 (parallel, after T010/T011)
    ↓
T014 → T017 (selector UI → handler logic)
    ↓
T015, T016 (parallel, after T017)
    ↓
T018 (after T015/T016)
    ↓
T019-T021 (test tasks, parallel, after T018)
    ↓
T022 → T023 → T024 (sequential)
    ↓
T025 (can be parallel with T022-T024)
    ↓
T026-T029 (test tasks, parallel)
    ↓
T030-T037 (polish tasks, mostly parallel)
```

## Parallel Execution Examples

### Phase 2 (CSS Migration)
```bash
# Terminal 1: Define CSS variables
# Work on: T003, T004

# Terminal 2: Update index.html styles
# Work on: T005 (waits for T003/T004)

# Terminal 3: Update results.html styles
# Work on: T006 (waits for T003/T004)
```

### Phase 3+4 (Core Feature)
```bash
# Terminal 1: Core JavaScript logic
# Work on: T007, T008, T009

# Terminal 2: Template integration
# Work on: T010, T011, T015, T016

# Terminal 3: UI components
# Work on: T014, T017, T018
```

## Implementation Strategy

### MVP Scope
User Stories 1 and 2 (System Detection + Manual Selection) provide complete user-facing functionality. US3 (Persistence) enhances UX but is not blocking for initial release.

**Suggested MVP**: T001-T021 (all of US1 and US2)

### Incremental Delivery
1. **Sprint 1**: Phase 1-3 (Setup, CSS Migration, System Detection) - Core functionality working
2. **Sprint 2**: Phase 4 (Manual Selection) - User control added
3. **Sprint 3**: Phase 5-6 (Persistence, Polish) - Production-ready

### Test-Driven Approach
For each user story phase:
1. Create test HTML file to validate acceptance criteria
2. Implement tasks
3. Run through test scenarios in quickstart.md
4. Mark story complete when all acceptance criteria pass

## Task Statistics

| Phase | Tasks | Story | Priority |
|-------|-------|-------|----------|
| Setup | 2 | - | - |
| Foundational | 4 | - | - |
| US1 | 7 | System Detection | P1 |
| US2 | 8 | Manual Selection | P1 |
| US3 | 8 | Persistence | P2 |
| Polish | 8 | - | - |
| **Total** | **37** | - | - |

## Constitution Alignment

✅ All tasks align with Constitution v1.0.0:
- **Principle III (Defensive)**: T024 (validation), T025 (error handling)
- **Principle IV (Clear Communication)**: T014-T018 (visible UI controls)
- **Principle V (Test Coverage)**: T012-T013, T019-T021, T026-T029 (test tasks)
