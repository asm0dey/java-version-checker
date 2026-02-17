# Resource Lifecycle Management

Quarkus REST provides the `Closer` interface for automatic cleanup of resources when requests complete, ensuring proper resource management and preventing resource leaks.

## Closer Interface

```java { .api }
package io.quarkus.resteasy.reactive.server;

interface Closer {
    /**
     * Register a Closeable resource to be closed when the request completes
     *
     * @param c the Closeable to close automatically
     */
    void add(Closeable c);
}
```

## Injecting Closer

Inject `Closer` into resource methods using `@Context`:

```java
import io.quarkus.resteasy.reactive.server.Closer;
import jakarta.ws.rs.core.Context;

@Path("/files")
public class FileResource {

    @GET
    @Path("/{id}")
    public String readFile(@PathParam("id") String id, @Context Closer closer) {
        InputStream stream = fileService.openFile(id);
        closer.add(stream);  // Stream will be closed automatically

        return new String(stream.readAllBytes());
    }
}
```

## Usage Patterns

### File Streams

Automatically close file input streams:

```java
@GET
@Path("/download/{filename}")
@Produces(MediaType.APPLICATION_OCTET_STREAM)
public InputStream downloadFile(@PathParam("filename") String filename, @Context Closer closer) {
    FileInputStream fis = new FileInputStream("/data/" + filename);
    closer.add(fis);  // Auto-close when response is sent
    return fis;
}
```

### Database Connections

Close database connections from non-CDI managed resources:

```java
@GET
@Path("/legacy-data")
public String getLegacyData(@Context Closer closer) {
    Connection conn = legacyDataSource.getConnection();
    closer.add(conn);  // Auto-close after request

    // Use connection
    return queryData(conn);
}
```

### Multiple Resources

Register multiple closeables for a single request:

```java
@POST
@Path("/process")
public Response processFiles(@Context Closer closer) {
    InputStream input = openInputFile();
    closer.add(input);

    OutputStream output = openOutputFile();
    closer.add(output);

    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
    closer.add(reader);

    // Process files
    processData(reader, output);

    return Response.ok().build();
}
```

### Custom Closeables

Implement `Closeable` for custom cleanup logic:

```java
class ResourceHandle implements Closeable {
    private final ExternalResource resource;

    ResourceHandle(ExternalResource resource) {
        this.resource = resource;
    }

    @Override
    public void close() {
        resource.release();
        resource.cleanup();
    }
}

@GET
@Path("/external")
public String useExternalResource(@Context Closer closer) {
    ExternalResource resource = externalService.acquire();
    closer.add(new ResourceHandle(resource));

    return resource.getData();
}
```

## Lifecycle

Resources registered with `Closer` are closed in the following scenarios:

1. **Successful Response**: After the response is fully sent to the client
2. **Exception**: When an exception is thrown and handled
3. **Request Cancellation**: If the client disconnects before completion

The closing happens automatically during request scope deactivation, ensuring cleanup even in error scenarios.

## Error Handling

The `Closer` suppresses exceptions thrown during cleanup to avoid masking the original error:

```java
@GET
@Path("/safe-cleanup")
public String safeCleanup(@Context Closer closer) {
    InputStream stream = openStream();
    closer.add(stream);

    // If both processing and closing fail,
    // the processing exception is propagated,
    // the closing exception is logged and suppressed
    return processStream(stream);
}
```

## CDI Integration

`Closer` is available as a CDI request-scoped bean:

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

@Singleton
class QuarkusContextProducers {
    @Produces
    @RequestScoped
    Closer closer();

    @Disposes
    void closeCloser(Closer closer);
}
```

This allows injection via `@Inject` instead of `@Context`:

```java
@Path("/files")
public class FileResource {

    @Inject
    Closer closer;

    @GET
    @Path("/{id}")
    public String readFile(@PathParam("id") String id) {
        InputStream stream = fileService.openFile(id);
        closer.add(stream);
        return new String(stream.readAllBytes());
    }
}
```

## With Reactive Types

`Closer` works with reactive return types:

```java
@GET
@Path("/async-file/{id}")
public Uni<String> readFileAsync(@PathParam("id") String id, @Context Closer closer) {
    return Uni.createFrom().item(() -> {
        InputStream stream = fileService.openFile(id);
        closer.add(stream);
        return new String(stream.readAllBytes());
    });
}
```

Resources are closed after the `Uni` or `Multi` completes.

## Comparison with Try-With-Resources

Traditional try-with-resources:

```java
@GET
@Path("/traditional")
public String traditional() {
    try (InputStream stream = openStream()) {
        return processStream(stream);
    } catch (IOException e) {
        throw new WebApplicationException(e);
    }
}
```

With `Closer`:

```java
@GET
@Path("/with-closer")
public String withCloser(@Context Closer closer) {
    InputStream stream = openStream();
    closer.add(stream);
    return processStream(stream);  // No try-catch needed for closing
}
```

Benefits of `Closer`:
- Cleaner code without nested try-with-resources
- Works across multiple methods in the request scope
- Integrates with reactive pipelines
- Consistent cleanup even with complex error handling

## Best Practices

1. **Register Early**: Add resources to `Closer` immediately after acquiring them

```java
// Good
InputStream stream = openStream();
closer.add(stream);

// Risky - exception before add() causes leak
InputStream stream = openStream();
doSomething();  // If this throws, stream leaks
closer.add(stream);
```

2. **Use for External Resources**: Ideal for resources not managed by CDI or connection pools

3. **Avoid for CDI Beans**: Don't use `Closer` for CDI beans - they're managed automatically

4. **Thread Safety**: `Closer` is request-scoped and not thread-safe. Don't share across threads

5. **Null Safety**: `Closer.add()` handles null gracefully (no-op)

```java
InputStream stream = maybeOpenStream();  // Might return null
closer.add(stream);  // Safe even if null
```

## Implementation Details

The `Closer` implementation maintains a list of `Closeable` objects and closes them during request scope deactivation:

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

class CloserImpl implements Closer {
    void add(Closeable c);
    // Package-private: called during request cleanup
    void close();
}
```

Exception suppression during close ensures the primary request exception is not masked by cleanup errors.
