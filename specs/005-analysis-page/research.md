# Research & Design Decisions: Analysis Page

**Feature**: 005-analysis-page
**Date**: 2026-02-17
**Status**: Phase 0 Complete

## Overview

This document captures technical research and design decisions for the Analysis Page feature. The feature adds a new user-facing page that displays Java version licensing analysis results with a "preview + form + reveal" interaction pattern.

## Technical Context Research

### 1. Existing Analysis Flow

**Current State**:
- User uploads `.properties` file or `.zip` archive via POST `/upload`
- `JavaVersionService.parsePropertiesFile()` extracts version info
- `JavaVersionService.getDistinctVersions()` deduplicates results
- `JavaVersionInfo` objects contain all analysis data (version, license, EOL, recommendation)
- Results rendered immediately via `results.html` Qute template

**Finding**: Analysis logic is complete and tested. No changes needed to core analysis.

**Decision**: Reuse existing `JavaVersionService` and `JavaVersionInfo` without modification.

**Rationale**: Constitution Principle I (Accuracy in Version Detection) requires tested, consistent version parsing. Reusing proven logic maintains accuracy.

### 2. Data Flow for Analysis Page

**Question**: When should analysis execute - during upload or when navigating to analysis page?

**Research**: Per clarification Q2, analysis executes during data upload phase.

**Decision**:
1. Modify upload flow to store analysis results in HTTP session
2. Analysis page retrieves pre-computed results from session
3. No re-analysis on page load

**Rationale**:
- Satisfies SC-001 ("immediately upon reaching analysis page")
- Avoids duplicate computation
- Session storage is simple, ephemeral (no persistence concerns)

**Alternatives Considered**:
- Re-run analysis on page load: Rejected due to unnecessary computation
- Database storage: Rejected due to FR-016 (no backend persistence)

### 3. Blur/Reveal Mechanism

**Question**: How to implement blurred preview before form submission?

**Research Options**:
A. CSS filter blur + fake data + JavaScript toggle
B. Server-side render two versions (fake blurred + real clear)
C. Canvas-based blur rendering

**Decision**: CSS filter blur + fake data with real data stored separately (Option A enhanced)

**Implementation**:
```css
.results-table.preview {
  filter: blur(8px);
  pointer-events: none;
  user-select: none;
}
```

**Template Structure**:
```html
<!-- Fake data table (initially visible, blurred) -->
<div class="results-table preview" id="preview-table">
  <!-- Render with fake JavaVersionInfo objects -->
  <table>
    <tr>
      <td>XX.X.XX</td>
      <td>XXXX — Xxxxxxxxxx xxxx...</td>
      <td>Xxxxxxx xxxxxxxx</td>
      <td>Xxx XXXX</td>
      <td>Xxxxxxxxx xxxxxxxx</td>
    </tr>
    <!-- More fake rows matching actual row count -->
  </table>
</div>

<!-- Real data table (initially hidden) -->
<div class="results-table hidden" id="real-table">
  <!-- Render with actual JavaVersionInfo from session -->
  <table>
    {#for version in versions}
    <tr>
      <td>{version.version}</td>
      <td>{version.licenseType} — {version.licenseExplanation}</td>
      <!-- etc -->
    </tr>
    {/for}
  </table>
</div>

<script>
// On form submit success:
document.getElementById('preview-table').classList.add('hidden');
document.getElementById('real-table').classList.remove('hidden');
</script>
```

**Fake Data Generation**:
```java
public static List<JavaVersionInfo> generateFakePreviewData(int count) {
    List<JavaVersionInfo> fakeData = new ArrayList<>();
    for (int i = 0; i < count; i++) {
        fakeData.add(new JavaVersionInfo(
            "XX.X.XX",  // Fake version
            "XXXX",     // Fake vendor
            "XXXX",     // Fake license type
            "Xxxxxxxxxx xxxx xxxxxxxxx xxxx xxxxxxx",  // Fake explanation
            "Xxxxxx +X",  // Fake source
            false,
            "Xxx XXXX",  // Fake EOL
            "Xxxxxxxxx xxxxxxxx"  // Fake recommendation
        ));
    }
    return fakeData;
}
```

**Rationale**:
- Fake data ensures actual analysis results cannot be read even if blur is bypassed
- Real data stored in hidden div (or JavaScript variable) ready for instant reveal
- Matches row count to maintain table structure perception
- Blur adds additional visual obfuscation layer
- Simple JavaScript swap on form success
- Prevents data scraping via inspect element before form submission

**Alternatives Considered**:
- Blur only with real data: Rejected (user could inspect DOM to read real values)
- Server-side render two versions separately: Rejected (doubles bandwidth, complicates caching)
- Store real data in JavaScript encrypted: Rejected (unnecessary complexity, easily reversible)

**Security Note**:
- Real data still sent to client (hidden in DOM or JS variable)
- This is acceptable since data was already computed server-side during upload
- User "earned" the data by uploading; form submission is a gate-keeping UX pattern, not security

### 4. Form Data Handling

**Question**: What happens with submitted form data (email, company name, role)?

**Research**: Per clarification Q1, send to backend but don't process/persist.

**Decision**:
1. Create `AnalysisFormData` DTO with validation annotations
2. POST `/analysis/submit` accepts form data
3. Endpoint validates input, returns success/error
4. Backend logs submission (INFO level) but does NOT persist to database

**Rationale**:
- Satisfies FR-016 (send to backend, don't process)
- Validation per FR-007 (email format) and Constitution Principle III (defensive data handling)
- Logging provides observability without persistence

**DTO Structure**:
```java
public record AnalysisFormData(
    @Email String email,
    @NotBlank String companyName,
    String role  // Optional
) {}
```

### 5. Empty Results Handling

**Question**: What happens when no Java versions are detected?

**Research**: Per clarification Q3, block navigation to analysis page and show error on data input page.

**Decision**:
1. Modify upload endpoint to check if `distinctVersions.isEmpty()`
2. If empty, return error response on upload page
3. Do NOT store empty results in session
4. Analysis page link only appears when session contains valid results

**Rationale**:
- Prevents user confusion (no empty analysis page)
- Validates data early in the flow
- Aligns with Constitution Principle III (defensive data handling)

**Implementation**:
```java
if (distinctVersions.isEmpty()) {
    return Templates.index()
        .data("error", "No valid Java versions detected. Please check your file.");
}
```

### 6. Result Sorting

**Question**: How should results table be sorted?

**Research**: Per clarification Q5, sort by recommendation severity.

**Decision**: Implement comparator in `JavaVersionInfo` or add sorting method in service:

**Severity Order**:
1. Migration required (highest urgency)
2. License required
3. Migration recommended
4. Keep as is (lowest urgency)

**Implementation**:
```java
public static List<JavaVersionInfo> sortByRecommendationSeverity(List<JavaVersionInfo> versions) {
    Map<String, Integer> severityOrder = Map.of(
        "Migration required", 1,
        "License required", 2,
        "Migration recommended", 3,
        "Keep as is", 4
    );
    return versions.stream()
        .sorted(Comparator.comparingInt(v ->
            severityOrder.getOrDefault(v.recommendation(), 999)))
        .toList();
}
```

**Rationale**: FR-018 mandates this sorting. Users see highest-risk versions first per Constitution Principle IV (clear risk communication).

### 7. Multiple Form Submissions

**Question**: How to prevent multiple form submissions?

**Research**: Per clarification Q4, disable submit button after first successful submission.

**Decision**: Client-side button disabling + visual feedback

**Implementation**:
```javascript
form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const submitButton = form.querySelector('button[type="submit"]');
    submitButton.disabled = true;
    submitButton.textContent = 'Processing...';

    try {
        const response = await fetch('/analysis/submit', {...});
        if (response.ok) {
            // Reveal results, keep button disabled
        } else {
            // Re-enable on error
            submitButton.disabled = false;
            submitButton.textContent = 'Get My Results';
        }
    } catch (error) {
        submitButton.disabled = false;
        submitButton.textContent = 'Get My Results';
    }
});
```

**Rationale**: Simple UX pattern, prevents accidental duplicate submissions per FR-017.

## Technology Stack Decisions

### Qute Templates

**Decision**: Use Qute for `analysis.html` template

**Rationale**: Consistent with existing codebase (`index.html`, `results.html` already use Qute)

**Template Parameters**:
```java
public static native TemplateInstance analysis(
    Collection<JavaVersionInfo> versions,
    boolean hasFormData
);
```

### Session Management

**Decision**: Use Jakarta Servlet HTTP Session for storing analysis results

**Rationale**:
- Built into Quarkus
- No external dependencies needed
- Automatic expiration handles cleanup
- Ephemeral (no persistence concerns)

**Session Attributes**:
- `"analysisResults"` → `List<JavaVersionInfo>`
- `"formSubmitted"` → `Boolean` (tracks if form was submitted)

## Testing Strategy

### Unit Tests

**AnalysisFormValidationTest**:
- Test email format validation (valid/invalid cases)
- Test company name blank/null handling
- Test optional role field

**JavaVersionService** (existing):
- No new tests needed for analysis logic
- Add test for `sortByRecommendationSeverity()` method

### Integration Tests

**AnalysisPageTest**:
- Test GET `/analysis` with valid session data
- Test GET `/analysis` without session data (redirect to index)
- Test POST `/analysis/submit` with valid form data
- Test POST `/analysis/submit` with invalid email
- Test POST `/analysis/submit` with missing required fields
- Test results table sorting order
- Test empty results prevention during upload

**Coverage Target**: Per Constitution Principle V, all new logic must have automated tests.

## Performance Considerations

**Session Storage Size**:
- Typical upload: 10-50 versions
- `JavaVersionInfo` object size: ~500 bytes
- Session data: ~5-25KB per user
- Acceptable for in-memory session storage

**Page Load Time**:
- Analysis results pre-computed (SC-001)
- Qute template rendering: <50ms
- Blur CSS applied immediately
- Target: < 200ms page load

**Form Submission**:
- Validation only (no persistence)
- Target: < 500ms response time (per Technical Context)

## Open Questions / Future Considerations

**None at this time.** All clarifications resolved during `/iikit-02-clarify`.

## Summary

All technical decisions documented. No blockers identified. Ready for Phase 1 (Design & Contracts).

**Validated Against**: Constitution v1.0.0
**Next Phase**: Create data-model.md and API contracts
