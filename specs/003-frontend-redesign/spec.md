# Feature Specification: Frontend Redesign - Multi-tab Version Selection

**Feature Branch**: `003-frontend-redesign`
**Created**: 2026-02-17
**Status**: Implemented
**Completed**: 2026-02-17
**Input**: User description: "run iikit-01-specify 
Now, look at the pics directory. There are 3 images:
1) upload_selector.png. On the main page I need to have something like this, but with three tabs: 
a) Quick Selection, b) Set list of versions 3) Detailed audit

2) In Quick Selection I need to have a dropbox with all java versions from java_versions.txt, reasonably grouped and sorted. Should be something like java_chooser.png, but with search in the combobox component
3) in \"List of Versions\" there should be a large text input, where one can either enter all the Java versions they have or drag and drop a file, in format \"one java version on one line\"
4) in Detailed Audit there should be something like what we have on the main page right now: they should be able to upload either zip archive, or a properties file. There should also be an input for email to send instructions to this email. Do not implement any backend changes yet, only frontend"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Quick Selection (Priority: P1)

As a developer, I want to quickly select a Java version from a predefined list so that I can see its audit results without having to provide my own property files.

**Why this priority**: Core value proposition for quick checks and easiest entry point for new users.

**Independent Test**: Can be tested by navigating to the "Quick Selection" tab, searching for a version (e.g., "17.0.1"), selecting it, and clicking the analyze button.

**Acceptance Scenarios**:

1. **Given** I am on the main page, **When** I click the "Quick Selection" tab, **Then** I see a searchable combobox containing Java versions from `java_versions.txt`.
2. **Given** the "Quick Selection" tab is active, **When** I type "11" in the search box, **Then** I only see Java 11 related versions in the list.
3. **Given** I have selected a version, **When** I click "Analyze", **Then** the form is submitted with the selected version.

---

### User Story 2 - Set List of Versions (Priority: P1)

As a sysadmin, I want to paste a list of Java versions I have in my fleet so that I can get a bulk audit report.

**Why this priority**: Essential for users who have version strings but not the full property files, enabling audit of multiple installations at once.

**Independent Test**: Can be tested by navigating to "Set list of versions" tab, pasting several versions (one per line), and clicking analyze.

**Acceptance Scenarios**:

1. **Given** I am on the "Set list of versions" tab, **When** I paste "1.8.0_202\n11.0.15\n17.0.1" into the text area, **Then** the input is accepted and ready for analysis.
2. **Given** I am on the "Set list of versions" tab, **When** I drag and drop a `.txt` file containing versions, **Then** the text area is populated with the file content.

---

### User Story 3 - Detailed Audit (Priority: P2)

As a security auditor, I want to upload full property files for deep analysis and have the instructions sent to my email.

**Why this priority**: This is the legacy functionality improved with email delivery instructions.

**Independent Test**: Can be tested by navigating to "Detailed Audit" tab, uploading a properties file, entering an email, and clicking analyze.

**Acceptance Scenarios**:

1. **Given** I am on the "Detailed Audit" tab, **When** I upload a `.properties` or `.zip` file, **Then** the file is registered for upload.
2. **Given** the "Detailed Audit" tab is active, **When** I enter an email address, **Then** it is validated as a properly formatted email.

---

### Edge Cases

- **Empty Inputs**: What happens if the user clicks "Analyze" with no version selected, empty text area, or no file uploaded? (System should prevent submission and show validation error).
- **Large Version List**: How does the system handle a list of 1000+ versions in the text area? (Should handle gracefully without freezing UI).
- **Invalid Email**: How does the system handle malformed email addresses in the "Detailed Audit" tab? (Frontend validation should trigger).
- **Missing java_versions.txt**: What if the backend fails to provide the versions list? (Frontend should show a graceful error or empty state for the combobox).


#### 1. Tab Navigation Style
- **Choice**: Option A (Horizontal Pill/Button style)
- **Rationale**: Aligns with the modern "selector" look and existing `theme.css`.

#### 2. Detailed Audit Layout
- **Choice**: Keep collapsible headers inside the tab.
- **Rationale**: Maintains a compact UI even with the Step-by-Step Guide included.

#### 3. Quick Selection Search
- **Choice**: Option A (Searchable dropdown)
- **Rationale**: Filters as you type for a more integrated feel.

### Session 2026-02-17
- Q: How should Java versions be grouped and sorted in the Quick Selection combobox? -> A: Grouped by major version (e.g., 8, 11, 17, 21) and sorted by version number.
- Q: What are the specific validation requirements for the "List of Versions" text area? -> A: No strict validation; any text is accepted and passed to the backend, with frontend trimming whitespace.
- Q: Is a "Clear" or "Reset" button required for any of the tabs? -> A: No, users can simply clear the text or change the file selection as usual.
- Q: For the "Detailed Audit" email input, is it required to be filled before submission? -> A: No, it is optional; if provided, it's used for instructions, otherwise ignored.
- Q: What happens to the "Step-by-Step Guide" currently on the main page? -> A: It should be moved to the "Detailed Audit" tab as it specifically pertains to gathering property files.


### Functional Requirements

- **FR-001**: Main page MUST feature a 3-tab navigation: "Quick Selection", "List of Versions", "Detailed Audit". ✅
- **FR-002**: "Quick Selection" tab MUST include a searchable combobox component. ✅
- **FR-003**: The combobox MUST be populated with data from `src/main/resources/java_versions.txt`. ✅
- **FR-004**: Versions in the combobox MUST be reasonably grouped (e.g., by Major version) and sorted. ✅
- **FR-005**: "List of Versions" tab MUST provide a large text area for manual entry. ✅
- **FR-006**: "List of Versions" tab MUST support drag-and-drop for `.txt` files to populate the text area. ✅
- **FR-007**: "Detailed Audit" tab MUST support ZIP and `.properties` file uploads (as currently implemented). ✅
- **FR-008**: "Detailed Audit" tab MUST include an optional email input field. ✅
- **FR-009**: The Step-by-Step Guide MUST be moved into the "Detailed Audit" tab. ✅
- **FR-010**: The UI MUST be responsive and follow the existing theme (`theme.css`). ✅
- **FR-011**: All tabs MUST submit to their respective (or a unified) endpoint, though backend implementation is deferred. ✅
- **FR-012**: Selected versions MUST appear as deletable chips inside the combobox (matching reference design). ✅
- **FR-013**: Dark mode MUST have proper text contrast for all elements. ✅
- **FR-014**: Light mode design MUST match the reference images (upload_selector.png, java_chooser.png). ✅
- **FR-015**: Both Quick Selection and List of Versions tabs MUST include a disclaimer about Oracle Java versions. ✅

### Key Entities *(include if feature involves data)*

- **JavaVersionList**: A collection of version strings parsed from `java_versions.txt`.
- **AuditRequest**: The data structure submitted by the frontend, containing either a single selected version, a list of versions, or uploaded files + email.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can switch between tabs without page reloads. ✅
- **SC-002**: Search in the Quick Selection combobox responds in under 100ms for the ~500 versions list. ✅
- **SC-003**: The UI correctly handles drag-and-drop for both version list files and audit archives in their respective tabs. ✅
- **SC-004**: Visual consistency is maintained with the existing "Java Version Analyzer" design. ✅

## Implementation Summary

### Completed Features

1. **Three-Tab Navigation System**
   - Implemented horizontal pill-style tabs with blue accent for active state
   - Smooth transitions between tabs without page reloads
   - Responsive design maintaining consistency with existing theme

2. **Quick Selection Tab**
   - Searchable combobox with ~500 Java versions from `java_versions.txt`
   - Versions grouped by major version (JDK 6, 7, 8, 11, 17, 21, etc.)
   - Selected versions displayed as deletable chips inside the combobox
   - Light blue chips in light mode, matching reference design
   - Dropdown stays open during selection for multi-select workflow
   - Info box with helpful tips and Oracle Java disclaimer

3. **List of Versions Tab**
   - Large text area for bulk version entry (one per line)
   - Drag-and-drop support for .txt files
   - Oracle Java version disclaimer
   - Submit button for analysis

4. **Detailed Audit Tab**
   - Preserved existing ZIP and properties file upload functionality
   - Added optional email input field
   - Relocated Step-by-Step Guide to this tab
   - Maintained all existing collapsible sections

5. **Design Enhancements**
   - Fixed dark mode text visibility issues with proper CSS variable definitions
   - Updated light mode to match reference images (upload_selector.png, java_chooser.png)
   - Light blue chips with dark blue text in light mode
   - Proper blue accent colors (#2563eb) throughout
   - Consistent border radius (12px for inputs, 20px for chips)
   - Blue info boxes with proper contrast

6. **Technical Improvements**
   - Fixed Qute template expression errors by using DOM event listeners instead of inline onclick
   - Event propagation properly handled to keep dropdown open during selections
   - Proper CSS variable inheritance for both light and dark themes
   - All versions grouped by major number (e.g., all 8u* under Java 8)

### Files Modified

- `src/main/resources/templates/JavaVersionResource/index.html` - Main UI implementation
- `src/main/resources/META-INF/resources/css/theme.css` - Dark mode text color fixes
