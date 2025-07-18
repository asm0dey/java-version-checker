package com.github.asm0dey;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class JavaVersionServiceTest {

    private JavaVersionService javaVersionService;


    @Test
    void testParseJava6Properties() throws IOException {
        String properties = """
                java.vm.version=20.45-b01
                java.runtime.version=1.6.0_45-b06
                java.version=1.6.0_45
                java.vm.specification.version=1.0
                java.vendor=Sun Microsystems Inc.
                java.vm.vendor=Sun Microsystems Inc.
                java.vendor.version=unavailable
                """;

        JavaVersionInfo info = JavaVersionService.parsePropertiesFile(
                new ByteArrayInputStream(properties.getBytes()), 
                "java1.properties"
        );

        assertNotNull(info);
        assertEquals("1.6.0_45", info.javaVersion());
        assertEquals("Sun Microsystems Inc.", info.javaVendor());
        assertTrue(info.isOlderThanJdk8(), "Java 1.6 should be flagged as older than JDK 8");
        assertFalse(info.requiresCommercialLicense(), "Sun Microsystems Java should not require commercial license");
    }

    @Test
    void testParseJava8OracleProperties() throws IOException {
        String properties = """
                java.vm.version=25.181-b13
                java.runtime.version=1.8.0_181-b13
                java.version=1.8.0_181
                java.vm.specification.version=1.0
                java.vendor=Oracle Corporation
                java.vm.vendor=Oracle Corporation
                java.vendor.version=25.181-b13
                """;

        JavaVersionInfo info = JavaVersionService.parsePropertiesFile(
                new ByteArrayInputStream(properties.getBytes()), 
                "java2.properties"
        );

        assertNotNull(info);
        assertEquals("1.8.0_181", info.javaVersion());
        assertEquals("Oracle Corporation", info.javaVendor());
        assertFalse(info.isOlderThanJdk8(), "Java 1.8 should NOT be flagged as older than JDK 8");
        assertFalse(info.requiresCommercialLicense(), "Oracle JDK 8 should not require commercial license");
    }

    @Test
    void testParseJava17EclipseAdoptiumProperties() throws IOException {
        String properties = """
                java.vm.version=17.0.2+8-Ubuntu-120.04
                java.runtime.version=17.0.2+8-Ubuntu-120.04
                java.version=17.0.2
                java.vm.specification.version=17
                java.vendor=Eclipse Adoptium
                java.vm.vendor=Eclipse Adoptium
                java.vendor.version=Temurin-17.0.2+8
                """;

        JavaVersionInfo info = JavaVersionService.parsePropertiesFile(
                new ByteArrayInputStream(properties.getBytes()), 
                "java3.properties"
        );

        assertNotNull(info);
        assertEquals("17.0.2", info.javaVersion());
        assertEquals("Eclipse Adoptium", info.javaVendor());
        assertFalse(info.isOlderThanJdk8(), "Java 17 should NOT be flagged as older than JDK 8");
        assertFalse(info.requiresCommercialLicense(), "Eclipse Adoptium should not require commercial license");
    }

    @Test
    void testParseOracleJdk11Properties() throws IOException {
        String properties = """
                java.vm.version=11.0.1+13-LTS
                java.runtime.version=11.0.1+13-LTS
                java.version=11.0.1
                java.vm.specification.version=11
                java.vendor=Oracle Corporation
                java.vm.vendor=Oracle Corporation
                java.vendor.version=11.0.1+13-LTS
                """;

        JavaVersionInfo info = JavaVersionService.parsePropertiesFile(
                new ByteArrayInputStream(properties.getBytes()), 
                "oracle-java11.properties"
        );

        assertNotNull(info);
        assertEquals("11.0.1", info.javaVersion());
        assertEquals("Oracle Corporation", info.javaVendor());
        assertFalse(info.isOlderThanJdk8(), "Java 11 should NOT be flagged as older than JDK 8");
        assertTrue(info.requiresCommercialLicense(), "Oracle JDK 11.0.1 requires commercial license (Java 11 always requires license)");
    }

    @Test
    void testParseOpenJdk11Properties() throws IOException {
        String properties = """
                java.vm.version=11.0.1+13
                java.runtime.version=11.0.1+13
                java.version=11.0.1
                java.vm.specification.version=11
                java.vendor=Eclipse Foundation
                java.vm.vendor=Eclipse Foundation
                java.vendor.version=11.0.1+13
                """;

        JavaVersionInfo info = JavaVersionService.parsePropertiesFile(
                new ByteArrayInputStream(properties.getBytes()), 
                "openjdk-java11.properties"
        );

        assertNotNull(info);
        assertEquals("11.0.1", info.javaVersion());
        assertEquals("Eclipse Foundation", info.javaVendor());
        assertFalse(info.isOlderThanJdk8(), "Java 11 should NOT be flagged as older than JDK 8");
        assertFalse(info.requiresCommercialLicense(), "OpenJDK should not require commercial license");
    }

    @Test
    void testParseJava7Properties() throws IOException {
        String properties = """
                java.vm.version=24.79-b02
                java.runtime.version=1.7.0_79-b15
                java.version=1.7.0_79
                java.vm.specification.version=1.0
                java.vendor=Oracle Corporation
                java.vm.vendor=Oracle Corporation
                java.vendor.version=24.79-b02
                """;

        JavaVersionInfo info = JavaVersionService.parsePropertiesFile(
                new ByteArrayInputStream(properties.getBytes()), 
                "java7.properties"
        );

        assertNotNull(info);
        assertEquals("1.7.0_79", info.javaVersion());
        assertEquals("Oracle Corporation", info.javaVendor());
        assertTrue(info.isOlderThanJdk8(), "Java 1.7 should be flagged as older than JDK 8");
        assertFalse(info.requiresCommercialLicense(), "Oracle JDK 7 should not require commercial license");
    }

    @Test
    void testOracleJava8BeforeThreshold() throws IOException {
        String properties = """
                java.vm.version=25.210-b13
                java.runtime.version=1.8.0_210-b13
                java.version=1.8.0_210
                java.vendor=Oracle Corporation
                java.vm.vendor=Oracle Corporation
                """;

        JavaVersionInfo info = JavaVersionService.parsePropertiesFile(
                new ByteArrayInputStream(properties.getBytes()), 
                "java8-before.properties"
        );

        assertNotNull(info);
        assertEquals("1.8.0_210", info.javaVersion());
        assertFalse(info.requiresCommercialLicense(), "Oracle JDK 8u210 should not require commercial license (before 8u211)");
    }

    @Test
    void testOracleJava8AtThreshold() throws IOException {
        String properties = """
                java.vm.version=25.211-b13
                java.runtime.version=1.8.0_211-b13
                java.version=1.8.0_211
                java.vendor=Oracle Corporation
                java.vm.vendor=Oracle Corporation
                """;

        JavaVersionInfo info = JavaVersionService.parsePropertiesFile(
                new ByteArrayInputStream(properties.getBytes()), 
                "java8-at-threshold.properties"
        );

        assertNotNull(info);
        assertEquals("1.8.0_211", info.javaVersion());
        assertTrue(info.requiresCommercialLicense(), "Oracle JDK 8u211 should require commercial license");
    }

    @Test
    void testOracleJava7AtThreshold() throws IOException {
        String properties = """
                java.vm.version=24.85-b02
                java.runtime.version=1.7.0_85-b15
                java.version=1.7.0_85
                java.vendor=Oracle Corporation
                java.vm.vendor=Oracle Corporation
                """;

        JavaVersionInfo info = JavaVersionService.parsePropertiesFile(
                new ByteArrayInputStream(properties.getBytes()), 
                "java7-at-threshold.properties"
        );

        assertNotNull(info);
        assertEquals("1.7.0_85", info.javaVersion());
        assertFalse(info.requiresCommercialLicense(), "Oracle JDK 7u85 is free (Java < 8 is end-of-life and free)");
    }

    @Test
    void testOracleJava11AtThreshold() throws IOException {
        String properties = """
                java.vm.version=11.0.3+7-LTS
                java.runtime.version=11.0.3+7-LTS
                java.version=11.0.3
                java.vendor=Oracle Corporation
                java.vm.vendor=Oracle Corporation
                """;

        JavaVersionInfo info = JavaVersionService.parsePropertiesFile(
                new ByteArrayInputStream(properties.getBytes()), 
                "java11-at-threshold.properties"
        );

        assertNotNull(info);
        assertEquals("11.0.3", info.javaVersion());
        assertTrue(info.requiresCommercialLicense(), "Oracle JDK 11.0.3 should require commercial license");
    }

    @Test
    void testOracleJava17BeforeThreshold() throws IOException {
        String properties = """
                java.vm.version=17.0.12+7-LTS
                java.runtime.version=17.0.12+7-LTS
                java.version=17.0.12
                java.vendor=Oracle Corporation
                java.vm.vendor=Oracle Corporation
                """;

        JavaVersionInfo info = JavaVersionService.parsePropertiesFile(
                new ByteArrayInputStream(properties.getBytes()), 
                "java17-before-threshold.properties"
        );

        assertNotNull(info);
        assertEquals("17.0.12", info.javaVersion());
        assertFalse(info.requiresCommercialLicense(), "Oracle JDK 17.0.12 should not require commercial license (before 17.0.13)");
    }

    @Test
    void testOracleJava17AtThreshold() throws IOException {
        String properties = """
                java.vm.version=17.0.13+11-LTS
                java.runtime.version=17.0.13+11-LTS
                java.version=17.0.13
                java.vendor=Oracle Corporation
                java.vm.vendor=Oracle Corporation
                """;

        JavaVersionInfo info = JavaVersionService.parsePropertiesFile(
                new ByteArrayInputStream(properties.getBytes()), 
                "java17-at-threshold.properties"
        );

        assertNotNull(info);
        assertEquals("17.0.13", info.javaVersion());
        assertTrue(info.requiresCommercialLicense(), "Oracle JDK 17.0.13 should require commercial license");
    }

    @Test
    void testOracleJava9AlwaysFree() throws IOException {
        String properties = """
                java.vm.version=9.0.4+11
                java.runtime.version=9.0.4+11
                java.version=9.0.4
                java.vendor=Oracle Corporation
                java.vm.vendor=Oracle Corporation
                """;

        JavaVersionInfo info = JavaVersionService.parsePropertiesFile(
                new ByteArrayInputStream(properties.getBytes()), 
                "java9.properties"
        );

        assertNotNull(info);
        assertEquals("9.0.4", info.javaVersion());
        assertTrue(info.requiresCommercialLicense(), "Oracle JDK 9 requires commercial license (non-LTS, end-of-life)");
    }

    @Test
    void testOracleJava18AlwaysFree() throws IOException {
        String properties = """
                java.vm.version=18.0.2+9-78
                java.runtime.version=18.0.2+9-78
                java.version=18.0.2
                java.vendor=Oracle Corporation
                java.vm.vendor=Oracle Corporation
                """;

        JavaVersionInfo info = JavaVersionService.parsePropertiesFile(
                new ByteArrayInputStream(properties.getBytes()), 
                "java18.properties"
        );

        assertNotNull(info);
        assertEquals("18.0.2", info.javaVersion());
        assertFalse(info.requiresCommercialLicense(), "Oracle JDK 18+ should always be free");
    }
}
