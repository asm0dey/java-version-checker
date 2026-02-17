# Configuration

Quarkus REST provides runtime configuration properties for controlling server behavior, multipart handling, and other runtime features.

## ResteasyReactiveServerRuntimeConfig

The main interface for runtime configuration properties, mapped to `quarkus.rest.*` properties.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

interface ResteasyReactiveServerRuntimeConfig {
    MultipartConfigGroup multipart();
}
```

## Multipart Configuration

Configuration for handling multipart form data uploads.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

interface MultipartConfigGroup {
    InputPartConfigGroup inputPart();
}

interface InputPartConfigGroup {
    Charset defaultCharset();
}
```

### Default Charset

Configure the default character encoding for multipart input parts.

**Configuration Property**:

```properties
# Default charset for multipart input parts (default: UTF-8)
quarkus.rest.multipart.input-part.default-charset=UTF-8
```

**Usage**:

```java
@Path("/api/upload")
public class UploadResource {

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(
        @FormParam("file") File file,
        @FormParam("description") String description
    ) {
        // description uses configured charset for decoding
        System.out.println("Description: " + description);
        return Response.ok().build();
    }
}
```

## HTTP Server Configuration

General HTTP server configuration properties affecting REST endpoints.

### Port Configuration

```properties
# HTTP port (default: 8080)
quarkus.http.port=8080

# HTTPS port (default: 8443)
quarkus.http.ssl-port=8443

# Test port (used in @QuarkusTest)
quarkus.http.test-port=8081
```

### Host Configuration

```properties
# Bind address (default: 0.0.0.0)
quarkus.http.host=0.0.0.0

# Advertised host for self-referencing URLs
quarkus.http.host-enabled=true
```

### Request Limits

```properties
# Maximum request body size (default: 10M)
quarkus.http.limits.max-body-size=10M

# Maximum header size (default: 20K)
quarkus.http.limits.max-header-size=20K

# Maximum chunk size for streaming (default: 8192)
quarkus.http.limits.max-chunk-size=8192

# Maximum form attribute size (default: 2048)
quarkus.http.limits.max-form-attribute-size=2048
```

### Timeout Configuration

```properties
# Idle timeout for connections (default: 30M)
quarkus.http.idle-timeout=30M

# Read timeout (default: 60s)
quarkus.http.read-timeout=60s

# Write timeout
quarkus.http.write-timeout=60s
```

## Compression Configuration

HTTP response compression settings.

```properties
# Enable HTTP compression (default: false)
quarkus.http.enable-compression=true

# Compression level (1-9, default: 6)
quarkus.http.compression-level=6

# Minimum response size to compress in bytes (default: 20)
quarkus.vertx.http.compression-threshold=1024
```

**Usage Example**:

```properties
# Enable compression for responses larger than 2KB
quarkus.http.enable-compression=true
quarkus.http.compression-level=6
quarkus.vertx.http.compression-threshold=2048
```

## CORS Configuration

Cross-Origin Resource Sharing (CORS) settings for REST endpoints.

```properties
# Enable CORS (default: false)
quarkus.http.cors=true

# Allowed origins
quarkus.http.cors.origins=http://localhost:3000,https://example.com

# Allow all origins (NOT recommended for production)
quarkus.http.cors.origins=*

# Allowed HTTP methods
quarkus.http.cors.methods=GET,POST,PUT,DELETE

# Allowed headers
quarkus.http.cors.headers=Content-Type,Authorization

# Exposed headers
quarkus.http.cors.exposed-headers=X-Custom-Header

# Allow credentials
quarkus.http.cors.access-control-allow-credentials=true

# Max age for preflight requests (seconds)
quarkus.http.cors.access-control-max-age=3600
```

**Production CORS Configuration**:

```properties
quarkus.http.cors=true
quarkus.http.cors.origins=https://app.example.com
quarkus.http.cors.methods=GET,POST,PUT,DELETE,OPTIONS
quarkus.http.cors.headers=Content-Type,Authorization,X-Request-ID
quarkus.http.cors.access-control-allow-credentials=true
quarkus.http.cors.access-control-max-age=86400
```

## SSL/TLS Configuration

HTTPS configuration for secure connections.

```properties
# Enable SSL (default: false)
quarkus.http.ssl.port=8443

# Certificate configuration
quarkus.http.ssl.certificate.key-store-file=keystore.jks
quarkus.http.ssl.certificate.key-store-password=password
quarkus.http.ssl.certificate.key-store-file-type=JKS

# Or use PEM files
quarkus.http.ssl.certificate.files=server-cert.pem
quarkus.http.ssl.certificate.key-files=server-key.pem

# Client certificate authentication
quarkus.http.ssl.client-auth=REQUEST
```

## Access Log Configuration

HTTP access logging configuration.

```properties
# Enable access log (default: false)
quarkus.http.access-log.enabled=true

# Log pattern (default: common)
quarkus.http.access-log.pattern=common

# Custom pattern
quarkus.http.access-log.pattern=%h %l %u %t "%r" %s %b "%{Referer}i" "%{User-Agent}i"

# Exclude paths
quarkus.http.access-log.exclude-pattern=/health,/metrics
```

## Body Handler Configuration

Configuration for request body handling.

```properties
# Enable file uploads (default: true)
quarkus.http.body.handle-file-uploads=true

# Uploads directory (default: file-uploads)
quarkus.http.body.uploads-directory=file-uploads

# Delete uploaded files after request (default: true)
quarkus.http.body.delete-uploaded-files-on-end=true

# Merge form attributes (default: true)
quarkus.http.body.merge-form-attributes=true

# Preallocate body buffer (default: false)
quarkus.http.body.preallocate-body-buffer=false
```

## Virtual Threads Configuration

Configure virtual thread usage for REST endpoints.

```properties
# Enable virtual threads (requires JDK 21+)
quarkus.virtual-threads.enabled=true

# Timeout for virtual thread tasks
quarkus.virtual-threads.shutdown-timeout=10s
```

**Usage with REST endpoints**:

```java
@Path("/api/blocking")
public class BlockingResource {

    @GET
    @RunOnVirtualThread  // Runs on virtual thread
    public String blockingOperation() {
        // Blocking I/O operation
        return performBlockingCall();
    }
}
```

## Root Path Configuration

Configure the root path for REST endpoints.

```properties
# Root path for REST endpoints (default: /)
quarkus.rest.path=/api

# With this setting, @Path("/users") becomes /api/users
```

**Example**:

```properties
# All REST endpoints under /api
quarkus.rest.path=/api

# All REST endpoints under /rest/v1
quarkus.rest.path=/rest/v1
```

## Error Page Configuration

Customize error pages for REST responses.

```properties
# Enable detailed error messages (dev mode default: true)
quarkus.http.enable-detailed-errors=false

# Custom error page templates
quarkus.http.error-page.default=custom-error.html
```

## HTTP/2 Configuration

Enable HTTP/2 support.

```properties
# Enable HTTP/2 (default: true)
quarkus.http.http2=true

# Max concurrent streams per connection
quarkus.http.http2.max-concurrent-streams=100

# Initial window size
quarkus.http.http2.initial-window-size=65535
```

## Proxy Configuration

Configuration for running behind a proxy.

```properties
# Enable proxy protocol (default: false)
quarkus.http.proxy.enable-forwarded-host=true
quarkus.http.proxy.enable-forwarded-prefix=true

# Trusted proxies
quarkus.http.proxy.trusted-proxies=10.0.0.0/8,172.16.0.0/12

# Allow X-Forwarded-* headers
quarkus.http.proxy.allow-forwarded=true
quarkus.http.proxy.allow-x-forwarded=true
```

## Authentication Configuration

Security and authentication settings.

```properties
# Proactive authentication (default: true)
quarkus.http.auth.proactive=true

# Session configuration
quarkus.http.auth.session.encryption-key=changeit
quarkus.http.auth.session.timeout=30M

# Basic authentication realm
quarkus.http.auth.basic.realm=Quarkus
quarkus.http.auth.basic.charset=UTF-8
```

## Management Interface Configuration

Separate management interface for health checks and metrics.

```properties
# Enable management interface
quarkus.management.enabled=true

# Management port (separate from application)
quarkus.management.port=9000

# Management host
quarkus.management.host=0.0.0.0

# Root path for management endpoints
quarkus.management.root-path=/
```

**Usage**:

```properties
# Application on port 8080
quarkus.http.port=8080

# Management (health, metrics) on port 9000
quarkus.management.enabled=true
quarkus.management.port=9000
```

## Development Mode Configuration

Configuration specific to development mode.

```properties
# Enable dev mode (automatic)
quarkus.dev.mode=true

# Enable live reload (default: true)
quarkus.live-reload.enabled=true

# Enable instrumentation for dev UI (default: true)
quarkus.dev.instrumentation=true

# Console color support (default: true)
quarkus.console.color=true
```

## Testing Configuration

Configuration for testing REST endpoints.

```properties
# Test HTTP port (default: 8081)
quarkus.http.test-port=8081

# Test SSL port
quarkus.http.test-ssl-port=8444

# Test timeout
quarkus.test.hang-detection-timeout=10m
```

## Programmatic Configuration Access

Access configuration programmatically.

**Using @ConfigProperty**:

```java
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/api/config")
public class ConfigResource {

    @ConfigProperty(name = "quarkus.http.port")
    int httpPort;

    @ConfigProperty(name = "quarkus.rest.multipart.input-part.default-charset")
    String charset;

    @GET
    @Path("/info")
    public Response getConfigInfo() {
        return Response.ok()
            .entity(new ConfigInfo(httpPort, charset))
            .build();
    }
}
```

**Using Config API**:

```java
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

@Path("/api/dynamic")
public class DynamicConfigResource {

    @GET
    public Response getDynamicConfig() {
        Config config = ConfigProvider.getConfig();

        int port = config.getValue("quarkus.http.port", Integer.class);
        String host = config.getValue("quarkus.http.host", String.class);
        boolean corsEnabled = config.getOptionalValue("quarkus.http.cors", Boolean.class)
            .orElse(false);

        return Response.ok()
            .entity(new ServerConfig(port, host, corsEnabled))
            .build();
    }
}
```

## Environment-Specific Configuration

Use profiles for environment-specific settings.

**application.properties**:

```properties
# Default configuration
quarkus.http.port=8080
quarkus.http.cors=false

# Development profile
%dev.quarkus.http.port=8080
%dev.quarkus.http.cors=true
%dev.quarkus.http.cors.origins=*

# Test profile
%test.quarkus.http.test-port=8081

# Production profile
%prod.quarkus.http.port=80
%prod.quarkus.http.ssl.port=443
%prod.quarkus.http.cors=true
%prod.quarkus.http.cors.origins=https://example.com
```

## Configuration Validation

Validate configuration at startup.

**Usage**:

```java
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class ConfigValidator {

    @ConfigProperty(name = "quarkus.http.port")
    int httpPort;

    @ConfigProperty(name = "quarkus.http.ssl.port")
    Optional<Integer> sslPort;

    void validateConfig(@Observes StartupEvent event) {
        if (httpPort < 1024) {
            throw new IllegalStateException("HTTP port must be >= 1024");
        }

        sslPort.ifPresent(port -> {
            if (port.equals(httpPort)) {
                throw new IllegalStateException("SSL port cannot equal HTTP port");
            }
        });
    }
}
```

## CDI Context Producers

Quarkus REST provides CDI producers for JAX-RS context types, making them available for injection via `@Context` or `@Inject`.

### QuarkusContextProducers

CDI singleton that produces JAX-RS context objects.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

@Singleton
class QuarkusContextProducers {
    // HTTP server response producer
    @Produces
    @RequestScoped
    HttpServerResponse httpServerResponse();

    // JAX-RS providers
    @Produces
    @ApplicationScoped
    Providers providers();

    // Resource closer
    @Produces
    @RequestScoped
    CloserImpl closer();

    // Disposer for closer
    @Disposes
    void closeCloser(CloserImpl closer);
}
```

**Usage**:

```java
import io.vertx.core.http.HttpServerResponse;
import jakarta.ws.rs.ext.Providers;

@Path("/api/context")
public class ContextResource {

    // Inject Vert.x HTTP response
    @Inject
    HttpServerResponse response;

    // Inject JAX-RS providers
    @Inject
    Providers providers;

    // Inject resource closer
    @Inject
    Closer closer;

    @GET
    @Path("/custom-response")
    public void customResponse() {
        response.setStatusCode(200);
        response.putHeader("X-Custom", "value");
        response.end("Custom response");
    }

    @GET
    @Path("/with-cleanup")
    public String withCleanup() throws IOException {
        FileInputStream fis = new FileInputStream("data.txt");

        // Register for automatic cleanup
        closer.add(fis);

        return readData(fis);
    }
}
```

### Closer Implementation

The Closer interface manages automatic resource cleanup when requests complete.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

@RequestScoped
class CloserImpl implements Closer {
    void add(Closeable c);
    // Package-private close() called automatically at request end
}
```

All Closeables added to the Closer are automatically closed when the request completes, ensuring proper resource cleanup.

### Context Injection Patterns

Both `@Context` and `@Inject` work for context types:

```java
@Path("/api/patterns")
public class InjectionPatternsResource {

    // JAX-RS @Context annotation
    @Context
    UriInfo uriInfo1;

    // CDI @Inject (equivalent)
    @Inject
    UriInfo uriInfo2;

    // Method parameter injection
    @GET
    public Response get(@Context SecurityContext security, @Inject HttpHeaders headers) {
        // Both injection styles work
        return Response.ok().build();
    }
}
```

## Complete Production Configuration Example

Comprehensive production configuration:

```properties
# Server
quarkus.http.port=8080
quarkus.http.host=0.0.0.0
quarkus.http.ssl.port=8443
quarkus.http.ssl.certificate.key-store-file=keystore.jks
quarkus.http.ssl.certificate.key-store-password=${KEYSTORE_PASSWORD}

# Limits
quarkus.http.limits.max-body-size=10M
quarkus.http.limits.max-header-size=20K
quarkus.http.idle-timeout=30M

# Compression
quarkus.http.enable-compression=true
quarkus.http.compression-level=6
quarkus.vertx.http.compression-threshold=2048

# CORS
quarkus.http.cors=true
quarkus.http.cors.origins=https://app.example.com
quarkus.http.cors.methods=GET,POST,PUT,DELETE,OPTIONS
quarkus.http.cors.headers=Content-Type,Authorization
quarkus.http.cors.access-control-allow-credentials=true

# Security
quarkus.http.auth.proactive=true

# Proxy
quarkus.http.proxy.enable-forwarded-host=true
quarkus.http.proxy.enable-forwarded-prefix=true
quarkus.http.proxy.trusted-proxies=10.0.0.0/8

# Multipart
quarkus.rest.multipart.input-part.default-charset=UTF-8
quarkus.http.body.uploads-directory=/tmp/uploads
quarkus.http.body.delete-uploaded-files-on-end=true

# Management
quarkus.management.enabled=true
quarkus.management.port=9000

# Access Log
quarkus.http.access-log.enabled=true
quarkus.http.access-log.pattern=combined
```
