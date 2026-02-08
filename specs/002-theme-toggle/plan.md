# Implementation Plan: Theme Toggle

**Branch**: `002-theme-toggle` | **Date**: 2026-02-08 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `/specs/002-theme-toggle/spec.md`

## Summary

Add a theme toggle feature allowing users to switch between dark, light, and auto (system-following) modes. The implementation uses CSS custom properties for instant theme switching, localStorage for persistence, and the `prefers-color-scheme` media query for system detection. The theme selector will be integrated into both Qute HTML templates with a JavaScript module handling state management.

## Technical Context

| Aspect | Decision |
|--------|----------|
| **Language/Version** | JavaScript ES6+ (browser native) |
| **Primary Dependencies** | None - vanilla JS/CSS implementation |
| **Storage** | localStorage (key: `jvc-theme-preference`) |
| **Testing** | Manual browser testing + existing Quarkus test framework |
| **Target Platform** | Web browsers (Chrome 88+, Firefox 78+, Safari 14+) |
| **Project Type** | Web application (Quarkus + Qute templates) |
| **Performance Goals** | Theme switch under 100ms, no page reload |
| **Constraints** | Must work without JavaScript (graceful degradation to light mode) |
| **Scale/Scope** | Single feature affecting two HTML templates |

## Constitution Check

✅ **VALIDATED** against Constitution v1.0.0

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Accuracy | ✓ Pass | Theme detection uses standard browser APIs |
| II. Transparency | N/A | Not a license-related feature |
| III. Defensive Handling | ✓ Pass | try/catch around localStorage, input validation |
| IV. Clear Communication | ✓ Pass | Visual toggle with immediate feedback |
| V. Test Coverage | ✓ Pass | Edge cases documented in quickstart |

## Project Structure

### Documentation (this feature)

```text
specs/002-theme-toggle/
  plan.md              # This file
  research.md          # Phase 0: Technology decisions
  data-model.md        # Phase 1: Theme state entities
  quickstart.md        # Phase 1: Testing guide
  checklists/
    requirements.md    # Spec quality checklist
```

### Source Code (repository root)

```text
src/main/resources/
  templates/
    JavaVersionResource/
      index.html       # Modified: Add theme toggle
      results.html     # Modified: Add theme toggle
  META-INF/
    resources/
      js/
        theme.js       # NEW: Theme management module
      css/
        theme.css      # NEW: CSS variables for theming
```

**Structure Decision**: Theme functionality is split between:
1. **JavaScript module** (`theme.js`): State management, localStorage, system detection
2. **CSS file** (`theme.css`): Variable definitions for both themes
3. **Template updates**: Add theme selector UI and include theme assets

## Implementation Approach

### Phase 0: Setup (research.md ✓ Complete)
- ✅ Technology stack decisions documented
- ✅ Browser support validated
- ✅ Risk mitigation strategies defined

### Phase 1: Design (data-model.md ✓ Complete)
- ✅ ThemePreference entity (localStorage schema)
- ✅ ThemeState entity (runtime derived state)
- ✅ State transition diagram
- ✅ CSS variable structure defined

### Phase 2: Contracts
No external APIs or interfaces required - this is a purely client-side feature.

### Key Components

#### 1. CSS Variables (theme.css)
Define color schemes using CSS custom properties:
- Light theme as default (`:root`)
- Dark theme override (`[data-theme="dark"]`)
- All existing CSS values replaced with variables

#### 2. Theme Manager (theme.js)
JavaScript module with responsibilities:
- Read initial preference from localStorage
- Detect system theme via `matchMedia`
- Apply theme by setting `data-theme` attribute
- Persist manual selections to localStorage
- Listen for system theme changes (in auto mode)

#### 3. Theme Selector UI
Dropdown/select element in both templates:
- Position: Header area (top-right)
- Options: Auto (follow system), Dark, Light
- Current selection reflects active preference
- Visual indicator (icon) showing current mode

### Integration Points

| File | Changes |
|------|---------|
| `index.html` | Add theme CSS/JS includes, add selector UI |
| `results.html` | Add theme CSS/JS includes, add selector UI |
| Existing CSS | Replace hardcoded colors with CSS variables |

### Color Mapping

Current colors → CSS Variables:
- `#667eea` / `#764ba2` (gradients) → `--color-primary`, `--color-secondary`
- `#333`, `#666`, `#999` (text) → `--text-primary`, `--text-secondary`, `--text-muted`
- `#f8f9fa`, `white` (backgrounds) → `--bg-primary`, `--bg-secondary`
- `#f8d7da`, `#fff3cd`, `#d4edda` (badges) → theme-appropriate variants

## Complexity Tracking

No constitution violations. Implementation is straightforward using native browser features without external dependencies.

## Tessl Integration

**Status**: ℹ️ Tessl not installed. Tile-based documentation unavailable.

Install Tessl for enhanced library documentation: https://tessl.io

## Next Steps

Plan complete! Continue with:
- `/iikit-04-checklist` — (Optional) Generate additional quality checklists
- `/iikit-06-tasks` — Generate task breakdown from plan
