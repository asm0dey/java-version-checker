package com.github.asm0dey;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static io.restassured.RestAssured.given;
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
        // Test upload without file should return 400
        given()
                .cookie("csrf-token", csrfToken)
                .formParam("csrf-token", csrfToken)
                .when()
                .post("/upload")
                .then()
                .statusCode(415);
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

}
