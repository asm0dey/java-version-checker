# Feature Specification: Analysis Page with Results

**Feature Branch**: `005-analysis-page`
**Created**: 2026-02-17
**Status**: Draft
**Input**: User description: "Now let's go to the analysis page. When we have data from either of the frontend tabs, we can proceed to analysis. Analysis should be performed on backend, in the same way as it is performed now, except if not properties and not zip were uploaded - we assume all versions to be oracle versions. When the analysis if finished we should show interface like pics/data_input.png. Do not act with inputs for now, just allow valid values to be input there. When the form is filled, we show the interface like on pics/results.png. Do not put the "CVE" part there just yet. Before the data is filled into the form — the data in the table should be fake and blurred"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Navigate to Analysis from Data Input (Priority: P1)

After uploading version data through any frontend tab (properties file, zip archive, or manual entry), users can navigate to an analysis page where their collected Java version information is automatically analyzed for licensing status, end-of-life dates, and upgrade recommendations.

**Why this priority**: This is the core value proposition - transforming raw version data into actionable insights. Without this, users cannot make informed decisions about their Java deployments.

**Independent Test**: Can be fully tested by uploading sample version data and navigating to the analysis page, verifying that analysis results appear without manual intervention.

**Acceptance Scenarios**:

1. **Given** user has uploaded a properties file with version information, **When** they navigate to the analysis page, **Then** the system automatically analyzes all detected versions and displays results
2. **Given** user has uploaded a zip archive containing multiple version sources, **When** analysis completes, **Then** all detected versions are categorized and recommendations are shown
3. **Given** user manually entered version numbers, **When** they proceed to analysis, **Then** the system treats all versions as Oracle distributions by default

---

### User Story 2 - View Blurred Placeholder Before Assessment Form (Priority: P2)

Before providing assessment context (email, company name), users see a preview of what their analysis report will contain, with data blurred or obscured to create anticipation and demonstrate value before personal information is collected.

**Why this priority**: This reduces friction in the user journey by showing value upfront, while deferring personal information collection until users are committed to viewing full results.

**Independent Test**: Can be tested by reaching the analysis page without filling the form - verify that table data is visible but blurred/obscured, and that the form is prominently displayed.

**Acceptance Scenarios**:

1. **Given** analysis has completed, **When** user first views the analysis page, **Then** they see a blurred preview table with fake/obscured data and a prominent form requesting assessment details
2. **Given** the blurred preview is displayed, **When** user observes the table, **Then** they can perceive the structure (columns, rows) but cannot read actual version numbers or recommendations
3. **Given** the form has not been submitted, **When** user attempts to interact with table data, **Then** they are prompted to complete the assessment form first

---

### User Story 3 - Complete Assessment Form to Reveal Results (Priority: P1)

Users provide context about their assessment (email address, company name, optional role) through a form, which unlocks the full, unblurred analysis results showing detailed licensing status, end-of-life dates, and recommendations for each detected Java version.

**Why this priority**: This is the critical conversion point where users exchange contact information for detailed results. The form must accept valid inputs and immediately reveal results to maintain trust.

**Independent Test**: Can be tested by filling out the form with valid data and verifying that the table transitions from blurred to clear, displaying actual analysis results.

**Acceptance Scenarios**:

1. **Given** the blurred preview is displayed, **When** user enters valid email and company name and submits the form, **Then** the table data becomes clear and shows actual version analysis results
2. **Given** user has submitted the form, **When** results are revealed, **Then** they see comprehensive information including version numbers, license types with explanations, license status, end-of-life dates, and specific recommendations
3. **Given** the form requires email and company name, **When** user submits with invalid email format, **Then** the system displays a clear validation error and does not reveal results
4. **Given** the form includes optional role field, **When** user leaves it blank, **Then** the form still submits successfully and results are revealed

---

### User Story 4 - Understand Analysis Results (Priority: P1)

After revealing results, users can read a comprehensive table showing each detected Java version with its licensing requirements, end-of-life status, and specific actionable recommendations (keep as-is, migrate, or license required) in a visually clear format.

**Why this priority**: The analysis results are the primary deliverable. Users must be able to understand their licensing obligations and upgrade paths at a glance.

**Independent Test**: Can be tested by reviewing the revealed results table and verifying that all columns contain meaningful, understandable information aligned with industry-standard Java version lifecycle data.

**Acceptance Scenarios**:

1. **Given** results are revealed, **When** user views the table, **Then** each row shows: Oracle JDK version number, license type with detailed explanation, commercial license status, end-of-life date, and recommendation
2. **Given** a version requires a paid license, **When** displayed in results, **Then** the license status column clearly states "License required" and the explanation describes the specific licensing terms
3. **Given** a version is under No-Fee Terms and Conditions (NFTC), **When** displayed, **Then** the license explanation includes any relevant constraints (e.g., production use restrictions, time-limited free usage)
4. **Given** a version has reached end-of-life, **When** displayed, **Then** the recommendation column indicates "Migration required" or "Migration recommended" based on urgency
5. **Given** multiple versions are detected with different licensing requirements, **When** viewing results, **Then** the table is sorted by recommendation severity with highest-risk versions (Migration required, License required) appearing first

---

### Edge Cases

- When analysis detects zero Java versions: Block navigation to analysis page and display error message on data input page requiring at least one valid version
- How does the system handle partial or corrupted version data that cannot be definitively categorized?
- Multiple form submissions: Submit button is disabled after first successful submission
- How does the system respond if backend analysis fails or times out?
- What happens when vendor licensing policies change after analysis results are generated?
- How does the system handle versions from non-Oracle distributions when user doesn't specify the vendor?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST execute backend analysis during data upload/entry phase, ensuring results are ready when user navigates to analysis page
- **FR-002**: System MUST perform analysis using existing backend logic for version categorization, license detection, and recommendation generation
- **FR-003**: System MUST default to treating all detected versions as Oracle distributions when no vendor information was provided during data collection (no properties file or zip archive uploaded)
- **FR-004**: System MUST display a blurred or obscured preview of analysis results before the assessment form is submitted
- **FR-005**: System MUST show fake placeholder data (e.g., "XX.X.XX", "XXXX") in the preview table that is visually blurred to prevent reading actual values, while storing real analysis results separately for reveal after form submission
- **FR-006**: System MUST display an assessment form prominently on the analysis page requesting: email address (required), company name (required), and role (optional). The form MUST be displayed as a centered overlay modal (600px max-width, z-index 1000) with a semi-transparent backdrop to ensure visual prominence over the blurred results table
- **FR-007**: System MUST validate email address format before accepting form submission. If validation fails, the system MUST display the error message "Please enter a valid email address" inline below the email field in red text
- **FR-020**: System MUST display validation errors inline below each invalid form field, maintaining focus within the form modal
- **FR-008**: System MUST allow submission of the form even if the optional role field is left blank
- **FR-009**: System MUST immediately reveal the full, unblurred analysis results when the assessment form is successfully submitted with valid data
- **FR-016**: System MUST send assessment form data (email, company name, role) to backend endpoint but backend MUST NOT process or persist this data at this stage
- **FR-017**: System MUST disable the submit button after the first successful form submission to prevent multiple submissions
- **FR-018**: System MUST sort the results table by recommendation severity in descending order of urgency: Migration required, License required, Migration recommended, Keep as is
- **FR-019**: System MUST display a loading indicator (spinner or progress indicator) when users first navigate to the analysis page while session data is being retrieved and the page is rendering
- **FR-021**: System MUST display a prominent "View Analysis Results" button on the upload success page, centered below the success message, styled as a primary call-to-action button that navigates to the analysis page
- **FR-010**: System MUST display analysis results in a table format with the following columns: Oracle JDK Update/Version, License Information (tag + explanation + source), License Status for Commercial Production, End of Life (EOL) date, Recommendation
- **FR-011**: System MUST NOT display CVE (vulnerability) information in the results at this stage
- **FR-012**: System MUST provide clear, actionable recommendations for each version (e.g., "Keep as is", "Migration recommended", "Migration required", "License required")
- **FR-013**: System MUST include detailed explanations for each license type, describing the terms and conditions in human-readable language
- **FR-014**: System MUST display end-of-life dates in a clear, unambiguous date format
- **FR-015**: System MUST prevent navigation to the analysis page when no valid version data is detected, and MUST display a clear error message on the data input page explaining that at least one valid Java version is required

### Key Entities

- **Analysis Result**: Represents the outcome of analyzing a single Java version, including version number, detected license type, commercial license requirement status, end-of-life date, and recommended action
- **Assessment Form Submission**: Captures user contact information (email, company name, role) provided in exchange for viewing detailed analysis results
- **Version Data Source**: The origin of version information (properties file upload, zip archive upload, or manual entry), which determines whether vendor is known or assumed to be Oracle

## Clarifications

### Session 2026-02-17

- Q: What should the system do with the submitted assessment form data? -> A: Send to backend, don't process data there
- Q: When should backend analysis execute? -> A: Analysis executes during data upload, results ready when navigating to analysis page
- Q: What should happen when no Java versions are detected? -> A: Block navigation to analysis page, show error on data input page
- Q: How should the system handle multiple form submissions? -> A: Disable submit button after first successful submission
- Q: How should the results table be sorted by default? -> A: Sort by recommendation severity (Migration required > License required > Migration recommended > Keep as is)

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can view blurred preview results immediately upon reaching the analysis page without waiting for additional processing
- **SC-002**: Users successfully complete the assessment form and reveal results in under 1 minute from reaching the analysis page
- **SC-003**: 95% of users who reach the analysis page complete the assessment form to reveal full results
- **SC-004**: Users can understand their licensing obligations and required actions by reading the results table without requiring external documentation
- **SC-005**: The blurred preview effectively demonstrates value while preventing actual data from being read before form submission
- **SC-006**: Analysis results accurately reflect current Oracle JDK licensing policies and end-of-life schedules as documented by the vendor
- **SC-007**: Users with mixed-version environments can distinguish which versions require action and which are compliant by scanning the recommendations column
