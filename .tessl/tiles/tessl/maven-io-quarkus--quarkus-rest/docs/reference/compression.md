# HTTP Response Compression

Quarkus REST provides HTTP response compression to reduce bandwidth usage and improve performance by compressing response bodies based on media types and client capabilities.

## ResteasyReactiveCompressionHandler

The compression handler manages HTTP compression for REST responses, integrating with Vert.x's compression capabilities.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

class ResteasyReactiveCompressionHandler implements ServerRestHandler {
    // Constructors
    ResteasyReactiveCompressionHandler();
    ResteasyReactiveCompressionHandler(Set<String> compressMediaTypes);

    // Compression control
    HttpCompression getCompression();
    void setCompression(HttpCompression compression);

    // Media type filtering
    Set<String> getCompressMediaTypes();
    void setCompressMediaTypes(Set<String> compressMediaTypes);

    // Produces annotation
    String getProduces();
    void setProduces(String produces);

    // Handler execution
    void handle(ResteasyReactiveRequestContext requestContext);
}
```

## Compression Configuration

Configure compression behavior through application properties.

**Configuration Properties**:

```properties
# Enable HTTP compression
quarkus.http.enable-compression=true

# Compression level (1-9, higher = better compression but slower)
quarkus.http.compression-level=6

# Minimum response size to compress (bytes)
quarkus.vertx.http.compression-threshold=1024
```

## Media Type Filtering

The compression handler can be configured to compress only specific media types.

**Usage**:

```java
import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveCompressionHandler;
import java.util.Set;

// Configure compressible media types
Set<String> compressibleTypes = Set.of(
    "application/json",
    "application/xml",
    "text/html",
    "text/plain",
    "text/css",
    "application/javascript"
);

ResteasyReactiveCompressionHandler handler =
    new ResteasyReactiveCompressionHandler(compressibleTypes);
```

## HttpCompression Enum

Controls compression state for responses.

```java { .api }
enum HttpCompression {
    ON,         // Force compression enabled
    OFF,        // Force compression disabled
    UNDEFINED   // Use default behavior
}
```

**Usage**:

```java
ResteasyReactiveCompressionHandler handler = new ResteasyReactiveCompressionHandler();

// Enable compression
handler.setCompression(HttpCompression.ON);

// Disable compression
handler.setCompression(HttpCompression.OFF);

// Use default behavior
handler.setCompression(HttpCompression.UNDEFINED);
```

## Resource-Level Compression Control

Configure compression for specific endpoints through handler customization.

**Usage**:

```java
import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveCompressionHandler;
import org.jboss.resteasy.reactive.server.handlers.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CompressionCustomizer implements HandlerChainCustomizer {

    @Override
    public List<ServerRestHandler> handlers(
        Phase phase,
        ResourceClass resourceClass,
        ServerResourceMethod resourceMethod
    ) {
        if (phase == Phase.AFTER_MATCH) {
            // Check if method produces JSON
            if (resourceMethod.getProduces() != null &&
                resourceMethod.getProduces().contains("application/json")) {

                ResteasyReactiveCompressionHandler handler =
                    new ResteasyReactiveCompressionHandler();
                handler.setCompression(HttpCompression.ON);
                handler.setProduces("application/json");

                return List.of(handler);
            }
        }
        return Collections.emptyList();
    }
}
```

## Default Compressible Media Types

By default, common text-based media types are compressed:

- `text/html`
- `text/css`
- `text/plain`
- `text/javascript`
- `application/javascript`
- `application/json`
- `application/xml`
- `application/xhtml+xml`

Binary formats like images and videos are typically not compressed as they're already compressed.

## Client Compression Support

Compression is automatically negotiated based on the `Accept-Encoding` header from the client.

**HTTP Request**:

```
GET /api/data HTTP/1.1
Host: example.com
Accept-Encoding: gzip, deflate
```

**HTTP Response**:

```
HTTP/1.1 200 OK
Content-Type: application/json
Content-Encoding: gzip
Vary: Accept-Encoding

[compressed JSON data]
```

## Selective Compression by Size

Configure minimum response size threshold for compression:

```properties
# Only compress responses larger than 2KB
quarkus.vertx.http.compression-threshold=2048
```

Small responses may not benefit from compression due to overhead.

## Compression Algorithms

Quarkus REST supports standard compression algorithms:

- **gzip**: Most widely supported, good compression
- **deflate**: Standard ZLIB compression
- **br** (Brotli): Better compression, less CPU intensive (if available)

The server selects the best algorithm based on client preferences.

## Programmatic Compression Control

Control compression programmatically for specific responses.

**Usage**:

```java
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.core.Context;

@Path("/api/data")
public class DataResource {

    @GET
    @Path("/large")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLargeData(@Context RoutingContext context) {
        // Force compression for this response
        context.response().putHeader("Content-Encoding", "gzip");

        LargeData data = dataService.fetchLargeData();
        return Response.ok(data).build();
    }

    @GET
    @Path("/small")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSmallData(@Context RoutingContext context) {
        // Disable compression for small response
        context.response().putHeader("Content-Encoding", "identity");

        SmallData data = dataService.fetchSmallData();
        return Response.ok(data).build();
    }
}
```

## Compression with Streaming

Compression works with streaming responses for large data sets.

**Usage**:

```java
import io.smallrye.mutiny.Multi;
import org.jboss.resteasy.reactive.RestStreamElementType;

@Path("/api/stream")
public class StreamResource {

    @GET
    @Path("/data")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<DataEvent> streamData() {
        // Response will be compressed if enabled
        return dataService.streamEvents();
    }

    @GET
    @Path("/logs")
    @Produces(MediaType.TEXT_PLAIN)
    public Multi<String> streamLogs() {
        // Text streaming with compression
        return logService.streamLogs();
    }
}
```

## Compression Performance Considerations

### Benefits

- Reduced bandwidth usage
- Faster transfer times for text data
- Lower data costs

### Trade-offs

- CPU overhead for compression
- Memory usage for compression buffers
- Latency increase for small responses

### Best Practices

**Compress text-based content**:

```java
// Good candidates for compression
@GET
@Produces(MediaType.APPLICATION_JSON)
public LargeJsonResponse getLargeJson() {
    return service.getJson();
}

@GET
@Produces(MediaType.TEXT_HTML)
public String getHtml() {
    return service.getHtml();
}
```

**Avoid compressing binary content**:

```java
// Already compressed - skip compression
@GET
@Produces("image/jpeg")
public byte[] getImage() {
    return imageService.getJpeg();
}

@GET
@Produces("video/mp4")
public File getVideo() {
    return videoService.getMp4();
}
```

## Compression Level Tuning

Balance compression ratio vs CPU usage:

```properties
# Level 1: Fast, lower compression
quarkus.http.compression-level=1

# Level 6: Balanced (default)
quarkus.http.compression-level=6

# Level 9: Best compression, slowest
quarkus.http.compression-level=9
```

## Vary Header

The compression handler automatically adds the `Vary: Accept-Encoding` header to indicate that the response varies based on client encoding capabilities:

```
HTTP/1.1 200 OK
Content-Type: application/json
Content-Encoding: gzip
Vary: Accept-Encoding
```

This ensures proper caching behavior by proxies and CDNs.

## Disable Compression for Specific Endpoints

Override global compression settings for specific endpoints:

```java
@Path("/api")
public class ApiResource {

    @GET
    @Path("/compressed")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCompressed() {
        // Uses global compression settings
        return Response.ok(data).build();
    }

    @GET
    @Path("/uncompressed")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUncompressed(@Context RoutingContext context) {
        // Disable compression for this endpoint
        context.response().putHeader("Content-Encoding", "identity");
        return Response.ok(data).build();
    }
}
```

## Testing Compression

Verify compression behavior in tests:

```java
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

public class CompressionTest {

    @Test
    public void testCompressionEnabled() {
        RestAssured.given()
            .header("Accept-Encoding", "gzip")
            .when()
            .get("/api/large-data")
            .then()
            .statusCode(200)
            .header("Content-Encoding", "gzip")
            .header("Vary", "Accept-Encoding");
    }

    @Test
    public void testNoCompressionForSmall() {
        RestAssured.given()
            .header("Accept-Encoding", "gzip")
            .when()
            .get("/api/small-data")
            .then()
            .statusCode(200)
            .header("Content-Encoding", nullValue());
    }
}
```

## Integration with CDN and Proxies

Compression works seamlessly with CDNs and reverse proxies:

```
Client -> CDN/Proxy -> Quarkus REST
       <-  compressed  <-  compressed
```

The `Vary: Accept-Encoding` header ensures proper cache key differentiation.
