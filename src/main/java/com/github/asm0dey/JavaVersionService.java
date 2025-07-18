package com.github.asm0dey;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class JavaVersionService {

    public static JavaVersionInfo parsePropertiesFile(InputStream inputStream, String fileName) throws IOException {
        Properties props = new Properties();
        props.load(inputStream);

        String javaVersion = props.getProperty("java.version");
        String javaRuntimeVersion = props.getProperty("java.runtime.version");
        String javaVmVersion = props.getProperty("java.vm.version");
        String javaVendor = props.getProperty("java.vendor");
        String javaVmVendor = props.getProperty("java.vm.vendor");

        // Only create JavaVersionInfo if we have at least the java.version
        if (javaVersion != null && !javaVersion.trim().isEmpty()) {
            boolean isOlderThanJdk8 = isVersionOlderThanJdk8(javaVersion);

            // Use OracleLicenseChecker instead of requiresCommercialLicense
            OracleLicenseChecker.LicenseResult licenseResult = OracleLicenseChecker.requiresCommercialLicense(props);
            boolean requiresCommercialLicense = licenseResult.requiresLicense();
            String licenseExplanation = licenseResult.explanation();

            // Determine version age for traffic lights scheme
            JavaVersionInfo.VersionAge versionAge = determineVersionAge(javaVersion);

            return new JavaVersionInfo(
                    javaVersion,
                    javaRuntimeVersion,
                    javaVmVersion,
                    javaVendor,
                    javaVmVendor,
                    fileName,
                    isOlderThanJdk8,
                    requiresCommercialLicense,
                    licenseExplanation,
                    versionAge
            );
        }

        return null;
    }

    public static List<JavaVersionInfo> getDistinctVersions(List<JavaVersionInfo> versions) {
        return versions.stream()
                .collect(Collectors.toMap(
                        v -> v.javaVersion() + "|" + v.javaRuntimeVersion() + "|" + v.javaVendor(),
                        v -> v,
                        (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .sorted(Comparator.comparing(JavaVersionInfo::javaVersion))
                .collect(Collectors.toList());
    }

    /**
     * Determines if the Java version is older than JDK 8
     */
    private static boolean isVersionOlderThanJdk8(String javaVersion) {
        if (javaVersion == null || javaVersion.trim().isEmpty()) return false;

        try {
            // Handle different version formats
            String version = javaVersion.trim();

            // For versions like "1.7.0_80", "1.6.0_45", etc.
            if (version.startsWith("1.")) {
                String[] parts = version.split("\\.");
                if (parts.length >= 2) {
                    int majorVersion = Integer.parseInt(parts[1]);
                    return majorVersion < 8;
                }
            }

            // For versions like "7", "6", etc. (less common but possible)
            if (version.matches("^\\d+$")) {
                int majorVersion = Integer.parseInt(version);
                return majorVersion < 8;
            }

            // For versions like "11.0.1", "17.0.2", etc. (newer format)
            if (version.matches("^\\d+\\..*")) {
                String[] parts = version.split("\\.");
                int majorVersion = Integer.parseInt(parts[0]);
                return majorVersion < 8;
            }

        } catch (NumberFormatException e) {
            // If we can't parse the version, assume it's not older than JDK 8
            return false;
        }

        return false;
    }

    /**
     * Determines the version age for traffic lights scheme:
     * - before 11: VERY_OLD
     * - 11-20: OLD
     * - 21+: OK
     */
    private static JavaVersionInfo.VersionAge determineVersionAge(String javaVersion) {
        if (javaVersion == null || javaVersion.trim().isEmpty()) return JavaVersionInfo.VersionAge.VERY_OLD;

        try {
            String version = javaVersion.trim();
            int majorVersion;

            // Handle different version formats
            if (version.startsWith("1.")) {
                // For versions like "1.8.0_271", "1.7.0_85", etc.
                String[] parts = version.split("\\.");
                if (parts.length >= 2) majorVersion = Integer.parseInt(parts[1]);
                else return JavaVersionInfo.VersionAge.VERY_OLD;
            } else if (version.matches("^\\d+\\..*") || version.matches("^\\d+$")) {
                // For versions like "11.0.1", "17.0.2", "21", etc.
                String[] parts = version.split("\\.");
                majorVersion = Integer.parseInt(parts[0]);
            } else return JavaVersionInfo.VersionAge.VERY_OLD;

            // Apply traffic lights scheme
            if (majorVersion < 11) return JavaVersionInfo.VersionAge.VERY_OLD;
            else if (majorVersion <= 20) return JavaVersionInfo.VersionAge.OLD;
            else return JavaVersionInfo.VersionAge.OK;

        } catch (NumberFormatException e) {
            return JavaVersionInfo.VersionAge.VERY_OLD;
        }
    }
}
