/**
 * Theme Manager - Handles theme detection, switching, and persistence
 * Feature: Theme Toggle (002-theme-toggle)
 */

(function() {
    'use strict';

    // Constants
    const STORAGE_KEY = 'jvc-theme-preference';
    const VALID_THEMES = ['auto', 'dark', 'light'];
    const SYSTEM_DARK_QUERY = '(prefers-color-scheme: dark)';

    // State
    let currentPreference = 'auto';
    let systemDarkMatcher = null;

    /**
     * Initialize theme system
     */
    function init() {
        // Load saved preference
        loadPreference();
        
        // Set up system theme detection
        initSystemDetection();
        
        // Apply initial theme
        applyTheme();
        
        // Set up UI
        initThemeSelector();
    }

    /**
     * Load theme preference from localStorage
     */
    function loadPreference() {
        try {
            const stored = localStorage.getItem(STORAGE_KEY);
            if (stored) {
                const parsed = JSON.parse(stored);
                if (isValidTheme(parsed)) {
                    currentPreference = parsed;
                } else {
                    console.warn('Invalid theme preference stored, falling back to auto');
                    currentPreference = 'auto';
                }
            } else {
                currentPreference = 'auto';
            }
        } catch (e) {
            // localStorage unavailable or corrupted
            console.warn('Could not load theme preference:', e);
            currentPreference = 'auto';
        }
    }

    /**
     * Save theme preference to localStorage
     */
    function savePreference(preference) {
        try {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(preference));
        } catch (e) {
            // localStorage unavailable (private mode, etc.)
            console.warn('Could not save theme preference:', e);
        }
    }

    /**
     * Validate theme value
     */
    function isValidTheme(theme) {
        return VALID_THEMES.includes(theme);
    }

    /**
     * Initialize system theme detection
     */
    function initSystemDetection() {
        if (window.matchMedia) {
            systemDarkMatcher = window.matchMedia(SYSTEM_DARK_QUERY);
            
            // Listen for system theme changes
            if (systemDarkMatcher.addEventListener) {
                systemDarkMatcher.addEventListener('change', handleSystemChange);
            } else if (systemDarkMatcher.addListener) {
                // Fallback for older browsers
                systemDarkMatcher.addListener(handleSystemChange);
            }
        }
    }

    /**
     * Handle system theme change
     */
    function handleSystemChange(e) {
        if (currentPreference === 'auto') {
            applyTheme();
        }
    }

    /**
     * Get the effective theme (dark or light) based on preference and system
     */
    function getEffectiveTheme() {
        if (currentPreference === 'auto') {
            if (systemDarkMatcher) {
                return systemDarkMatcher.matches ? 'dark' : 'light';
            }
            // Fallback if matchMedia not available
            return 'light';
        }
        return currentPreference;
    }

    /**
     * Apply the current theme to the document
     */
    function applyTheme() {
        const effectiveTheme = getEffectiveTheme();
        document.documentElement.setAttribute('data-theme', effectiveTheme);
        
        // Update selector if it exists
        updateSelectorUI();
    }

    /**
     * Set theme preference
     */
    function setPreference(preference) {
        if (!isValidTheme(preference)) {
            console.error('Invalid theme preference:', preference);
            return;
        }
        
        currentPreference = preference;
        savePreference(preference);
        applyTheme();
    }

    /**
     * Initialize theme selector UI
     */
    function initThemeSelector() {
        const selector = document.getElementById('theme-selector');
        if (!selector) return;
        
        // Set initial value
        selector.value = currentPreference;
        
        // Listen for changes
        selector.addEventListener('change', function(e) {
            setPreference(e.target.value);
        });
    }

    /**
     * Update selector UI to match current state
     */
    function updateSelectorUI() {
        const selector = document.getElementById('theme-selector');
        if (selector) {
            selector.value = currentPreference;
        }
    }

    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    // Expose API for debugging/testing
    window.ThemeManager = {
        setPreference,
        getPreference: () => currentPreference,
        getEffectiveTheme,
        applyTheme
    };
})();
