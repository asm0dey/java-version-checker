# Parameter Annotations

Quarkus REST provides specialized parameter annotations as simplified alternatives to standard JAX-RS annotations (`@PathParam`, `@QueryParam`, etc.). These `@Rest*` annotations offer the same functionality with shorter names and are part of the RESTEasy Reactive API.

## Capabilities

### Path Parameters

Extract values from URI path templates using `@RestPath`.

```java { .api }
package org.jboss.resteasy.reactive;

/**
 * Simplified annotation for path parameter binding.
 * Equivalent to JAX-RS @PathParam but with shorter name.
 *
 * Binds method parameter to a URI template variable.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@interface RestPath {
    /** Name of the URI template parameter. If not specified, uses the method parameter name. */
    String value() default "";
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.RestPath;
import jakarta.ws.rs.*;

@Path("/users")
public class UserResource {

    // Explicit parameter name
    @GET
    @Path("/{userId}")
    public User getUser(@RestPath("userId") long id) {
        return findUser(id);
    }

    // Implicit parameter name (uses parameter name "id")
    @GET
    @Path("/{id}")
    public User getUserById(@RestPath long id) {
        return findUser(id);
    }

    // Multiple path parameters
    @GET
    @Path("/{userId}/orders/{orderId}")
    public Order getUserOrder(
            @RestPath long userId,
            @RestPath long orderId) {
        return findOrder(userId, orderId);
    }

    // Nested paths
    @GET
    @Path("/{userId}/posts/{postId}/comments/{commentId}")
    public Comment getComment(
            @RestPath long userId,
            @RestPath long postId,
            @RestPath long commentId) {
        return findComment(userId, postId, commentId);
    }

    private User findUser(long id) { return new User(); }
    private Order findOrder(long userId, long orderId) { return new Order(); }
    private Comment findComment(long u, long p, long c) { return new Comment(); }
}

class User {}
class Order {}
class Comment {}
```

### Query Parameters

Extract values from URL query strings using `@RestQuery`.

```java { .api }
package org.jboss.resteasy.reactive;

/**
 * Simplified annotation for query parameter binding.
 * Equivalent to JAX-RS @QueryParam but with shorter name.
 *
 * Binds method parameter to a URI query parameter.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@interface RestQuery {
    /** Name of the query parameter. If not specified, uses the method parameter name. */
    String value() default "";
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.RestQuery;
import jakarta.ws.rs.*;
import java.util.List;

@Path("/products")
public class ProductResource {

    // Single query parameter with default value
    @GET
    public List<Product> getProducts(
            @RestQuery @DefaultValue("10") int limit,
            @RestQuery @DefaultValue("0") int offset) {
        return findProducts(limit, offset);
    }

    // Optional query parameter
    @GET
    @Path("/search")
    public List<Product> search(
            @RestQuery String query,
            @RestQuery String category,
            @RestQuery @DefaultValue("price") String sortBy) {
        return searchProducts(query, category, sortBy);
    }

    // Multiple values (comma-separated or repeated params)
    @GET
    @Path("/filter")
    public List<Product> filter(@RestQuery List<String> tags) {
        // Handles: ?tags=electronics&tags=sale
        // Or: ?tags=electronics,sale
        return filterByTags(tags);
    }

    // Boolean query parameters
    @GET
    @Path("/featured")
    public List<Product> getFeatured(
            @RestQuery @DefaultValue("false") boolean inStock,
            @RestQuery @DefaultValue("true") boolean featured) {
        return findFeatured(inStock, featured);
    }

    private List<Product> findProducts(int limit, int offset) {
        return List.of();
    }
    private List<Product> searchProducts(String q, String cat, String sort) {
        return List.of();
    }
    private List<Product> filterByTags(List<String> tags) {
        return List.of();
    }
    private List<Product> findFeatured(boolean inStock, boolean featured) {
        return List.of();
    }
}

class Product {}
```

### Form Parameters

Extract values from HTML form submissions using `@RestForm`.

```java { .api }
package org.jboss.resteasy.reactive;

/**
 * Simplified annotation for form parameter binding.
 * Equivalent to JAX-RS @FormParam but with shorter name.
 *
 * Binds method parameter to a form field value.
 * Used with application/x-www-form-urlencoded or multipart/form-data.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@interface RestForm {
    /** Name of the form field. If not specified, uses the method parameter name. */
    String value() default "";
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.RestForm;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/forms")
public class FormResource {

    // Simple form submission
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response login(
            @RestForm String username,
            @RestForm String password) {
        if (authenticate(username, password)) {
            return Response.ok("Login successful").build();
        }
        return Response.status(401).build();
    }

    // Form with optional fields
    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response register(
            @RestForm String username,
            @RestForm String email,
            @RestForm String password,
            @RestForm @DefaultValue("false") boolean newsletter) {
        createUser(username, email, password, newsletter);
        return Response.ok("Registration successful").build();
    }

    // Form POJO binding
    @POST
    @Path("/contact")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response contact(@BeanParam ContactForm form) {
        sendContactEmail(form);
        return Response.ok("Message sent").build();
    }

    private boolean authenticate(String u, String p) { return true; }
    private void createUser(String u, String e, String p, boolean n) {}
    private void sendContactEmail(ContactForm f) {}
}

// POJO for form binding
class ContactForm {
    @RestForm
    String name;

    @RestForm
    String email;

    @RestForm
    String message;
}
```

### Header Parameters

Extract values from HTTP headers using `@RestHeader`.

```java { .api }
package org.jboss.resteasy.reactive;

/**
 * Simplified annotation for HTTP header binding.
 * Equivalent to JAX-RS @HeaderParam but with shorter name.
 *
 * Binds method parameter to an HTTP header value.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@interface RestHeader {
    /** Name of the HTTP header. If not specified, uses the method parameter name. */
    String value() default "";
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.RestHeader;
import jakarta.ws.rs.*;

@Path("/api")
public class HeaderResource {

    // Single header
    @GET
    @Path("/protected")
    public Response getProtected(@RestHeader("Authorization") String token) {
        if (isValidToken(token)) {
            return Response.ok("Access granted").build();
        }
        return Response.status(401).build();
    }

    // Multiple headers
    @POST
    @Path("/data")
    public Response postData(
            @RestHeader("Content-Type") String contentType,
            @RestHeader("X-Request-ID") String requestId,
            @RestHeader @DefaultValue("unknown") String userAgent,
            String body) {
        logRequest(contentType, requestId, userAgent);
        return Response.ok().build();
    }

    // Optional header with default
    @GET
    @Path("/locale")
    public Response getLocalized(
            @RestHeader("Accept-Language") @DefaultValue("en-US") String language) {
        String content = getLocalizedContent(language);
        return Response.ok(content).build();
    }

    // Custom headers
    @GET
    @Path("/trace")
    public Response traced(
            @RestHeader("X-Trace-ID") String traceId,
            @RestHeader("X-Span-ID") String spanId) {
        processWithTracing(traceId, spanId);
        return Response.ok().build();
    }

    private boolean isValidToken(String token) { return true; }
    private void logRequest(String ct, String id, String ua) {}
    private String getLocalizedContent(String lang) { return ""; }
    private void processWithTracing(String t, String s) {}
}
```

### Cookie Parameters

Extract values from HTTP cookies using `@RestCookie`.

```java { .api }
package org.jboss.resteasy.reactive;

/**
 * Simplified annotation for cookie parameter binding.
 * Equivalent to JAX-RS @CookieParam but with shorter name.
 *
 * Binds method parameter to a cookie value.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@interface RestCookie {
    /** Name of the cookie. If not specified, uses the method parameter name. */
    String value() default "";
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.RestCookie;
import jakarta.ws.rs.*;

@Path("/session")
public class CookieResource {

    // Single cookie
    @GET
    @Path("/profile")
    public Response getProfile(@RestCookie("sessionId") String sessionId) {
        if (isValidSession(sessionId)) {
            return Response.ok(getUser SessionProfile(sessionId)).build();
        }
        return Response.status(401).build();
    }

    // Multiple cookies
    @GET
    @Path("/preferences")
    public Response getPreferences(
            @RestCookie("sessionId") String sessionId,
            @RestCookie @DefaultValue("light") String theme,
            @RestCookie @DefaultValue("en") String language) {
        Preferences prefs = loadPreferences(sessionId, theme, language);
        return Response.ok(prefs).build();
    }

    // Optional cookie
    @GET
    @Path("/tracking")
    public Response trackVisit(
            @RestCookie("trackingId") String trackingId) {
        if (trackingId != null) {
            recordVisit(trackingId);
        } else {
            String newId = createTrackingId();
            return Response.ok()
                .cookie(new NewCookie.Builder("trackingId")
                    .value(newId)
                    .maxAge(31536000)
                    .build())
                .build();
        }
        return Response.ok().build();
    }

    private boolean isValidSession(String sid) { return true; }
    private Object getUserSessionProfile(String sid) { return new Object(); }
    private Preferences loadPreferences(String sid, String theme, String lang) {
        return new Preferences();
    }
    private void recordVisit(String tid) {}
    private String createTrackingId() { return "abc123"; }
}

class Preferences {}
```

### Matrix Parameters

Extract values from URI matrix parameters using `@RestMatrix`.

```java { .api }
package org.jboss.resteasy.reactive;

/**
 * Simplified annotation for matrix parameter binding.
 * Equivalent to JAX-RS @MatrixParam but with shorter name.
 *
 * Binds method parameter to a URI matrix parameter.
 * Matrix parameters use semicolon syntax: /path;key=value;key2=value2
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@interface RestMatrix {
    /** Name of the matrix parameter. If not specified, uses the method parameter name. */
    String value() default "";
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.RestMatrix;
import jakarta.ws.rs.*;

@Path("/products")
public class MatrixParamResource {

    // Single matrix parameter
    // Example: /products;color=red
    @GET
    public List<Product> getProducts(
            @RestMatrix String color) {
        return findProductsByColor(color);
    }

    // Multiple matrix parameters
    // Example: /products;color=red;size=large;inStock=true
    @GET
    @Path("/filter")
    public List<Product> filterProducts(
            @RestMatrix String color,
            @RestMatrix String size,
            @RestMatrix @DefaultValue("false") boolean inStock) {
        return filterProducts(color, size, inStock);
    }

    // Matrix params on path segments
    // Example: /products/electronics;brand=sony;priceRange=100-500
    @GET
    @Path("/{category}")
    public List<Product> getCategoryProducts(
            @PathParam("category") String category,
            @RestMatrix String brand,
            @RestMatrix String priceRange) {
        return findInCategory(category, brand, priceRange);
    }

    private List<Product> findProductsByColor(String color) {
        return List.of();
    }
    private List<Product> filterProducts(String color, String size, boolean inStock) {
        return List.of();
    }
    private List<Product> findInCategory(String cat, String brand, String range) {
        return List.of();
    }
}
```

## Additional Annotations

### List Parameter Separator

Split List parameter values using a custom separator.

```java { .api }
package org.jboss.resteasy.reactive;

/**
 * Specifies a custom separator for splitting List parameter values.
 * By default, JAX-RS splits list parameters on commas.
 * This annotation allows using a different separator.
 *
 * Works with @QueryParam, @HeaderParam, @PathParam, etc.
 * The parameter type must be List or array.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@interface Separator {
    /** The separator string to use for splitting values */
    String value();
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.Separator;
import jakarta.ws.rs.*;
import java.util.List;

@Path("/items")
public class ItemResource {

    // Default comma separator: ?tags=java,kotlin,scala
    @GET
    @Path("/search")
    public List<Item> search(@QueryParam("tags") List<String> tags) {
        return searchByTags(tags);
    }

    // Pipe separator: ?tags=java|kotlin|scala
    @GET
    @Path("/filter-pipe")
    public List<Item> filterWithPipe(
            @QueryParam("tags")
            @Separator("|")
            List<String> tags) {
        return searchByTags(tags);
    }

    // Semicolon separator: ?categories=books;electronics;clothing
    @GET
    @Path("/by-categories")
    public List<Item> byCategories(
            @QueryParam("categories")
            @Separator(";")
            List<String> categories) {
        return findByCategories(categories);
    }

    // Space separator: ?keywords=quick brown fox
    @GET
    @Path("/keywords")
    public List<Item> searchKeywords(
            @QueryParam("keywords")
            @Separator(" ")
            List<String> keywords) {
        return searchByKeywords(keywords);
    }

    // Colon separator with path param: /items/1:2:3:4
    @GET
    @Path("/{ids}")
    public List<Item> getMultiple(
            @PathParam("ids")
            @Separator(":")
            List<Long> ids) {
        return findByIds(ids);
    }

    // Custom separator in header: X-User-Roles: admin~editor~viewer
    @GET
    @Path("/by-roles")
    public List<Item> byRoles(
            @HeaderParam("X-User-Roles")
            @Separator("~")
            List<String> roles) {
        return findByRoles(roles);
    }

    // Dash separator: ?range=1-5-10-20
    @GET
    @Path("/range")
    public List<Item> getRange(
            @QueryParam("range")
            @Separator("-")
            List<Integer> values) {
        return findByRange(values);
    }

    private List<Item> searchByTags(List<String> tags) { return List.of(); }
    private List<Item> findByCategories(List<String> categories) { return List.of(); }
    private List<Item> searchByKeywords(List<String> keywords) { return List.of(); }
    private List<Item> findByIds(List<Long> ids) { return List.of(); }
    private List<Item> findByRoles(List<String> roles) { return List.of(); }
    private List<Item> findByRange(List<Integer> values) { return List.of(); }
}

class Item {}
```

**Array Support:**

```java
// Works with arrays too
@GET
@Path("/array")
public Response processArray(
        @QueryParam("values")
        @Separator(";")
        String[] values) {
    return Response.ok(Arrays.asList(values)).build();
}
```

**Important Notes:**
- The separator is used to split the parameter value string
- Default JAX-RS behavior uses comma (,) as separator
- Custom separators override the default behavior
- Works with any parameter annotation (@QueryParam, @PathParam, @HeaderParam, etc.)
- Parameter type must be List or array

### Streaming Element Types

Specify content types for Server-Sent Events and streaming responses.

```java { .api }
package org.jboss.resteasy.reactive;

/**
 * Specifies the media type for Server-Sent Events elements.
 * Used with Multi<T> return types for SSE endpoints.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@interface RestSseElementType {
    /** Media type for SSE elements (e.g., "application/json") */
    String value();
}

/**
 * Specifies the media type for streaming response elements.
 * Used with Multi<T> return types for streaming endpoints.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@interface RestStreamElementType {
    /** Media type for stream elements */
    String value();
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.RestSseElementType;
import org.jboss.resteasy.reactive.RestStreamElementType;
import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.time.Duration;

@Path("/stream")
public class StreamingResource {

    // Server-Sent Events with JSON elements
    @GET
    @Path("/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestSseElementType(MediaType.APPLICATION_JSON)
    public Multi<Event> streamEvents() {
        return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
            .map(tick -> new Event("event-" + tick, System.currentTimeMillis()));
    }

    // Streaming with custom element type
    @GET
    @Path("/data")
    @Produces(MediaType.APPLICATION_JSON)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<DataPoint> streamData() {
        return Multi.createFrom().items(
            new DataPoint(1, 100.0),
            new DataPoint(2, 200.0),
            new DataPoint(3, 300.0)
        );
    }

    // Text streaming
    @GET
    @Path("/logs")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestSseElementType(MediaType.TEXT_PLAIN)
    public Multi<String> streamLogs() {
        return Multi.createFrom().items("Log entry 1", "Log entry 2", "Log entry 3");
    }
}

class Event {
    public String id;
    public long timestamp;
    public Event(String id, long timestamp) {
        this.id = id;
        this.timestamp = timestamp;
    }
}

class DataPoint {
    public int id;
    public double value;
    public DataPoint(int id, double value) {
        this.id = id;
        this.value = value;
    }
}
```

## Comparison with JAX-RS Annotations

The `@Rest*` annotations are functionally equivalent to standard JAX-RS annotations but with shorter names:

| RESTEasy Reactive | JAX-RS Standard | Purpose |
|-------------------|-----------------|---------|
| `@RestPath` | `@PathParam` | Path parameters |
| `@RestQuery` | `@QueryParam` | Query parameters |
| `@RestForm` | `@FormParam` | Form parameters |
| `@RestHeader` | `@HeaderParam` | HTTP headers |
| `@RestCookie` | `@CookieParam` | Cookies |
| `@RestMatrix` | `@MatrixParam` | Matrix parameters |

Both annotation styles can be used interchangeably in the same application. The `@Rest*` variants are provided for convenience and brevity.

```java
// These are equivalent:
@GET
@Path("/{id}")
public User get1(@RestPath long id) { return null; }

@GET
@Path("/{id}")
public User get2(@PathParam("id") long id) { return null; }
```
