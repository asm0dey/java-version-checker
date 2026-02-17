# Declarative Response Configuration

Quarkus REST provides declarative annotations for setting HTTP response status codes and headers directly on resource methods, eliminating the need to return `Response` or `RestResponse` objects for simple cases.

## Capabilities

### Response Status Annotation

Declaratively set HTTP response status codes on resource methods.

```java { .api }
package org.jboss.resteasy.reactive;

/**
 * Sets the HTTP status code for successful resource method execution.
 * Only applies when the method completes without exception and
 * does not return Response or RestResponse.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface ResponseStatus {
    /** The HTTP status code to return */
    int value();
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.ResponseStatus;
import jakarta.ws.rs.*;

@Path("/users")
public class UserResource {

    // Return 201 Created instead of default 200 OK
    @POST
    @ResponseStatus(201)
    public User createUser(User user) {
        return userService.create(user);
    }

    // Return 204 No Content for delete
    @DELETE
    @Path("/{id}")
    @ResponseStatus(204)
    public void deleteUser(@PathParam("id") Long id) {
        userService.delete(id);
    }

    // Return 202 Accepted for async processing
    @POST
    @Path("/{id}/process")
    @ResponseStatus(202)
    public String processUser(@PathParam("id") Long id) {
        asyncProcessor.submit(id);
        return "Processing started";
    }

    // Return 200 OK (default, annotation not needed)
    @GET
    @Path("/{id}")
    public User getUser(@PathParam("id") Long id) {
        return userService.find(id);
    }
}
```

### Response Header Annotation

Declaratively add HTTP response headers to resource methods.

```java { .api }
package org.jboss.resteasy.reactive;

/**
 * Adds specified HTTP response headers when method completes successfully.
 * Only applies when the method completes without exception and
 * does not return Response or RestResponse.
 *
 * Do not use for Content-Type or Content-Length headers,
 * as those are set automatically by Quarkus REST.
 *
 * Repeatable annotation - use multiple @ResponseHeader annotations
 * or @ResponseHeader.List for multiple headers.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(ResponseHeader.List.class)
@interface ResponseHeader {
    /** The header name */
    String name();

    /** The header value(s) */
    String[] value();

    /** Container annotation for multiple @ResponseHeader annotations */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        ResponseHeader[] value();
    }
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.ResponseHeader;
import jakarta.ws.rs.*;

@Path("/api")
public class ApiResource {

    // Single header
    @GET
    @Path("/data")
    @ResponseHeader(name = "X-API-Version", value = "1.0")
    public Data getData() {
        return dataService.load();
    }

    // Multiple values for same header
    @GET
    @Path("/info")
    @ResponseHeader(name = "X-Custom-Header", value = {"value1", "value2"})
    public Info getInfo() {
        return infoService.load();
    }

    // Multiple different headers (repeatable annotation)
    @GET
    @Path("/report")
    @ResponseHeader(name = "X-Report-Version", value = "2.0")
    @ResponseHeader(name = "X-Generated-By", value = "Quarkus REST")
    @ResponseHeader(name = "X-Cache-Status", value = "MISS")
    public Report getReport() {
        return reportService.generate();
    }

    // Cache control headers
    @GET
    @Path("/static-content")
    @ResponseHeader(name = "Cache-Control", value = "max-age=3600, public")
    @ResponseHeader(name = "X-Content-Source", value = "database")
    public Content getStaticContent() {
        return contentService.getStatic();
    }

    // CORS headers
    @GET
    @Path("/public-data")
    @ResponseHeader(name = "Access-Control-Allow-Origin", value = "*")
    @ResponseHeader(name = "Access-Control-Allow-Methods", value = "GET, OPTIONS")
    public PublicData getPublicData() {
        return publicDataService.load();
    }

    // Security headers
    @GET
    @Path("/secure")
    @ResponseHeader(name = "X-Frame-Options", value = "DENY")
    @ResponseHeader(name = "X-Content-Type-Options", value = "nosniff")
    @ResponseHeader(name = "X-XSS-Protection", value = "1; mode=block")
    public SecureData getSecureData() {
        return secureDataService.load();
    }
}
```

### Combining Status and Headers

Use both annotations together for complete response configuration.

```java
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.ResponseHeader;

@Path("/resources")
public class ResourceEndpoint {

    // Created (201) with Location header
    @POST
    @ResponseStatus(201)
    @ResponseHeader(name = "Location", value = "/resources/{id}")
    public Resource createResource(Resource resource) {
        Resource created = resourceService.create(resource);
        // Location header will use placeholder {id} from path
        return created;
    }

    // Accepted (202) with custom tracking header
    @POST
    @Path("/batch")
    @ResponseStatus(202)
    @ResponseHeader(name = "X-Batch-ID", value = "${batch.id}")
    @ResponseHeader(name = "X-Processing-Status", value = "queued")
    public BatchRequest submitBatch(BatchRequest request) {
        return batchService.submit(request);
    }

    // No Content (204) with operation result header
    @PUT
    @Path("/{id}")
    @ResponseStatus(204)
    @ResponseHeader(name = "X-Updated-At", value = "${timestamp}")
    public void updateResource(@PathParam("id") Long id, Resource resource) {
        resourceService.update(id, resource);
    }
}
```

## Important Notes

### When Annotations Don't Apply

The `@ResponseStatus` and `@ResponseHeader` annotations are **ignored** in these cases:

1. **Method throws an exception**: Annotations don't apply, exception mappers handle the response
2. **Method returns `Response`**: Full control via `Response.ResponseBuilder`
3. **Method returns `RestResponse`**: Full control via `RestResponse.ResponseBuilder`

```java
@GET
@Path("/conditional")
@ResponseStatus(201)  // IGNORED - method returns Response
@ResponseHeader(name = "X-Custom", value = "value")  // IGNORED
public Response conditionalResponse() {
    // Annotations ignored because returning Response
    return Response.ok()
        .status(200)  // This takes precedence
        .header("X-Custom", "different-value")  // This takes precedence
        .build();
}

@GET
@Path("/error-case")
@ResponseStatus(200)  // IGNORED if exception thrown
@ResponseHeader(name = "X-Success", value = "true")  // IGNORED if exception thrown
public Data getData() {
    if (someError) {
        // Annotations ignored, exception mapper handles response
        throw new ServiceException("Data not available");
    }
    return data; // Annotations apply here
}
```

### Headers Not To Use

**Do not use** `@ResponseHeader` for:

- **Content-Type**: Set via `@Produces` annotation instead
- **Content-Length**: Automatically calculated by Quarkus REST
- **Transfer-Encoding**: Managed automatically

```java
// INCORRECT - Don't do this
@GET
@ResponseHeader(name = "Content-Type", value = "application/json")
public Data getData() { ... }

// CORRECT - Use @Produces instead
@GET
@Produces(MediaType.APPLICATION_JSON)
public Data getData() { ... }
```

## Comparison with Response API

Declarative annotations vs programmatic Response building:

**Declarative Approach** (Simple, concise):
```java
@POST
@ResponseStatus(201)
@ResponseHeader(name = "X-Request-ID", value = "${request.id}")
public User createUser(User user) {
    return userService.create(user);
}
```

**Programmatic Approach** (More control):
```java
@POST
public Response createUser(User user) {
    User created = userService.create(user);
    return Response.status(201)
        .entity(created)
        .header("X-Request-ID", UUID.randomUUID().toString())
        .build();
}
```

**When to use declarative annotations:**
- Fixed status codes (always 201, always 204, etc.)
- Static or simple header values
- Standard REST operations
- Cleaner, more readable code

**When to use Response/RestResponse:**
- Dynamic status codes based on logic
- Conditional headers
- Complex response building
- Need for additional Response methods (cookies, links, etc.)

## Integration with Other Features

Declarative response annotations work seamlessly with:

- **Exception Mapping**: Exceptions bypass annotations, mappers handle response
- **Filters**: Response filters can modify or add headers after annotations apply
- **Caching**: `@Cache` and `@NoCache` annotations (see [HTTP Caching](./caching.md))
- **Security**: Security annotations evaluated before response annotations
- **Content Negotiation**: `@Produces` determines Content-Type, not `@ResponseHeader`

```java
@GET
@Path("/cached")
@ResponseStatus(200)
@ResponseHeader(name = "X-Data-Source", value = "database")
@Cache(maxAge = 3600)  // Works together with @ResponseHeader
@RolesAllowed("user")  // Security checked first
@Produces(MediaType.APPLICATION_JSON)  // Content-Type set automatically
public Data getCachedData() {
    return dataService.load();
}
```
