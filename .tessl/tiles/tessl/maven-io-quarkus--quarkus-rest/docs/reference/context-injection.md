# Context Injection

Quarkus REST supports injecting various context types into REST resources, including standard JAX-RS types and Vert.x types. The framework provides seamless integration with CDI, allowing @Context to work as an alias for @Inject.

## JAX-RS Context Types

Standard JAX-RS context types can be injected using the @Context annotation.

### Context Annotation

```java { .api }
package jakarta.ws.rs.core;

@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@interface Context {}
```

### HttpHeaders

Access HTTP request headers and media type information.

```java { .api }
package jakarta.ws.rs.core;

interface HttpHeaders {
    List<String> getRequestHeader(String name);
    String getHeaderString(String name);
    MultivaluedMap<String, String> getRequestHeaders();
    List<MediaType> getAcceptableMediaTypes();
    List<Locale> getAcceptableLanguages();
    MediaType getMediaType();
    Locale getLanguage();
    Map<String, Cookie> getCookies();
}
```

**Usage**:

```java
@Path("/api/headers")
public class HeaderResource {

    @GET
    public Response getWithHeaders(@Context HttpHeaders headers) {
        String contentType = headers.getHeaderString(HttpHeaders.CONTENT_TYPE);
        String accept = headers.getHeaderString(HttpHeaders.ACCEPT);
        List<MediaType> acceptableTypes = headers.getAcceptableMediaTypes();
        Map<String, Cookie> cookies = headers.getCookies();

        return Response.ok()
            .entity(new HeaderInfo(contentType, accept, acceptableTypes))
            .build();
    }

    @POST
    public Response processWithLanguage(@Context HttpHeaders headers) {
        List<Locale> languages = headers.getAcceptableLanguages();
        Locale preferredLanguage = languages.isEmpty() ? Locale.ENGLISH : languages.get(0);

        String message = messageService.getMessage(preferredLanguage);
        return Response.ok(message).build();
    }
}
```

### UriInfo

Access URI and path information for the current request.

```java { .api }
package jakarta.ws.rs.core;

interface UriInfo {
    String getPath();
    String getPath(boolean decode);
    List<PathSegment> getPathSegments();
    URI getRequestUri();
    UriBuilder getRequestUriBuilder();
    URI getAbsolutePath();
    UriBuilder getAbsolutePathBuilder();
    URI getBaseUri();
    UriBuilder getBaseUriBuilder();
    MultivaluedMap<String, String> getPathParameters();
    MultivaluedMap<String, String> getPathParameters(boolean decode);
    MultivaluedMap<String, String> getQueryParameters();
    MultivaluedMap<String, String> getQueryParameters(boolean decode);
}
```

**Usage**:

```java
@Path("/api/resources")
public class ResourceInfoResource {

    @GET
    @Path("/{id}")
    public Response getResource(
        @PathParam("id") Long id,
        @Context UriInfo uriInfo
    ) {
        Resource resource = resourceService.findById(id);

        // Build self link
        URI selfUri = uriInfo.getAbsolutePathBuilder().build();

        // Build related links
        URI collectionUri = uriInfo.getBaseUriBuilder()
            .path(ResourceInfoResource.class)
            .build();

        return Response.ok(resource)
            .link(selfUri, "self")
            .link(collectionUri, "collection")
            .build();
    }

    @GET
    public Response search(@Context UriInfo uriInfo) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        String searchTerm = queryParams.getFirst("q");
        String sortBy = queryParams.getFirst("sort");

        List<Resource> results = resourceService.search(searchTerm, sortBy);
        return Response.ok(results).build();
    }
}
```

### SecurityContext

Access security information about the current request.

```java { .api }
package jakarta.ws.rs.core;

interface SecurityContext {
    Principal getUserPrincipal();
    boolean isUserInRole(String role);
    boolean isSecure();
    String getAuthenticationScheme();

    String BASIC_AUTH = "BASIC";
    String CLIENT_CERT_AUTH = "CLIENT_CERT";
    String DIGEST_AUTH = "DIGEST";
    String FORM_AUTH = "FORM";
}
```

**Usage**:

```java
@Path("/api/user")
public class UserResource {

    @GET
    @Path("/profile")
    public Response getProfile(@Context SecurityContext securityContext) {
        Principal principal = securityContext.getUserPrincipal();
        if (principal == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        String username = principal.getName();
        boolean isAdmin = securityContext.isUserInRole("admin");
        boolean isSecure = securityContext.isSecure();
        String authScheme = securityContext.getAuthenticationScheme();

        UserProfile profile = new UserProfile(username, isAdmin, isSecure, authScheme);
        return Response.ok(profile).build();
    }

    @GET
    @Path("/admin-check")
    public Response checkAdmin(@Context SecurityContext securityContext) {
        if (securityContext.isUserInRole("admin")) {
            return Response.ok("Admin access granted").build();
        }
        return Response.status(Response.Status.FORBIDDEN).build();
    }
}
```

### Request

Handle conditional requests and content negotiation.

```java { .api }
package jakarta.ws.rs.core;

interface Request {
    String getMethod();
    Variant selectVariant(List<Variant> variants);
    ResponseBuilder evaluatePreconditions(EntityTag eTag);
    ResponseBuilder evaluatePreconditions(Date lastModified);
    ResponseBuilder evaluatePreconditions(Date lastModified, EntityTag eTag);
}
```

**Usage**:

```java
@Path("/api/documents")
public class DocumentResource {

    @GET
    @Path("/{id}")
    public Response getDocument(
        @PathParam("id") Long id,
        @Context Request request
    ) {
        Document document = documentService.findById(id);

        EntityTag etag = new EntityTag(String.valueOf(document.getVersion()));
        Date lastModified = document.getLastModified();

        // Check if-none-match and if-modified-since
        ResponseBuilder builder = request.evaluatePreconditions(lastModified, etag);
        if (builder != null) {
            return builder.build(); // 304 Not Modified
        }

        return Response.ok(document)
            .tag(etag)
            .lastModified(lastModified)
            .build();
    }

    @GET
    @Path("/{id}/variants")
    public Response getDocumentVariant(
        @PathParam("id") Long id,
        @Context Request request
    ) {
        List<Variant> variants = Variant.mediaTypes(
            MediaType.APPLICATION_JSON_TYPE,
            MediaType.APPLICATION_XML_TYPE,
            MediaType.TEXT_PLAIN_TYPE
        ).build();

        Variant selectedVariant = request.selectVariant(variants);
        if (selectedVariant == null) {
            return Response.notAcceptable(variants).build();
        }

        Document document = documentService.findById(id);
        return Response.ok(document, selectedVariant.getMediaType()).build();
    }
}
```

## Vert.x Context Types

Quarkus REST allows injecting Vert.x types for low-level HTTP access.

### HttpServerRequest

Access the underlying Vert.x HTTP request.

```java { .api }
package io.vertx.core.http;

interface HttpServerRequest {
    String uri();
    String path();
    String query();
    HttpMethod method();
    MultiMap headers();
    MultiMap params();
    SocketAddress remoteAddress();
    SocketAddress localAddress();
    HttpVersion version();
    // ... and many more methods
}
```

**Usage**:

```java
import io.vertx.core.http.HttpServerRequest;

@Path("/api/request-info")
public class RequestInfoResource {

    @GET
    public Response getRequestInfo(@Context HttpServerRequest request) {
        String remoteIp = request.remoteAddress().host();
        int remotePort = request.remoteAddress().port();
        String userAgent = request.getHeader("User-Agent");
        String method = request.method().name();
        String path = request.path();

        RequestInfo info = new RequestInfo(remoteIp, remotePort, userAgent, method, path);
        return Response.ok(info).build();
    }

    @POST
    public Response handleRequest(@Context HttpServerRequest request) {
        // Access raw headers
        MultiMap headers = request.headers();
        String customHeader = headers.get("X-Custom-Header");

        // Check HTTP version
        HttpVersion version = request.version();

        return Response.ok().build();
    }
}
```

### HttpServerResponse

Access the underlying Vert.x HTTP response for low-level control.

```java { .api }
package io.vertx.core.http;

interface HttpServerResponse {
    HttpServerResponse setStatusCode(int statusCode);
    HttpServerResponse setStatusMessage(String statusMessage);
    HttpServerResponse putHeader(String name, String value);
    MultiMap headers();
    void end();
    void end(String chunk);
    // ... and many more methods
}
```

**Usage**:

```java
import io.vertx.core.http.HttpServerResponse;

@Path("/api/custom")
public class CustomResponseResource {

    @GET
    @Path("/headers")
    public String setCustomHeaders(@Context HttpServerResponse response) {
        // Set custom headers directly
        response.putHeader("X-Custom-Header", "CustomValue");
        response.putHeader("X-Request-Id", UUID.randomUUID().toString());
        response.putHeader("X-Response-Time", String.valueOf(System.currentTimeMillis()));

        return "Headers set";
    }

    @GET
    @Path("/redirect")
    public void customRedirect(@Context HttpServerResponse response) {
        // Manual redirect with custom status
        response.setStatusCode(302);
        response.putHeader("Location", "https://example.com");
        response.putHeader("Cache-Control", "no-cache");
        response.end();
    }
}
```

### RoutingContext

Access the full Vert.x routing context for complete request/response control.

```java { .api }
package io.vertx.ext.web;

interface RoutingContext {
    HttpServerRequest request();
    HttpServerResponse response();
    void next();
    void fail(int statusCode);
    void fail(Throwable throwable);
    Map<String, Object> data();
    String getBodyAsString();
    // ... and many more methods
}
```

**Usage**:

```java
import io.vertx.ext.web.RoutingContext;

@Path("/api/routing")
public class RoutingContextResource {

    @GET
    @Path("/info")
    public Response getRoutingInfo(@Context RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        // Access request data
        String path = request.path();
        String remoteAddress = request.remoteAddress().host();

        // Set response headers
        response.putHeader("X-Routing-Info", "processed");

        return Response.ok(new RoutingInfo(path, remoteAddress)).build();
    }

    @POST
    @Path("/data")
    public Response processWithContext(@Context RoutingContext context) {
        // Access request body
        String body = context.getBodyAsString();

        // Store data in context for other handlers
        context.data().put("processed", true);
        context.data().put("timestamp", System.currentTimeMillis());

        return Response.ok().build();
    }
}
```

## CDI Integration

Quarkus REST makes @Context work seamlessly with CDI through producer methods.

### QuarkusContextProducers

CDI producer providing context objects as injectable beans.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

@Singleton
class QuarkusContextProducers {
    @Produces
    @RequestScoped
    HttpServerResponse httpServerResponse();

    @Produces
    @ApplicationScoped
    Providers providers();

    @Produces
    @RequestScoped
    CloserImpl closer();

    @Disposes
    void closeCloser(CloserImpl closer);
}
```

This allows @Context and @Inject to be used interchangeably for context types.

## @Context vs @Inject

In Quarkus REST, @Context and @Inject are functionally equivalent for JAX-RS context types.

**Using @Context** (standard JAX-RS):

```java
@Path("/api/context")
public class ContextResource {

    @Context
    UriInfo uriInfo;

    @Context
    HttpHeaders headers;

    @GET
    public Response get() {
        String path = uriInfo.getPath();
        String accept = headers.getHeaderString(HttpHeaders.ACCEPT);
        return Response.ok().build();
    }
}
```

**Using @Inject** (CDI style):

```java
@Path("/api/inject")
public class InjectResource {

    @Inject
    UriInfo uriInfo;

    @Inject
    HttpHeaders headers;

    @GET
    public Response get() {
        String path = uriInfo.getPath();
        String accept = headers.getHeaderString(HttpHeaders.ACCEPT);
        return Response.ok().build();
    }
}
```

Both approaches work identically in Quarkus REST.

## Field vs Parameter Injection

Context types can be injected as fields or method parameters.

**Field Injection**:

```java
@Path("/api/field")
public class FieldInjectionResource {

    @Context
    UriInfo uriInfo;

    @Context
    SecurityContext securityContext;

    @GET
    public Response get() {
        // Use injected fields
        String path = uriInfo.getPath();
        Principal user = securityContext.getUserPrincipal();
        return Response.ok().build();
    }
}
```

**Parameter Injection**:

```java
@Path("/api/parameter")
public class ParameterInjectionResource {

    @GET
    public Response get(
        @Context UriInfo uriInfo,
        @Context SecurityContext securityContext
    ) {
        // Use injected parameters
        String path = uriInfo.getPath();
        Principal user = securityContext.getUserPrincipal();
        return Response.ok().build();
    }
}
```

Parameter injection is preferred when context is only needed in specific methods.

## Combining Context Types

Multiple context types can be injected together.

**Usage**:

```java
@Path("/api/combined")
public class CombinedContextResource {

    @GET
    public Response get(
        @Context UriInfo uriInfo,
        @Context HttpHeaders headers,
        @Context SecurityContext securityContext,
        @Context HttpServerRequest request
    ) {
        // JAX-RS context
        URI requestUri = uriInfo.getRequestUri();
        String accept = headers.getHeaderString(HttpHeaders.ACCEPT);
        Principal user = securityContext.getUserPrincipal();

        // Vert.x context
        String remoteIp = request.remoteAddress().host();

        CompleteRequestInfo info = new CompleteRequestInfo(
            requestUri, accept, user, remoteIp
        );

        return Response.ok(info).build();
    }
}
```

## Request-Scoped vs Application-Scoped

Context objects have different scopes:

- **Request-scoped**: UriInfo, HttpHeaders, SecurityContext, Request, HttpServerRequest, HttpServerResponse, RoutingContext
- **Application-scoped**: Providers

**Usage**:

```java
@ApplicationScoped
public class ContextService {

    @Inject
    UriInfo uriInfo; // Request-scoped - safe to inject

    public String getCurrentPath() {
        return uriInfo.getPath();
    }
}
```

Request-scoped contexts are safely injectable into singleton beans; they use CDI proxies.

## Providers Context

Access registered JAX-RS providers.

```java { .api }
package jakarta.ws.rs.ext;

interface Providers {
    <T> MessageBodyReader<T> getMessageBodyReader(
        Class<T> type,
        Type genericType,
        Annotation[] annotations,
        MediaType mediaType
    );

    <T> MessageBodyWriter<T> getMessageBodyWriter(
        Class<T> type,
        Type genericType,
        Annotation[] annotations,
        MediaType mediaType
    );

    <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> type);

    <T> ContextResolver<T> getContextResolver(Class<T> contextType, MediaType mediaType);
}
```

**Usage**:

```java
@Path("/api/providers")
public class ProvidersResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response processWithProviders(
        @Context Providers providers,
        String rawJson
    ) {
        // Get message body reader
        MessageBodyReader<MyData> reader = providers.getMessageBodyReader(
            MyData.class,
            MyData.class,
            new Annotation[0],
            MediaType.APPLICATION_JSON_TYPE
        );

        // Process data using reader
        return Response.ok().build();
    }
}
```
