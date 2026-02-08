# Data Model: Theme Toggle

**Feature**: Theme Toggle  
**Date**: 2026-02-08

## Entities

### ThemePreference

Represents the user's selected theme mode stored in browser localStorage.

| Field | Type | Description | Constraints |
|-------|------|-------------|-------------|
| `value` | Enum String | User's theme preference | One of: `'auto'`, `'dark'`, `'light'` |
| `storageKey` | String Constant | localStorage key | `'jvc-theme-preference'` |

**Default Value**: `'auto'`

**Validation Rules**:
1. Must be one of the three allowed values
2. Invalid/corrupted values fall back to `'auto'`

---

### ThemeState (Derived)

Represents the actual visual theme currently applied to the interface. This is computed at runtime based on ThemePreference and system state.

| Field | Type | Description | Derivation |
|-------|------|-------------|------------|
| `effectiveTheme` | Enum String | Currently active theme | If preference='auto': system theme; else: preference value |
| `isDark` | Boolean | Whether dark mode is active | `effectiveTheme === 'dark'` |

**Derivation Logic**:
```
if (ThemePreference.value === 'auto') {
  ThemeState.effectiveTheme = SystemThemeDetector.currentTheme;
} else {
  ThemeState.effectiveTheme = ThemePreference.value;
}
```

---

### SystemTheme (Runtime)

Represents the operating system's current theme preference, detected via browser API.

| Field | Type | Description | Source |
|-------|------|-------------|--------|
| `prefersDark` | Boolean | Whether system prefers dark mode | `window.matchMedia('(prefers-color-scheme: dark)').matches` |
| `supported` | Boolean | Whether system theme detection is available | Media query support check |

**Default (unsupported)**: `prefersDark = false` (light mode fallback)

## State Transitions

```
┌─────────────────┐     set('dark')      ┌─────────────┐
│     AUTO        │ ───────────────────→ │    DARK     │
│  (follows sys)  │ ←─────────────────── │  (manual)   │
└─────────────────┘     set('auto')      └─────────────┘
        │   ▲                                  ▲   │
        │   │                                  │   │
   system │   │ set('light')              set('dark')│
  changes │   └──────────────────────────────┘   │
        │                                        │
        └────────────────────────────────────────┘
                    set('auto')
        ┌─────────────────┐
        │     LIGHT       │
        │   (manual)      │
        └─────────────────┘
```

## Storage Schema

### localStorage Entry

```javascript
// Key: jvc-theme-preference
// Value: JSON string
"\"auto\""   // Valid
"\"dark\""   // Valid
"\"light\""  // Valid
"\"invalid\"" // Invalid - will fall back to 'auto'
```

### CSS Data Attribute

Applied to `<html>` or `<body>` element:
```html
<html data-theme="dark">
<html data-theme="light">
<html data-theme="auto">  <!-- Computed to actual theme at runtime -->
```

Note: When preference is 'auto', the data attribute reflects the *effective* theme (dark/light), not 'auto'.

## Relationships

```
┌─────────────────────┐         ┌──────────────────────┐
│  ThemePreference    │         │   SystemTheme        │
│  (localStorage)     │         │   (Browser API)      │
└──────────┬──────────┘         └──────────┬───────────┘
           │                               │
           │    ┌────────────────────┐     │
           └───→│   ThemeManager     │←────┘
                │   (JavaScript)     │
                └─────────┬──────────┘
                          │
                          ↓
                ┌────────────────────┐
                │    ThemeState      │
                │  (CSS Variables)   │
                └────────────────────┘
```
