# Request and Response Filters

Quarkus REST provides simplified annotations for implementing request and response filters without needing to implement JAX-RS `ContainerRequestFilter` or `ContainerResponseFilter` interfaces directly. The framework generates filter implementations from annotated methods with automatic parameter injection.

## Capabilities

### Request Filtering

Process requests before they reach endpoint methods using method-level filter annotations with automatic parameter injection and flexible return types.

```java { .api }
package org.jboss.resteasy.reactive.server;

/**
 * Annotation for simplified request filtering.
 * When applied to a method, generates a ContainerRequestFilter implementation.
 *
 * Supported method parameters (any order):
 * - ContainerRequestContext
 * - UriInfo
 * - HttpHeaders
 * - Request
 * - ResourceInfo
 * - SimpleResourceInfo
 *
 * Supported return types:
 * - void: No blocking ops, cannot abort
 * - Response or RestResponse: No blocking ops, aborts if non-null
 * - Optional<Response> or Optional<RestResponse>: No blocking ops, aborts if present
 * - Uni<Void>: Reactive, cannot abort
 * - Uni<Response> or Uni<RestResponse>: Reactive, aborts if non-null
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface ServerRequestFilter {
    /** Priority for filter execution (default: Priorities.USER) */
    int priority() default Priorities.USER;

    /** Whether filter runs before resource matching */
    boolean preMatching() default false;

    /** Run on event-loop even when target method is blocking */
    boolean nonBlocking() default false;

    /** @deprecated Use @WithFormRead instead */
    @Deprecated
    boolean readBody() default false;
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

public class CustomFilters {

    // Simple void filter - no abort capability
    @ServerRequestFilter
    public void logRequest(UriInfo uriInfo) {
        System.out.println("Request to: " + uriInfo.getPath());
    }

    // Filter that can abort with Response
    @ServerRequestFilter(priority = 100)
    public Response checkAuth(HttpHeaders headers) {
        String token = headers.getHeaderString("Authorization");
        if (token == null) {
            return Response.status(401).entity("Missing token").build();
        }
        return null; // Continue processing
    }

    // Filter with Optional for conditional abort
    @ServerRequestFilter
    public Optional<Response> validateApiKey(UriInfo uriInfo, HttpHeaders headers) {
        if (uriInfo.getPath().startsWith("/api/")) {
            String key = headers.getHeaderString("X-API-Key");
            if (!isValidKey(key)) {
                return Optional.of(Response.status(403).build());
            }
        }
        return Optional.empty();
    }

    // Reactive filter with Uni
    @ServerRequestFilter
    public Uni<RestResponse<String>> asyncValidation(UriInfo uriInfo) {
        return validateAsync(uriInfo.getPath())
            .onItem().transform(valid ->
                valid ? null : RestResponse.status(403, "Invalid request")
            );
    }

    // Pre-matching filter (runs before resource matching)
    @ServerRequestFilter(preMatching = true, priority = 1)
    public void earlyFilter(ContainerRequestContext context) {
        // Runs before resource matching
    }

    private boolean isValidKey(String key) {
        return key != null && key.startsWith("sk-");
    }

    private Uni<Boolean> validateAsync(String path) {
        return Uni.createFrom().item(true);
    }
}
```

### Response Filtering

Modify responses after endpoint execution using method-level filter annotations with automatic parameter injection.

```java { .api }
package org.jboss.resteasy.reactive.server;

/**
 * Annotation for simplified response filtering.
 * When applied to a method, generates a ContainerResponseFilter implementation.
 *
 * Supported method parameters (any order):
 * - ContainerRequestContext
 * - ContainerResponseContext
 * - ResourceInfo
 * - UriInfo
 * - SimpleResourceInfo
 * - Throwable (the thrown exception, or null if no exception)
 *
 * Supported return types:
 * - void: Synchronous filtering
 * - Uni<Void>: Reactive filtering
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface ServerResponseFilter {
    /** Priority for filter execution (default: Priorities.USER) */
    int priority() default Priorities.USER;
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.server.ServerResponseFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.UriInfo;

public class ResponseFilters {

    // Add headers to all responses
    @ServerResponseFilter
    public void addHeaders(ContainerResponseContext response) {
        response.getHeaders().add("X-Custom-Header", "MyValue");
        response.getHeaders().add("X-Powered-By", "Quarkus REST");
    }

    // Conditional header based on path
    @ServerResponseFilter
    public void addCorsHeaders(UriInfo uriInfo, ContainerResponseContext response) {
        if (uriInfo.getPath().startsWith("/api/")) {
            response.getHeaders().add("Access-Control-Allow-Origin", "*");
        }
    }

    // Handle exceptions in response filter
    @ServerResponseFilter
    public void logErrors(Throwable throwable, ContainerResponseContext response) {
        if (throwable != null) {
            System.err.println("Request failed with: " + throwable.getMessage());
            response.getHeaders().add("X-Error-Logged", "true");
        }
    }

    // Reactive response filtering
    @ServerResponseFilter
    public Uni<Void> asyncLogging(ContainerResponseContext response) {
        return logResponseAsync(response.getStatus())
            .replaceWithVoid();
    }

    private Uni<Void> logResponseAsync(int status) {
        return Uni.createFrom().voidItem();
    }
}
```

### Exception Mapping

Map exceptions to HTTP responses using simplified annotation-based approach with support for both global and resource-local exception handling.

```java { .api }
package org.jboss.resteasy.reactive.server;

/**
 * Annotation for simplified exception mapping.
 * When applied to a method, generates an ExceptionMapper implementation.
 *
 * Can be used:
 * - On non-Resource class methods: Global exception handling
 * - On Resource class methods: Resource-local exception handling (takes precedence)
 *
 * Supported method parameters (exception type required, others optional in any order):
 * - The exception type being handled (required)
 * - ContainerRequestContext
 * - UriInfo
 * - HttpHeaders
 * - Request
 * - ResourceInfo
 * - SimpleResourceInfo
 *
 * Supported return types:
 * - Response
 * - Uni<Response>
 * - RestResponse
 * - Uni<RestResponse>
 *
 * Exception type inference:
 * - If value() not set, exception type is inferred from method parameter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface ServerExceptionMapper {
    /** Exception type(s) to handle (optional if deducible from parameters) */
    Class<? extends Throwable>[] value() default {};

    /** Priority for exception mapper execution (default: Priorities.USER) */
    int priority() default Priorities.USER;
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.RestResponse;
import jakarta.ws.rs.core.Response;

// Global exception mappers
public class GlobalExceptionMappers {

    // Inferred exception type from parameter
    @ServerExceptionMapper
    public Response mapIllegalArgument(IllegalArgumentException ex) {
        return Response.status(400)
            .entity("Invalid input: " + ex.getMessage())
            .build();
    }

    // Explicit exception type
    @ServerExceptionMapper(value = NullPointerException.class, priority = 100)
    public RestResponse<String> mapNullPointer(NullPointerException ex) {
        return RestResponse.status(500, "Null value encountered");
    }

    // Multiple exception types
    @ServerExceptionMapper({IOException.class, TimeoutException.class})
    public Response mapIOErrors(Exception ex) {
        return Response.status(503)
            .entity("Service temporarily unavailable")
            .build();
    }

    // With additional context parameters
    @ServerExceptionMapper
    public Response mapValidation(ValidationException ex, UriInfo uriInfo) {
        return Response.status(400)
            .entity("Validation failed for " + uriInfo.getPath() + ": " + ex.getMessage())
            .build();
    }

    // Reactive exception mapping
    @ServerExceptionMapper
    public Uni<RestResponse<ErrorDetails>> mapAsync(CustomException ex) {
        return logErrorAsync(ex)
            .map(logged -> RestResponse.status(500, new ErrorDetails(ex)));
    }

    private Uni<Boolean> logErrorAsync(Exception ex) {
        return Uni.createFrom().item(true);
    }
}

// Resource-local exception mapper (takes precedence for this resource)
@Path("/users")
public class UserResource {

    @GET
    @Path("/{id}")
    public User getUser(@PathParam("id") long id) {
        if (id < 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }
        return findUser(id);
    }

    // Only handles exceptions from this resource
    @ServerExceptionMapper
    public Response handleLocalException(IllegalArgumentException ex) {
        return Response.status(400)
            .entity("Invalid user ID format")
            .build();
    }

    private User findUser(long id) {
        return new User();
    }
}

class ValidationException extends Exception {
    public ValidationException(String message) { super(message); }
}

class CustomException extends Exception {
    public CustomException(String message) { super(message); }
}

class ErrorDetails {
    public ErrorDetails(Exception ex) {}
}

class User {}
```

### Filter-Related Annotations

Additional annotations for controlling filter behavior and form handling.

```java { .api }
package org.jboss.resteasy.reactive.server;

/**
 * Forces form body reading and parsing before filters/endpoints execute.
 * Use when filters need access to form data.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@interface WithFormRead {}

/**
 * Unwraps exception before passing to exception mapper.
 * Useful for handling wrapped exceptions like CompletionException.
 * Applied at class level (exception class or filter/mapper class).
 *
 * When an exception of the configured type is thrown and no ExceptionMapper exists,
 * RESTEasy Reactive attempts to locate an ExceptionMapper for the cause.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface UnwrapException {
    /** Exception types to unwrap (if not set, uses the annotated exception class) */
    Class<? extends Exception>[] value() default {};
}

/**
 * Lightweight alternative to ResourceInfo for injecting into filters.
 * Provides resource class, method name, and parameter types without reflective lookup.
 * Can be injected anywhere ResourceInfo can be injected.
 */
interface SimpleResourceInfo {
    /** Get the resource class that is the target of a request */
    Class<?> getResourceClass();

    /** Get the name of the resource method that is the target of a request */
    String getMethodName();

    /** Get the parameter types of the resource method that is the target of a request */
    Class<?>[] parameterTypes();
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.server.WithFormRead;
import org.jboss.resteasy.reactive.server.UnwrapException;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.concurrent.CompletionException;

public class AdvancedFilters {

    // Force form reading before filter - method level
    @ServerRequestFilter
    @WithFormRead
    public Response validateFormData(ContainerRequestContext context) {
        // Form data is already parsed and available
        MultivaluedMap<String, String> formParams = context.getUriInfo()
            .getQueryParameters(); // Form data accessible
        return null;
    }

    // Class-level WithFormRead applies to all filter methods
    @WithFormRead
    public class FormValidationFilters {

        @ServerRequestFilter
        public Response checkFormSize(ContainerRequestContext context) {
            // Form is already read for all methods in this class
            return null;
        }

        @ServerRequestFilter
        public Response validateRequiredFields(ContainerRequestContext context) {
            // Form data available here too
            return null;
        }
    }

    // Unwrap CompletionException to access root cause
    @UnwrapException(CompletionException.class)
    public class MyExceptionMappers {
        @ServerExceptionMapper
        public Response handleWrapped(RuntimeException ex) {
            // ex is the unwrapped root cause, not CompletionException
            return Response.status(500)
                .entity("Error: " + ex.getMessage())
                .build();
        }
    }

    // Unwrap multiple exception types
    @ServerExceptionMapper
    @UnwrapException({CompletionException.class, ExecutionException.class})
    public Response handleMultipleWrapped(RuntimeException ex) {
        // Unwraps either CompletionException or ExecutionException
        return Response.status(500)
            .entity("Unwrapped error: " + ex.getClass().getSimpleName())
            .build();
    }

    // Without UnwrapException - handles wrapper
    @ServerExceptionMapper
    public Response handleCompletionException(CompletionException ex) {
        // Must manually unwrap
        Throwable cause = ex.getCause();
        return Response.status(500)
            .entity("Wrapped error: " + cause.getMessage())
            .build();
    }

    // Applied to resource endpoint
    @POST
    @Path("/submit")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @WithFormRead  // Ensures form is read before any filters
    public Response submitForm(@FormParam("data") String data) {
        return Response.ok("Received: " + data).build();
    }
}
```

## Integration with CDI

All filter classes support CDI injection. Constructor injection is recommended for injecting beans into filters.

```java
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SecurityFilter {

    private final AuthService authService;
    private final AuditLogger auditLogger;

    // CDI constructor injection
    @Inject
    public SecurityFilter(AuthService authService, AuditLogger auditLogger) {
        this.authService = authService;
        this.auditLogger = auditLogger;
    }

    @ServerRequestFilter(priority = 1000)
    public Uni<RestResponse<String>> authenticate(HttpHeaders headers) {
        String token = headers.getHeaderString("Authorization");
        return authService.validateToken(token)
            .onItem().transformToUni(valid -> {
                if (!valid) {
                    return Uni.createFrom().item(
                        RestResponse.status(401, "Invalid token")
                    );
                }
                return Uni.createFrom().nullItem();
            })
            .onItem().invoke(() -> auditLogger.logAccess(token));
    }
}

interface AuthService {
    Uni<Boolean> validateToken(String token);
}

interface AuditLogger {
    void logAccess(String token);
}
```

## Priority and Execution Order

Filters execute based on priority values (lower values execute first for request filters, higher values execute first for response filters):

- `Priorities.AUTHENTICATION` (1000): Authentication filters
- `Priorities.AUTHORIZATION` (2000): Authorization filters
- `Priorities.HEADER_DECORATOR` (3000): Header manipulation
- `Priorities.ENTITY_CODER` (4000): Content encoding/decoding
- `Priorities.USER` (5000): Default user filters

```java
public class PriorityExample {

    @ServerRequestFilter(priority = Priorities.AUTHENTICATION)
    public Response authenticate(HttpHeaders headers) {
        // Runs first (priority 1000)
        return null;
    }

    @ServerRequestFilter(priority = Priorities.AUTHORIZATION)
    public Response authorize(HttpHeaders headers) {
        // Runs second (priority 2000)
        return null;
    }

    @ServerRequestFilter(priority = Priorities.USER)
    public void log(UriInfo uriInfo) {
        // Runs later (priority 5000)
    }
}
```
