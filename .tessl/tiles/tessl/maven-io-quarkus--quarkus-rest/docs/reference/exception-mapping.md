# Exception Mapping

Quarkus REST provides flexible exception handling through standard JAX-RS ExceptionMapper and the enhanced @ServerExceptionMapper annotation, allowing you to map exceptions to HTTP responses with fine-grained control.

## ServerExceptionMapper Annotation

The @ServerExceptionMapper annotation is the preferred way to define exception mappers in Quarkus REST, supporting reactive programming and automatic exception type inference.

```java { .api }
package org.jboss.resteasy.reactive.server;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface ServerExceptionMapper {
    Class<? extends Throwable> value() default Throwable.class;
    int priority() default Priorities.USER;
}
```

**Usage**:

```java
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import jakarta.ws.rs.core.Response;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CustomExceptionMappers {

    @ServerExceptionMapper
    public Response handleBusinessException(BusinessException ex) {
        // Exception type inferred from parameter
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(new ErrorResponse(ex.getMessage()))
            .build();
    }

    @ServerExceptionMapper(priority = 1000)
    public Response handleValidationException(ValidationException ex) {
        // Custom priority (lower value = higher priority)
        return Response.status(422)
            .entity(new ErrorResponse("Validation failed", ex.getErrors()))
            .build();
    }

    @ServerExceptionMapper
    public Response handleNotFoundException(NotFoundException ex) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(new ErrorResponse("Resource not found"))
            .build();
    }
}
```

## Reactive Exception Mapping

ServerExceptionMapper supports reactive return types for async exception handling.

**Usage**:

```java
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class ReactiveExceptionMappers {

    @ServerExceptionMapper
    public Uni<Response> handleAsyncException(AsyncProcessingException ex) {
        // Return Uni for reactive error handling
        return logService.logErrorAsync(ex)
            .onItem().transform(logId ->
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error logged with ID: " + logId))
                    .build()
            );
    }

    @ServerExceptionMapper
    public Uni<Response> handleTimeoutException(TimeoutException ex) {
        return Uni.createFrom().item(
            Response.status(Response.Status.REQUEST_TIMEOUT)
                .entity(new ErrorResponse("Request timed out"))
                .build()
        );
    }
}
```

## Standard JAX-RS ExceptionMapper

You can also use the standard JAX-RS ExceptionMapper interface.

```java { .api }
package jakarta.ws.rs.ext;

@Provider
interface ExceptionMapper<E extends Throwable> {
    Response toResponse(E exception);
}
```

**Usage**:

```java
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(new ErrorResponse(exception.getMessage()))
            .build();
    }
}

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        // Catch-all for unmapped exceptions
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(new ErrorResponse("An unexpected error occurred"))
            .build();
    }
}
```

## Built-in Security Exception Mappers

Quarkus REST provides built-in exception mappers for security exceptions.

### Authentication Completion Exception

Maps authentication completion failures to 401 Unauthorized.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.exceptionmappers;

class AuthenticationCompletionExceptionMapper
    implements ExceptionMapper<AuthenticationCompletionException> {
    Response toResponse(AuthenticationCompletionException exception);
}
```

### Authentication Failed Exception

Maps authentication failures to HTTP responses with WWW-Authenticate challenge.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.exceptionmappers;

class AuthenticationFailedExceptionMapper {
    @ServerExceptionMapper(value = AuthenticationFailedException.class, priority = Priorities.USER + 1)
    Uni<Response> handle(RoutingContext routingContext);
}
```

This mapper returns a reactive response with appropriate authentication challenge headers.

### Authentication Redirect Exception

Maps authentication redirects to HTTP 302 responses with Location header.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.exceptionmappers;

class AuthenticationRedirectExceptionMapper
    implements ExceptionMapper<AuthenticationRedirectException> {
    Response toResponse(AuthenticationRedirectException exception);
}
```

The response includes cache control headers to prevent caching of redirect responses.

### Forbidden Exception

Maps authorization failures to 403 Forbidden.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.exceptionmappers;

class ForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {
    Response toResponse(ForbiddenException exception);
}
```

### Unauthorized Exception

Maps unauthorized access to HTTP responses with WWW-Authenticate challenge.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.exceptionmappers;

class UnauthorizedExceptionMapper {
    @ServerExceptionMapper(value = UnauthorizedException.class, priority = Priorities.USER + 1)
    Uni<Response> handle(RoutingContext routingContext);
}
```

## Exception Mapper Priority

Control the order of exception mapper evaluation using priority values. Lower priority values are evaluated first.

**Usage**:

```java
@ApplicationScoped
public class PriorityExceptionMappers {

    @ServerExceptionMapper(priority = 1)  // Highest priority
    public Response handleSpecificException(SpecificException ex) {
        return Response.status(400).entity("Specific handler").build();
    }

    @ServerExceptionMapper(priority = 1000)  // Lower priority
    public Response handleGeneralException(Exception ex) {
        return Response.status(500).entity("General handler").build();
    }
}
```

Priority constants from `jakarta.ws.rs.Priorities`:

- `Priorities.USER = 5000` - Default for user-defined mappers
- Custom values can be any integer (lower = higher priority)

## Building Error Responses

Create structured error responses with consistent formatting.

**Usage**:

```java
public class ErrorResponse {
    private String message;
    private String code;
    private List<String> details;
    private long timestamp;

    public ErrorResponse(String message) {
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public ErrorResponse(String message, String code) {
        this.message = message;
        this.code = code;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and setters
}

@ApplicationScoped
public class ErrorResponseMappers {

    @ServerExceptionMapper
    public Response handleValidationException(ValidationException ex) {
        ErrorResponse error = new ErrorResponse(
            "Validation failed",
            "VALIDATION_ERROR"
        );
        error.setDetails(ex.getViolations().stream()
            .map(v -> v.getMessage())
            .collect(Collectors.toList()));

        return Response.status(422)
            .entity(error)
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    @ServerExceptionMapper
    public Response handleBusinessException(BusinessException ex) {
        ErrorResponse error = new ErrorResponse(
            ex.getMessage(),
            ex.getErrorCode()
        );

        return Response.status(ex.getStatusCode())
            .entity(error)
            .header("X-Error-Code", ex.getErrorCode())
            .build();
    }
}
```

## Context Injection in Exception Mappers

Inject JAX-RS context types and CDI beans into exception mappers.

**Usage**:

```java
@ApplicationScoped
public class ContextAwareExceptionMappers {

    @Inject
    Logger logger;

    @Context
    UriInfo uriInfo;

    @ServerExceptionMapper
    public Response handleException(ApplicationException ex) {
        String path = uriInfo.getPath();
        logger.error("Exception at path: " + path, ex);

        return Response.status(500)
            .entity(new ErrorResponse("Error at: " + path))
            .build();
    }
}
```

## Reactive Context Access

Access Vert.x routing context in reactive exception mappers.

**Usage**:

```java
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class RoutingContextMappers {

    @ServerExceptionMapper
    public Uni<Response> handleAuthException(
        AuthenticationFailedException ex,
        RoutingContext context
    ) {
        // Access Vert.x routing context
        String clientIp = context.request().remoteAddress().host();

        return securityService.logFailedAuth(clientIp, ex)
            .onItem().transform(logId ->
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("Authentication failed"))
                    .header("X-Log-Id", logId)
                    .build()
            );
    }
}
```

## Exception Hierarchy

More specific exception mappers take precedence over general ones, regardless of priority.

**Usage**:

```java
@ApplicationScoped
public class HierarchyExceptionMappers {

    @ServerExceptionMapper
    public Response handleIOException(IOException ex) {
        // Handles IOException and its subclasses
        return Response.status(500)
            .entity(new ErrorResponse("I/O error"))
            .build();
    }

    @ServerExceptionMapper
    public Response handleFileNotFoundException(FileNotFoundException ex) {
        // More specific - takes precedence over IOException mapper
        return Response.status(404)
            .entity(new ErrorResponse("File not found"))
            .build();
    }

    @ServerExceptionMapper(priority = 10000)
    public Response handleException(Exception ex) {
        // Catch-all with low priority
        return Response.status(500)
            .entity(new ErrorResponse("Unexpected error"))
            .build();
    }
}
```

## Exception Mapper Registration

Exception mappers are automatically registered when using @ServerExceptionMapper or @Provider annotations in CDI beans.

**Programmatic Registration**:

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

class ResteasyReactiveRecorder {
    void registerExceptionMapper(
        RuntimeValue<Deployment> deployment,
        String className,
        Supplier<?> supplier,
        Class<? extends Throwable> exceptionClass,
        int priority
    );
}
```

This method is primarily used by the Quarkus build process for automatic registration.

## Best Practices

### Return Appropriate Status Codes

Use appropriate HTTP status codes for different error types:

```java
@ApplicationScoped
public class StatusCodeExceptionMappers {

    @ServerExceptionMapper
    public Response handleNotFoundException(NotFoundException ex) {
        return Response.status(404).entity(new ErrorResponse("Not found")).build();
    }

    @ServerExceptionMapper
    public Response handleValidationException(ValidationException ex) {
        return Response.status(422).entity(new ErrorResponse("Invalid input")).build();
    }

    @ServerExceptionMapper
    public Response handleConflictException(ConflictException ex) {
        return Response.status(409).entity(new ErrorResponse("Conflict")).build();
    }

    @ServerExceptionMapper
    public Response handleAuthException(UnauthorizedException ex) {
        return Response.status(401).entity(new ErrorResponse("Unauthorized")).build();
    }
}
```

### Include Correlation IDs

Add correlation IDs to error responses for tracing:

```java
@ApplicationScoped
public class CorrelationIdExceptionMappers {

    @ServerExceptionMapper
    public Response handleException(Exception ex, @Context HttpHeaders headers) {
        String correlationId = headers.getHeaderString("X-Correlation-ID");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        return Response.status(500)
            .entity(new ErrorResponse("Internal error", correlationId))
            .header("X-Correlation-ID", correlationId)
            .build();
    }
}
```

### Avoid Exposing Sensitive Information

Don't expose stack traces or internal details in production:

```java
@ApplicationScoped
public class SecureExceptionMappers {

    @Inject
    @ConfigProperty(name = "quarkus.profile")
    String profile;

    @ServerExceptionMapper
    public Response handleException(Exception ex) {
        ErrorResponse error;

        if ("dev".equals(profile)) {
            // Include details in development
            error = new ErrorResponse(ex.getMessage());
            error.setStackTrace(getStackTraceAsString(ex));
        } else {
            // Generic message in production
            error = new ErrorResponse("An error occurred");
        }

        return Response.status(500).entity(error).build();
    }
}
```
