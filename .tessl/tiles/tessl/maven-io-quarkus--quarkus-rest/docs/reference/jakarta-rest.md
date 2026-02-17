# Jakarta REST Standard Features

This document covers the standard Jakarta REST (JAX-RS) annotations and features supported by Quarkus REST.

## REST Resource Definition

### Path Annotation

Define the URI path for resource classes and methods.

```java { .api }
package jakarta.ws.rs;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@interface Path {
    String value();
}
```

**Usage**:

```java
@Path("/api/books")
public class BookResource {

    @GET
    @Path("/{id}")
    public Book getBook(@PathParam("id") Long id) {
        return bookService.findById(id);
    }
}
```

## HTTP Method Annotations

Map resource methods to HTTP verbs.

```java { .api }
package jakarta.ws.rs;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@HttpMethod("GET")
@interface GET {}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@HttpMethod("POST")
@interface POST {}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@HttpMethod("PUT")
@interface PUT {}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@HttpMethod("DELETE")
@interface DELETE {}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@HttpMethod("PATCH")
@interface PATCH {}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@HttpMethod("HEAD")
@interface HEAD {}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@HttpMethod("OPTIONS")
@interface OPTIONS {}
```

**Usage**:

```java
@POST
@Path("/create")
public Response createBook(Book book) {
    bookService.save(book);
    return Response.status(Response.Status.CREATED).build();
}

@DELETE
@Path("/{id}")
public Response deleteBook(@PathParam("id") Long id) {
    bookService.delete(id);
    return Response.noContent().build();
}
```

## Content Negotiation

### Produces Annotation

Specify the media types a resource method can produce.

```java { .api }
package jakarta.ws.rs;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@interface Produces {
    String[] value();
}
```

**Usage**:

```java
@GET
@Produces(MediaType.APPLICATION_JSON)
public List<Book> getBooksJson() {
    return bookService.findAll();
}

@GET
@Produces(MediaType.APPLICATION_XML)
public List<Book> getBooksXml() {
    return bookService.findAll();
}

@GET
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public List<Book> getBooks() {
    return bookService.findAll();
}
```

### Consumes Annotation

Specify the media types a resource method can consume.

```java { .api }
package jakarta.ws.rs;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@interface Consumes {
    String[] value();
}
```

**Usage**:

```java
@POST
@Consumes(MediaType.APPLICATION_JSON)
public Response createBook(Book book) {
    bookService.save(book);
    return Response.status(Response.Status.CREATED).build();
}

@POST
@Path("/xml")
@Consumes(MediaType.APPLICATION_XML)
public Response createBookXml(Book book) {
    bookService.save(book);
    return Response.status(Response.Status.CREATED).build();
}
```

## Parameter Binding

### Path Parameters

Extract values from URI path templates.

```java { .api }
package jakarta.ws.rs;

@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@interface PathParam {
    String value();
}
```

**Usage**:

```java
@GET
@Path("/books/{id}")
public Book getBook(@PathParam("id") Long id) {
    return bookService.findById(id);
}

@GET
@Path("/authors/{authorId}/books/{bookId}")
public Book getAuthorBook(
    @PathParam("authorId") Long authorId,
    @PathParam("bookId") Long bookId
) {
    return bookService.findByAuthorAndId(authorId, bookId);
}
```

### Query Parameters

Extract values from query string.

```java { .api }
package jakarta.ws.rs;

@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@interface QueryParam {
    String value();
}
```

**Usage**:

```java
@GET
@Path("/search")
public List<Book> searchBooks(
    @QueryParam("title") String title,
    @QueryParam("author") String author,
    @QueryParam("year") Integer year
) {
    return bookService.search(title, author, year);
}

// Query parameter with default value
@GET
@Path("/list")
public List<Book> listBooks(
    @QueryParam("page") @DefaultValue("0") int page,
    @QueryParam("size") @DefaultValue("10") int size
) {
    return bookService.findAll(page, size);
}
```

### Header Parameters

Extract values from HTTP headers.

```java { .api }
package jakarta.ws.rs;

@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@interface HeaderParam {
    String value();
}
```

**Usage**:

```java
@GET
public Response getWithHeader(
    @HeaderParam("Authorization") String authHeader,
    @HeaderParam("Accept-Language") String language
) {
    // Use header values
    return Response.ok().build();
}
```

### Cookie Parameters

Extract values from cookies.

```java { .api }
package jakarta.ws.rs;

@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@interface CookieParam {
    String value();
}
```

**Usage**:

```java
@GET
public Response getWithCookie(
    @CookieParam("sessionId") String sessionId
) {
    // Use cookie value
    return Response.ok().build();
}
```

### Form Parameters

Extract values from HTML form submissions.

```java { .api }
package jakarta.ws.rs;

@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@interface FormParam {
    String value();
}
```

**Usage**:

```java
@POST
@Path("/login")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public Response login(
    @FormParam("username") String username,
    @FormParam("password") String password
) {
    // Process login
    return Response.ok().build();
}
```

### Matrix Parameters

Extract values from matrix parameters in URI paths.

```java { .api }
package jakarta.ws.rs;

@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@interface MatrixParam {
    String value();
}
```

**Usage**:

```java
@GET
@Path("/books/{year}")
public List<Book> getBooksByYear(
    @PathParam("year") int year,
    @MatrixParam("author") String author,
    @MatrixParam("genre") String genre
) {
    // URI: /books/2023;author=Smith;genre=Fiction
    return bookService.findByYearAndCriteria(year, author, genre);
}
```

### Default Values

Provide default values for parameters when not present in the request.

```java { .api }
package jakarta.ws.rs;

@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@interface DefaultValue {
    String value();
}
```

**Usage**:

```java
@GET
public List<Book> getBooks(
    @QueryParam("page") @DefaultValue("1") int page,
    @QueryParam("size") @DefaultValue("20") int size,
    @QueryParam("sort") @DefaultValue("title") String sortBy
) {
    return bookService.findAll(page, size, sortBy);
}
```

## Context Injection

Inject context objects into resource methods or fields.

```java { .api }
package jakarta.ws.rs.core;

@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@interface Context {}
```

**Usage**:

```java
@GET
public Response getWithContext(
    @Context HttpHeaders headers,
    @Context UriInfo uriInfo,
    @Context SecurityContext securityContext
) {
    String acceptHeader = headers.getHeaderString(HttpHeaders.ACCEPT);
    URI requestUri = uriInfo.getRequestUri();
    Principal user = securityContext.getUserPrincipal();

    return Response.ok().build();
}
```

## Response Building

### Response Class

Build HTTP responses with fine-grained control.

```java { .api }
package jakarta.ws.rs.core;

class Response {
    static ResponseBuilder ok();
    static ResponseBuilder ok(Object entity);
    static ResponseBuilder status(Status status);
    static ResponseBuilder status(int status);
    static ResponseBuilder created(URI location);
    static ResponseBuilder noContent();
    static ResponseBuilder notModified();

    enum Status {
        OK(200),
        CREATED(201),
        ACCEPTED(202),
        NO_CONTENT(204),
        MOVED_PERMANENTLY(301),
        SEE_OTHER(303),
        NOT_MODIFIED(304),
        BAD_REQUEST(400),
        UNAUTHORIZED(401),
        FORBIDDEN(403),
        NOT_FOUND(404),
        METHOD_NOT_ALLOWED(405),
        NOT_ACCEPTABLE(406),
        CONFLICT(409),
        INTERNAL_SERVER_ERROR(500),
        SERVICE_UNAVAILABLE(503);

        int getStatusCode();
    }

    Object getEntity();
    int getStatus();
    MultivaluedMap<String, Object> getMetadata();
}

class ResponseBuilder {
    Response build();
    ResponseBuilder entity(Object entity);
    ResponseBuilder status(int status);
    ResponseBuilder header(String name, Object value);
    ResponseBuilder type(MediaType type);
    ResponseBuilder type(String type);
    ResponseBuilder cookie(NewCookie... cookies);
    ResponseBuilder expires(Date expires);
    ResponseBuilder lastModified(Date lastModified);
    ResponseBuilder location(URI location);
    ResponseBuilder tag(EntityTag tag);
}
```

**Usage**:

```java
@POST
public Response createBook(Book book) {
    Book created = bookService.save(book);
    URI location = URI.create("/api/books/" + created.getId());
    return Response.created(location).entity(created).build();
}

@GET
@Path("/{id}")
public Response getBook(@PathParam("id") Long id) {
    Book book = bookService.findById(id);
    if (book == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
    }
    return Response.ok(book)
        .header("X-Custom-Header", "value")
        .build();
}
```

## Application Class

Define application configuration and resource discovery.

```java { .api }
package jakarta.ws.rs.core;

@ApplicationPath(String value)
@interface ApplicationPath {}

abstract class Application {
    Set<Class<?>> getClasses();
    Set<Object> getSingletons();
    Map<String, Object> getProperties();
}
```

**Usage**:

```java
@ApplicationPath("/api")
public class MyApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new HashSet<>();
        resources.add(BookResource.class);
        resources.add(AuthorResource.class);
        return resources;
    }
}
```

## Context Types

### HttpHeaders

Access HTTP request headers.

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

### UriInfo

Access URI and path information.

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

### SecurityContext

Access security information.

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

## Media Types

Common media type constants.

```java { .api }
package jakarta.ws.rs.core;

class MediaType {
    static final String APPLICATION_JSON = "application/json";
    static final String APPLICATION_XML = "application/xml";
    static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
    static final String MULTIPART_FORM_DATA = "multipart/form-data";
    static final String TEXT_PLAIN = "text/plain";
    static final String TEXT_HTML = "text/html";
    static final String TEXT_XML = "text/xml";
    static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    static final String WILDCARD = "*/*";

    static final MediaType APPLICATION_JSON_TYPE;
    static final MediaType APPLICATION_XML_TYPE;
    static final MediaType TEXT_PLAIN_TYPE;
    static final MediaType TEXT_HTML_TYPE;
    static final MediaType WILDCARD_TYPE;

    String getType();
    String getSubtype();
    Map<String, String> getParameters();
    boolean isCompatible(MediaType other);
}
```

## Bean Validation

Validate request parameters and entities using Bean Validation annotations.

```java
import jakarta.validation.constraints.*;

@POST
public Response createBook(@Valid Book book) {
    bookService.save(book);
    return Response.status(Response.Status.CREATED).build();
}

public class Book {
    @NotNull
    @Size(min = 1, max = 200)
    private String title;

    @NotNull
    private String author;

    @Min(1000)
    @Max(9999)
    private Integer year;
}
```

Validation failures automatically result in HTTP 400 Bad Request responses.
