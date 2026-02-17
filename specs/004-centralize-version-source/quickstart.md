# Quickstart Guide: Centralize Java Versions Data Source

**Feature**: 004-centralize-version-source
**Date**: 2026-02-17

## Overview

This guide helps developers quickly understand and test the centralized Java version data source feature.

## What This Feature Does

**Before**: Frontend had hardcoded JavaScript array of 523 Java versions. Backend read from `java_versions.txt`. Risk of inconsistency.

**After**: Frontend fetches versions from backend API at page load. Backend serves data from `java_versions.txt`. Single source of truth guaranteed.

## Quick Test Scenarios

### Scenario 1: Verify API Returns Version List

**Purpose**: Confirm the new `/api/versions` endpoint works correctly

**Steps**:
```bash
# Start the application
./gradlew quarkusDev

# Call the API endpoint
curl http://localhost:8080/api/versions

# Expected: JSON array of 500+ version strings
# ["1.0","1.0.1","1.0.2",...,"25","25.0.1","25.0.2"]
```

**Success Criteria**:
- ✅ Returns HTTP 200
- ✅ Content-Type is `application/json`
- ✅ Response is a JSON array
- ✅ Array contains 500+ strings
- ✅ Array includes "1.0", "11", "17", "21", "25"

### Scenario 2: Verify Frontend Uses API

**Purpose**: Confirm frontend loads versions dynamically from backend

**Steps**:
1. Start application: `./gradlew quarkusDev`
2. Open browser to http://localhost:8080
3. Open DevTools Network tab
4. Reload page
5. Look for request to `/api/versions`

**Success Criteria**:
- ✅ Network tab shows GET request to `/api/versions`
- ✅ Response is JSON array
- ✅ Version dropdown populates with versions
- ✅ No hardcoded `javaVersions` array in JavaScript console

### Scenario 3: Verify Consistency Between Frontend and Backend

**Purpose**: Ensure backend and frontend show identical versions

**Steps**:
```bash
# Get versions from API
curl http://localhost:8080/api/versions > api-versions.json

# Get versions from file
cat src/main/resources/java_versions.txt > file-versions.txt

# Compare (frontend should match API, API should match file)
# Manual verification: Open frontend, check dropdown matches file
```

**Success Criteria**:
- ✅ API response matches file content (line-by-line)
- ✅ Frontend dropdown shows same versions as API
- ✅ Order is preserved (file order → API → frontend)

### Scenario 4: Update Version List

**Purpose**: Verify updates to `java_versions.txt` are reflected after restart

**Steps**:
```bash
# Add a test version to the file
echo "99.99.99-test" >> src/main/resources/java_versions.txt

# Restart application
# (Stop with Ctrl+C, then ./gradlew quarkusDev)

# Verify new version appears in API
curl http://localhost:8080/api/versions | grep "99.99.99-test"

# Verify new version appears in frontend dropdown
# Open http://localhost:8080, search for "99.99.99-test" in dropdown
```

**Success Criteria**:
- ✅ New version appears in API response
- ✅ New version appears in frontend dropdown
- ✅ No code changes were required, only file edit + restart

**Cleanup**:
```bash
# Remove test version
sed -i '/99.99.99-test/d' src/main/resources/java_versions.txt
```

### Scenario 5: Error Handling

**Purpose**: Verify graceful degradation when API fails

**Steps**:
1. Start application
2. Open DevTools Network tab
3. Block `/api/versions` request (DevTools → Network → Right-click → Block request URL)
4. Reload page

**Success Criteria**:
- ✅ Page loads without crashing
- ✅ Error message displayed: "Failed to load Java versions"
- ✅ Error logged to console
- ✅ Page remains interactive (upload tab still works)

## Running Tests

### Unit Tests

```bash
# Test VersionListService
./gradlew test --tests VersionListServiceTest

# Test API endpoint
./gradlew test --tests JavaVersionResourceTest
```

### Integration Tests

```bash
# Run all tests
./gradlew test

# Verify version list consistency
./gradlew test --tests '*VersionConsistency*'
```

## Key Files

| File | Purpose | Action Required |
|------|---------|-----------------|
| `src/main/resources/java_versions.txt` | Single source of truth for versions | Edit to add/remove versions |
| `src/main/java/.../VersionListService.java` | Loads and caches version list | No changes after implementation |
| `src/main/java/.../JavaVersionResource.java` | Serves `/api/versions` endpoint | No changes after implementation |
| `src/main/resources/templates/.../index.html` | Frontend fetches versions | No changes after implementation |

## Common Issues & Solutions

### Issue: Frontend shows empty dropdown

**Symptoms**: Dropdown is empty, no versions visible

**Diagnosis**:
```bash
# Check if API is working
curl http://localhost:8080/api/versions

# Check browser console for errors
# (Open DevTools, look for red errors)
```

**Solutions**:
1. Verify application is running: `./gradlew quarkusDev`
2. Check API endpoint returns data: `curl http://localhost:8080/api/versions`
3. Check browser console for JavaScript errors
4. Clear browser cache and reload

### Issue: API returns 500 error

**Symptoms**: `curl /api/versions` returns 500 Internal Server Error

**Diagnosis**:
```bash
# Check application logs for exceptions
# Look for errors related to VersionListService or java_versions.txt
```

**Solutions**:
1. Verify `src/main/resources/java_versions.txt` exists
2. Check file is not empty
3. Check file has valid content (non-empty lines)
4. Restart application: `./gradlew clean quarkusDev`

### Issue: New versions not appearing

**Symptoms**: Added version to file, but not showing in frontend/API

**Diagnosis**:
```bash
# Check if version is in file
grep "new-version" src/main/resources/java_versions.txt

# Check if application restarted
# (VersionListService loads file once at startup)
```

**Solutions**:
1. Verify version was added to `src/main/resources/java_versions.txt`
2. **Restart application** (required to pick up file changes)
3. Clear browser cache
4. Verify with: `curl http://localhost:8080/api/versions | grep "new-version"`

### Issue: Versions in wrong order

**Symptoms**: Frontend dropdown shows versions in unexpected order

**Diagnosis**:
```bash
# Check file order
cat -n src/main/resources/java_versions.txt

# Check API response order
curl http://localhost:8080/api/versions | jq '.'
```

**Solutions**:
- API preserves file order exactly
- Frontend groups by major version, but within groups preserves order
- Edit `java_versions.txt` to change order
- Restart application

## Performance Verification

### API Response Time

```bash
# Measure API response time
time curl -s http://localhost:8080/api/versions > /dev/null

# Expected: < 100ms (should be < 10ms for in-memory data)
```

### Frontend Load Time

```bash
# Use browser DevTools Network tab
# Reload page, check timing for /api/versions request
# Expected: < 100ms
```

### Memory Usage

```bash
# Check heap usage before and after loading versions
# Expected increase: < 10KB (small string array)
```

## Next Steps After Verification

1. ✅ All test scenarios pass
2. ✅ Run full test suite: `./gradlew test`
3. ✅ Manual testing in browser
4. ✅ Update CLAUDE.md if needed
5. ✅ Commit changes
6. ✅ Create PR with test results

## Reference Links

- [Specification](spec.md)
- [Implementation Plan](plan.md)
- [Research](research.md)
- [Data Model](data-model.md)
- [API Contract](contracts/versions-api.yaml)

## Support

For issues or questions:
1. Check application logs
2. Review error messages in browser console
3. Verify `java_versions.txt` file is valid
4. Restart application
5. Run tests: `./gradlew test`
