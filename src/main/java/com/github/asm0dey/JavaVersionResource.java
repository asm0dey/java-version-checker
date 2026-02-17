package com.github.asm0dey;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
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

        // Calculate counts for summary
        int outdatedCount = (int) distinctVersions.stream().filter(JavaVersionInfo::isOlderThanJdk8).count();
        int paidCount = (int) distinctVersions.stream().filter(JavaVersionInfo::requiresCommercialLicense).count();

        return Templates.results(distinctVersions, allVersions.size(), distinctVersions.size(), outdatedCount, paidCount);
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
