# Research: Theme Toggle Implementation

**Date**: 2026-02-08  
**Feature**: Theme Toggle  
**Spec Reference**: [spec.md](spec.md)

## Technical Context Decisions

### Technology Stack

| Aspect | Decision | Rationale |
|--------|----------|-----------|
| **Language/Version** | JavaScript (ES6+) | Native browser support, no build step required for this feature |
| **Storage** | localStorage | Spec requirement for local-only persistence; simple key-value API |
| **CSS Approach** | CSS Custom Properties (Variables) | Enables runtime theme switching without class manipulation on every element |
| **System Detection** | `matchMedia('(prefers-color-scheme: dark)')` | Standard CSSOM API, widely supported (95%+ browsers) |
| **Persistence Format** | JSON stringified object | Allows future extension without migration |

### Browser Support Assessment

**Target Browsers** (based on Quarkus default support):
- Chrome/Edge 88+
- Firefox 78+
- Safari 14+

**API Support**:
- `localStorage`: 100% in target browsers
- `matchMedia`: 100% in target browsers
- `prefers-color-scheme`: 95%+ in target browsers
- CSS Custom Properties: 100% in target browsers

### Alternatives Considered

| Alternative | Why Not Chosen |
|-------------|----------------|
| `sessionStorage` | Doesn't persist across sessions (violates FR-005) |
| `indexedDB` | Overkill for a single key-value pair |
| `cookies` | Sent to server unnecessarily, size limitations |
| CSS-in-JS library | Adds dependency for simple feature, violates YAGNI |
| Tailwind dark mode | Would require build tool integration, adds complexity |

## Architecture Decisions

### Decision 1: CSS Variable-Based Theming
**Chosen**: Use CSS custom properties defined on `:root` with `[data-theme]` attribute selector overrides.

**Rationale**:
- Single source of truth for colors
- Instant theme switching (no FOUC - Flash of Unstyled Content)
- Easy to extend with new themes in future
- Works with existing CSS without major refactoring

### Decision 2: Theme State Management
**Chosen**: Simple JavaScript module pattern with event-driven updates.

**State Flow**:
```
User Action → ThemeManager.update(theme) → localStorage → CSS Update → UI Reflects
```

**Rationale**:
- No external state management library needed
- Direct localStorage persistence meets requirements
- Event listeners allow UI components to react to changes

### Decision 3: Three-State Selector
**Chosen**: Dropdown/select with "Auto", "Dark", "Light" options.

**Rationale**:
- Clear mental model (matches system settings UX)
- "Auto" explicitly shows system is being followed
- Prevents confusion about why theme might change (when in auto mode)

## Key Implementation Details

### localStorage Schema
```javascript
{
  "theme-preference": "dark" | "light" | "auto"
}
```

**Storage Key**: `jvc-theme-preference` (prefixed to avoid collisions)

### CSS Variable Structure
```css
:root {
  /* Light theme (default) */
  --bg-primary: #ffffff;
  --bg-secondary: #f8f9fa;
  --text-primary: #333333;
  /* ... etc */
}

[data-theme="dark"] {
  --bg-primary: #1a1a2e;
  --bg-secondary: #16213e;
  --text-primary: #e2e8f0;
  /* ... etc */
}
```

### System Theme Change Detection
```javascript
const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
mediaQuery.addEventListener('change', (e) => {
  if (currentPreference === 'auto') {
    applyTheme(e.matches ? 'dark' : 'light');
  }
});
```

## Risk Mitigation

### Private Browsing Mode
- **Risk**: localStorage may be unavailable or cleared unexpectedly
- **Mitigation**: Wrap all localStorage calls in try/catch; fall back to session-only mode
- **Graceful Degradation**: Feature works without persistence (user must reselect each session)

### Rapid Theme Switching
- **Risk**: Flickering or performance issues if user toggles quickly
- **Mitigation**: CSS transitions limited to 200ms; no JavaScript debouncing needed for this use case

### Invalid Stored Data
- **Risk**: Corrupted localStorage value
- **Mitigation**: Validate stored value against allowed enum ['dark', 'light', 'auto']; reset to 'auto' if invalid

## References

- [MDN: prefers-color-scheme](https://developer.mozilla.org/en-US/docs/Web/CSS/@media/prefers-color-scheme)
- [MDN: CSS Custom Properties](https://developer.mozilla.org/en-US/docs/Web/CSS/--*)
- [Can I Use: CSS Variables](https://caniuse.com/css-variables)
