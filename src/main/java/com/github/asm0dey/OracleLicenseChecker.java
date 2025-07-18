package com.github.asm0dey;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OracleLicenseChecker {

    public record LicenseResult(boolean requiresLicense, String explanation) {
    }


    public static LicenseResult requiresCommercialLicense(Properties props) {
        String vendor = props.getProperty("java.vendor", "").toLowerCase();
        String javaVersion = props.getProperty("java.version", "");
        String vendorVersion = props.getProperty("java.vendor.version", "").toLowerCase();
        String buildDate = props.getProperty("java.version.date", "");

        // 1. Vendor check - only Oracle/Sun
        if (!vendor.contains("oracle") && !vendor.contains("sun"))
            return new LicenseResult(false, "Not an Oracle/Sun distribution. Vendor: " + vendor);

        // 2. Check for Oracle OpenJDK (always free but unsupported)
        if (vendorVersion.contains("openjdk"))
            return new LicenseResult(false, "Oracle OpenJDK is free (GPLv2+CPE) but lacks long-term support");

        int majorVersion = parseMajorVersion(javaVersion);

        // 3. Dispatch to version-specific handlers
        if (majorVersion < 8) return new LicenseResult(false, "Java " + majorVersion + " is end-of-life and free");
        else if (majorVersion == 8) return handleJava8(javaVersion, buildDate);
        else if (majorVersion == 9 || majorVersion == 10)
            return new LicenseResult(true, "Java " + majorVersion + " requires commercial license (non-LTS, end-of-life)");
        else if (majorVersion == 11) return new LicenseResult(true, "Java 11 always requires commercial license");
        else if (majorVersion <= 16)
            return new LicenseResult(true, "Java " + majorVersion + " requires commercial license (non-LTS, end-of-life)");
        else if (majorVersion == 17) return handleJava17(javaVersion, buildDate);
        else if (majorVersion <= 20)
            return new LicenseResult(false, "Java " + majorVersion + " is free under NFTC (non-LTS)");
        else return handleJava21Plus(buildDate);

    }

    private static int parseMajorVersion(String javaVersion) {
        if (javaVersion.startsWith("1.")) {
            String[] parts = javaVersion.split("\\.");
            if (parts.length >= 2) try {
                return Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
            }
            return 0;
        }

        try {
            String[] parts = javaVersion.split("\\.");
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return 0;
        }
    }

    private static LicenseResult handleJava8(String version, String buildDate) {
        Integer update = extractJava8Update(version);
        if (update != null) return update >= 211 ?
                new LicenseResult(true, "Java 8 Update " + update + " requires license (released ≥ April 2019)") :
                new LicenseResult(false, "Java 8 Update " + update + " is free (< April 2019)");

        if (!buildDate.isEmpty()) try {
            LocalDate date = LocalDate.parse(buildDate);
            LocalDate cutoff = LocalDate.of(2019, 4, 16);
            return date.isBefore(cutoff) ?
                    new LicenseResult(false, "Java 8 build " + buildDate + " is free (< 2019-04-16)") :
                    new LicenseResult(true, "Java 8 build " + buildDate + " requires license (≥ 2019-04-16)");
        } catch (DateTimeParseException ignored) {
        }

        return new LicenseResult(false, "Java 8 free (update/date unavailable)");
    }

    private static Integer extractJava8Update(String version) {
        Pattern pattern = Pattern.compile("^1\\.8\\.0_(\\d+).*");
        Matcher matcher = pattern.matcher(version);
        if (matcher.matches()) try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private static LicenseResult handleJava17(String version, String buildDate) {
        String[] parts = version.split("\\.");
        if (parts.length >= 3)
            try {
                int minor = Integer.parseInt(parts[2]);
                return minor >= 13 ?
                        new LicenseResult(true, "Java 17.0." + minor + " requires license (≥ October 2024)") :
                        new LicenseResult(false, "Java 17.0." + minor + " is free (≤ 17.0.12)");
            } catch (NumberFormatException ignored) {
            }

        if (!buildDate.isEmpty()) try {
            LocalDate date = LocalDate.parse(buildDate);
            LocalDate cutoff = LocalDate.of(2024, 10, 1);
            return date.isBefore(cutoff) ?
                    new LicenseResult(false, "Java 17 build " + buildDate + " is free (< 2024-10-01)") :
                    new LicenseResult(true, "Java 17 build " + buildDate + " requires license (≥ 2024-10-01)");
        } catch (DateTimeParseException ignored) {
        }

        return new LicenseResult(false, "Java 17 free (update/date unavailable)");
    }

    private static LicenseResult handleJava21Plus(String buildDate) {
        final LocalDate nftcCutoff = LocalDate.of(2026, 10, 1);

        if (!buildDate.isEmpty()) try {
            LocalDate date = LocalDate.parse(buildDate);
            return date.isBefore(nftcCutoff) ?
                    new LicenseResult(false, "Java 21+ build " + buildDate + " is free under NFTC (< 2026-10-01)") :
                    new LicenseResult(true, "Java 21+ build " + buildDate + " requires license (≥ 2026-10-01)");
        } catch (DateTimeParseException ignored) {
        }

        return new LicenseResult(false, "Java 21+ is free under NFTC until 2026-10-01 (build date unavailable)");
    }
}