package com.github.asm0dey;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
public class ZipBombTest {

    @Test
    public void testZipBombProtection() throws IOException {
        // Create a ZIP with a very high compression ratio
        // 1MB of zeros compresses very well
        byte[] zipData = createZipBomb();

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
                .statusCode(400); // Expecting a Bad Request due to ZIP bomb
    }

    private byte[] createZipBomb() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("huge.properties");
            zos.putNextEntry(entry);
            // 1MB of data
            byte[] data = new byte[1024 * 1024];
            zos.write(data);
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}
