package com.github.asm0dey;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.StringContains.containsString;

@QuarkusTest
public class JavaVersionResourceTest {


    @Test
    public void testZipFileUpload() throws IOException {
        // Create a test zip file with Java version properties
        byte[] zipData = createTestZipFile();
        String csrfToken = given()
                .when().get("/")
                .then().statusCode(200)
                .extract().cookie("csrf-token");
        // Test the upload endpoint
        given()
                .cookie("csrf-token", csrfToken)
                .formParam("csrf-token", csrfToken)
                .multiPart("file", "test.zip", zipData, "application/zip")
                .when()
                .post("/upload")
                .then()
                .statusCode(200)
                .contentType(ContentType.HTML)
                .body(containsString("Java Version Analysis Results"));
    }

    @Test
    public void testZipFileUploadWithoutFile() {
        String csrfToken = given()
                .when().get("/")
                .then().statusCode(200)
                .extract().cookie("csrf-token");
        // Test upload without file should return 415
        given()
                .cookie("csrf-token", csrfToken)
                .formParam("csrf-token", csrfToken)
                .when()
                .post("/upload")
                .then()
                .statusCode(415);
    }

    @Test
    public void testPropertiesFileUpload() {
        String properties = """
                java.version=21.0.1
                java.vendor=Eclipse Adoptium
                java.vm.name=OpenJDK 64-Bit Server VM
                java.runtime.version=21.0.1+12-LTS
                """;
        String csrfToken = given()
                .when().get("/")
                .then().statusCode(200)
                .extract().cookie("csrf-token");

        given()
                .cookie("csrf-token", csrfToken)
                .formParam("csrf-token", csrfToken)
                .multiPart("file", "java.version.properties", properties.getBytes(), "text/plain")
                .when()
                .post("/upload")
                .then()
                .statusCode(200)
                .contentType(ContentType.HTML)
                .body(containsString("Java Version Analysis Results"))
                .body(containsString("21.0.1"))
                .body(containsString("Eclipse Adoptium"));
    }

    private byte[] createTestZipFile() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Add a test properties file to the zip
            ZipEntry entry = new ZipEntry("test/java.version.properties");
            zos.putNextEntry(entry);

            String properties = """
                    java.version=21.0.1
                    java.vendor=Eclipse Adoptium
                    java.vm.name=OpenJDK 64-Bit Server VM
                    java.runtime.version=21.0.1+12-LTS
                    """;
            zos.write(properties.getBytes());
            zos.closeEntry();

            // Add another properties file
            entry = new ZipEntry("test2/java.version.properties");
            zos.putNextEntry(entry);

            properties = """
                    java.version=17.0.8
                    java.vendor=Eclipse Adoptium
                    java.vm.name=OpenJDK 64-Bit Server VM
                    java.runtime.version=17.0.8+7-LTS
                    """;
            zos.write(properties.getBytes());
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    @Test
    public void testZipBombTooLarge() throws IOException {
        byte[] zipData = createLargeZipFile();
        String csrfToken = given()
                .when().get("/")
                .then().statusCode(200)
                .extract().cookie("csrf-token");

        given()
                .cookie("csrf-token", csrfToken)
                .formParam("csrf-token", csrfToken)
                .multiPart("file", "bomb.zip", zipData, "application/zip")
                .when()
                .post("/upload")
                .then()
                .statusCode(400);
    }

    private byte[] createLargeZipFile() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("large.properties");
            zos.putNextEntry(entry);
            // 100MB + 1 byte
            byte[] largeData = new byte[100 * 1024 * 1024 + 1];
            zos.write(largeData);
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    /**
     * TS-010: GET /api/versions returns JSON array
     */
    @Test
    public void testGetVersionsEndpoint() {
        given()
                .when().get("/api/versions")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", instanceOf(java.util.List.class))
                .body("size()", greaterThan(500));
    }

    /**
     * TS-011: GET /api/versions preserves file order
     */
    @Test
    public void testVersionsPreservesOrder() {
        String[] versions = given()
                .when().get("/api/versions")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract().as(String[].class);

        // Verify we have versions and they're in expected order
        // The file starts with 1.0, 1.0.1, 1.0.2, etc.
        assertTrue(versions.length > 500, "Should have 500+ versions");
        assertEquals("1.0", versions[0], "First version should be 1.0");
        assertEquals("1.0.1", versions[1], "Second version should be 1.0.1");
        assertEquals("1.0.2", versions[2], "Third version should be 1.0.2");
    }

    /**
     * TS-012: GET /api/versions returns cache headers
     */
    @Test
    public void testVersionsCacheHeaders() {
        given()
                .when().get("/api/versions")
                .then()
                .statusCode(200)
                .header("Cache-Control", "public, max-age=3600");
    }

    /**
     * TS-013: GET /api/versions responds within 100ms
     */
    @Test
    public void testVersionsPerformance() {
        long start = System.currentTimeMillis();

        given()
                .when().get("/api/versions")
                .then()
                .statusCode(200);

        long duration = System.currentTimeMillis() - start;
        assertTrue(duration < 100, "Response should be under 100ms, was: " + duration + "ms");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(String expected, String actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " - expected: " + expected + ", actual: " + actual);
        }
    }
}
