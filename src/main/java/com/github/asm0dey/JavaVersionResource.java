package com.github.asm0dey;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Path("/")
public class JavaVersionResource {

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

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance upload(@FormParam("file") FileUpload fileUpload) throws IOException {
        ZipSecureFile.setMinInflateRatio(.01);
        ZipSecureFile.setMaxEntrySize(1024);
        try (ZipSecureFile zipSecureFile = new ZipSecureFile(fileUpload.uploadedFile().toFile());) {
            var allVersions = allVersions(zipSecureFile);
            List<JavaVersionInfo> distinctVersions = JavaVersionService.getDistinctVersions(allVersions);

            // Calculate counts for summary
            int outdatedCount = (int) distinctVersions.stream().filter(JavaVersionInfo::isOlderThanJdk8).count();
            int paidCount = (int) distinctVersions.stream().filter(JavaVersionInfo::requiresCommercialLicense).count();

            return Templates.results(distinctVersions, allVersions.size(), distinctVersions.size(), outdatedCount, paidCount);

        }
    }

    private List<JavaVersionInfo> allVersions(ZipSecureFile zipSecureFile) throws IOException {
        List<JavaVersionInfo> versions = new ArrayList<>();
        for (var entry : new ZipArchiveEntryIterable(zipSecureFile))
            if (!entry.isDirectory() && entry.getName().endsWith(".properties"))
                try (var zis = zipSecureFile.getInputStream(entry)) {
                    var versionInfo = JavaVersionService.parsePropertiesFile(zis, entry.getName());
                    if (versionInfo != null) versions.add(versionInfo);
                }
        return versions;
    }

    private record ZipArchiveEntryIterable(ZipSecureFile zipSecureFile) implements Iterable<ZipArchiveEntry> {
        @Override
        public Iterator<ZipArchiveEntry> iterator() {
            return zipSecureFile.getEntries().asIterator();
        }
    }
}
