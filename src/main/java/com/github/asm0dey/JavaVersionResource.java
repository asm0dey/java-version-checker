package com.github.asm0dey;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Path("/")
public class JavaVersionResource {

    @Inject
    VersionListService versionListService;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index();

        public static native TemplateInstance results(Collection<JavaVersionInfo> versions, Integer totalFiles, Integer distinctCount, Integer outdatedCount, Integer paidCount);

        public static native TemplateInstance analysisPage();
    }


    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        return Templates.index();
    }

    @GET
    @Path("/api/versions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVersions() {
        List<String> versions = versionListService.getVersions();
        return Response.ok(versions)
                .header("Cache-Control", "public, max-age=3600")
                .build();
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance upload(@FormParam("file") FileUpload fileUpload) throws IOException {
        List<JavaVersionInfo> allVersions;
        String fileName = fileUpload.fileName();

        try {
            if (fileName != null && fileName.toLowerCase().endsWith(".properties")) {
                allVersions = new ArrayList<>();
                try (var is = java.nio.file.Files.newInputStream(fileUpload.uploadedFile())) {
                    JavaVersionInfo info = JavaVersionService.parsePropertiesFile(is, fileName);
                    if (info != null) {
                        allVersions.add(info);
                    }
                }
            } else {
                try (ZipFile zipFile = new ZipFile(fileUpload.uploadedFile().toFile())) {
                    allVersions = allVersions(zipFile);
                }
            }
        } catch (ZipBombException e) {
            throw new BadRequestException("Zip bomb detected: " + e.getMessage());
        }

        List<JavaVersionInfo> distinctVersions = JavaVersionService.getDistinctVersions(allVersions);

        // Validate that we have at least one valid version (FR-015)
        if (distinctVersions.isEmpty()) {
            throw new BadRequestException("No valid Java version information found. Please upload a file containing at least one valid Java runtime properties file.");
        }

        // Sort by recommendation severity
        List<JavaVersionInfo> sortedVersions = JavaVersionService.sortByRecommendationSeverity(distinctVersions);

        // Calculate counts for summary
        int outdatedCount = (int) distinctVersions.stream().filter(JavaVersionInfo::isOlderThanJdk8).count();
        int paidCount = (int) distinctVersions.stream().filter(JavaVersionInfo::requiresCommercialLicense).count();

        return Templates.results(sortedVersions, allVersions.size(), distinctVersions.size(), outdatedCount, paidCount);
    }

    @GET
    @Path("/analysis")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance analysis() {
        // Return empty template - data will be loaded from localStorage by JavaScript
        return Templates.analysisPage();
    }

    @POST
    @Path("/analysis/submit")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response submitAnalysisForm(@Valid @BeanParam AnalysisFormData formData) {
        // Note: FR-016 - Backend receives form data but does not persist it at this stage
        // The form data is simply validated and acknowledged
        return Response.ok("{\"success\": true}").build();
    }

    @POST
    @Path("/analyze/quick")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response analyzeQuickSelection(List<String> versions) {
        if (versions == null || versions.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"No versions provided\"}")
                    .build();
        }

        // Parse versions as Oracle versions (FR-003)
        List<JavaVersionInfo> analysisResults = parseOracleVersions(versions);

        // Validate that we have at least one valid version (FR-015)
        if (analysisResults.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"No valid Java version information found\"}")
                    .build();
        }

        // Sort by recommendation severity and return as JSON
        List<JavaVersionInfo> sortedVersions = JavaVersionService.sortByRecommendationSeverity(analysisResults);

        return Response.ok(sortedVersions).build();
    }

    @POST
    @Path("/analyze/list")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response analyzeVersionList(@FormParam("versions") String versionsText) {
        if (versionsText == null || versionsText.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"No versions provided\"}")
                    .build();
        }

        // Split by newlines and parse as Oracle versions (FR-003)
        List<String> versions = versionsText.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();

        List<JavaVersionInfo> analysisResults = parseOracleVersions(versions);

        // Validate that we have at least one valid version (FR-015)
        if (analysisResults.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"No valid Java version information found\"}")
                    .build();
        }

        // Sort by recommendation severity and return as JSON
        List<JavaVersionInfo> sortedVersions = JavaVersionService.sortByRecommendationSeverity(analysisResults);

        return Response.ok(sortedVersions).build();
    }

    /**
     * Parse version strings as Oracle JDK versions (FR-003)
     * Creates synthetic properties with Oracle vendor
     */
    private List<JavaVersionInfo> parseOracleVersions(List<String> versions) {
        List<JavaVersionInfo> results = new ArrayList<>();

        for (String version : versions) {
            try {
                // Create synthetic properties for Oracle JDK
                var props = new java.util.Properties();
                props.setProperty("java.version", version);
                props.setProperty("java.vendor", "Oracle Corporation");
                props.setProperty("java.vm.vendor", "Oracle Corporation");

                // Use OracleLicenseChecker to determine license requirements
                OracleLicenseChecker.LicenseResult licenseResult = OracleLicenseChecker.requiresCommercialLicense(props);

                // Determine version age
                JavaVersionInfo.VersionAge versionAge = determineVersionAge(version);

                // Check if older than JDK 8
                boolean isOlderThanJdk8 = isVersionOlderThanJdk8(version);

                results.add(new JavaVersionInfo(
                        version,
                        version, // runtime version same as version
                        version, // vm version same as version
                        "Oracle Corporation",
                        "Oracle Corporation",
                        "manual-entry",
                        isOlderThanJdk8,
                        licenseResult.requiresLicense(),
                        licenseResult.explanation(),
                        versionAge
                ));
            } catch (Exception e) {
                // Skip invalid versions
                System.err.println("Failed to parse version: " + version + " - " + e.getMessage());
            }
        }

        return JavaVersionService.getDistinctVersions(results);
    }

    /**
     * Determines if the Java version is older than JDK 8
     */
    private static boolean isVersionOlderThanJdk8(String javaVersion) {
        if (javaVersion == null || javaVersion.trim().isEmpty()) return false;

        try {
            String version = javaVersion.trim();

            if (version.startsWith("1.")) {
                String[] parts = version.split("\\.");
                if (parts.length >= 2) {
                    int majorVersion = Integer.parseInt(parts[1]);
                    return majorVersion < 8;
                }
            }

            if (version.matches("^\\d+$")) {
                int majorVersion = Integer.parseInt(version);
                return majorVersion < 8;
            }

            if (version.matches("^\\d+\\..*")) {
                String[] parts = version.split("\\.");
                int majorVersion = Integer.parseInt(parts[0]);
                return majorVersion < 8;
            }
        } catch (NumberFormatException e) {
            return false;
        }

        return false;
    }

    /**
     * Determines the version age for traffic lights scheme
     */
    private static JavaVersionInfo.VersionAge determineVersionAge(String javaVersion) {
        if (javaVersion == null || javaVersion.trim().isEmpty()) return JavaVersionInfo.VersionAge.VERY_OLD;

        try {
            String version = javaVersion.trim();
            int majorVersion;

            if (version.startsWith("1.")) {
                String[] parts = version.split("\\.");
                if (parts.length >= 2) majorVersion = Integer.parseInt(parts[1]);
                else return JavaVersionInfo.VersionAge.VERY_OLD;
            } else if (version.matches("^\\d+\\..*") || version.matches("^\\d+$")) {
                String[] parts = version.split("\\.");
                majorVersion = Integer.parseInt(parts[0]);
            } else return JavaVersionInfo.VersionAge.VERY_OLD;

            if (majorVersion < 11) return JavaVersionInfo.VersionAge.VERY_OLD;
            else if (majorVersion <= 20) return JavaVersionInfo.VersionAge.OLD;
            else return JavaVersionInfo.VersionAge.OK;

        } catch (NumberFormatException e) {
            return JavaVersionInfo.VersionAge.VERY_OLD;
        }
    }


    private List<JavaVersionInfo> allVersions(ZipFile zipFile) throws IOException {
        List<JavaVersionInfo> versions = new ArrayList<>();
        for (FileHeader header : zipFile.getFileHeaders()) {
            if (!header.isDirectory() && header.getFileName().endsWith(".properties")) {
                validateEntry(header);
                try (var zis = zipFile.getInputStream(header)) {
                    var versionInfo = JavaVersionService.parsePropertiesFile(zis, header.getFileName());
                    if (versionInfo != null) versions.add(versionInfo);
                }
            }
        }
        return versions;
    }

    private void validateEntry(FileHeader entry) {
        long uncompressedSize = entry.getUncompressedSize();
        long compressedSize = entry.getCompressedSize();
        if (uncompressedSize > 100 * 1024 * 1024) { // 100MB max
            throw new ZipBombException("Entry too large: " + entry.getFileName());
        }
        if (compressedSize > 0) {
            double ratio = (double) uncompressedSize / compressedSize;
            if (ratio > 100) { // Max ratio 100
                throw new ZipBombException("Compression ratio too high: " + ratio + " for " + entry.getFileName());
            }
        }
    }

    private static class ZipBombException extends RuntimeException {
        public ZipBombException(String message) {
            super(message);
        }
    }
}
