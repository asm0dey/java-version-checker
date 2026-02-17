# Data Model: Analysis Page

**Feature**: 005-analysis-page
**Date**: 2026-02-17
**Status**: Phase 1

## Overview

This document defines the data entities and their relationships for the Analysis Page feature. Most entities already exist in the codebase; this feature adds minimal new data structures.

## Entities

### 1. AnalysisFormData (NEW)

**Purpose**: Captures user-provided contact information for unlocking analysis results.

**Lifecycle**: Created on form submission, validated, logged, then discarded (not persisted per FR-016).

**Attributes**:

| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| email | String | Yes | Email format (@Email annotation) | User's email address |
| companyName | String | Yes | Not blank (@NotBlank annotation) | Company or organization name |
| role | String | No | None (optional field) | User's role/title (e.g., "CTO", "Developer") |

**Java Representation**:
```java
public record AnalysisFormData(
    @Email(message = "Invalid email format")
    @NotNull(message = "Email is required")
    String email,

    @NotBlank(message = "Company name is required")
    String companyName,

    String role  // Optional, no validation
) {}
```

**Validation Rules** (per Constitution Principle III):
- Email MUST match RFC 5322 format
- Company name MUST NOT be blank or whitespace-only
- Role MAY be null or empty

**Privacy Considerations**:
- Data sent to backend but NOT persisted (FR-016)
- Logged at INFO level for observability only
- No PII retention

---

### 2. JavaVersionInfo (EXISTING)

**Purpose**: Represents analysis result for a single Java version.

**Source**: `src/main/java/com/github/asm0dey/JavaVersionInfo.java` (already exists)

**Relevant Attributes** (for Analysis Page display):

| Field | Type | Description |
|-------|------|-------------|
| version | String | Java version string (e.g., "17.0.13", "21.0.10") |
| vendor | String | Detected vendor (e.g., "Oracle", "Adoptium", "OpenJDK") |
| licenseType | String | License tag (e.g., "NFTC", "OTN", "BCL") |
| licenseExplanation | String | Human-readable license description |
| licenseSource | String | Source URL or reference for license info |
| requiresCommercialLicense | boolean | True if paid license needed for production |
| endOfLife | String | EOL date (ISO format or descriptive string) |
| recommendation | String | Action to take (e.g., "Keep as is", "Migration required") |

**No changes needed** to this entity. Analysis Page consumes existing fields.

---

### 3. AnalysisSession (CONCEPTUAL)

**Purpose**: HTTP session state for managing analysis results between upload and analysis page.

**Storage**: Jakarta Servlet HttpSession (in-memory)

**Attributes**:

| Session Key | Value Type | Description |
|-------------|------------|-------------|
| `analysisResults` | `List<JavaVersionInfo>` | Pre-computed real analysis results from upload (rendered in hidden div) |
| `fakePreviewData` | `List<JavaVersionInfo>` | Generated fake data matching row count (shown blurred until form submission) |
| `formSubmitted` | `Boolean` | Tracks if assessment form was submitted |
| `uploadTimestamp` | `Long` | Timestamp of upload (for session expiration tracking) |

**Lifecycle**:
1. Created during POST `/upload` (existing endpoint, modified)
   - Stores real `analysisResults`
   - Generates `fakePreviewData` with matching row count (e.g., "XX.X.XX", "XXXX" placeholders)
2. Read by GET `/analysis` (new endpoint)
   - Renders both fake data (visible, blurred) and real data (hidden)
3. Updated by POST `/analysis/submit` (new endpoint, sets `formSubmitted=true`)
4. Client-side JavaScript swaps fake table with real table on successful form submission
5. Expires automatically via session timeout (default: 30 minutes)

**Expiration Handling**:
- If session expired, redirect to index page with message "Session expired. Please upload again."

---

## Data Flow

### Upload → Analysis Flow

```
1. User uploads file → POST /upload
2. JavaVersionService analyzes versions
3. Generate fake preview data: generateFakePreviewData(distinctVersions.size())
4. Store in session:
   - session.setAttribute("analysisResults", distinctVersions)  // Real data
   - session.setAttribute("fakePreviewData", fakeData)          // Fake data
5. If versions.isEmpty() → show error, block navigation
6. Else → show success with "View Analysis" link

7. User clicks "View Analysis" → GET /analysis
8. Retrieve session data:
   - realResults = session.getAttribute("analysisResults")
   - fakeData = session.getAttribute("fakePreviewData")
9. If null → redirect to index (expired/missing session)
10. Else → render analysis.html with:
    - Fake data table (visible, blurred)
    - Real data table (hidden in DOM)

11. User fills form → POST /analysis/submit
12. Validate AnalysisFormData
13. If valid → log submission, set session.setAttribute("formSubmitted", true)
14. Return success → client-side JavaScript:
    - Hides fake table
    - Shows real table
15. If invalid → return validation errors → show on form
```

### Session State Diagram

```
[No Session] → POST /upload → [Session with Results]
                                       ↓
                                GET /analysis
                                       ↓
                         [Viewing Blurred Preview]
                                       ↓
                            POST /analysis/submit
                                       ↓
                            [Form Submitted, Revealed]
```

---

## Validation Rules

### AnalysisFormData Validation

**Email Validation** (FR-007):
- Pattern: `^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$` (standard Jakarta @Email)
- Examples:
  - Valid: `user@example.com`, `name+tag@domain.co.uk`
  - Invalid: `plaintext`, `@missing.com`, `user@`

**Company Name Validation**:
- Must not be blank (after trimming)
- Examples:
  - Valid: `Acme Corp`, `My Company Inc.`
  - Invalid: ``, ` ` (spaces only), `null`

**Role Validation**:
- No validation (optional field)

### Session Data Validation

**analysisResults Validation**:
- Must be non-null List<JavaVersionInfo>
- Must contain at least 1 element (enforced during upload)
- If empty or null on GET /analysis → redirect to index

---

## Sorting and Ordering

### Results Table Sorting (FR-018)

**Sort Order**: By recommendation severity (descending urgency)

**Priority**:
1. "Migration required" (highest)
2. "License required"
3. "Migration recommended"
4. "Keep as is" (lowest)

**Implementation**: Sort `List<JavaVersionInfo>` before passing to template

**Tie-breaking**: If multiple versions have same recommendation, sort by version number (descending)

---

## Relationships

```
AnalysisSession (HTTP Session)
    └── analysisResults: List<JavaVersionInfo>  [1:N]

AnalysisFormData (transient)
    └── No relationships (discarded after validation)

JavaVersionInfo (existing)
    └── No changes to existing relationships
```

---

## State Transitions

### Analysis Results State

```
[Not Analyzed] → POST /upload → [Analyzed, Stored in Session]
                                        ↓
                                 [Blurred Preview]
                                        ↓
                            POST /analysis/submit (valid)
                                        ↓
                                  [Revealed Results]
```

### Form Submission State

```
[Not Submitted] → POST /analysis/submit (valid) → [Submitted]
                         ↓ (invalid)
                   [Validation Errors Shown]
                         ↓
                   [Retry Submission]
```

---

## Edge Cases

### Missing Session Data
- **Scenario**: User navigates to `/analysis` without uploading first
- **Handling**: Redirect to index with message "Please upload your Java version data first"

### Session Expiration
- **Scenario**: User uploads, waits > 30 minutes, then navigates to analysis
- **Handling**: Session expired → redirect to index with message "Session expired. Please upload again."

### Empty Analysis Results
- **Scenario**: Upload succeeds but no versions detected
- **Handling**: Blocked during upload (per clarification Q3). Analysis page never reached.

### Malformed Form Data
- **Scenario**: Invalid email, blank company name
- **Handling**: Return 400 Bad Request with validation errors in JSON format

---

## Privacy & Security

### Data Retention
- **AnalysisFormData**: Logged at INFO level, not persisted to database
- **AnalysisResults**: Stored in HTTP session only (ephemeral, expires automatically)
- **No PII**: Long-term storage (per FR-016 and Constitution Principle III)

### Input Sanitization
- Email: Validated via Jakarta @Email (prevents injection)
- Company Name: Trimmed, validated non-blank (prevents whitespace-only)
- Role: Optional, no special sanitization (stored transiently only)

---

## Summary

Minimal new entities introduced:
- **AnalysisFormData** (transient DTO)
- **AnalysisSession** (conceptual, uses standard HttpSession)

Reuses existing:
- **JavaVersionInfo** (no modifications)

**Validated Against**: Constitution v1.0.0 (Principle III: Defensive Data Handling)
**Next**: Create API contracts
