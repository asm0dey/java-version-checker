# RestResponse API

`RestResponse` is a generic response wrapper providing full control over HTTP responses with type-safe entity handling and fluent builder API. It's an alternative to JAX-RS `Response` with better generic type support.

## Capabilities

### RestResponse Class

Type-safe HTTP response wrapper with complete control over status, headers, cookies, and entity.

```java { .api }
package org.jboss.resteasy.reactive;

/**
 * Generic response container with type-safe entity handling.
 * Use static factory methods or ResponseBuilder for construction.
 *
 * @param <T> The entity type
 */
abstract class RestResponse<T> implements AutoCloseable {
    // Status methods
    int getStatus();
    StatusType getStatusInfo();

    // Entity methods
    T getEntity();
    <OtherT> OtherT readEntity(Class<OtherT> entityType);
    <OtherT> OtherT readEntity(GenericType<OtherT> entityType);
    <OtherT> OtherT readEntity(Class<OtherT> entityType, Annotation[] annotations);
    <OtherT> OtherT readEntity(GenericType<OtherT> entityType, Annotation[] annotations);
    boolean hasEntity();
    boolean bufferEntity();
    void close();

    // Media type and content
    MediaType getMediaType();
    Locale getLanguage();
    int getLength();

    // Headers
    MultivaluedMap<String, Object> getHeaders();
    MultivaluedMap<String, Object> getMetadata(); // deprecated, use getHeaders()
    MultivaluedMap<String, String> getStringHeaders();
    String getHeaderString(String name);

    // HTTP metadata
    Set<String> getAllowedMethods();
    Map<String, NewCookie> getCookies();
    EntityTag getEntityTag();
    Date getDate();
    Date getLastModified();
    URI getLocation();

    // Links
    Set<Link> getLinks();
    boolean hasLink(String relation);
    Link getLink(String relation);
    Link.Builder getLinkBuilder(String relation);

    // Conversion
    Response toResponse();

    // Static factory methods
    static <T> RestResponse<T> fromResponse(RestResponse<T> response);
    static <IGNORED> RestResponse<IGNORED> status(StatusType status);
    static <T> RestResponse<T> status(StatusType status, T entity);
    static <IGNORED> RestResponse<IGNORED> status(Status status);
    static <T> RestResponse<T> status(Status status, T entity);
    static <IGNORED> RestResponse<IGNORED> status(int status);
    static <IGNORED> RestResponse<IGNORED> status(int status, String reasonPhrase);
    static <IGNORED> RestResponse<IGNORED> ok();
    static <T> RestResponse<T> ok(T entity);
    static <T> RestResponse<T> ok(T entity, MediaType type);
    static <T> RestResponse<T> ok(T entity, String type);
    static <T> RestResponse<T> ok(T entity, Variant variant);
    static <IGNORED> RestResponse<IGNORED> serverError();
    static <IGNORED> RestResponse<IGNORED> created(URI location);
    static <IGNORED> RestResponse<IGNORED> accepted();
    static <T> RestResponse<T> accepted(T entity);
    static <IGNORED> RestResponse<IGNORED> noContent();
    static <IGNORED> RestResponse<IGNORED> notModified();
    static <IGNORED> RestResponse<IGNORED> notModified(EntityTag tag);
    static <IGNORED> RestResponse<IGNORED> notModified(String tag);
    static <IGNORED> RestResponse<IGNORED> seeOther(URI location);
    static <IGNORED> RestResponse<IGNORED> temporaryRedirect(URI location);
    static <IGNORED> RestResponse<IGNORED> notAcceptable(List<Variant> variants);
    static <IGNORED> RestResponse<IGNORED> notFound();
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.RestResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

@Path("/api")
public class RestResponseExamples {

    // Simple OK response
    @GET
    @Path("/hello")
    public RestResponse<String> hello() {
        return RestResponse.ok("Hello, World!");
    }

    // OK response with media type
    @GET
    @Path("/json")
    public RestResponse<User> getUser() {
        User user = new User("Alice", 30);
        return RestResponse.ok(user, MediaType.APPLICATION_JSON);
    }

    // Custom status with entity
    @POST
    @Path("/users")
    public RestResponse<User> createUser(User user) {
        User saved = saveUser(user);
        URI location = URI.create("/api/users/" + saved.getId());
        return RestResponse.created(location).entity(saved).build();
    }

    // Error responses
    @GET
    @Path("/users/{id}")
    public RestResponse<User> getUserById(@PathParam("id") long id) {
        User user = findUser(id);
        if (user == null) {
            return RestResponse.notFound();
        }
        return RestResponse.ok(user);
    }

    // Custom status code
    @DELETE
    @Path("/users/{id}")
    public RestResponse<Void> deleteUser(@PathParam("id") long id) {
        boolean deleted = deleteUserById(id);
        if (deleted) {
            return RestResponse.noContent();
        }
        return RestResponse.status(404);
    }

    // Response with custom headers
    @GET
    @Path("/download")
    public RestResponse<byte[]> downloadFile() {
        byte[] data = readFileData();
        return RestResponse.ResponseBuilder.ok(data)
            .header("Content-Disposition", "attachment; filename=\"file.pdf\"")
            .type(MediaType.APPLICATION_OCTET_STREAM)
            .build();
    }

    // Accepted response for async processing
    @POST
    @Path("/jobs")
    public RestResponse<JobStatus> createJob(JobRequest request) {
        JobStatus status = new JobStatus("pending", "Job queued");
        return RestResponse.accepted(status);
    }

    // See Other redirect
    @POST
    @Path("/login")
    public RestResponse<Void> login(Credentials creds) {
        if (authenticate(creds)) {
            return RestResponse.seeOther(URI.create("/dashboard"));
        }
        return RestResponse.status(401);
    }

    // Conditional responses with ETags
    @GET
    @Path("/resource/{id}")
    public RestResponse<Resource> getResource(
            @PathParam("id") String id,
            @HeaderParam("If-None-Match") String ifNoneMatch) {
        Resource resource = loadResource(id);
        String etag = computeETag(resource);

        if (etag.equals(ifNoneMatch)) {
            return RestResponse.notModified(etag);
        }

        return RestResponse.ResponseBuilder.ok(resource)
            .tag(etag)
            .build();
    }

    private User saveUser(User user) { return user; }
    private User findUser(long id) { return null; }
    private boolean deleteUserById(long id) { return true; }
    private byte[] readFileData() { return new byte[0]; }
    private boolean authenticate(Credentials creds) { return true; }
    private Resource loadResource(String id) { return new Resource(); }
    private String computeETag(Resource r) { return "\"123\""; }
}

class User {
    private String name;
    private int age;
    private long id;

    public User() {}
    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public long getId() { return id; }
}

class JobStatus {
    public JobStatus(String status, String message) {}
}

class JobRequest {}
class Credentials {}
class Resource {}
```

### ResponseBuilder

Fluent builder API for constructing `RestResponse` instances with full control over all response attributes.

```java { .api }
package org.jboss.resteasy.reactive;

/**
 * Fluent builder for constructing RestResponse instances.
 * Obtain via RestResponse static methods or ResponseBuilder.create().
 *
 * @param <T> The entity type
 */
abstract class RestResponse.ResponseBuilder<T> {
    // Build method
    RestResponse<T> build();
    ResponseBuilder<T> clone();

    // Status methods
    <Ret extends T> ResponseBuilder<Ret> status(int status);
    <Ret extends T> ResponseBuilder<Ret> status(int status, String reasonPhrase);
    <Ret extends T> ResponseBuilder<Ret> status(StatusType status);
    ResponseBuilder<T> status(Status status);

    // Entity methods
    ResponseBuilder<T> entity(T entity);
    ResponseBuilder<T> entity(T entity, Annotation[] annotations);

    // Header methods
    ResponseBuilder<T> header(String name, Object value);
    ResponseBuilder<T> replaceAll(MultivaluedMap<String, Object> headers);

    // Content metadata
    ResponseBuilder<T> type(MediaType type);
    ResponseBuilder<T> type(String type);
    ResponseBuilder<T> language(String language);
    ResponseBuilder<T> language(Locale language);
    ResponseBuilder<T> encoding(String encoding);
    ResponseBuilder<T> variant(Variant variant);

    // HTTP metadata
    ResponseBuilder<T> allow(String... methods);
    ResponseBuilder<T> allow(Set<String> methods);
    ResponseBuilder<T> cacheControl(CacheControl cacheControl);
    ResponseBuilder<T> expires(Date expires);
    ResponseBuilder<T> lastModified(Date lastModified);
    ResponseBuilder<T> location(URI location);
    ResponseBuilder<T> contentLocation(URI location);
    ResponseBuilder<T> tag(EntityTag tag);
    ResponseBuilder<T> tag(String tag);
    ResponseBuilder<T> cookie(NewCookie... cookies);

    // Variants and links
    ResponseBuilder<T> variants(Variant... variants);
    ResponseBuilder<T> variants(List<Variant> variants);
    ResponseBuilder<T> links(Link... links);
    ResponseBuilder<T> link(URI uri, String rel);
    ResponseBuilder<T> link(String uri, String rel);

    // Static factory methods
    static <T> ResponseBuilder<T> fromResponse(RestResponse<T> response);
    static <IGNORED> ResponseBuilder<IGNORED> create(StatusType status);
    static <T> ResponseBuilder<T> create(StatusType status, T entity);
    static <IGNORED> ResponseBuilder<IGNORED> create(Status status);
    static <T> ResponseBuilder<T> create(Status status, T entity);
    static <IGNORED> ResponseBuilder<IGNORED> create(int status);
    static <IGNORED> ResponseBuilder<IGNORED> create(int status, String reasonPhrase);
    static <IGNORED> ResponseBuilder<IGNORED> ok();
    static <T> ResponseBuilder<T> ok(T entity);
    static <T> ResponseBuilder<T> ok(T entity, MediaType type);
    static <T> ResponseBuilder<T> ok(T entity, String type);
    static <T> ResponseBuilder<T> ok(T entity, Variant variant);
    static <IGNORED> ResponseBuilder<IGNORED> serverError();
    static <IGNORED> ResponseBuilder<IGNORED> created(URI location);
    static <IGNORED> ResponseBuilder<IGNORED> accepted();
    static <T> ResponseBuilder<T> accepted(T entity);
    static <IGNORED> ResponseBuilder<IGNORED> noContent();
    static <IGNORED> ResponseBuilder<IGNORED> notModified();
    static <IGNORED> ResponseBuilder<IGNORED> notModified(EntityTag tag);
    static <IGNORED> ResponseBuilder<IGNORED> notModified(String tag);
    static <IGNORED> ResponseBuilder<IGNORED> seeOther(URI location);
    static <IGNORED> ResponseBuilder<IGNORED> temporaryRedirect(URI location);
    static <IGNORED> ResponseBuilder<IGNORED> notAcceptable(List<Variant> variants);
    static <IGNORED> ResponseBuilder<IGNORED> notFound();
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestResponse.ResponseBuilder;
import jakarta.ws.rs.core.*;

public class ResponseBuilderExamples {

    // Complex response with multiple attributes
    public RestResponse<String> complexResponse() {
        return RestResponse.ResponseBuilder.<String>create(200)
            .entity("Success")
            .type(MediaType.TEXT_PLAIN)
            .language(Locale.ENGLISH)
            .header("X-Custom-Header", "value")
            .cookie(new NewCookie.Builder("sessionId")
                .value("abc123")
                .path("/")
                .maxAge(3600)
                .build())
            .build();
    }

    // Cache control
    public RestResponse<Data> withCacheControl() {
        CacheControl cc = new CacheControl();
        cc.setMaxAge(3600);
        cc.setPrivate(true);

        return RestResponse.ResponseBuilder.ok(new Data())
            .cacheControl(cc)
            .lastModified(new Date())
            .tag("\"v1.0\"")
            .build();
    }

    // Content negotiation
    public RestResponse<User> negotiatedResponse() {
        User user = new User();

        return RestResponse.ResponseBuilder.ok(user)
            .variants(
                new Variant(MediaType.APPLICATION_JSON_TYPE, Locale.ENGLISH, null),
                new Variant(MediaType.APPLICATION_XML_TYPE, Locale.ENGLISH, null)
            )
            .build();
    }

    // Links and HATEOAS
    public RestResponse<User> withLinks() {
        User user = new User();

        return RestResponse.ResponseBuilder.ok(user)
            .link("/api/users/123", "self")
            .link("/api/users/123/orders", "orders")
            .link("/api/users/123/profile", "profile")
            .build();
    }

    // Multiple headers
    public RestResponse<byte[]> downloadWithHeaders() {
        byte[] data = new byte[1024];

        return RestResponse.ResponseBuilder.ok(data)
            .header("Content-Disposition", "attachment; filename=\"report.pdf\"")
            .header("X-File-Size", "1024")
            .header("X-Generated-At", System.currentTimeMillis())
            .type(MediaType.APPLICATION_OCTET_STREAM)
            .build();
    }

    // Conditional response building
    public RestResponse<String> conditionalResponse(boolean includeDetails) {
        ResponseBuilder<String> builder = RestResponse.ResponseBuilder.ok("Basic info");

        if (includeDetails) {
            builder.header("X-Details", "Extended information");
            builder.cookie(new NewCookie.Builder("detail-level").value("full").build());
        }

        return builder.build();
    }

    // Cloning and modifying responses
    public RestResponse<Data> modifiedClone(RestResponse<Data> original) {
        return RestResponse.ResponseBuilder.fromResponse(original)
            .header("X-Modified", "true")
            .header("X-Modified-At", System.currentTimeMillis())
            .build();
    }
}

class Data {}
```

### Status Enum

Comprehensive enumeration of HTTP status codes with family classification.

```java { .api }
package org.jboss.resteasy.reactive;

/**
 * HTTP status codes enum implementing StatusType.
 * Complete set of standard HTTP status codes.
 */
enum RestResponse.Status implements StatusType {
    // 1xx Informational
    CONTINUE(100, "Continue"),
    SWITCHING_PROTOCOLS(101, "Switching Protocols"),

    // 2xx Success
    OK(200, "OK"),
    CREATED(201, "Created"),
    ACCEPTED(202, "Accepted"),
    NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information"),
    NO_CONTENT(204, "No Content"),
    RESET_CONTENT(205, "Reset Content"),
    PARTIAL_CONTENT(206, "Partial Content"),

    // 3xx Redirection
    MULTIPLE_CHOICES(300, "Multiple Choices"),
    MOVED_PERMANENTLY(301, "Moved Permanently"),
    FOUND(302, "Found"),
    SEE_OTHER(303, "See Other"),
    NOT_MODIFIED(304, "Not Modified"),
    USE_PROXY(305, "Use Proxy"),
    TEMPORARY_REDIRECT(307, "Temporary Redirect"),
    PERMANENT_REDIRECT(308, "Permanent Redirect"),

    // 4xx Client Error
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    PAYMENT_REQUIRED(402, "Payment Required"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    NOT_ACCEPTABLE(406, "Not Acceptable"),
    PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),
    REQUEST_TIMEOUT(408, "Request Timeout"),
    CONFLICT(409, "Conflict"),
    GONE(410, "Gone"),
    LENGTH_REQUIRED(411, "Length Required"),
    PRECONDITION_FAILED(412, "Precondition Failed"),
    PAYLOAD_TOO_LARGE(413, "Payload Too Large"),
    URI_TOO_LONG(414, "URI Too Long"),
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
    REQUESTED_RANGE_NOT_SATISFIABLE(416, "Requested Range Not Satisfiable"),
    EXPECTATION_FAILED(417, "Expectation Failed"),
    UPGRADE_REQUIRED(426, "Upgrade Required"),
    PRECONDITION_REQUIRED(428, "Precondition Required"),
    TOO_MANY_REQUESTS(429, "Too Many Requests"),
    REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large"),

    // 5xx Server Error
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    NOT_IMPLEMENTED(501, "Not Implemented"),
    BAD_GATEWAY(502, "Bad Gateway"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    GATEWAY_TIMEOUT(504, "Gateway Timeout"),
    HTTP_VERSION_NOT_SUPPORTED(505, "Http Version Not Supported"),
    NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required");

    // Methods
    Family getFamily();
    int getStatusCode();
    String getReasonPhrase();
    String toString();
    static Status fromStatusCode(int statusCode);
}
```

### StatusCode Constants

Integer constants for HTTP status codes.

```java { .api }
package org.jboss.resteasy.reactive;

/**
 * HTTP status code integer constants.
 * Use when numeric codes are needed instead of enum values.
 */
class RestResponse.StatusCode {
    // 1xx
    static final int CONTINUE = 100;
    static final int SWITCHING_PROTOCOLS = 101;

    // 2xx
    static final int OK = 200;
    static final int CREATED = 201;
    static final int ACCEPTED = 202;
    static final int NON_AUTHORITATIVE_INFORMATION = 203;
    static final int NO_CONTENT = 204;
    static final int RESET_CONTENT = 205;
    static final int PARTIAL_CONTENT = 206;

    // 3xx
    static final int MULTIPLE_CHOICES = 300;
    static final int MOVED_PERMANENTLY = 301;
    static final int FOUND = 302;
    static final int SEE_OTHER = 303;
    static final int NOT_MODIFIED = 304;
    static final int USE_PROXY = 305;
    static final int TEMPORARY_REDIRECT = 307;
    static final int PERMANENT_REDIRECT = 308;

    // 4xx
    static final int BAD_REQUEST = 400;
    static final int UNAUTHORIZED = 401;
    static final int PAYMENT_REQUIRED = 402;
    static final int FORBIDDEN = 403;
    static final int NOT_FOUND = 404;
    static final int METHOD_NOT_ALLOWED = 405;
    static final int NOT_ACCEPTABLE = 406;
    static final int PROXY_AUTHENTICATION_REQUIRED = 407;
    static final int REQUEST_TIMEOUT = 408;
    static final int CONFLICT = 409;
    static final int GONE = 410;
    static final int LENGTH_REQUIRED = 411;
    static final int PRECONDITION_FAILED = 412;
    static final int PAYLOAD_TOO_LARGE = 413;
    static final int URI_TOO_LONG = 414;
    static final int UNSUPPORTED_MEDIA_TYPE = 415;
    static final int REQUESTED_RANGE_NOT_SATISFIABLE = 416;
    static final int EXPECTATION_FAILED = 417;
    static final int UPGRADE_REQUIRED = 426;
    static final int PRECONDITION_REQUIRED = 428;
    static final int TOO_MANY_REQUESTS = 429;
    static final int REQUEST_HEADER_FIELDS_TOO_LARGE = 431;

    // 5xx
    static final int INTERNAL_SERVER_ERROR = 500;
    static final int NOT_IMPLEMENTED = 501;
    static final int BAD_GATEWAY = 502;
    static final int SERVICE_UNAVAILABLE = 503;
    static final int GATEWAY_TIMEOUT = 504;
    static final int HTTP_VERSION_NOT_SUPPORTED = 505;
    static final int NETWORK_AUTHENTICATION_REQUIRED = 511;
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestResponse.Status;
import org.jboss.resteasy.reactive.RestResponse.StatusCode;

public class StatusExamples {

    // Using Status enum
    public RestResponse<String> withStatusEnum() {
        return RestResponse.status(Status.CREATED, "Resource created");
    }

    // Using StatusCode constants
    public RestResponse<String> withStatusCode() {
        return RestResponse.status(StatusCode.ACCEPTED)
            .entity("Processing")
            .build();
    }

    // Status family checks
    public void handleResponse(RestResponse<?> response) {
        Status.Family family = response.getStatusInfo().getFamily();

        switch (family) {
            case SUCCESSFUL:
                System.out.println("Success: " + response.getStatus());
                break;
            case CLIENT_ERROR:
                System.out.println("Client error: " + response.getStatus());
                break;
            case SERVER_ERROR:
                System.out.println("Server error: " + response.getStatus());
                break;
        }
    }

    // Convert status code to enum
    public void processStatusCode(int code) {
        Status status = Status.fromStatusCode(code);
        if (status != null) {
            System.out.println("Status: " + status.getReasonPhrase());
        }
    }
}
```

## Type Safety Benefits

`RestResponse<T>` provides better type safety than `Response`:

```java
// With RestResponse - type-safe
@GET
public RestResponse<User> getUser() {
    User user = new User();
    return RestResponse.ok(user); // Compiler knows entity is User
}

// With Response - type unsafe
@GET
public Response getUserUnsafe() {
    User user = new User();
    return Response.ok(user).build(); // Generic Entity type lost
}
```

## Integration with Mutiny

`RestResponse` works seamlessly with Mutiny reactive types:

```java
import io.smallrye.mutiny.Uni;

@Path("/api")
public class ReactiveRestResponse {

    @GET
    @Path("/async")
    public Uni<RestResponse<Data>> getAsync() {
        return loadDataAsync()
            .map(data -> RestResponse.ok(data))
            .onFailure().recoverWithItem(
                ex -> RestResponse.serverError().<Data>build()
            );
    }

    @GET
    @Path("/conditional")
    public Uni<RestResponse<Data>> getConditional() {
        return checkAvailability()
            .chain(available -> {
                if (available) {
                    return loadDataAsync()
                        .map(RestResponse::ok);
                } else {
                    return Uni.createFrom().item(
                        RestResponse.<Data>status(503)
                    );
                }
            });
    }

    private Uni<Data> loadDataAsync() {
        return Uni.createFrom().item(new Data());
    }

    private Uni<Boolean> checkAvailability() {
        return Uni.createFrom().item(true);
    }
}
```
