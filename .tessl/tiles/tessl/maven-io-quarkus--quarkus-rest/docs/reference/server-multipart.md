# Server-Side Multipart Processing

Quarkus REST provides comprehensive server-side APIs for processing multipart form data, including classes for reading multipart requests and generating multipart responses.

## Capabilities

### Multipart Form Data Input

Interface for reading multipart request data on the server side.

```java { .api }
package org.jboss.resteasy.reactive.server.multipart;

/**
 * Interface for accessing multipart form data in server endpoints.
 * Provides access to all parts of a multipart/form-data request.
 */
interface MultipartFormDataInput {
    /**
     * Get all form values as a map.
     * Key is the form field name, value is a collection of FormValue objects.
     *
     * @return Map of field names to their values
     */
    Map<String, Collection<FormValue>> getValues();
}
```

**Usage Example:**

```java
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataInput;
import org.jboss.resteasy.reactive.server.multipart.FormValue;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Collection;

@Path("/upload")
public class UploadResource {

    @POST
    @Path("/form")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response processMultipartForm(MultipartFormDataInput input) {
        Map<String, Collection<FormValue>> values = input.getValues();

        // Access text field
        Collection<FormValue> descriptions = values.get("description");
        if (descriptions != null && !descriptions.isEmpty()) {
            String description = descriptions.iterator().next().getValue();
            System.out.println("Description: " + description);
        }

        // Access file field
        Collection<FormValue> files = values.get("file");
        if (files != null && !files.isEmpty()) {
            FormValue fileValue = files.iterator().next();
            if (fileValue.isFileItem()) {
                FileItem fileItem = fileValue.getFileItem();
                processFile(fileItem);
            }
        }

        return Response.ok("Processed").build();
    }

    private void processFile(FileItem fileItem) {
        // Process file
    }
}
```

### Form Value

Interface representing a single part of a multipart request.

```java { .api }
package org.jboss.resteasy.reactive.server.multipart;

/**
 * Represents one part of a multipart form data request.
 * Can be either a simple text value or a file upload.
 */
interface FormValue {
    /**
     * Get the value as a string.
     * For text fields, returns the field value.
     * For file fields, returns the filename or content.
     *
     * @return String value
     */
    String getValue();

    /**
     * Get the character encoding of this part.
     *
     * @return Charset string (e.g., "UTF-8")
     */
    String getCharset();

    /**
     * Get the FileItem if this is a file upload.
     *
     * @return FileItem or null if not a file
     */
    FileItem getFileItem();

    /**
     * Check if this form value represents a file upload.
     *
     * @return true if this is a file item
     */
    boolean isFileItem();

    /**
     * Get the original filename from the upload.
     *
     * @return Filename or null if not a file
     */
    String getFileName();

    /**
     * Get HTTP headers for this part.
     *
     * @return MultivaluedMap of headers
     */
    MultivaluedMap<String, String> getHeaders();
}
```

**Usage Example:**

```java
import org.jboss.resteasy.reactive.server.multipart.FormValue;
import org.jboss.resteasy.reactive.server.multipart.FileItem;
import jakarta.ws.rs.core.MultivaluedMap;

public void processFormValue(FormValue formValue) {
    // Check if it's a file
    if (formValue.isFileItem()) {
        FileItem fileItem = formValue.getFileItem();
        String filename = formValue.getFileName();
        System.out.println("Processing file: " + filename);

        // Get headers
        MultivaluedMap<String, String> headers = formValue.getHeaders();
        String contentType = headers.getFirst("Content-Type");
        System.out.println("Content-Type: " + contentType);

        // Process file
        processFileItem(fileItem);
    } else {
        // It's a text field
        String value = formValue.getValue();
        String charset = formValue.getCharset();
        System.out.println("Text value: " + value + " (charset: " + charset + ")");
    }
}

private void processFileItem(FileItem fileItem) {
    // Process the file
}
```

### File Item

Interface for accessing uploaded file data on the server.

```java { .api }
package org.jboss.resteasy.reactive.server.multipart;

/**
 * Represents an uploaded file in a multipart request.
 * Provides access to file content, metadata, and persistence operations.
 */
interface FileItem {
    /**
     * Check if the file is stored in memory.
     *
     * @return true if in memory, false if on disk
     */
    boolean isInMemory();

    /**
     * Get the path to the temporary file.
     * File may be in memory or on disk depending on size.
     *
     * @return Path to the file
     */
    Path getFile();

    /**
     * Get the size of the uploaded file in bytes.
     *
     * @return File size
     */
    long getFileSize();

    /**
     * Get an InputStream to read the file content.
     *
     * @return InputStream for reading file data
     * @throws IOException if file cannot be read
     */
    InputStream getInputStream() throws IOException;

    /**
     * Delete the temporary file.
     * Should be called when file processing is complete.
     */
    void delete();

    /**
     * Write the uploaded file to a target path.
     *
     * @param target Destination path
     * @throws IOException if write fails
     */
    void write(Path target) throws IOException;
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.server.multipart.FileItem;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileProcessor {

    public void processFileItem(FileItem fileItem) throws IOException {
        // Check file size
        long size = fileItem.getFileSize();
        System.out.println("File size: " + size + " bytes");

        // Check storage location
        if (fileItem.isInMemory()) {
            System.out.println("File is in memory");
        } else {
            System.out.println("File is on disk at: " + fileItem.getFile());
        }

        // Read file content
        try (InputStream input = fileItem.getInputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                // Process bytes
                processBytes(buffer, bytesRead);
            }
        }

        // Or save to permanent location
        Path targetPath = Paths.get("/uploads", "uploaded-file.dat");
        fileItem.write(targetPath);

        // Clean up temporary file
        fileItem.delete();
    }

    public void validateAndSave(FileItem fileItem) throws IOException {
        // Validate file size
        if (fileItem.getFileSize() > 10_000_000) {  // 10MB limit
            throw new IllegalArgumentException("File too large");
        }

        // Validate content type (if available through FormValue)
        // Save to permanent storage
        Path uploadDir = Paths.get("/var/uploads");
        Path targetFile = uploadDir.resolve(generateUniqueFilename());
        fileItem.write(targetFile);

        // Delete temp file
        fileItem.delete();

        System.out.println("File saved to: " + targetFile);
    }

    private void processBytes(byte[] buffer, int length) {
        // Process file bytes
    }

    private String generateUniqueFilename() {
        return System.currentTimeMillis() + "-" + java.util.UUID.randomUUID() + ".dat";
    }
}
```

### Multipart Form Data Output

Class for generating multipart responses from the server.

```java { .api }
package org.jboss.resteasy.reactive.server.multipart;

/**
 * Builder class for creating multipart/form-data responses.
 * Used to return multiple parts (files, JSON, text) in a single response.
 */
class MultipartFormDataOutput {
    /**
     * Get all form data parts as a map.
     *
     * @return Map of field names to lists of PartItem objects
     */
    Map<String, List<PartItem>> getAllFormData();

    /**
     * Get form data as a simple map (deprecated).
     *
     * @return Map of field names to single PartItem
     * @deprecated Use getAllFormData() instead
     */
    @Deprecated
    Map<String, PartItem> getFormData();

    /**
     * Add a form data part with entity and media type.
     *
     * @param key Field name
     * @param entity Object to serialize
     * @param mediaType Content type for this part
     * @return Created PartItem
     */
    PartItem addFormData(String key, Object entity, MediaType mediaType);

    /**
     * Add a form data part with generic type and media type.
     *
     * @param key Field name
     * @param entity Object to serialize
     * @param genericType Generic type string for serialization
     * @param mediaType Content type for this part
     * @return Created PartItem
     */
    PartItem addFormData(String key, Object entity, String genericType, MediaType mediaType);

    /**
     * Add a form data part with filename.
     *
     * @param key Field name
     * @param entity Object to serialize (typically File or byte[])
     * @param mediaType Content type for this part
     * @param filename Filename for this part
     * @return Created PartItem
     */
    PartItem addFormData(String key, Object entity, MediaType mediaType, String filename);
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataOutput;
import org.jboss.resteasy.reactive.server.multipart.PartItem;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.io.File;

@Path("/download")
public class DownloadResource {

    // Simple multipart response
    @GET
    @Path("/package")
    @Produces(MediaType.MULTIPART_FORM_DATA)
    public MultipartFormDataOutput downloadPackage() {
        MultipartFormDataOutput output = new MultipartFormDataOutput();

        // Add JSON metadata
        PackageMetadata metadata = new PackageMetadata();
        metadata.version = "1.0.0";
        metadata.description = "Package description";
        output.addFormData("metadata", metadata, MediaType.APPLICATION_JSON_TYPE);

        // Add text part
        output.addFormData("readme", "This is the README content",
            MediaType.TEXT_PLAIN_TYPE);

        // Add file
        File dataFile = new File("/path/to/data.bin");
        output.addFormData("dataFile", dataFile,
            MediaType.APPLICATION_OCTET_STREAM_TYPE, "data.bin");

        return output;
    }

    // Multiple files in response
    @GET
    @Path("/batch")
    @Produces(MediaType.MULTIPART_FORM_DATA)
    public MultipartFormDataOutput downloadBatch() {
        MultipartFormDataOutput output = new MultipartFormDataOutput();

        // Add multiple files
        File file1 = new File("/path/to/file1.pdf");
        File file2 = new File("/path/to/file2.pdf");
        File file3 = new File("/path/to/file3.pdf");

        output.addFormData("file1", file1, MediaType.valueOf("application/pdf"), "document1.pdf");
        output.addFormData("file2", file2, MediaType.valueOf("application/pdf"), "document2.pdf");
        output.addFormData("file3", file3, MediaType.valueOf("application/pdf"), "document3.pdf");

        // Add manifest
        BatchManifest manifest = new BatchManifest();
        manifest.fileCount = 3;
        manifest.timestamp = System.currentTimeMillis();
        output.addFormData("manifest", manifest, MediaType.APPLICATION_JSON_TYPE);

        return output;
    }

    // Mixed content types
    @GET
    @Path("/report")
    @Produces(MediaType.MULTIPART_FORM_DATA)
    public MultipartFormDataOutput generateReport() {
        MultipartFormDataOutput output = new MultipartFormDataOutput();

        // JSON report data
        ReportData reportData = reportService.generateData();
        output.addFormData("data", reportData, MediaType.APPLICATION_JSON_TYPE);

        // XML summary
        String xmlSummary = reportService.generateXmlSummary();
        output.addFormData("summary", xmlSummary,
            MediaType.APPLICATION_XML_TYPE);

        // CSV export
        byte[] csvData = reportService.generateCsv();
        output.addFormData("export", csvData,
            MediaType.valueOf("text/csv"), "report.csv");

        // PDF render
        File pdfFile = reportService.generatePdf();
        output.addFormData("pdf", pdfFile,
            MediaType.valueOf("application/pdf"), "report.pdf");

        return output;
    }

    @Inject
    ReportService reportService;
}

class PackageMetadata {
    public String version;
    public String description;
}

class BatchManifest {
    public int fileCount;
    public long timestamp;
}

class ReportData {
    public String title;
    public int recordCount;
}

interface ReportService {
    ReportData generateData();
    String generateXmlSummary();
    byte[] generateCsv();
    File generatePdf();
}
```

### Part Item

Class representing one part in a multipart response.

```java { .api }
package org.jboss.resteasy.reactive.server.multipart;

/**
 * Represents one part of a multipart response.
 * Contains the entity, headers, media type, and optional filename.
 */
class PartItem {
    /**
     * Get HTTP headers for this part.
     *
     * @return MultivaluedMap of headers
     */
    MultivaluedMap<String, Object> getHeaders();

    /**
     * Get the entity object for this part.
     *
     * @return The entity object to be serialized
     */
    Object getEntity();

    /**
     * Get the generic type string for this part.
     *
     * @return Generic type or null
     */
    String getGenericType();

    /**
     * Get the media type for this part.
     *
     * @return MediaType for content negotiation
     */
    MediaType getMediaType();

    /**
     * Get the filename for this part.
     *
     * @return Filename or null
     */
    String getFilename();
}
```

**Usage Example:**

```java
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataOutput;
import org.jboss.resteasy.reactive.server.multipart.PartItem;
import jakarta.ws.rs.core.MultivaluedMap;

public void inspectMultipartOutput(MultipartFormDataOutput output) {
    Map<String, List<PartItem>> allParts = output.getAllFormData();

    for (Map.Entry<String, List<PartItem>> entry : allParts.entrySet()) {
        String fieldName = entry.getKey();
        List<PartItem> parts = entry.getValue();

        System.out.println("Field: " + fieldName);

        for (PartItem part : parts) {
            // Get metadata
            MediaType mediaType = part.getMediaType();
            String filename = part.getFilename();
            Object entity = part.getEntity();

            System.out.println("  Media Type: " + mediaType);
            System.out.println("  Filename: " + filename);
            System.out.println("  Entity Type: " + entity.getClass().getName());

            // Get headers
            MultivaluedMap<String, Object> headers = part.getHeaders();
            for (String headerName : headers.keySet()) {
                System.out.println("  Header " + headerName + ": " + headers.getFirst(headerName));
            }
        }
    }
}
```

## Complete Multipart Processing Example

```java
import org.jboss.resteasy.reactive.server.multipart.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;

@Path("/documents")
public class DocumentResource {

    // Process multipart upload
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadDocument(MultipartFormDataInput input) throws IOException {
        Map<String, Collection<FormValue>> values = input.getValues();

        // Extract metadata
        String title = null;
        String author = null;
        Collection<FormValue> titleValues = values.get("title");
        if (titleValues != null && !titleValues.isEmpty()) {
            title = titleValues.iterator().next().getValue();
        }

        Collection<FormValue> authorValues = values.get("author");
        if (authorValues != null && !authorValues.isEmpty()) {
            author = authorValues.iterator().next().getValue();
        }

        // Process uploaded file
        Collection<FormValue> fileValues = values.get("document");
        if (fileValues == null || fileValues.isEmpty()) {
            return Response.status(400).entity("No file uploaded").build();
        }

        FormValue fileValue = fileValues.iterator().next();
        if (!fileValue.isFileItem()) {
            return Response.status(400).entity("Invalid file").build();
        }

        FileItem fileItem = fileValue.getFileItem();
        String filename = fileValue.getFileName();

        // Save file
        Path uploadPath = Paths.get("/uploads", filename);
        fileItem.write(uploadPath);
        fileItem.delete();

        // Store metadata
        documentService.store(title, author, uploadPath.toString());

        return Response.ok("Document uploaded: " + filename).build();
    }

    // Generate multipart download
    @GET
    @Path("/{id}/download")
    @Produces(MediaType.MULTIPART_FORM_DATA)
    public MultipartFormDataOutput downloadDocument(@PathParam("id") Long id) {
        MultipartFormDataOutput output = new MultipartFormDataOutput();

        // Get document
        Document doc = documentService.findById(id);

        // Add metadata as JSON
        DocumentMetadata metadata = new DocumentMetadata();
        metadata.title = doc.title;
        metadata.author = doc.author;
        metadata.created = doc.createdDate;
        output.addFormData("metadata", metadata, MediaType.APPLICATION_JSON_TYPE);

        // Add document file
        File docFile = new File(doc.filePath);
        output.addFormData("document", docFile,
            MediaType.APPLICATION_OCTET_STREAM_TYPE, doc.filename);

        // Add thumbnail if available
        if (doc.thumbnailPath != null) {
            File thumbnail = new File(doc.thumbnailPath);
            output.addFormData("thumbnail", thumbnail,
                MediaType.valueOf("image/png"), "thumbnail.png");
        }

        return output;
    }

    @Inject
    DocumentService documentService;
}

class Document {
    public Long id;
    public String title;
    public String author;
    public String filename;
    public String filePath;
    public String thumbnailPath;
    public java.time.LocalDateTime createdDate;
}

class DocumentMetadata {
    public String title;
    public String author;
    public java.time.LocalDateTime created;
}

interface DocumentService {
    void store(String title, String author, String filePath);
    Document findById(Long id);
}
```

## Configuration

Multipart processing can be configured via application properties:

```properties
# Default charset for multipart input parts
quarkus.rest.multipart.input-part.default-charset=UTF-8

# Maximum file size for uploads
quarkus.http.body.uploads-directory=/tmp/uploads
```

## Integration with File Uploads

The server-side multipart APIs work seamlessly with the file upload handlers documented in [File Upload Handling](./file-uploads.md). Use `MultipartFormDataInput` and `FileItem` for low-level control, or use the simplified `File` and `Path` parameter injection for simpler use cases.
