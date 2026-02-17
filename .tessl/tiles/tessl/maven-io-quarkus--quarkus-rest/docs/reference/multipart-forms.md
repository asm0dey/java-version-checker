# Multipart Form Handling

Quarkus REST provides comprehensive support for multipart form data with annotations for specifying content types, filenames, and partial file uploads.

## Capabilities

### Multipart Form POJOs

Map multipart/form-data requests to POJOs with annotated fields.

```java { .api }
package org.jboss.resteasy.reactive;

/**
 * Annotation for POJOs mapping to multipart/form-data HTTP bodies.
 * Each field should be annotated with @RestForm to map to a body part.
 * Use @PartType to specify the media type of each part.
 *
 * @deprecated Not required anymore - use @BeanParam or omit entirely.
 *             POJOs with @RestForm, @RestCookie, @RestHeader, @RestPath,
 *             @RestMatrix, @RestQuery or JAX-RS equivalents work automatically.
 */
@Deprecated(forRemoval = true)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.TYPE})
@interface MultipartForm {
    String value() default "";
}
```

**Usage Example:**

```java
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.PartType;
import jakarta.ws.rs.core.MediaType;

// No longer need @MultipartForm annotation
public class FileUploadForm {

    @RestForm
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public File file;

    @RestForm
    public String description;

    @RestForm
    public String category;
}

@POST
@Path("/upload")
@Consumes(MediaType.MULTIPART_FORM_DATA)
public Response handleUpload(FileUploadForm form) {
    // Process form.file, form.description, form.category
    return Response.ok("Uploaded: " + form.file.getName()).build();
}
```

### Part Type Specification

Specify the media type for multipart body parts.

```java { .api }
package org.jboss.resteasy.reactive;

/**
 * Designates the media type for a multipart body part.
 * Used on fields of multipart POJOs or form parameters.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@interface PartType {
    String value();
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.PartType;
import jakarta.ws.rs.core.MediaType;

public class MultipartData {

    @RestForm
    @PartType(MediaType.APPLICATION_JSON)
    public UserMetadata metadata;

    @RestForm
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    public File document;

    @RestForm
    @PartType("image/png")
    public File avatar;
}

@POST
@Path("/submit")
@Consumes(MediaType.MULTIPART_FORM_DATA)
public Response submit(MultipartData data) {
    // JSON part deserialized to UserMetadata
    // Files handled as File objects
    return Response.ok().build();
}
```

### Part Filename Specification

Specify the filename for multipart parts (client-side).

```java { .api }
package org.jboss.resteasy.reactive;

/**
 * Designates the filename of a multipart part.
 * Only applicable in the client, not the server.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@interface PartFilename {
    String value();
}
```

**Client Usage Example:**

```java
import org.jboss.resteasy.reactive.PartFilename;

// REST Client interface
@Path("/upload")
public interface UploadClient {

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    Response upload(
        @RestForm @PartFilename("document.pdf") File file,
        @RestForm String description
    );
}
```

### Partial File Uploads

Send partial file content using offset and byte count.

```java { .api }
package org.jboss.resteasy.reactive;

/**
 * Wrapper for sending a partial File object.
 * Allows sending specific byte range of a file.
 */
class FilePart {
    /** The file to send */
    public final File file;

    /** The starting byte of the file */
    public final long offset;

    /** The number of bytes to send */
    public final long count;

    /**
     * Create a new partial File object.
     * @param file The file to send
     * @param offset Starting byte (must be >= 0)
     * @param count Number of bytes (must be >= 0, offset+count <= file size)
     */
    public FilePart(File file, long offset, long count);
}

/**
 * Wrapper for sending a partial Path object.
 * Allows sending specific byte range of a file.
 */
class PathPart {
    /** The file to send */
    public final Path file;

    /** The starting byte of the file */
    public final long offset;

    /** The number of bytes to send */
    public final long count;

    /**
     * Create a new partial Path object.
     * @param file The file to send
     * @param offset Starting byte (must be >= 0)
     * @param count Number of bytes (must be >= 0, offset+count <= file size)
     */
    public PathPart(Path file, long offset, long count);
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.FilePart;
import org.jboss.resteasy.reactive.PathPart;
import java.io.File;
import java.nio.file.Path;

// Send partial file (first 1MB)
File largeFile = new File("/path/to/large-file.dat");
FilePart partial = new FilePart(largeFile, 0, 1024 * 1024);

@POST
@Path("/upload-partial")
@Consumes(MediaType.MULTIPART_FORM_DATA)
public Response uploadPartial(@RestForm FilePart chunk) {
    // Server receives only the specified byte range
    return Response.ok("Received " + chunk.count + " bytes").build();
}

// Using PathPart
Path logFile = Path.of("/var/log/app.log");
long fileSize = Files.size(logFile);
// Send last 10KB of log file
PathPart logTail = new PathPart(logFile, fileSize - 10240, 10240);

@POST
@Path("/upload-log-tail")
@Consumes(MediaType.MULTIPART_FORM_DATA)
public Response uploadLogTail(@RestForm PathPart logChunk) {
    // Process last 10KB of log
    return Response.ok().build();
}

// Chunked upload pattern
public void uploadInChunks(File file) {
    long chunkSize = 5 * 1024 * 1024; // 5MB chunks
    long fileLength = file.length();

    for (long offset = 0; offset < fileLength; offset += chunkSize) {
        long count = Math.min(chunkSize, fileLength - offset);
        FilePart chunk = new FilePart(file, offset, count);
        uploadChunk(chunk);
    }
}
```

### Date Format Specification

Specify date/time parsing format for method parameters.

```java { .api }
package org.jboss.resteasy.reactive;

/**
 * Specifies the format for parsing date JAX-RS Resource method parameters.
 *
 * Supported types:
 * - java.time.LocalDate
 * - java.time.LocalDateTime
 * - java.time.LocalTime
 * - java.time.OffsetDateTime
 * - java.time.OffsetTime
 * - java.time.ZonedDateTime
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@interface DateFormat {
    /**
     * Pattern string for DateTimeFormatter.ofPattern().
     * Used to parse the input String into the desired type.
     */
    String pattern() default UNSET_PATTERN;

    /**
     * Provider class for custom DateTimeFormatter.
     * Used when pattern is not sufficient.
     */
    Class<? extends DateTimeFormatterProvider> dateTimeFormatterProvider()
        default DateTimeFormatterProvider.UnsetDateTimeFormatterProvider.class;

    String UNSET_PATTERN = "<<unset>>";

    interface DateTimeFormatterProvider extends Supplier<DateTimeFormatter> {
        class UnsetDateTimeFormatterProvider implements DateTimeFormatterProvider {
            DateTimeFormatter get();
        }
    }
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.DateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

@GET
@Path("/events")
public List<Event> getEventsByDate(
    @QueryParam("date")
    @DateFormat(pattern = "yyyy-MM-dd")
    LocalDate date
) {
    return eventService.findByDate(date);
}

@GET
@Path("/appointments")
public List<Appointment> getAppointments(
    @QueryParam("from")
    @DateFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime from,

    @QueryParam("to")
    @DateFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime to
) {
    return appointmentService.findBetween(from, to);
}

// Custom formatter provider
public class CustomDateTimeFormatterProvider
    implements DateFormat.DateTimeFormatterProvider {

    @Override
    public DateTimeFormatter get() {
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("UTC"));
    }
}

@GET
@Path("/reports")
public Report getReport(
    @QueryParam("timestamp")
    @DateFormat(dateTimeFormatterProvider = CustomDateTimeFormatterProvider.class)
    ZonedDateTime timestamp
) {
    return reportService.findByTimestamp(timestamp);
}

// European date format
@GET
@Path("/bookings")
public List<Booking> getBookings(
    @QueryParam("bookingDate")
    @DateFormat(pattern = "dd.MM.yyyy")
    LocalDate bookingDate
) {
    return bookingService.findByDate(bookingDate);
}
```

## File Upload and Download Interfaces

Advanced multipart file handling interfaces for upload and download operations.

```java { .api }
package org.jboss.resteasy.reactive.multipart;

/**
 * Base interface for file parts in multipart requests/responses.
 * Provides metadata about the file part.
 */
interface FilePart {
    /** The form parameter name for this file part */
    String name();

    /** The path to the file (temporary location for uploads) */
    Path filePath();

    /** The original filename from the client */
    String fileName();

    /** The size of the file in bytes */
    long size();

    /** The content type (MIME type) of the file */
    String contentType();

    /** The character set of the file content, if text */
    String charSet();
}

/**
 * Represents an uploaded file in a multipart request.
 * Extends FilePart with server-side upload functionality.
 * Automatically cleaned up after request completes.
 */
interface FileUpload extends FilePart {
    /** Wildcard constant for getting all uploaded files */
    String ALL = "*";

    /**
     * Get the path where the file was uploaded.
     * Default implementation returns filePath().
     */
    default Path uploadedFile() {
        return filePath();
    }
}

/**
 * Represents a file to be downloaded in a multipart response.
 * Extends FilePart for server-to-client file transfer.
 * Server-side only, not used for uploads.
 */
interface FileDownload extends FilePart {
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.multipart.FileDownload;
import org.jboss.resteasy.reactive.multipart.FilePart;
import org.jboss.resteasy.reactive.RestForm;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Path("/files")
public class FileResource {

    // Using FileUpload for detailed file information
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(@RestForm("file") FileUpload fileUpload) throws IOException {
        // Access file metadata
        String originalName = fileUpload.fileName();
        String contentType = fileUpload.contentType();
        long size = fileUpload.size();
        String paramName = fileUpload.name();

        System.out.println("Uploading file: " + originalName);
        System.out.println("Content type: " + contentType);
        System.out.println("Size: " + size + " bytes");
        System.out.println("Form field: " + paramName);

        // Get the uploaded file path (temporary location)
        Path uploadedPath = fileUpload.uploadedFile();

        // Process or move the file
        Path permanentLocation = Paths.get("/storage", originalName);
        Files.move(uploadedPath, permanentLocation);

        return Response.ok("File uploaded: " + originalName).build();
    }

    // Multiple file uploads
    @POST
    @Path("/upload-multiple")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadMultiple(@RestForm("files") List<FileUpload> uploads) throws IOException {
        int count = 0;
        for (FileUpload upload : uploads) {
            String filename = upload.fileName();
            Path destination = Paths.get("/storage", filename);
            Files.move(upload.uploadedFile(), destination);
            count++;

            System.out.println("Uploaded " + filename + " (" + upload.size() + " bytes)");
        }

        return Response.ok("Uploaded " + count + " files").build();
    }

    // Using wildcard to get all uploaded files
    @POST
    @Path("/upload-any")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadAny(@RestForm(FileUpload.ALL) List<FileUpload> allFiles) throws IOException {
        // Receives all file uploads regardless of form field name
        for (FileUpload file : allFiles) {
            System.out.println("File: " + file.fileName() + " from field: " + file.name());
            processFile(file);
        }

        return Response.ok("Processed " + allFiles.size() + " files").build();
    }

    // Validate file before processing
    @POST
    @Path("/upload-validated")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadValidated(@RestForm("document") FileUpload upload) throws IOException {
        // Validate file size
        if (upload.size() > 10_000_000) {  // 10MB
            return Response.status(400).entity("File too large").build();
        }

        // Validate content type
        String contentType = upload.contentType();
        if (!contentType.equals("application/pdf")) {
            return Response.status(400).entity("Only PDF files allowed").build();
        }

        // Validate filename
        String filename = upload.fileName();
        if (!filename.toLowerCase().endsWith(".pdf")) {
            return Response.status(400).entity("Invalid file extension").build();
        }

        // Process valid file
        Path destination = Paths.get("/documents", filename);
        Files.copy(upload.uploadedFile(), destination);

        return Response.ok("Document uploaded").build();
    }

    // Mixed upload with metadata
    @POST
    @Path("/upload-with-metadata")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadWithMetadata(
            @RestForm("file") FileUpload file,
            @RestForm("title") String title,
            @RestForm("description") String description,
            @RestForm("tags") List<String> tags) throws IOException {

        // Create document record
        Document doc = new Document();
        doc.filename = file.fileName();
        doc.contentType = file.contentType();
        doc.size = file.size();
        doc.title = title;
        doc.description = description;
        doc.tags = tags;

        // Save file
        Path storagePath = Paths.get("/documents", file.fileName());
        Files.copy(file.uploadedFile(), storagePath);
        doc.filePath = storagePath.toString();

        // Save metadata to database
        documentService.save(doc);

        return Response.ok(doc).build();
    }

    // Using FilePart interface for generic handling
    public void processFilePart(FilePart filePart) {
        String name = filePart.name();
        String filename = filePart.fileName();
        long size = filePart.size();
        String contentType = filePart.contentType();
        Path path = filePart.filePath();

        // Generic file processing
        System.out.println("Processing " + filename + " from " + name);
    }

    // FileDownload usage (server multipart responses)
    // Note: FileDownload is typically used internally by the framework
    // when generating multipart responses with MultipartFormDataOutput

    private void processFile(FileUpload file) throws IOException {
        // File processing logic
    }

    @Inject
    DocumentService documentService;
}

class Document {
    public String filename;
    public String contentType;
    public long size;
    public String title;
    public String description;
    public List<String> tags;
    public String filePath;
}

interface DocumentService {
    void save(Document doc);
}
```

**Key Differences:**
- `FileUpload`: Used for receiving files in requests (client-to-server)
- `FileDownload`: Used for sending files in responses (server-to-client)
- `FilePart`: Base interface with common file metadata

**Automatic Cleanup:**
Files uploaded via `FileUpload` are stored in temporary locations and automatically cleaned up after request processing completes. To persist files, copy or move them to permanent storage during request handling.

## Configuration

Multipart handling is configured via application properties:

```properties
# Default charset for multipart input parts (default: UTF-8)
quarkus.rest.multipart.input-part.default-charset=UTF-8

# Upload directory for temporary files
quarkus.http.body.uploads-directory=/tmp/uploads
```

## Integration with File Uploads

All multipart annotations work seamlessly with the file upload handlers documented in [File Upload Handling](./file-uploads.md) and [Server-Side Multipart Processing](./server-multipart.md). The annotations provide additional control over content types, filenames, and partial uploads.
