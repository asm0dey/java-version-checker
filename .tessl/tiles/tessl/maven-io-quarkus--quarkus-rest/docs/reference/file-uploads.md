# File Upload Handling

Quarkus REST provides built-in support for handling file uploads with automatic temporary file management, configurable upload directories, and support for both `java.io.File` and `java.nio.file.Path` types.

## Basic File Upload

Handle file uploads using `@FormParam` with `File` or `Path` types:

```java
import java.io.File;
import java.nio.file.Path;

@POST
@Path("/upload")
@Consumes(MediaType.MULTIPART_FORM_DATA)
public Response uploadFile(@FormParam("file") File file) {
    // Process the uploaded file
    long size = file.length();
    String name = file.getName();

    // File is automatically cleaned up after request completes
    return Response.ok("Uploaded: " + name + " (" + size + " bytes)").build();
}
```

## Using Path Instead of File

`Path` is recommended for modern file handling:

```java
@POST
@Path("/upload-path")
@Consumes(MediaType.MULTIPART_FORM_DATA)
public Response uploadFilePath(@FormParam("file") Path file) {
    try {
        long size = Files.size(file);
        String name = file.getFileName().toString();

        return Response.ok("Uploaded: " + name + " (" + size + " bytes)").build();
    } catch (IOException e) {
        return Response.serverError().build();
    }
}
```

## Multipart Form Data

Handle multiple form fields including files:

```java
@POST
@Path("/upload-with-metadata")
@Consumes(MediaType.MULTIPART_FORM_DATA)
public Response uploadWithMetadata(
    @FormParam("file") File file,
    @FormParam("description") String description,
    @FormParam("category") String category
) {
    // Process file with additional metadata
    return Response.ok()
        .entity("File: " + file.getName() + ", Description: " + description)
        .build();
}
```

## Multiple File Uploads

Handle multiple files in a single request:

```java
@POST
@Path("/upload-multiple")
@Consumes(MediaType.MULTIPART_FORM_DATA)
public Response uploadMultipleFiles(
    @FormParam("files") List<File> files
) {
    List<String> filenames = files.stream()
        .map(File::getName)
        .collect(Collectors.toList());

    return Response.ok("Uploaded " + files.size() + " files: " + filenames).build();
}
```

## Reactive File Upload

Handle file uploads reactively:

```java
@POST
@Path("/upload-async")
@Consumes(MediaType.MULTIPART_FORM_DATA)
public Uni<Response> uploadFileAsync(@FormParam("file") File file) {
    return Uni.createFrom().item(() -> {
        // Process file asynchronously
        long size = file.length();
        return Response.ok("Uploaded: " + size + " bytes").build();
    });
}
```

## File Upload Configuration

Configure multipart form handling via `quarkus.rest.multipart.*` properties:

```properties
# Default charset for multipart input parts
quarkus.rest.multipart.input-part.default-charset=UTF-8

# Maximum file upload size (Quarkus HTTP config)
quarkus.http.limits.max-body-size=10M
```

### Configuration Interface

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

interface ResteasyReactiveServerRuntimeConfig {
    MultipartConfigGroup multipart();

    interface MultipartConfigGroup {
        InputPartConfigGroup inputPart();

        interface InputPartConfigGroup {
            Charset defaultCharset();  // Default: UTF-8
        }
    }
}
```

## Temporary File Management

### Automatic Cleanup

Uploaded files are stored as temporary files and automatically deleted after the request completes:

```java
@POST
@Path("/upload")
@Consumes(MediaType.MULTIPART_FORM_DATA)
public Response uploadFile(@FormParam("file") Path uploadedFile) {
    // uploadedFile is a temporary file
    // It will be automatically deleted when the request completes

    // If you need to keep the file, copy it to a permanent location
    Path permanent = Paths.get("/permanent/storage", uploadedFile.getFileName().toString());
    Files.copy(uploadedFile, permanent);

    return Response.ok().build();
}
```

### Custom Upload Directory

Configure the upload directory for temporary files:

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

class QuarkusServerPathBodyHandler implements ServerMessageBodyReader<Path> {
    /**
     * Creates a temporary file in the configured upload directory
     * with an optional completion callback for cleanup
     */
    static Path createFile(ServerRequestContext context);
}
```

## File Size Limits

Control maximum upload sizes via HTTP configuration:

```properties
# Maximum request body size (applies to all requests including file uploads)
quarkus.http.limits.max-body-size=100M

# Disable body size limit (not recommended for production)
quarkus.http.limits.max-body-size=-1
```

## Streaming Large Files

For very large files, consider streaming to avoid memory issues:

```java
@POST
@Path("/upload-stream")
@Consumes(MediaType.MULTIPART_FORM_DATA)
public Response uploadLargeFile(@FormParam("file") Path file) {
    try (InputStream input = Files.newInputStream(file);
         OutputStream output = Files.newOutputStream(Paths.get("/storage/large-file"))) {

        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }

        return Response.ok("Large file uploaded successfully").build();
    } catch (IOException e) {
        return Response.serverError().build();
    }
}
```

## File Metadata

Access file metadata from multipart uploads:

```java
import org.jboss.resteasy.reactive.multipart.FileUpload;

@POST
@Path("/upload-info")
@Consumes(MediaType.MULTIPART_FORM_DATA)
public Response uploadWithInfo(@RestForm FileUpload file) {
    String filename = file.fileName();
    String contentType = file.contentType();
    long size = file.size();
    Path filePath = file.filePath();

    return Response.ok()
        .entity("File: " + filename + ", Type: " + contentType + ", Size: " + size)
        .build();
}
```

## Message Body Handlers

Quarkus REST uses `ServerMessageBodyReader` implementations for file handling:

### File Body Handler

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

class QuarkusServerFileBodyHandler implements ServerMessageBodyReader<File> {
    boolean isReadable(Class<?> type, Type genericType,
        ResteasyReactiveResourceInfo lazyMethod, MediaType mediaType);

    File readFrom(Class<File> type, Type genericType,
        MediaType mediaType, ServerRequestContext context);
}
```

### Path Body Handler

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

class QuarkusServerPathBodyHandler implements ServerMessageBodyReader<Path> {
    boolean isReadable(Class<?> type, Type genericType,
        ResteasyReactiveResourceInfo lazyMethod, MediaType mediaType);

    Path readFrom(Class<Path> type, Type genericType,
        MediaType mediaType, ServerRequestContext context);

    static Path createFile(ServerRequestContext context);
}
```

## Error Handling

Handle upload errors appropriately:

```java
@POST
@Path("/upload-safe")
@Consumes(MediaType.MULTIPART_FORM_DATA)
public Response uploadSafe(@FormParam("file") File file) {
    if (file == null) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("No file provided")
            .build();
    }

    if (file.length() == 0) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("Empty file")
            .build();
    }

    try {
        // Process file
        processFile(file);
        return Response.ok("File uploaded successfully").build();
    } catch (Exception e) {
        return Response.serverError()
            .entity("Failed to process file: " + e.getMessage())
            .build();
    }
}
```

## Security Considerations

1. **Validate File Types**: Check content type and file extensions

```java
@POST
@Path("/upload-image")
@Consumes(MediaType.MULTIPART_FORM_DATA)
public Response uploadImage(@RestForm FileUpload file) {
    String contentType = file.contentType();
    if (!contentType.startsWith("image/")) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("Only image files allowed")
            .build();
    }

    // Process image
    return Response.ok().build();
}
```

2. **Limit File Sizes**: Always configure maximum upload sizes

3. **Scan for Malware**: Consider virus scanning for user-uploaded files

4. **Sanitize Filenames**: Clean filenames before storing

```java
private String sanitizeFilename(String filename) {
    return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
}
```

5. **Store Outside Web Root**: Never store uploads in publicly accessible directories

## Best Practices

1. **Use Path over File**: `Path` is more modern and flexible

2. **Handle Cleanup**: Files are auto-cleaned, but copy to permanent storage if needed

3. **Validate Early**: Check file size, type, and content before processing

4. **Use Streaming**: For large files, stream instead of loading into memory

5. **Configure Limits**: Always set `max-body-size` in production

6. **Temporary Storage**: Be aware of disk space for temporary file storage

7. **Error Messages**: Provide clear error messages for validation failures
