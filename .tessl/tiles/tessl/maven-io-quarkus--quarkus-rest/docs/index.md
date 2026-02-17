# Quarkus REST

Quarkus REST is a modern Jakarta REST (JAX-RS) implementation built on Vert.x for high-performance, reactive REST endpoints with build-time optimization.

## Installation

**Maven**:
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest</artifactId>
</dependency>
```

**Gradle**:
```gradle
implementation("io.quarkus:quarkus-rest")
```

## Quick Start

### Basic REST Endpoint

```java
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class HelloResource {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello, World!";
    }
}
```

### Reactive Endpoint

```java
import io.smallrye.mutiny.Uni;

@Path("/async")
public class AsyncResource {
    @GET
    public Uni<String> getData() {
        return Uni.createFrom().item("Async response");
    }
}
```

### Secured Endpoint

```java
import jakarta.annotation.security.RolesAllowed;

@Path("/admin")
@RolesAllowed("admin")
public class AdminResource {
    @GET
    public List<User> getUsers() {
        return userService.findAll();
    }
}
```

## Core Concepts

### Threading Model
- **Reactive methods** (Uni, Multi, CompletionStage): Run on I/O threads (event loop)
- **Blocking methods** (regular return types): Run on worker threads
- Override with `@Blocking` or `@NonBlocking` annotations

### Request Processing
1. Request received on Vert.x event loop
2. Security checks (if configured)
3. Request filters execute
4. Resource method invoked (I/O or worker thread)
5. Response filters execute
6. Response sent to client

## Feature Overview

| Feature | Description | Reference |
|---------|-------------|-----------|
| **Jakarta REST** | Standard JAX-RS annotations (@GET, @POST, @Path, etc.) | [jakarta-rest.md](reference/jakarta-rest.md) |
| **Reactive** | Mutiny Uni/Multi for async operations | [reactive-programming.md](reference/reactive-programming.md) |
| **Security** | RBAC, permissions, authentication | [security.md](reference/security.md) |
| **Filters** | Request/response filtering, exception mapping | [filters.md](reference/filters.md) |
| **REST Response** | Type-safe response wrapper | [rest-response.md](reference/rest-response.md) |
| **REST Client** | Declarative REST clients | [rest-client.md](reference/rest-client.md) |
| **Streaming** | SSE and reactive streams | [streaming.md](reference/streaming.md) |
| **Multipart** | File uploads and multipart forms | [multipart-forms.md](reference/multipart-forms.md) |
| **Caching** | HTTP caching directives | [caching.md](reference/caching.md) |
| **Configuration** | Runtime configuration options | [configuration.md](reference/configuration.md) |

## Common Annotations

### HTTP Methods
```java
@GET @POST @PUT @DELETE @PATCH @HEAD @OPTIONS
```

### Parameter Binding
```java
@PathParam("id")        // URL path parameter
@QueryParam("filter")   // Query string parameter
@HeaderParam("auth")    // HTTP header
@FormParam("data")      // Form field
@Context                // Inject context objects
```

### Quarkus Shortcuts
```java
@RestPath("id")         // Shorter @PathParam
@RestQuery("filter")    // Shorter @QueryParam
@RestHeader("auth")     // Shorter @HeaderParam
```

### Security
```java
@RolesAllowed("admin")           // Role-based access
@PermissionsAllowed("doc:read")  // Permission-based access
@Authenticated                    // Require authentication
@PermitAll                        // Allow all users
```

### Filters & Mapping
```java
@ServerRequestFilter      // Request filter method
@ServerResponseFilter     // Response filter method
@ServerExceptionMapper    // Exception to Response mapping
```

## Response Types

| Return Type | Execution | Use Case |
|-------------|-----------|----------|
| `T` | Blocking (worker thread) | Simple synchronous operations |
| `Uni<T>` | Non-blocking (I/O thread) | Single async value |
| `Multi<T>` | Non-blocking (I/O thread) | Stream of values |
| `CompletionStage<T>` | Non-blocking (I/O thread) | Java async API |
| `Response` | Depends on content | Full HTTP control |
| `RestResponse<T>` | Depends on content | Type-safe HTTP control |

## Quick Reference

### Creating Responses

```java
// Simple return
return "text";
return new User("Alice");

// RestResponse
return RestResponse.ok(user);
return RestResponse.notFound();
return RestResponse.status(201, created);

// Response builder
return Response.ok(entity)
    .header("X-Custom", "value")
    .build();
```

### Error Handling

```java
@ServerExceptionMapper
public Response handleError(MyException ex) {
    return Response.status(400)
        .entity(ex.getMessage())
        .build();
}
```

### Reactive Patterns

```java
// Transform
return repository.findById(id)
    .onItem().transform(entity -> new DTO(entity));

// Error recovery
return service.getData()
    .onFailure().recoverWithItem(defaultValue);

// Retry
return client.call()
    .onFailure().retry().atMost(3);

// Combine
return Uni.combine().all()
    .unis(uni1, uni2, uni3)
    .asTuple();
```

## Documentation Structure

### Guides
Step-by-step instructions for common tasks:
- **[Quick Start](guides/quick-start.md)** - Getting started with Quarkus REST
- **[Building REST APIs](guides/building-rest-apis.md)** - Creating REST endpoints
- **[Security Setup](guides/security-setup.md)** - Implementing authentication and authorization
- **[Reactive Programming](guides/reactive-programming.md)** - Using Uni and Multi
- **[REST Clients](guides/rest-clients.md)** - Building REST clients

### Examples
Real-world usage scenarios:
- **[Common Scenarios](examples/common-scenarios.md)** - Typical use cases
- **[Advanced Patterns](examples/advanced-patterns.md)** - Complex implementations
- **[Integration Examples](examples/integration-examples.md)** - Working with other Quarkus features

### Reference
Detailed API documentation:

**Core APIs**:
- [Jakarta REST](reference/jakarta-rest.md) - Standard JAX-RS features
- [Reactive Programming](reference/reactive-programming.md) - Mutiny integration
- [Security](reference/security.md) - Authentication and authorization
- [Filters](reference/filters.md) - Request/response filtering
- [REST Response](reference/rest-response.md) - Response API
- [REST Client](reference/rest-client.md) - Client features

**Features**:
- [Streaming](reference/streaming.md) - SSE and reactive streams
- [Multipart Forms](reference/multipart-forms.md) - File uploads
- [File Uploads](reference/file-uploads.md) - File handling
- [Caching](reference/caching.md) - HTTP caching
- [Declarative Responses](reference/declarative-responses.md) - Response annotations
- [Exception Mapping](reference/exception-mapping.md) - Error handling
- [Parameter Annotations](reference/parameter-annotations.md) - Parameter binding

**Configuration & Integration**:
- [Configuration](reference/configuration.md) - Runtime configuration
- [Context Injection](reference/context-injection.md) - CDI integration
- [Compression](reference/compression.md) - HTTP compression
- [Observability](reference/observability.md) - Metrics and tracing
- [Conditional Endpoints](reference/conditional-endpoints.md) - Dynamic endpoints
- [Resource Management](reference/resource-management.md) - Lifecycle management

**Advanced**:
- [Architecture](reference/architecture.md) - Internal architecture
- [Handler Chain](reference/handler-chain.md) - Pipeline customization
- [Server SPI](reference/server-spi.md) - Low-level server APIs
- [Build SPI](reference/build-spi.md) - Extension development
- [Server Multipart](reference/server-multipart.md) - Multipart processing
- [Jackson Serialization](reference/jackson-serialization.md) - JSON customization
- [Qute Integration](reference/qute-integration.md) - Template engine
- [REST Data Panache](reference/rest-data-panache.md) - CRUD generation
- [REST Links](reference/rest-links.md) - HATEOAS support
- [WebSockets](reference/websockets.md) - WebSocket integration

## Key Features

### Build-Time Optimization
Quarkus processes REST endpoints at build time, generating optimized bytecode for faster startup and lower memory usage.

### Reactive by Default
Seamless integration with Mutiny for non-blocking, reactive programming. Methods returning `Uni` or `Multi` automatically run on I/O threads.

### Security Integration
Built-in support for role-based and permission-based access control with automatic security context management.

### Flexible Filtering
Simplified filter annotations (`@ServerRequestFilter`, `@ServerResponseFilter`) with automatic parameter injection and reactive support.

### Type-Safe Responses
`RestResponse<T>` provides type-safe response handling with full control over status, headers, and entity.

### Declarative REST Clients
Create REST clients using interfaces with JAX-RS annotations. Supports reactive types, custom headers, and error handling.

## Configuration

Common configuration properties:

```properties
# Base path for REST endpoints
quarkus.rest.path=/api

# Multipart configuration
quarkus.rest.multipart.input-part.default-charset=UTF-8

# Buffer sizes
quarkus.rest.input-buffer-size=10240
quarkus.rest.output-buffer-size=8191
```

See [Configuration Reference](reference/configuration.md) for complete options.

## Integration

Quarkus REST integrates with:
- **JSON**: Jackson or JSON-B for serialization
- **XML**: JAXB for XML support
- **Security**: OIDC, JWT, Elytron
- **Database**: Hibernate Reactive, Panache
- **Messaging**: Reactive Messaging, Kafka
- **Observability**: Micrometer, OpenTelemetry
- **Templates**: Qute template engine

## Performance Tips

1. **Use reactive types** (Uni/Multi) for I/O operations
2. **Avoid blocking** on I/O threads
3. **Use `@NonBlocking`** for fast, non-blocking operations
4. **Enable HTTP/2** for better performance
5. **Configure buffer sizes** for large payloads
6. **Use caching** annotations for cacheable responses

## Getting Help

- **Quarkus Guides**: https://quarkus.io/guides/
- **API Docs**: https://javadoc.io/doc/io.quarkus/quarkus-rest
- **Community**: https://quarkus.io/community/

## Next Steps

1. **Start with**: [Quick Start Guide](guides/quick-start.md)
2. **Learn reactive**: [Reactive Programming Guide](guides/reactive-programming.md)
3. **Secure your API**: [Security Setup Guide](guides/security-setup.md)
4. **Explore examples**: [Common Scenarios](examples/common-scenarios.md)
5. **Deep dive**: [Reference Documentation](reference/configuration.md)
