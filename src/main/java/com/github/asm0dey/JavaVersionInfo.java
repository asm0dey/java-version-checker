package com.github.asm0dey;

public record JavaVersionInfo(
        String javaVersion,
        String javaRuntimeVersion,
        String javaVmVersion,
        String javaVendor,
        String javaVmVendor,
        String fileName,
        boolean isOlderThanJdk8,
        boolean requiresCommercialLicense,
        String licenseExplanation,
        VersionAge versionAge
) {
    public enum VersionAge {
        VERY_OLD,  // before 11
        OLD,       // 11-20
        OK         // 21+
    }
}
