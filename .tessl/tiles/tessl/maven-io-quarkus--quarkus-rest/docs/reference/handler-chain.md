# Handler Chain

Quarkus REST uses a handler chain architecture for request processing, allowing customization through phases and providing extension points for adding custom handlers.

## HandlerChainCustomizer Interface

The HandlerChainCustomizer interface allows adding custom handlers to the request processing pipeline.

```java { .api }
package org.jboss.resteasy.reactive.server.handlers;

interface HandlerChainCustomizer {
    List<ServerRestHandler> handlers(
        Phase phase,
        ResourceClass resourceClass,
        ServerResourceMethod resourceMethod
    );
}
```

Customizers are invoked during deployment to build the handler chain for each endpoint.

## Handler Phases

The handler chain is divided into phases that represent different stages of request processing.

```java { .api }
enum Phase {
    AFTER_PRE_MATCH,    // After initial request matching, before security
    AFTER_MATCH,        // After resource method matched, before invocation
    AFTER_METHOD_INVOKE // After resource method executed, before response
}
```

### AFTER_PRE_MATCH Phase

Handlers in this phase execute after the request is initially matched but before resource method matching and security checks.

**Use Cases**:
- Early request filtering
- Request logging
- Protocol upgrades
- Pre-security processing

**Example**:

```java
import org.jboss.resteasy.reactive.server.handlers.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

@ApplicationScoped
public class PreMatchCustomizer implements HandlerChainCustomizer {

    @Override
    public List<ServerRestHandler> handlers(
        Phase phase,
        ResourceClass resourceClass,
        ServerResourceMethod resourceMethod
    ) {
        if (phase == Phase.AFTER_PRE_MATCH) {
            return List.of(new RequestLoggingHandler());
        }
        return Collections.emptyList();
    }
}
```

### AFTER_MATCH Phase

Handlers in this phase execute after the resource method is matched but before it is invoked.

**Use Cases**:
- Security checks
- Observability tracking
- Request validation
- Rate limiting
- Compression configuration

**Example**:

```java
@ApplicationScoped
public class AfterMatchCustomizer implements HandlerChainCustomizer {

    @Override
    public List<ServerRestHandler> handlers(
        Phase phase,
        ResourceClass resourceClass,
        ServerResourceMethod resourceMethod
    ) {
        if (phase == Phase.AFTER_MATCH) {
            return List.of(
                new SecurityHandler(),
                new ObservabilityHandler(),
                new RateLimitHandler()
            );
        }
        return Collections.emptyList();
    }
}
```

### AFTER_METHOD_INVOKE Phase

Handlers in this phase execute after the resource method has been invoked but before the response is sent.

**Use Cases**:
- Response modification
- WebSocket upgrades
- Response logging
- Custom serialization

**Example**:

```java
@ApplicationScoped
public class AfterInvokeCustomizer implements HandlerChainCustomizer {

    @Override
    public List<ServerRestHandler> handlers(
        Phase phase,
        ResourceClass resourceClass,
        ServerResourceMethod resourceMethod
    ) {
        if (phase == Phase.AFTER_METHOD_INVOKE) {
            return List.of(
                new ResponseLoggingHandler(),
                new ResponseEnrichmentHandler()
            );
        }
        return Collections.emptyList();
    }
}
```

## ServerRestHandler Interface

Custom handlers implement the ServerRestHandler interface.

```java { .api }
package org.jboss.resteasy.reactive.server.spi;

interface ServerRestHandler {
    void handle(ResteasyReactiveRequestContext requestContext) throws Exception;
}
```

## Creating Custom Handlers

Implement ServerRestHandler to create custom request processing logic.

**Example - Request Logging Handler**:

```java
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.logging.Logger;

public class RequestLoggingHandler implements ServerRestHandler {

    private static final Logger LOG = Logger.getLogger(RequestLoggingHandler.class);

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) {
        HttpServerRequest request = requestContext.getContext().request();

        LOG.infof("Request: %s %s from %s",
            request.method(),
            request.path(),
            request.remoteAddress().host()
        );

        // Continue to next handler
    }
}
```

**Example - Rate Limiting Handler**:

```java
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitHandler implements ServerRestHandler {

    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMs;

    public RateLimitHandler(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) {
        HttpServerRequest request = requestContext.getContext().request();
        String clientIp = request.remoteAddress().host();

        AtomicInteger count = requestCounts.computeIfAbsent(clientIp, k -> new AtomicInteger(0));

        if (count.incrementAndGet() > maxRequests) {
            requestContext.getContext().response()
                .setStatusCode(429)
                .end("Rate limit exceeded");
            requestContext.suspend();
            return;
        }

        // Reset counter after window
        requestContext.getContext().vertx().setTimer(windowMs, id -> {
            requestCounts.remove(clientIp);
        });
    }
}
```

**Example - Custom Header Handler**:

```java
public class CustomHeaderHandler implements ServerRestHandler {

    private final String headerName;
    private final String headerValue;

    public CustomHeaderHandler(String headerName, String headerValue) {
        this.headerName = headerName;
        this.headerValue = headerValue;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) {
        requestContext.getContext().response()
            .putHeader(headerName, headerValue);
    }
}
```

## Built-in Handler Examples

Quarkus REST includes several built-in handlers that demonstrate the pattern.

### EagerSecurityHandler

Performs eager security checks.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.security;

class EagerSecurityHandler implements ServerRestHandler {
    void handle(ResteasyReactiveRequestContext requestContext);

    abstract class Customizer implements HandlerChainCustomizer {
        static HandlerChainCustomizer newInstance(boolean onlyCheckForHttpPermissions);
        List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass, ServerResourceMethod resourceMethod);
    }
}
```

Adds security handlers in the AFTER_MATCH phase.

### ObservabilityHandler

Sets URL template paths for metrics and tracing.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.observability;

class ObservabilityHandler implements ServerRestHandler {
    String getTemplatePath();
    void setTemplatePath(String templatePath);
    boolean isSubResource();
    void setSubResource(boolean subResource);
    void handle(ResteasyReactiveRequestContext requestContext);
}
```

Added in the AFTER_MATCH phase by ObservabilityCustomizer.

### ResteasyReactiveCompressionHandler

Manages HTTP compression.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

class ResteasyReactiveCompressionHandler implements ServerRestHandler {
    HttpCompression getCompression();
    void setCompression(HttpCompression compression);
    Set<String> getCompressMediaTypes();
    void setCompressMediaTypes(Set<String> compressMediaTypes);
    String getProduces();
    void setProduces(String produces);
    void handle(ResteasyReactiveRequestContext requestContext);
}
```

## Handler Chain Execution Flow

The complete request processing flow:

```
1. Request received
2. AFTER_PRE_MATCH handlers
   - Early filtering
   - Request logging
3. Resource matching
4. AFTER_MATCH handlers
   - Security checks (EagerSecurityHandler)
   - Observability tracking (ObservabilityHandler)
   - Compression setup (ResteasyReactiveCompressionHandler)
   - Custom validators
5. Parameter extraction
6. Resource method invocation
7. AFTER_METHOD_INVOKE handlers
   - WebSocket upgrade (VertxWebSocketRestHandler)
   - Response modification
8. Response serialization
9. Response sent
```

## Conditional Handler Registration

Register handlers conditionally based on resource method characteristics.

**Example**:

```java
@ApplicationScoped
public class ConditionalCustomizer implements HandlerChainCustomizer {

    @Override
    public List<ServerRestHandler> handlers(
        Phase phase,
        ResourceClass resourceClass,
        ServerResourceMethod resourceMethod
    ) {
        if (phase == Phase.AFTER_MATCH) {
            List<ServerRestHandler> handlers = new ArrayList<>();

            // Add security handler for secured endpoints
            if (resourceMethod.hasAnnotation(RolesAllowed.class)) {
                handlers.add(new CustomSecurityHandler());
            }

            // Add validation handler for POST/PUT methods
            String httpMethod = resourceMethod.getHttpMethod();
            if ("POST".equals(httpMethod) || "PUT".equals(httpMethod)) {
                handlers.add(new ValidationHandler());
            }

            // Add caching handler for GET methods
            if ("GET".equals(httpMethod)) {
                handlers.add(new CachingHandler());
            }

            return handlers;
        }
        return Collections.emptyList();
    }
}
```

## Accessing Request Context

The ResteasyReactiveRequestContext provides access to request state and Vert.x context.

**Example**:

```java
public class ContextAccessHandler implements ServerRestHandler {

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) {
        // Access Vert.x routing context
        RoutingContext vertxContext = requestContext.getContext();

        // Access HTTP request/response
        HttpServerRequest request = vertxContext.request();
        HttpServerResponse response = vertxContext.response();

        // Access resource method info
        ServerResourceMethod method = requestContext.getTarget();
        String methodName = method.getName();

        // Store data for later handlers
        requestContext.setProperty("customKey", "customValue");

        // Retrieve stored data
        Object value = requestContext.getProperty("customKey");
    }
}
```

## Async Handler Processing

Handlers can perform async operations using Vert.x or Mutiny.

**Example**:

```java
import io.smallrye.mutiny.Uni;

public class AsyncValidationHandler implements ServerRestHandler {

    @Inject
    ValidationService validationService;

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) {
        // Suspend request processing
        requestContext.suspend();

        // Perform async validation
        validationService.validateAsync(requestContext)
            .subscribe().with(
                valid -> {
                    if (valid) {
                        // Resume processing
                        requestContext.resume();
                    } else {
                        // Fail with validation error
                        requestContext.getContext().response()
                            .setStatusCode(400)
                            .end("Validation failed");
                    }
                },
                error -> {
                    // Handle error
                    requestContext.getContext().fail(error);
                }
            );
    }
}
```

## Handler Priority and Ordering

Handlers are executed in the order returned by the customizer.

**Example**:

```java
@ApplicationScoped
public class OrderedCustomizer implements HandlerChainCustomizer {

    @Override
    public List<ServerRestHandler> handlers(
        Phase phase,
        ResourceClass resourceClass,
        ServerResourceMethod resourceMethod
    ) {
        if (phase == Phase.AFTER_MATCH) {
            // Handlers execute in this order:
            return List.of(
                new AuthenticationHandler(),    // 1. First
                new AuthorizationHandler(),     // 2. Second
                new ValidationHandler(),        // 3. Third
                new AuditHandler()             // 4. Last
            );
        }
        return Collections.emptyList();
    }
}
```

## Multiple Customizers

Multiple HandlerChainCustomizer implementations can be registered.

**Example**:

```java
@ApplicationScoped
public class SecurityCustomizer implements HandlerChainCustomizer {
    @Override
    public List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass, ServerResourceMethod resourceMethod) {
        if (phase == Phase.AFTER_MATCH) {
            return List.of(new SecurityHandler());
        }
        return Collections.emptyList();
    }
}

@ApplicationScoped
public class ObservabilityCustomizer implements HandlerChainCustomizer {
    @Override
    public List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass, ServerResourceMethod resourceMethod) {
        if (phase == Phase.AFTER_MATCH) {
            return List.of(new MetricsHandler());
        }
        return Collections.emptyList();
    }
}
```

All customizers are invoked and their handlers are combined.

## Error Handling in Handlers

Handlers can fail the request or throw exceptions.

**Example**:

```java
public class ErrorHandlingHandler implements ServerRestHandler {

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        try {
            // Processing logic
            performValidation(requestContext);
        } catch (ValidationException e) {
            // Fail with specific error
            requestContext.getContext().response()
                .setStatusCode(400)
                .end("Validation error: " + e.getMessage());
            requestContext.suspend();
            return;
        } catch (Exception e) {
            // Throw to exception mappers
            throw e;
        }
    }

    private void performValidation(ResteasyReactiveRequestContext ctx) throws ValidationException {
        // Validation logic
    }
}
```

## Testing Custom Handlers

Test handler behavior in integration tests.

**Example**:

```java
@QuarkusTest
public class CustomHandlerTest {

    @Test
    public void testCustomHeaderAdded() {
        RestAssured.given()
            .when().get("/api/test")
            .then()
            .statusCode(200)
            .header("X-Custom-Header", "CustomValue");
    }

    @Test
    public void testRateLimitApplied() {
        // Make requests up to limit
        for (int i = 0; i < 10; i++) {
            RestAssured.given()
                .when().get("/api/test")
                .then()
                .statusCode(200);
        }

        // Next request should be rate limited
        RestAssured.given()
            .when().get("/api/test")
            .then()
            .statusCode(429);
    }
}
```
