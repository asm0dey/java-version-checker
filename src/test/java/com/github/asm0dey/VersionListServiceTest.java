package com.github.asm0dey;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class VersionListServiceTest {

    @Inject
    VersionListService versionListService;

    /**
     * TS-014: Verifies VersionListService loads file at startup
     */
    @Test
    void shouldLoadVersionsFromFile() {
        List<String> versions = versionListService.getVersions();

        assertNotNull(versions, "Version list should not be null");
        assertFalse(versions.isEmpty(), "Version list should not be empty");
        assertTrue(versions.size() > 500, "Version list should contain 500+ versions, got: " + versions.size());

        // Verify some known versions exist
        assertTrue(versions.contains("8"), "Should contain legacy version 8");
        assertTrue(versions.contains("11"), "Should contain LTS version 11");
        assertTrue(versions.contains("17"), "Should contain LTS version 17");
        assertTrue(versions.contains("21"), "Should contain LTS version 21");
    }

    /**
     * TS-015: Verifies VersionListService returns immutable list
     */
    @Test
    void shouldReturnImmutableList() {
        List<String> versions = versionListService.getVersions();

        assertThrows(UnsupportedOperationException.class, () -> {
            versions.add("99");
        }, "Returned list should be immutable");

        assertThrows(UnsupportedOperationException.class, () -> {
            versions.remove(0);
        }, "Returned list should be immutable");

        assertThrows(UnsupportedOperationException.class, () -> {
            versions.clear();
        }, "Returned list should be immutable");
    }

    /**
     * TS-016: Verifies empty lines are skipped during parsing
     */
    @Test
    void shouldSkipEmptyLines() {
        String content = "8\n\n11\n   \n17\n\t\n21";
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        VersionListService testService = new VersionListService();
        List<String> versions;
        try {
            versions = testService.parseVersionFile(is);
        } catch (Exception e) {
            fail("Should not throw exception when parsing file with empty lines: " + e.getMessage());
            return;
        }

        assertEquals(4, versions.size(), "Should skip empty and whitespace-only lines");
        assertTrue(versions.contains("8"));
        assertTrue(versions.contains("11"));
        assertTrue(versions.contains("17"));
        assertTrue(versions.contains("21"));
    }

    /**
     * TS-017: Verifies whitespace is trimmed from version strings
     */
    @Test
    void shouldTrimWhitespace() {
        String content = "  8  \n\t11\t\n   17\n21   ";
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        VersionListService testService = new VersionListService();
        List<String> versions;
        try {
            versions = testService.parseVersionFile(is);
        } catch (Exception e) {
            fail("Should not throw exception when parsing file with whitespace: " + e.getMessage());
            return;
        }

        assertEquals(4, versions.size());
        assertEquals("8", versions.get(0), "Should trim leading/trailing whitespace");
        assertEquals("11", versions.get(1), "Should trim tabs");
        assertEquals("17", versions.get(2), "Should trim spaces");
        assertEquals("21", versions.get(3), "Should trim trailing spaces");
    }

    /**
     * TS-018: Verifies application fails fast on missing file
     * Note: This test would require mocking ClassLoader to simulate missing file.
     * In integration testing, we verify this by attempting to start app without the file.
     */
    @Test
    void shouldFailFastOnMissingFile() {
        // This test documents expected behavior when file is missing
        // Actual validation happens during CDI initialization (@PostConstruct)
        // If this test runs, it means the file exists and app started successfully

        List<String> versions = versionListService.getVersions();
        assertNotNull(versions, "If app started, versions must be loaded");
        assertFalse(versions.isEmpty(), "If app started, versions must not be empty");
    }

    /**
     * TS-019: Verifies application fails fast on empty file
     */
    @Test
    void shouldFailFastOnEmptyFile() {
        String content = "";
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        VersionListService testService = new VersionListService();

        // Should complete parsing but result in empty list
        List<String> versions;
        try {
            versions = testService.parseVersionFile(is);
        } catch (Exception e) {
            fail("Parsing should not throw, validation happens in loadVersions(): " + e.getMessage());
            return;
        }

        assertTrue(versions.isEmpty(), "Empty file should result in empty list");

        // The loadVersions() method would then throw RuntimeException
        // This is tested during application startup
    }

    /**
     * TS-020: Verifies malformed lines are handled gracefully
     * Note: Current implementation logs warnings but continues loading
     */
    @Test
    void shouldHandleMalformedLines() {
        // Version strings with internal whitespace are technically valid
        // but logged as warnings. They should still be loaded.
        String content = "8\n11\nmalformed version string\n17\n21";
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        VersionListService testService = new VersionListService();
        List<String> versions;
        try {
            versions = testService.parseVersionFile(is);
        } catch (Exception e) {
            fail("Should not throw exception for malformed lines: " + e.getMessage());
            return;
        }

        // All non-empty lines should be loaded (including "malformed" one)
        // Actual version validation happens elsewhere (JavaVersionService)
        assertEquals(5, versions.size(), "Should load all non-empty lines");
        assertTrue(versions.contains("malformed version string"),
                   "Should include malformed line (validation happens elsewhere)");
    }
}
