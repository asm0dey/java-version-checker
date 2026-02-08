# Quickstart: Theme Toggle

**Feature**: Theme Toggle  
**Branch**: `002-theme-toggle`

## Setup

No additional setup required. This feature uses native browser APIs.

## Testing the Feature

### Manual Test Scenarios

#### Test 1: First Visit - System Dark Mode
1. Set your OS to dark mode
2. Clear browser localStorage for this site (or use incognito)
3. Visit the application
4. **Expected**: Interface renders in dark mode automatically

#### Test 2: First Visit - System Light Mode
1. Set your OS to light mode
2. Clear browser localStorage
3. Visit the application
4. **Expected**: Interface renders in light mode automatically

#### Test 3: Manual Override
1. Open the theme selector (top-right corner)
2. Select "Dark" while in light mode
3. **Expected**: Interface immediately switches to dark mode
4. Select "Light"
5. **Expected**: Interface immediately switches to light mode

#### Test 4: Persistence
1. Select "Dark" mode
2. Close the browser tab
3. Reopen the application
4. **Expected**: Interface still in dark mode

#### Test 5: Auto Mode
1. Select "Auto" from the selector
2. Change your OS theme
3. **Expected**: Application theme updates to match system

#### Test 6: Storage Unavailable
1. Open browser in private mode with localStorage disabled
2. Change theme
3. **Expected**: Theme changes work but don't persist after reload

### Automated Test Checklist

- [ ] Unit: Theme preference validation accepts only 'auto', 'dark', 'light'
- [ ] Unit: Invalid stored values fall back to 'auto'
- [ ] Unit: System theme detection returns correct boolean
- [ ] Integration: Theme switch updates CSS variables
- [ ] Integration: localStorage is written on manual change
- [ ] Integration: localStorage is read on page load
- [ ] E2E: Full user journey from first visit to persistence

## Troubleshooting

### Theme doesn't persist
- Check if localStorage is enabled in browser
- Check browser console for JavaScript errors
- Verify storage key: `jvc-theme-preference`

### Theme doesn't follow system
- Verify browser supports `prefers-color-scheme` media query
- Check that "Auto" is selected in theme dropdown
- Some browsers require page refresh to detect system changes

### Flash of wrong theme on load
- Ensure theme script runs in `<head>` before render
- Verify CSS variables are defined before JavaScript executes
