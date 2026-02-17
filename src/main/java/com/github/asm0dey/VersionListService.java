package com.github.asm0dey;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service that loads and serves the Java version list from a centralized data source.
 * Thread-safe, immutable after initialization.
 */
@ApplicationScoped
public class VersionListService {
    private static final Logger LOG = Logger.getLogger(VersionListService.class);
    private static final String VERSION_FILE = "java_versions.txt";

    private List<String> versions;

    /**
     * Loads versions from java_versions.txt at application startup.
     * Fails fast if file is missing or empty.
     */
    @PostConstruct
    public void loadVersions() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(VERSION_FILE)) {
            if (is == null) {
                throw new RuntimeException(
                    "Failed to load version list: " + VERSION_FILE + " not found in classpath. " +
                    "This file is required for application operation."
                );
            }

            List<String> loadedVersions = parseVersionFile(is);

            if (loadedVersions.isEmpty()) {
                throw new RuntimeException(
                    "Failed to load version list: " + VERSION_FILE + " is empty or contains no valid versions. " +
                    "At least one version is required for application operation."
                );
            }

            this.versions = Collections.unmodifiableList(loadedVersions);
            LOG.infof("Successfully loaded %d Java versions from %s", versions.size(), VERSION_FILE);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read version file: " + VERSION_FILE, e);
        }
    }

    /**
     * Parses version file, skipping empty lines and trimming whitespace.
     * Logs warnings for malformed lines but continues loading.
     * Package-private for testing.
     */
    List<String> parseVersionFile(InputStream is) throws IOException {
        List<String> result = new ArrayList<>();
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();

                // Skip empty lines
                if (trimmed.isEmpty()) {
                    continue;
                }

                // Basic validation: warn if line looks malformed
                if (trimmed.contains(" ") || trimmed.contains("\t")) {
                    LOG.warnf("Line %d in %s contains whitespace (will be used as-is): '%s'",
                             lineNumber, VERSION_FILE, trimmed);
                }

                result.add(trimmed);
            }
        }

        return result;
    }

    /**
     * Returns immutable list of Java versions.
     * Thread-safe for concurrent access.
     */
    public List<String> getVersions() {
        return versions;
    }
}
