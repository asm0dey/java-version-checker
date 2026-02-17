# Quickstart Guide: Analysis Page Feature

**Feature**: 005-analysis-page
**Date**: 2026-02-17
**Purpose**: Quick reference for testing and validating the Analysis Page implementation

## Prerequisites

- Quarkus development server running (`./gradlew quarkusDev`)
- Test data files available in `test-data/` directory
- Modern web browser (Chrome, Firefox, Safari, Edge)

## User Journey Testing

### Test Scenario 1: Happy Path (Complete Flow)

**Objective**: Verify end-to-end flow from upload through form submission to revealed results.

**Steps**:
1. Navigate to `http://localhost:8080/`
2. Upload `test-data/java21-oracle.properties`
3. Verify upload success message appears
4. Click "View Analysis" link/button
5. Observe analysis page with:
   - Blurred results table showing fake data (e.g., "XX.X.XX", "XXXX" placeholders)
   - Assessment form visible (email, company name, role fields)
   - Real data not visible in table (hidden in DOM until form submission)
6. Fill form:
   - Email: `test@example.com`
   - Company Name: `Test Corp`
   - Role: `Developer` (optional)
7. Click "Get My Results" button
8. Verify:
   - Form submits successfully
   - Fake data table is hidden
   - Real data table is revealed (no longer hidden)
   - Submit button becomes disabled
   - Results table shows actual analysis data (not fake placeholders) with columns:
     - Oracle JDK Update/Version
     - License Information
     - License Status
     - End of Life (EOL)
     - Recommendation

**Expected Outcome**: User sees complete analysis with licensing details for Java 21.

**Success Criteria**:
- ✓ No JavaScript errors in browser console
- ✓ Fake data cannot be read before form submission (even with inspect element)
- ✓ Real data appears after form submission (swap from fake to real)
- ✓ Results table sorted by recommendation severity
- ✓ Submit button disabled after successful submission
- ✓ All table columns populated with valid data

---

### Test Scenario 2: Invalid Email Validation

**Objective**: Verify email format validation per FR-007.

**Steps**:
1. Navigate through upload flow to analysis page
2. Fill form with:
   - Email: `invalid-email` (no @ symbol)
   - Company Name: `Test Corp`
3. Click "Get My Results"

**Expected Outcome**:
- Form does NOT submit
- Validation error displayed: "Invalid email format"
- Results table remains blurred
- Submit button re-enabled for retry

**Test Variations**:
- `@nodomain.com` (missing username)
- `user@` (missing domain)
- `plaintext` (no @ symbol)
- `` (empty)

---

### Test Scenario 3: Missing Required Fields

**Objective**: Verify required field validation.

**Steps**:
1. Navigate to analysis page
2. Submit form with:
   - Email: (empty)
   - Company Name: (empty)
   - Role: `CTO` (optional, filled)
3. Click "Get My Results"

**Expected Outcome**:
- Form does NOT submit
- Validation errors displayed:
  - "Email is required"
  - "Company name is required"
- Results remain blurred

**Test Variation**: Fill only email, leave company name blank → should show "Company name is required"

---

### Test Scenario 4: Optional Role Field

**Objective**: Verify role field is truly optional per FR-008.

**Steps**:
1. Navigate to analysis page
2. Fill form with:
   - Email: `test@example.com`
   - Company Name: `Test Corp`
   - Role: (leave empty)
3. Click "Get My Results"

**Expected Outcome**:
- Form submits successfully
- Results revealed
- No validation error for empty role

---

### Test Scenario 5: Empty Analysis Results (Blocked Navigation)

**Objective**: Verify FR-015 - prevent navigation to analysis when no versions detected.

**Steps**:
1. Create a test file `test-data/empty.properties` with no Java version properties
2. Upload the empty file at index page
3. Attempt upload

**Expected Outcome**:
- Upload page shows error message: "No valid Java versions detected. Please check your file."
- No "View Analysis" link appears
- User remains on upload/index page

**Verification**: Try navigating directly to `/analysis` → should redirect to index

---

### Test Scenario 6: Session Expiration

**Objective**: Verify session expiration handling.

**Steps**:
1. Upload file and navigate to analysis page
2. Wait for session timeout (default: 30 minutes, or manually invalidate session)
3. Attempt to access `/analysis`

**Expected Outcome**:
- Redirect to index page
- Error message displayed: "Session expired. Please upload again."

**Quick Test** (for development):
```java
// In JavaVersionResource, add temporary endpoint:
@GET
@Path("/debug/clear-session")
public Response clearSession(@Context HttpServletRequest request) {
    request.getSession().invalidate();
    return Response.seeOther(URI.create("/")).build();
}
```
- Visit `/debug/clear-session`, then try `/analysis`

---

### Test Scenario 7: Multiple Form Submissions Prevention

**Objective**: Verify FR-017 - disable button after first successful submission.

**Steps**:
1. Complete flow to analysis page
2. Fill form with valid data
3. Click "Get My Results"
4. Wait for success response
5. Attempt to click submit button again (should be disabled)

**Expected Outcome**:
- Submit button disabled after first successful submission
- Button text may change to "Submitted" or similar
- Clicking disabled button has no effect (no second request sent)

**Verification**: Check browser Network tab - should see only ONE POST to `/analysis/submit`

---

### Test Scenario 8: Results Sorting by Severity

**Objective**: Verify FR-018 - results sorted by recommendation severity.

**Steps**:
1. Upload ZIP file with multiple versions spanning different recommendations:
   - `java21-oracle.properties` → "Keep as is"
   - `java7-at-threshold.properties` → "Migration required"
   - `java17-oracle-at-threshold.properties` → "License required"
2. Navigate to analysis page
3. Submit form to reveal results
4. Observe table row order

**Expected Outcome**:
- Row 1: Java 7 versions with "Migration required"
- Row 2: Java 17 versions with "License required"
- Row N: Java 21 versions with "Keep as is"

**Severity Order**:
1. Migration required (top)
2. License required
3. Migration recommended
4. Keep as is (bottom)

---

### Test Scenario 9: Default to Oracle Vendor

**Objective**: Verify FR-003 - treat manually entered versions as Oracle when no vendor info provided.

**Note**: This scenario tests upload flow modification, not directly analysis page UI.

**Steps**:
1. Upload a properties file without vendor identification
2. Navigate to analysis page
3. Submit form and reveal results
4. Check "License Information" column

**Expected Outcome**:
- Versions should show Oracle-specific licensing (NFTC, OTN, etc.)
- License explanations should reference Oracle terms

---

## API Testing with cURL

### Test GET /analysis (With Valid Session)

**Prerequisite**: Perform upload first to create session.

```bash
# Upload file first
curl -X POST http://localhost:8080/upload \
  -F "file=@test-data/java21-oracle.properties" \
  -c cookies.txt

# Then GET analysis page
curl -X GET http://localhost:8080/analysis \
  -b cookies.txt \
  -v
```

**Expected**: 200 OK, HTML content with blurred table and form

---

### Test GET /analysis (Without Session)

```bash
curl -X GET http://localhost:8080/analysis -v
```

**Expected**: 302 redirect to `/` (or index page)

---

### Test POST /analysis/submit (Valid Data)

```bash
# Upload and get session cookie first
curl -X POST http://localhost:8080/upload \
  -F "file=@test-data/java21-oracle.properties" \
  -c cookies.txt

# Navigate to analysis page (establishes context)
curl -X GET http://localhost:8080/analysis \
  -b cookies.txt -c cookies.txt > /dev/null

# Submit form
curl -X POST http://localhost:8080/analysis/submit \
  -b cookies.txt \
  -d "email=test@example.com" \
  -d "companyName=Test Corp" \
  -d "role=Developer" \
  -H "Content-Type: application/x-www-form-urlencoded"
```

**Expected Response**:
```json
{
  "success": true,
  "message": "Assessment submitted successfully"
}
```

---

### Test POST /analysis/submit (Invalid Email)

```bash
curl -X POST http://localhost:8080/analysis/submit \
  -b cookies.txt \
  -d "email=invalid-email" \
  -d "companyName=Test Corp" \
  -H "Content-Type: application/x-www-form-urlencoded"
```

**Expected Response**:
```json
{
  "success": false,
  "errors": {
    "email": "Invalid email format"
  }
}
```

---

### Test POST /analysis/submit (Missing Required Fields)

```bash
curl -X POST http://localhost:8080/analysis/submit \
  -b cookies.txt \
  -H "Content-Type: application/x-www-form-urlencoded"
```

**Expected Response**:
```json
{
  "success": false,
  "errors": {
    "email": "Email is required",
    "companyName": "Company name is required"
  }
}
```

---

## Integration Test Reference

**Test Class**: `AnalysisPageTest.java`

### Key Test Methods

```java
@Test
void testAnalysisPageWithValidSession() {
    // Upload file to create session
    given()
        .multiPart("file", new File("test-data/java21-oracle.properties"))
    .when()
        .post("/upload")
    .then()
        .statusCode(200)
        .extract().cookies();

    // Access analysis page
    given()
        .cookies(cookies)
    .when()
        .get("/analysis")
    .then()
        .statusCode(200)
        .contentType("text/html")
        .body(containsString("Get Your Instant Assessment Results"));
}

@Test
void testAnalysisPageWithoutSession() {
    given()
    .when()
        .get("/analysis")
    .then()
        .statusCode(302)
        .header("Location", containsString("/"));
}

@Test
void testFormSubmissionWithValidData() {
    // ... setup session ...

    given()
        .cookies(cookies)
        .formParam("email", "test@example.com")
        .formParam("companyName", "Test Corp")
        .formParam("role", "Developer")
    .when()
        .post("/analysis/submit")
    .then()
        .statusCode(200)
        .body("success", equalTo(true));
}

@Test
void testFormValidationInvalidEmail() {
    // ... setup session ...

    given()
        .cookies(cookies)
        .formParam("email", "invalid-email")
        .formParam("companyName", "Test Corp")
    .when()
        .post("/analysis/submit")
    .then()
        .statusCode(400)
        .body("success", equalTo(false))
        .body("errors.email", notNullValue());
}

@Test
void testResultsSortedBySeverity() {
    // Upload multiple versions with different recommendations
    // ... upload multi-version zip ...

    List<String> recommendations =
        given().cookies(cookies).get("/analysis")
            .then().extract().path("versions.recommendation");

    assertThat(recommendations.get(0)).contains("Migration required");
    assertThat(recommendations.get(recommendations.size()-1)).contains("Keep as is");
}
```

---

## Manual QA Checklist

### Visual & UX Validation

- [ ] Analysis page layout matches design (per pics/data_input.png, pics/results.png)
- [ ] Blur effect applies to entire results table
- [ ] Blur effect is visually effective (cannot read data)
- [ ] Form is prominently displayed over blurred results
- [ ] Form fields have clear labels and placeholders
- [ ] Submit button has hover/focus states
- [ ] Submit button disabled state is visually distinct
- [ ] Error messages display inline near relevant fields
- [ ] Success transition (blur removal) is smooth
- [ ] Table headers are clear and aligned
- [ ] Table data is readable after reveal
- [ ] Page is responsive on mobile devices

### Functional Validation

- [ ] Upload → Analysis navigation works
- [ ] Analysis page loads immediately (no waiting spinner)
- [ ] Form validation works for all fields
- [ ] Optional role field truly optional
- [ ] Submit button disables after success
- [ ] Results table shows all required columns
- [ ] Results sorted by severity correctly
- [ ] Empty results blocked during upload
- [ ] Session expiration redirects properly
- [ ] No JavaScript errors in console
- [ ] No backend errors in logs (except expected validation errors)

### Accessibility

- [ ] Form fields have associated labels
- [ ] Error messages announced to screen readers
- [ ] Submit button accessible via keyboard (Tab + Enter)
- [ ] Focus visible on all interactive elements
- [ ] Color contrast meets WCAG AA standards

### Performance

- [ ] Analysis page loads in < 200ms (after session retrieval)
- [ ] Form submission responds in < 500ms
- [ ] Blur effect renders without lag
- [ ] Large result sets (50+ versions) render smoothly

---

## Common Issues & Troubleshooting

### Issue: Analysis page shows "Session expired" immediately after upload

**Cause**: Session not properly created/stored during upload

**Fix**: Verify `session.setAttribute("analysisResults", distinctVersions)` in upload endpoint

---

### Issue: Blur effect not applying

**Cause**: CSS class not added or CSS not loaded

**Fix**: Check browser console for CSS load errors, verify `.blurred` class defined in theme.css or analysis.html

---

### Issue: Form submission returns 400 with no error details

**Cause**: Exception during validation not caught

**Fix**: Add exception handling in POST /analysis/submit endpoint, return structured ValidationErrorResponse

---

### Issue: Results appear immediately without blur

**Cause**: JavaScript not applying initial blur class or `hasFormData` flag incorrectly set

**Fix**: Verify initial state in template: `<div class="results-table {#if !hasFormData}blurred{/if}">`

---

## Next Steps

After manual testing completes:
1. Run full integration test suite: `./gradlew test`
2. Check test coverage: `./gradlew jacocoTestReport`
3. Review coverage report at `build/reports/jacoco/test/html/index.html`
4. Target: 100% coverage of new endpoints and validation logic (per Constitution Principle V)

**Validated Against**: Constitution v1.0.0
**Ready For**: Implementation (`/iikit-06-tasks` → `/iikit-08-implement`)
