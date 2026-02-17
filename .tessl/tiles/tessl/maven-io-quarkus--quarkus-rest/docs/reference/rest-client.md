# REST Client

Quarkus REST provides comprehensive REST client capabilities with declarative parameter annotations, custom exception mapping, redirect handling, and authentication support.

## Capabilities

### Client Query Parameters

Declaratively specify query parameters for REST client requests with support for computed values and optional parameters.

```java { .api }
package io.quarkus.rest.client.reactive;

/**
 * Specifies a query parameter to include in REST client requests.
 * Can be repeated to define multiple query parameters.
 * Values can be static strings or method references for computed values.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(ClientQueryParams.class)
@interface ClientQueryParam {
    /** The query parameter name */
    String name();

    /**
     * The query parameter value(s).
     * Can be literal strings or method reference names.
     * Referenced methods must be default methods (interface) or static methods.
     */
    String[] value();

    /** Whether this parameter is required (default: true) */
    boolean required() default true;
}

/**
 * Container annotation for multiple @ClientQueryParam annotations.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@interface ClientQueryParams {
    ClientQueryParam[] value();
}
```

**Usage Examples:**

```java
import io.quarkus.rest.client.reactive.ClientQueryParam;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api")
@RegisterRestClient(configKey = "api-client")
public interface ApiClient {

    // Static query parameter
    @GET
    @Path("/data")
    @ClientQueryParam(name = "apiKey", value = "secret-key-123")
    Response getData();

    // Multiple static parameters
    @GET
    @Path("/search")
    @ClientQueryParam(name = "version", value = "v2")
    @ClientQueryParam(name = "format", value = "json")
    List<Result> search(@QueryParam("q") String query);

    // Computed query parameter using method reference
    @GET
    @Path("/secured")
    @ClientQueryParam(name = "token", value = "{generateToken}")
    Response getSecured();

    // Default method for computed value
    default String generateToken() {
        return "Bearer " + System.getenv("AUTH_TOKEN");
    }

    // Optional parameter
    @GET
    @Path("/optional")
    @ClientQueryParam(name = "debug", value = "false", required = false)
    Response getWithOptionalDebug();

    // Multiple values for same parameter
    @GET
    @Path("/filter")
    @ClientQueryParam(name = "tags", value = {"java", "quarkus"})
    List<Item> getFiltered();

    // Computed value via static method
    @GET
    @Path("/timestamped")
    @ClientQueryParam(name = "ts", value = "{currentTimestamp}")
    Response getTimestamped();

    static String currentTimestamp() {
        return String.valueOf(System.currentTimeMillis());
    }
}

class Result {}
class Item {}
```

### Client Form Parameters

Declaratively specify form parameters for REST client requests with support for computed values.

```java { .api }
package io.quarkus.rest.client.reactive;

/**
 * Specifies a form parameter to include in REST client requests.
 * Can be repeated to define multiple form parameters.
 * Values can be static strings or method references for computed values.
 * Used with application/x-www-form-urlencoded content type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(ClientFormParams.class)
@interface ClientFormParam {
    /** The form parameter name */
    String name();

    /**
     * The form parameter value(s).
     * Can be literal strings or method reference names.
     * Referenced methods must be default methods (interface) or static methods.
     */
    String[] value();

    /** Whether this parameter is required (default: true) */
    boolean required() default true;
}

/**
 * Container annotation for multiple @ClientFormParam annotations.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@interface ClientFormParams {
    ClientFormParam[] value();
}
```

**Usage Examples:**

```java
import io.quarkus.rest.client.reactive.ClientFormParam;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/auth")
@RegisterRestClient(configKey = "auth-client")
public interface AuthClient {

    // Static form parameters
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @ClientFormParam(name = "grant_type", value = "password")
    @ClientFormParam(name = "client_id", value = "my-app")
    Response login(
        @FormParam("username") String username,
        @FormParam("password") String password
    );

    // Computed form parameter
    @POST
    @Path("/token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @ClientFormParam(name = "client_secret", value = "{getClientSecret}")
    Response getToken(@FormParam("code") String authCode);

    default String getClientSecret() {
        return System.getenv("CLIENT_SECRET");
    }

    // Multiple values
    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @ClientFormParam(name = "roles", value = {"user", "subscriber"})
    Response register(@FormParam("email") String email);

    // Optional parameter
    @POST
    @Path("/update")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @ClientFormParam(name = "source", value = "api", required = false)
    Response updateProfile(@FormParam("name") String name);
}
```

### Client Exception Mapping

Map HTTP error responses to custom exceptions in REST clients.

```java { .api }
package io.quarkus.rest.client.reactive;

/**
 * Defines a custom exception mapper for REST client error responses.
 * Annotated method must be static and return a RuntimeException subclass.
 * The mapper is invoked when the server returns an error status code.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface ClientExceptionMapper {
    /** Priority of this mapper (default: Priorities.USER) */
    int priority() default 5000;
}
```

**Supported Exception Mapper Parameters:**
- `Response` - The HTTP response
- `Method` - The invoked client method
- `URI` - The request URI
- `Map<String, Object>` - Configuration properties map
- `MultivaluedMap<String, String>` - Response headers

**Usage Examples:**

```java
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import java.lang.reflect.Method;
import java.net.URI;

@Path("/api")
@RegisterRestClient(configKey = "api-client")
public interface ApiClient {

    @GET
    @Path("/data/{id}")
    Data getData(@PathParam("id") Long id);

    // Simple exception mapper
    @ClientExceptionMapper
    static RuntimeException toException(Response response) {
        if (response.getStatus() == 404) {
            return new NotFoundException("Resource not found");
        }
        if (response.getStatus() == 401) {
            return new UnauthorizedException("Authentication required");
        }
        if (response.getStatus() >= 500) {
            return new ServerException("Server error: " + response.getStatus());
        }
        return new ApiException("API error: " + response.getStatus());
    }

    // Advanced exception mapper with multiple parameters
    @ClientExceptionMapper
    static RuntimeException handleError(Response response, Method method, URI uri) {
        String methodName = method.getName();
        int status = response.getStatus();
        String body = response.readEntity(String.class);

        if (status == 404) {
            return new NotFoundException(
                "Resource not found: " + uri + " (method: " + methodName + ")"
            );
        }
        if (status == 400) {
            return new BadRequestException(
                "Bad request: " + body + " (method: " + methodName + ")"
            );
        }
        return new ApiException("Error calling " + methodName + ": " + status);
    }

    // Priority-based exception mapper
    @ClientExceptionMapper(priority = 1000)  // Higher priority
    static RuntimeException highPriorityMapper(Response response) {
        // This mapper runs before default priority mappers
        if (response.getStatus() == 429) {
            return new RateLimitException("Rate limit exceeded");
        }
        return null;  // Continue to next mapper
    }
}

class Data {}
class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}
class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) { super(message); }
}
class ServerException extends RuntimeException {
    public ServerException(String message) { super(message); }
}
class ApiException extends RuntimeException {
    public ApiException(String message) { super(message); }
}
class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}
class RateLimitException extends RuntimeException {
    public RateLimitException(String message) { super(message); }
}
```

### Client Redirect Handling

Define custom redirect handling logic for REST client requests.

```java { .api }
package io.quarkus.rest.client.reactive;

/**
 * Defines a custom redirect handler for REST client redirects.
 * Annotated method must be static and return a URI.
 * Takes a single Response parameter representing the redirect response.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface ClientRedirectHandler {
    /** Priority of this handler (default: Priorities.USER) */
    int priority() default 5000;
}
```

**Usage Examples:**

```java
import io.quarkus.rest.client.reactive.ClientRedirectHandler;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import java.net.URI;

@Path("/api")
@RegisterRestClient(configKey = "api-client")
public interface ApiClient {

    @GET
    @Path("/resource")
    Resource getResource();

    // Custom redirect handler
    @ClientRedirectHandler
    static URI handleRedirect(Response response) {
        URI location = response.getLocation();

        // Log redirect
        System.out.println("Redirecting to: " + location);

        // Modify redirect URL if needed
        if (location.getHost().equals("old-domain.com")) {
            return URI.create(location.toString().replace(
                "old-domain.com", "new-domain.com"
            ));
        }

        // Follow redirect as-is
        return location;
    }

    // Conditional redirect handler
    @ClientRedirectHandler
    static URI conditionalRedirect(Response response) {
        URI location = response.getLocation();
        int status = response.getStatus();

        // Only follow 302 redirects
        if (status == 302) {
            return location;
        }

        // Don't follow other redirects
        return null;
    }

    // Priority-based redirect handler
    @ClientRedirectHandler(priority = 1000)
    static URI highPriorityRedirect(Response response) {
        // This handler runs first
        URI location = response.getLocation();

        // Block redirects to external domains
        if (!location.getHost().endsWith("mycompany.com")) {
            throw new SecurityException("Redirect to external domain blocked");
        }

        return location;
    }
}

class Resource {}
```

### HTTP Basic Authentication

Configure HTTP Basic Authentication for REST client requests.

```java { .api }
package io.quarkus.rest.client.reactive;

/**
 * Configures HTTP Basic Authentication for a REST client.
 * Supports property placeholders for externalized configuration.
 * Applied at class or method level.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@interface ClientBasicAuth {
    /**
     * Username for Basic Auth.
     * Supports property resolution: ${property.name}
     */
    String username();

    /**
     * Password for Basic Auth.
     * Supports property resolution: ${property.name}
     */
    String password();
}
```

**Usage Examples:**

```java
import io.quarkus.rest.client.reactive.ClientBasicAuth;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

// Class-level authentication (applies to all methods)
@Path("/api")
@RegisterRestClient(configKey = "api-client")
@ClientBasicAuth(username = "admin", password = "secret")
public interface ApiClient {

    @GET
    @Path("/data")
    Data getData();

    @POST
    @Path("/update")
    Response updateData(Data data);
}

// Property-based authentication
@Path("/secure")
@RegisterRestClient(configKey = "secure-client")
@ClientBasicAuth(username = "${api.username}", password = "${api.password}")
public interface SecureClient {

    @GET
    @Path("/info")
    Info getInfo();
}

// Method-level authentication (overrides class-level)
@Path("/mixed")
@RegisterRestClient(configKey = "mixed-client")
@ClientBasicAuth(username = "default-user", password = "default-pass")
public interface MixedAuthClient {

    // Uses class-level auth
    @GET
    @Path("/public")
    Data getPublicData();

    // Overrides with different credentials
    @GET
    @Path("/admin")
    @ClientBasicAuth(username = "admin", password = "admin-pass")
    Data getAdminData();
}

class Data {}
class Info {}
```

**Configuration via application.properties:**

```properties
# Property-based authentication
api.username=my-username
api.password=my-password
```

### Not Body Parameter Marker

Mark method parameters that should not be treated as the request body.

```java { .api }
package io.quarkus.rest.client.reactive;

/**
 * Marks a parameter as not representing the request body.
 * Useful when multiple unannotated parameters are present
 * and only one should be treated as the body.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@interface NotBody {
}
```

**Usage Examples:**

```java
import io.quarkus.rest.client.reactive.NotBody;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api")
@RegisterRestClient(configKey = "api-client")
public interface ApiClient {

    // Without @NotBody, this would be ambiguous
    @POST
    @Path("/process")
    Response process(
        @NotBody String metadata,  // Not the body
        Data data                   // This is the body
    );

    // Multiple non-body parameters
    @POST
    @Path("/complex")
    Response complexOperation(
        @NotBody String operation,
        @NotBody boolean dryRun,
        Payload payload  // This is the body
    );

    // Combining with other annotations
    @POST
    @Path("/upload")
    Response upload(
        @HeaderParam("Content-Type") @NotBody String contentType,
        @QueryParam("filename") @NotBody String filename,
        byte[] fileData  // This is the body
    );
}

class Data {}
class Payload {}
```

### Header Filler Interface

Programmatically add headers to REST client requests.

```java { .api }
package io.quarkus.rest.client.reactive;

/**
 * Interface for programmatically adding headers to client requests.
 * Implement this interface and register as a CDI bean to customize headers.
 */
@FunctionalInterface
interface HeaderFiller {
    /**
     * Add headers to the request.
     *
     * @param headers The headers map to populate
     */
    void addHeaders(MultivaluedMap<String, String> headers);
}
```

**Usage Example:**

```java
import io.quarkus.rest.client.reactive.HeaderFiller;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MultivaluedMap;

@ApplicationScoped
public class CustomHeaderFiller implements HeaderFiller {

    @Override
    public void addHeaders(MultivaluedMap<String, String> headers) {
        // Add custom headers to all client requests
        headers.add("X-Request-ID", generateRequestId());
        headers.add("X-Client-Version", "1.0.0");
        headers.add("X-Timestamp", String.valueOf(System.currentTimeMillis()));

        // Add authentication token
        String token = getAuthToken();
        if (token != null) {
            headers.add("Authorization", "Bearer " + token);
        }
    }

    private String generateRequestId() {
        return java.util.UUID.randomUUID().toString();
    }

    private String getAuthToken() {
        return System.getenv("AUTH_TOKEN");
    }
}
```

### Computed Parameter Context

Context object passed to computed parameter methods for accessing request metadata.

```java { .api }
package io.quarkus.rest.client.reactive;

/**
 * Context object for computed client parameters.
 * Provides access to the client method and URI being invoked.
 * Can be injected as a parameter in computed value methods.
 */
interface ComputedParamContext {
    /** The client interface method being invoked */
    Method getMethod();

    /** The target URI for the request */
    URI getUri();
}
```

**Usage Example:**

```java
import io.quarkus.rest.client.reactive.ClientQueryParam;
import io.quarkus.rest.client.reactive.ComputedParamContext;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api")
@RegisterRestClient(configKey = "api-client")
public interface ApiClient {

    @GET
    @Path("/data")
    @ClientQueryParam(name = "signature", value = "{computeSignature}")
    Data getData(@QueryParam("id") String id);

    // Computed parameter method with context
    default String computeSignature(ComputedParamContext context) {
        String method = context.getMethod().getName();
        String uri = context.getUri().toString();

        // Generate signature based on method and URI
        String toSign = method + ":" + uri;
        return generateHmacSignature(toSign);
    }

    private String generateHmacSignature(String data) {
        // HMAC signature generation logic
        return "signature-" + data.hashCode();
    }
}

class Data {}
```

### Server-Sent Events (SSE) Support

Consume Server-Sent Events from REST clients with filtering support.

```java { .api }
package org.jboss.resteasy.reactive.client;

/**
 * Represents an SSE (Server-Sent Events) response from the server.
 * Generic type T represents the type of the event data.
 */
interface SseEvent<T> {
    /**
     * Get event identifier.
     * Contains value of SSE "id" field.
     * This field is optional - may return null if not specified.
     *
     * @return the event id or null if not set
     */
    String id();

    /**
     * Get event name.
     * Contains value of SSE "event" field.
     * This field is optional - may return null if not specified.
     *
     * @return the event name or null if not set
     */
    String name();

    /**
     * Get comment string that accompanies the event.
     * Contains value of the comment associated with SSE event.
     * This field is optional - may return null if not specified.
     *
     * @return the comment associated with the event or null if not set
     */
    String comment();

    /**
     * Get event data.
     *
     * @return the event data of type T
     */
    T data();
}
```

```java { .api }
package org.jboss.resteasy.reactive.client;

import java.util.function.Predicate;

/**
 * Filters SSE events on the client side.
 * Used when not all SSE events streamed from the server should be included
 * in the event stream returned by the client.
 * Implementations MUST contain a no-args constructor.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface SseEventFilter {
    /**
     * Predicate which decides whether an event should be included
     * in the event stream returned by the client.
     *
     * @return the predicate class (must have no-args constructor)
     */
    Class<? extends Predicate<SseEvent<String>>> value();
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.client.SseEvent;
import org.jboss.resteasy.reactive.client.SseEventFilter;
import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import java.util.function.Predicate;

@Path("/events")
@RegisterRestClient(configKey = "events-client")
public interface EventsClient {

    // Consume SSE without filtering
    @GET
    @Path("/stream")
    @Produces("text/event-stream")
    Multi<SseEvent<String>> streamEvents();

    // Consume SSE with type conversion
    @GET
    @Path("/notifications")
    @Produces("text/event-stream")
    Multi<SseEvent<Notification>> streamNotifications();

    // Filter SSE events
    @GET
    @Path("/filtered")
    @Produces("text/event-stream")
    @SseEventFilter(ImportantEventsFilter.class)
    Multi<SseEvent<String>> streamImportantEvents();

    // Multiple filters can be chained
    @GET
    @Path("/alerts")
    @Produces("text/event-stream")
    @SseEventFilter(CriticalEventsFilter.class)
    Multi<SseEvent<Alert>> streamCriticalAlerts();
}

// Filter implementation - must have no-args constructor
class ImportantEventsFilter implements Predicate<SseEvent<String>> {
    @Override
    public boolean test(SseEvent<String> event) {
        // Only include events with "important" name
        return "important".equals(event.name());
    }
}

class CriticalEventsFilter implements Predicate<SseEvent<String>> {
    @Override
    public boolean test(SseEvent<String> event) {
        // Filter by event data content
        String data = event.data();
        return data != null && data.contains("CRITICAL");
    }
}

// Example: Processing SSE events
@ApplicationScoped
public class EventConsumer {

    @Inject
    @RestClient
    EventsClient eventsClient;

    public void consumeEvents() {
        eventsClient.streamEvents()
            .subscribe().with(
                event -> {
                    System.out.println("Event ID: " + event.id());
                    System.out.println("Event name: " + event.name());
                    System.out.println("Event data: " + event.data());
                    System.out.println("Comment: " + event.comment());
                },
                failure -> System.err.println("Error: " + failure.getMessage())
            );
    }
}

class Notification {}
class Alert {}
```

### TLS/SSL Configuration

Configure TLS/SSL settings for REST clients including key stores, trust stores, and hostname verification.

```java { .api }
package org.jboss.resteasy.reactive.client;

import java.security.KeyStore;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.SSLOptions;
import io.vertx.core.net.TrustOptions;

/**
 * Interface for configuring TLS/SSL settings for REST clients.
 * Provides access to key stores, trust stores, SSL contexts, and various TLS options.
 */
interface TlsConfig {
    /**
     * Returns the key store.
     *
     * @return the key store if configured, null otherwise
     */
    KeyStore getKeyStore();

    /**
     * Returns the key store options.
     *
     * @return the key store options if configured, null otherwise
     */
    KeyCertOptions getKeyStoreOptions();

    /**
     * Returns the trust store.
     *
     * @return the trust store if configured, null otherwise
     */
    KeyStore getTrustStore();

    /**
     * Returns the trust store options.
     *
     * @return the trust store options if configured, null otherwise
     */
    TrustOptions getTrustStoreOptions();

    /**
     * Returns the (Vert.x) SSL options.
     *
     * @return the Vert.x SSLOptions, null if not configured
     */
    SSLOptions getSSLOptions();

    /**
     * Creates and returns the SSL Context.
     *
     * @return the SSLContext, null if not configured
     * @throws Exception if the SSL Context cannot be created
     */
    SSLContext createSSLContext() throws Exception;

    /**
     * Returns the hostname verification algorithm for this configuration.
     * "NONE" means no hostname verification.
     *
     * @return the hostname verification algorithm, "NONE" indicates no verification
     */
    Optional<String> getHostnameVerificationAlgorithm();

    /**
     * Returns whether the key store is configured to use SNI.
     * When SNI is used, the client indicates the server name during the TLS handshake,
     * allowing the server to select the right certificate.
     *
     * @return true if SNI (Server Name Indication) is enabled, false otherwise
     */
    boolean usesSni();

    /**
     * Returns whether the trust store is configured to trust all certificates.
     *
     * @return true if configured to trust all certificates, false otherwise
     */
    boolean isTrustAll();
}
```

**Configuration via application.properties:**

```properties
# TLS/SSL Configuration for REST Client
quarkus.rest-client.my-api.trust-store=/path/to/truststore.jks
quarkus.rest-client.my-api.trust-store-password=secret
quarkus.rest-client.my-api.trust-store-type=JKS

quarkus.rest-client.my-api.key-store=/path/to/keystore.jks
quarkus.rest-client.my-api.key-store-password=secret
quarkus.rest-client.my-api.key-store-type=JKS

# Hostname verification
quarkus.rest-client.my-api.hostname-verifier=NONE  # Disable for testing only

# Trust all certificates (development only - not recommended for production)
quarkus.rest-client.my-api.trust-all=true

# Enable SNI
quarkus.rest-client.my-api.sni=true
```

**Usage Example:**

```java
import org.jboss.resteasy.reactive.client.TlsConfig;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/secure")
@RegisterRestClient(configKey = "secure-api")
public interface SecureApiClient {

    @GET
    @Path("/data")
    Data getSecureData();
}
```

**Note:** TLS configuration is typically done via application.properties rather than programmatically. The `TlsConfig` interface provides access to the configured TLS settings when needed for advanced use cases.

### Client Exception Handling

Exception type for REST client errors that distinguishes client-side exceptions from server-side exceptions.

```java { .api }
package org.jboss.resteasy.reactive;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * A WebApplicationException that indicates the exception was generated by the client.
 * Implements ResteasyReactiveClientProblem marker interface to stop the server
 * from treating it as a server-side WebApplicationException.
 */
class ClientWebApplicationException extends WebApplicationException {
    // No-args constructor
    public ClientWebApplicationException() {}

    // Message constructors
    public ClientWebApplicationException(String message) {}

    // Response constructors
    public ClientWebApplicationException(Response response) {}
    public ClientWebApplicationException(String message, Response response) {}

    // Status code constructors
    public ClientWebApplicationException(int status) {}
    public ClientWebApplicationException(String message, int status) {}
    public ClientWebApplicationException(Response.Status status) {}
    public ClientWebApplicationException(String message, Response.Status status) {}

    // Cause constructors
    public ClientWebApplicationException(Throwable cause) {}
    public ClientWebApplicationException(String message, Throwable cause) {}

    // Cause + Response constructors
    public ClientWebApplicationException(Throwable cause, Response response) {}
    public ClientWebApplicationException(String message, Throwable cause, Response response) {}

    // Cause + Status constructors
    public ClientWebApplicationException(Throwable cause, int status) {}
    public ClientWebApplicationException(String message, Throwable cause, int status) {}
    public ClientWebApplicationException(Throwable cause, Response.Status status) {}
    public ClientWebApplicationException(String message, Throwable cause, Response.Status status) {}
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api")
@RegisterRestClient(configKey = "api-client")
public interface ApiClient {

    @GET
    @Path("/data/{id}")
    Data getData(@PathParam("id") Long id);
}

// Using ClientWebApplicationException
@ApplicationScoped
public class ApiService {

    @Inject
    @RestClient
    ApiClient apiClient;

    public Data fetchData(Long id) {
        try {
            return apiClient.getData(id);
        } catch (ClientWebApplicationException e) {
            // Handle client-side errors (4xx)
            Response response = e.getResponse();
            int status = response.getStatus();

            if (status == 404) {
                throw new DataNotFoundException("Data not found for id: " + id);
            } else if (status == 401) {
                throw new UnauthorizedException("Authentication required");
            }

            // Re-throw or handle other client errors
            throw new ApiClientException("Client error: " + status, e);
        }
    }

    // Creating ClientWebApplicationException programmatically
    public void validateAndThrow(String data) {
        if (data == null) {
            throw new ClientWebApplicationException(
                "Data cannot be null",
                Response.Status.BAD_REQUEST
            );
        }
    }
}

class Data {}
class DataNotFoundException extends RuntimeException {
    public DataNotFoundException(String message) { super(message); }
}
class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) { super(message); }
}
class ApiClientException extends RuntimeException {
    public ApiClientException(String message, Throwable cause) { super(message, cause); }
}
```

**Key Points:**
- Extends `jakarta.ws.rs.WebApplicationException` for JAX-RS compatibility
- Implements `ResteasyReactiveClientProblem` marker interface to distinguish from server exceptions
- Provides comprehensive constructors for various error scenarios
- Prevents server from mishandling client-generated exceptions as server responses

### Programmatic REST Client Builder

Create REST clients programmatically using the `QuarkusRestClientBuilder` API, which provides full control over client configuration including timeouts, TLS, proxies, and custom headers.

```java { .api }
package io.quarkus.rest.client.reactive;

import java.net.URI;
import java.net.URL;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import jakarta.ws.rs.core.Configurable;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.jboss.resteasy.reactive.client.api.ClientLogger;
import org.jboss.resteasy.reactive.client.api.LoggingScope;
import io.quarkus.tls.TlsConfiguration;
import io.vertx.core.http.HttpClientOptions;

/**
 * Main entry point for creating type-safe Quarkus REST clients programmatically.
 * Provides fluent builder API for complete client configuration.
 */
public interface QuarkusRestClientBuilder extends Configurable<QuarkusRestClientBuilder> {

    /**
     * Creates a new builder instance.
     * @return new QuarkusRestClientBuilder instance
     */
    static QuarkusRestClientBuilder newBuilder();

    /**
     * Specifies the base URL for REST requests.
     * @param url the base URL for the service
     * @return current builder with baseUrl set
     */
    QuarkusRestClientBuilder baseUrl(URL url);

    /**
     * Specifies the base URI for REST requests.
     * @param uri the base URI for the service
     * @return current builder with baseUri set
     */
    QuarkusRestClientBuilder baseUri(URI uri);

    /**
     * Set the connect timeout (0 = infinity).
     * @param timeout maximum time to wait
     * @param unit time unit of timeout argument
     * @return current builder with connect timeout set
     */
    QuarkusRestClientBuilder connectTimeout(long timeout, TimeUnit unit);

    /**
     * Set the read timeout (0 = infinity).
     * @param timeout maximum time to wait
     * @param unit time unit of timeout argument
     * @return current builder with read timeout set
     */
    QuarkusRestClientBuilder readTimeout(long timeout, TimeUnit unit);

    /**
     * Set TLS configuration configured by Quarkus.
     * @param tlsConfiguration the TLS configuration
     * @return current builder with TLS configuration set
     */
    QuarkusRestClientBuilder tlsConfiguration(TlsConfiguration tlsConfiguration);

    /**
     * Set SSL context for secured connections.
     * @param sslContext the SSL context
     * @return current builder with SSL context set
     */
    QuarkusRestClientBuilder sslContext(SSLContext sslContext);

    /**
     * Set whether hostname verification is enabled.
     * @param verifyHost whether hostname verification is enabled
     * @return current builder with hostname verification set
     */
    QuarkusRestClientBuilder verifyHost(boolean verifyHost);

    /**
     * Set the client-side trust store.
     * @param trustStore key store
     * @return current builder with trust store set
     */
    QuarkusRestClientBuilder trustStore(KeyStore trustStore);

    /**
     * Set the client-side trust store with password.
     * @param trustStore key store
     * @param trustStorePassword password for the trust store
     * @return current builder with trust store set
     */
    QuarkusRestClientBuilder trustStore(KeyStore trustStore, String trustStorePassword);

    /**
     * Set the client-side key store.
     * @param keyStore key store
     * @param keystorePassword password for the key store
     * @return current builder with key store set
     */
    QuarkusRestClientBuilder keyStore(KeyStore keyStore, String keystorePassword);

    /**
     * Set the hostname verifier.
     * @param hostnameVerifier the hostname verifier
     * @return current builder with hostname verifier set
     */
    QuarkusRestClientBuilder hostnameVerifier(HostnameVerifier hostnameVerifier);

    /**
     * Set whether to follow HTTP redirects (30x).
     * @param follow true to follow redirects, false otherwise
     * @return current builder with followRedirect property set
     */
    QuarkusRestClientBuilder followRedirects(boolean follow);

    /**
     * Set form data encoding mode (HTML5, RFC1738, RFC3986).
     * @param mode the encoder mode
     * @return current builder with multipart encoder mode set
     */
    QuarkusRestClientBuilder multipartPostEncoderMode(String mode);

    /**
     * Set HTTP proxy address.
     * @param proxyHost hostname or IP address of proxy server
     * @param proxyPort port of proxy server
     * @return current builder with proxy host set
     */
    QuarkusRestClientBuilder proxyAddress(String proxyHost, int proxyPort);

    /**
     * Set proxy username.
     * @param proxyUser the proxy username
     * @return current builder
     */
    QuarkusRestClientBuilder proxyUser(String proxyUser);

    /**
     * Set proxy password.
     * @param proxyPassword the proxy password
     * @return current builder
     */
    QuarkusRestClientBuilder proxyPassword(String proxyPassword);

    /**
     * Set hosts to access without proxy.
     * @param nonProxyHosts hosts to access without proxy
     * @return current builder
     */
    QuarkusRestClientBuilder nonProxyHosts(String nonProxyHosts);

    /**
     * Set URI formatting style for multiple query parameter values.
     * @param style the query param style
     * @return current builder with query param style set
     */
    QuarkusRestClientBuilder queryParamStyle(QueryParamStyle style);

    /**
     * Set client headers factory class.
     * @param clientHeadersFactoryClass the client headers factory class
     * @return current builder
     */
    QuarkusRestClientBuilder clientHeadersFactory(Class<? extends ClientHeadersFactory> clientHeadersFactoryClass);

    /**
     * Set client headers factory instance.
     * @param clientHeadersFactory the client headers factory
     * @return current builder
     */
    QuarkusRestClientBuilder clientHeadersFactory(ClientHeadersFactory clientHeadersFactory);

    /**
     * Set HTTP client options class.
     * @param httpClientOptionsClass the HTTP client options class
     * @return current builder
     */
    QuarkusRestClientBuilder httpClientOptions(Class<? extends HttpClientOptions> httpClientOptionsClass);

    /**
     * Set HTTP client options instance.
     * @param httpClientOptions the HTTP client options
     * @return current builder
     */
    QuarkusRestClientBuilder httpClientOptions(HttpClientOptions httpClientOptions);

    /**
     * Set client logger.
     * @param clientLogger the client logger
     * @return current builder
     */
    QuarkusRestClientBuilder clientLogger(ClientLogger clientLogger);

    /**
     * Set logging scope.
     * @param loggingScope the logging scope
     * @return current builder
     */
    QuarkusRestClientBuilder loggingScope(LoggingScope loggingScope);

    /**
     * Set maximum number of characters to log for request/response bodies.
     * @param limit the body character limit
     * @return current builder
     */
    QuarkusRestClientBuilder loggingBodyLimit(Integer limit);

    /**
     * Enable trusting all certificates (disable by default, development only).
     * @param trustAll whether to trust all certificates
     * @return current builder
     */
    QuarkusRestClientBuilder trustAll(boolean trustAll);

    /**
     * Set the User-Agent header.
     * @param userAgent the user agent string
     * @return current builder
     */
    QuarkusRestClientBuilder userAgent(String userAgent);

    /**
     * Build the REST client instance.
     * @param clazz the REST client interface class
     * @return new instance implementing the REST client interface
     * @throws IllegalStateException if prerequisites not satisfied (e.g., base URI not set)
     */
    <T> T build(Class<T> clazz) throws IllegalStateException;
}
```

**Usage Examples:**

```java
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import jakarta.ws.rs.*;
import java.net.URI;
import java.util.concurrent.TimeUnit;

// REST client interface
@Path("/api")
public interface ApiClient {
    @GET
    @Path("/data")
    Response getData();
}

// Create client programmatically
public class ClientCreator {

    public ApiClient createSimpleClient() {
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create("https://api.example.com"))
                .build(ApiClient.class);
    }

    public ApiClient createAdvancedClient() {
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create("https://api.example.com"))
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .verifyHost(true)
                .proxyAddress("proxy.company.com", 8080)
                .build(ApiClient.class);
    }

    public ApiClient createClientWithCustomHeaders() {
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create("https://api.example.com"))
                .clientHeadersFactory(new CustomHeadersFactory())
                .build(ApiClient.class);
    }

    public ApiClient createClientWithTrustAll() {
        // Development only - trusts all certificates
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create("https://localhost:8443"))
                .trustAll(true)
                .verifyHost(false)
                .build(ApiClient.class);
    }
}
```

### Client Headers Factory

Implement `ClientHeadersFactory` to dynamically inject custom headers into REST client requests. Useful for adding authentication tokens, correlation IDs, or other dynamic headers.

```java { .api }
package org.eclipse.microprofile.rest.client.ext;

import jakarta.ws.rs.core.MultivaluedMap;

/**
 * MicroProfile REST Client extension for dynamically adding headers to outgoing requests.
 * Implementations can access incoming JAX-RS request headers and add custom outgoing headers.
 */
public interface ClientHeadersFactory {

    /**
     * Updates the outgoing HTTP request with additional headers.
     *
     * @param incomingHeaders read-only map of incoming JAX-RS request headers (from server context)
     * @param clientOutgoingHeaders read-only map of headers specified on REST client interface
     * @return map of headers to add to outgoing REST client request
     */
    MultivaluedMap<String, String> update(
        MultivaluedMap<String, String> incomingHeaders,
        MultivaluedMap<String, String> clientOutgoingHeaders
    );
}
```

**Usage Examples:**

```java
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.enterprise.context.ApplicationScoped;

// Custom headers factory implementation
@ApplicationScoped
public class AuthHeadersFactory implements ClientHeadersFactory {

    @Override
    public MultivaluedMap<String, String> update(
            MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {

        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();

        // Add authorization token from environment
        String token = System.getenv("API_TOKEN");
        if (token != null) {
            result.add("Authorization", "Bearer " + token);
        }

        // Propagate correlation ID from incoming request
        String correlationId = incomingHeaders.getFirst("X-Correlation-ID");
        if (correlationId != null) {
            result.add("X-Correlation-ID", correlationId);
        }

        // Add custom application headers
        result.add("X-Client-Version", "1.0.0");
        result.add("X-Request-Source", "quarkus-app");

        return result;
    }
}

// Use with programmatic client builder
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;

public class ClientFactory {

    public ApiClient createClientWithHeaders() {
        return QuarkusRestClientBuilder.newBuilder()
                .baseUri(URI.create("https://api.example.com"))
                .clientHeadersFactory(new AuthHeadersFactory())
                .build(ApiClient.class);
    }
}

// Use with declarative client (MicroProfile style)
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "external-api")
@RegisterClientHeaders(AuthHeadersFactory.class)
@Path("/api")
public interface ExternalApiClient {
    @GET
    @Path("/data")
    Response getData();
}

// Advanced example: Conditional header injection
@ApplicationScoped
public class ConditionalHeadersFactory implements ClientHeadersFactory {

    @Override
    public MultivaluedMap<String, String> update(
            MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {

        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();

        // Only add API key if not already present in client headers
        if (!clientOutgoingHeaders.containsKey("X-API-Key")) {
            result.add("X-API-Key", lookupApiKey());
        }

        // Propagate tracing headers
        incomingHeaders.keySet().stream()
                .filter(key -> key.startsWith("X-Trace-") || key.startsWith("X-B3-"))
                .forEach(key -> result.addAll(key, incomingHeaders.get(key)));

        return result;
    }

    private String lookupApiKey() {
        return System.getProperty("api.key", "default-key");
    }
}
```

**Key Points:**
- Implement `ClientHeadersFactory` to inject dynamic headers into outgoing REST client requests
- Access to both incoming server request headers and client-defined headers
- Useful for authentication tokens, correlation IDs, and header propagation
- Can be used with programmatic `QuarkusRestClientBuilder` or declarative `@RegisterClientHeaders`
- Headers are computed per request, allowing dynamic values based on runtime context

### Reactive Client Headers Factory

Reactive version of `ClientHeadersFactory` that returns headers asynchronously using Mutiny `Uni`.

```java { .api }
package io.quarkus.rest.client.reactive;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

/**
 * Reactive ClientHeadersFactory for Quarkus REST Client.
 * Allows asynchronous header computation and injection.
 * Extends ClientHeadersFactory but uses reactive `Uni` return type.
 */
public abstract class ReactiveClientHeadersFactory implements ClientHeadersFactory {

    /**
     * Updates HTTP headers to send to remote service, returning headers asynchronously.
     * Allows non-blocking operations like database lookups or external API calls.
     *
     * @param incomingHeaders map of headers from inbound JAX-RS request (empty if not part of JAX-RS request)
     * @param clientOutgoingHeaders read-only map of header parameters specified on client interface
     * @return Uni with map of HTTP headers to merge with clientOutgoingHeaders
     */
    public abstract Uni<MultivaluedMap<String, String>> getHeaders(
        MultivaluedMap<String, String> incomingHeaders,
        MultivaluedMap<String, String> clientOutgoingHeaders
    );
}
```

**Usage Examples:**

```java
import io.quarkus.rest.client.reactive.ReactiveClientHeadersFactory;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.enterprise.context.ApplicationScoped;

// Reactive headers factory with async token retrieval
@ApplicationScoped
public class AsyncAuthHeadersFactory extends ReactiveClientHeadersFactory {

    @Override
    public Uni<MultivaluedMap<String, String>> getHeaders(
            MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {

        // Fetch auth token asynchronously
        return fetchAuthTokenAsync()
            .onItem().transform(token -> {
                MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
                headers.add("Authorization", "Bearer " + token);
                headers.add("X-Request-ID", generateRequestId());
                return headers;
            });
    }

    private Uni<String> fetchAuthTokenAsync() {
        // Async token retrieval (e.g., from cache, database, or auth service)
        return Uni.createFrom().item("async-token-12345");
    }

    private String generateRequestId() {
        return java.util.UUID.randomUUID().toString();
    }
}

// Reactive headers with database lookup
@ApplicationScoped
public class DatabaseHeadersFactory extends ReactiveClientHeadersFactory {

    @Inject
    TokenRepository tokenRepository;

    @Override
    public Uni<MultivaluedMap<String, String>> getHeaders(
            MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {

        String userId = incomingHeaders.getFirst("X-User-ID");
        if (userId == null) {
            return Uni.createFrom().item(new MultivaluedHashMap<>());
        }

        // Async database lookup for user's API key
        return tokenRepository.findApiKeyByUserId(userId)
            .onItem().transform(apiKey -> {
                MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
                headers.add("X-API-Key", apiKey);
                headers.add("X-User-ID", userId);
                return headers;
            })
            .onFailure().recoverWithItem(new MultivaluedHashMap<>());
    }
}

// Reactive headers with external service call
@ApplicationScoped
public class ExternalAuthHeadersFactory extends ReactiveClientHeadersFactory {

    @RestClient
    AuthServiceClient authServiceClient;

    @Override
    public Uni<MultivaluedMap<String, String>> getHeaders(
            MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {

        // Call external auth service to get session token
        return authServiceClient.getCurrentSession()
            .onItem().transform(session -> {
                MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
                headers.add("X-Session-Token", session.token);
                headers.add("X-Session-ID", session.id);
                return headers;
            })
            .onFailure().recoverWithItem(() -> {
                // Fallback to basic headers on failure
                MultivaluedMap<String, String> fallback = new MultivaluedHashMap<>();
                fallback.add("X-Auth-Failed", "true");
                return fallback;
            });
    }
}

// Combining multiple async operations
@ApplicationScoped
public class CompositeHeadersFactory extends ReactiveClientHeadersFactory {

    @Inject
    TokenService tokenService;

    @Inject
    UserService userService;

    @Override
    public Uni<MultivaluedMap<String, String>> getHeaders(
            MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {

        String userId = incomingHeaders.getFirst("X-User-ID");

        // Combine multiple async operations
        Uni<String> tokenUni = tokenService.getToken(userId);
        Uni<String> permissionsUni = userService.getPermissions(userId);

        return Uni.combine().all().unis(tokenUni, permissionsUni)
            .asTuple()
            .onItem().transform(tuple -> {
                String token = tuple.getItem1();
                String permissions = tuple.getItem2();

                MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
                headers.add("Authorization", "Bearer " + token);
                headers.add("X-User-Permissions", permissions);
                return headers;
            });
    }
}

interface TokenRepository {
    Uni<String> findApiKeyByUserId(String userId);
}

interface AuthServiceClient {
    Uni<Session> getCurrentSession();
}

class Session {
    public String token;
    public String id;
}

interface TokenService {
    Uni<String> getToken(String userId);
}

interface UserService {
    Uni<String> getPermissions(String userId);
}
```

**Key Advantages of ReactiveClientHeadersFactory:**
- Non-blocking header computation - no thread blocking during I/O operations
- Supports async operations like database queries, cache lookups, or external API calls
- Integrates seamlessly with Mutiny reactive programming model
- Maintains reactive execution model throughout request processing
- Allows parallel header computation with `Uni.combine()`

**Usage with Client:**

```java
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;

@RegisterRestClient(configKey = "external-api")
@RegisterClientHeaders(AsyncAuthHeadersFactory.class)
@Path("/api")
public interface ExternalApiClient {
    @GET
    @Path("/data")
    Uni<Response> getData();
}
```

## Integration with MicroProfile REST Client

All Quarkus REST Client features integrate seamlessly with MicroProfile REST Client:

```java
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import jakarta.inject.Inject;

@RegisterRestClient(configKey = "my-api")
@ClientBasicAuth(username = "${api.user}", password = "${api.pass}")
@ClientQueryParam(name = "apiVersion", value = "v2")
public interface MyApiClient {
    // Client methods
}

// Inject and use
@ApplicationScoped
public class MyService {

    @Inject
    @RestClient
    MyApiClient apiClient;

    public void callApi() {
        apiClient.getData();
    }
}
```

**Configuration:**

```properties
quarkus.rest-client.my-api.url=https://api.example.com
quarkus.rest-client.my-api.scope=jakarta.inject.Singleton

# Authentication credentials
api.user=my-username
api.pass=my-password
```

## Types

```java { .api }
// Query parameter style for array/collection parameters
package org.eclipse.microprofile.rest.client.ext;

enum QueryParamStyle {
    MULTI_PAIRS,      // ?tags=java&tags=quarkus
    COMMA_SEPARATED,  // ?tags=java,quarkus
    ARRAY_PAIRS       // ?tags[]=java&tags[]=quarkus
}

// Client logging interface
package org.jboss.resteasy.reactive.client.api;

interface ClientLogger {
    void setBodySize(int bodySize);
    void logRequest(ClientRequestContext context, OutputStream stream);
    void logResponse(ClientResponseContext context);
}

// Logging scope configuration
enum LoggingScope {
    REQUEST_RESPONSE,  // Log both request and response
    REQUEST,           // Log only request
    RESPONSE           // Log only response
}

// TLS/SSL configuration
package io.quarkus.tls;

interface TlsConfiguration {
    KeyStore getKeyStore();
    KeyStore getTrustStore();
    SSLContext createSSLContext() throws Exception;
    Optional<String> getHostnameVerificationAlgorithm();
    boolean usesSni();
    boolean isTrustAll();
}
```
