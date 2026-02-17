# Architecture

This document provides an overview of the Quarkus REST architecture, including the request lifecycle, handler chain, CDI integration, reactive layer, and Vert.x foundation.

## Overview

Quarkus REST (formerly RESTEasy Reactive) is a modern Jakarta REST implementation built on reactive principles. It combines compile-time processing, reactive programming, and Vert.x's event loop for high performance with low resource consumption.

**Key Architectural Principles**:

- **Build-time processing**: Maximum work done at compile time
- **Reactive by default**: Non-blocking I/O throughout the stack
- **Vert.x foundation**: Built on Vert.x event loops
- **CDI integration**: Seamless integration with Quarkus CDI
- **Handler chain**: Customizable request processing pipeline
- **Zero reflection**: GraalVM native image compatible

## Architectural Layers

```
┌─────────────────────────────────────────────┐
│         JAX-RS / Jakarta REST API           │
│    (@Path, @GET, @POST, @Produces, etc.)    │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│         Quarkus REST Runtime Layer          │
│   (Resource method invocation, parameter    │
│    extraction, serialization)               │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│          Handler Chain Pipeline             │
│  (Security, Observability, Compression)     │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│            CDI / Arc Container              │
│     (Dependency injection, producers)       │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│         Vert.x HTTP Server / Router         │
│      (Event loop, routing, HTTP/2)          │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│              Netty (NIO)                    │
│          (Network I/O layer)                │
└─────────────────────────────────────────────┘
```

## Request Lifecycle

The complete flow of an HTTP request through Quarkus REST:

### 1. Request Reception

```
HTTP Request
    ↓
Netty NIO Thread
    ↓
Vert.x Event Loop
    ↓
Vert.x Router
```

The request is received by Netty's NIO layer and dispatched to a Vert.x event loop thread.

### 2. Initial Routing

```
Vert.x Router
    ↓
RestInitialHandler
    ↓
Route Matching (by path/method)
    ↓
QuarkusResteasyReactiveRequestContext created
```

The Vert.x router matches the request to a REST route and creates the request context.

### 3. Request Scope Activation

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

class QuarkusResteasyReactiveRequestContext extends ResteasyReactiveRequestContext {
    protected void handleRequestScopeActivation();
    protected void requestScopeDeactivated();
    protected SecurityContext createSecurityContext();
}
```

The request scope is activated, making CDI request-scoped beans available.

### 4. Handler Chain Execution - AFTER_PRE_MATCH Phase

```
AFTER_PRE_MATCH handlers execute:
    ↓
- Early request filtering
- Request logging
- Protocol upgrades
```

Handlers registered for the AFTER_PRE_MATCH phase execute.

### 5. Resource Method Matching

```
Resource matching
    ↓
Resource class identified
    ↓
Resource method selected
    ↓
ServerResourceMethod resolved
```

The framework matches the request to a specific resource method.

### 6. Handler Chain Execution - AFTER_MATCH Phase

```
AFTER_MATCH handlers execute:
    ↓
- Security checks (EagerSecurityHandler)
    ↓
- Observability tracking (ObservabilityHandler)
    ↓
- Compression setup (ResteasyReactiveCompressionHandler)
    ↓
- Custom validators
```

Security, observability, and other handlers execute before method invocation.

### 7. Security Processing

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.security;

@Singleton
class EagerSecurityContext {
    static EagerSecurityContext instance;
    Uni<SecurityIdentity> getDeferredIdentity();
    Uni<SecurityIdentity> getPermissionCheck(
        ResteasyReactiveRequestContext requestContext,
        SecurityIdentity identity
    );
}
```

Security checks are performed asynchronously using Mutiny Uni.

### 8. Parameter Extraction

```
Extract method parameters:
    ↓
- @PathParam from URI path
- @QueryParam from query string
- @HeaderParam from headers
- @Context from CDI/JAX-RS context
- Request body deserialization
```

Method parameters are extracted and converted.

### 9. Resource Method Invocation

```
Invoke resource method
    ↓
Execute business logic
    ↓
Return response (Object, Response, Uni, Multi)
```

The actual REST resource method executes.

### 10. Handler Chain Execution - AFTER_METHOD_INVOKE Phase

```
AFTER_METHOD_INVOKE handlers execute:
    ↓
- WebSocket upgrade (if applicable)
- Response modification
- Response logging
```

Post-invocation handlers process the response.

### 11. Response Serialization

```
Response object
    ↓
MessageBodyWriter selection
    ↓
Serialization (JSON, XML, etc.)
    ↓
HTTP response entity
```

The response is serialized using appropriate message body writers.

### 12. Response Transmission

```
HTTP Response
    ↓
Compression (if enabled)
    ↓
Vert.x write to socket
    ↓
Netty NIO
    ↓
Network
```

The response is sent back to the client.

### 13. Request Scope Deactivation

```
Request complete
    ↓
Closer cleanup (registered Closeables)
    ↓
Request scope deactivation
    ↓
CDI @Disposes methods
```

Cleanup happens automatically through the Closer mechanism.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

@Singleton
class QuarkusContextProducers {
    @Produces
    @RequestScoped
    CloserImpl closer();

    @Disposes
    void closeCloser(CloserImpl closer);
}
```

## Handler Chain Architecture

The handler chain is the core request processing pipeline.

### Handler Interface

```java { .api }
package org.jboss.resteasy.reactive.server.spi;

interface ServerRestHandler {
    void handle(ResteasyReactiveRequestContext requestContext) throws Exception;
}
```

All handlers implement this simple interface.

### Handler Phases

```java { .api }
enum Phase {
    AFTER_PRE_MATCH,    // Before resource matching
    AFTER_MATCH,        // After matching, before invocation
    AFTER_METHOD_INVOKE // After invocation, before response
}
```

### Handler Chain Customization

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

Applications can add custom handlers at each phase.

### Built-in Handlers

**Security Handlers**:

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.security;

class EagerSecurityHandler implements ServerRestHandler {
    void handle(ResteasyReactiveRequestContext requestContext);
}

class EagerSecurityInterceptorHandler implements ServerRestHandler {
    void handle(ResteasyReactiveRequestContext requestContext);
}

class SecurityContextOverrideHandler implements ServerRestHandler {
    void handle(ResteasyReactiveRequestContext requestContext);
}
```

**Observability Handlers**:

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.observability;

class ObservabilityHandler implements ServerRestHandler {
    String getTemplatePath();
    void setTemplatePath(String templatePath);
    void handle(ResteasyReactiveRequestContext requestContext);
}
```

**Compression Handlers**:

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

class ResteasyReactiveCompressionHandler implements ServerRestHandler {
    HttpCompression getCompression();
    void setCompression(HttpCompression compression);
    void handle(ResteasyReactiveRequestContext requestContext);
}
```

## CDI Integration

Quarkus REST provides seamless CDI integration through Arc, Quarkus's CDI implementation.

### Context Producers

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

These producers make JAX-RS context types available for CDI injection.

### @Context as @Inject Alias

In Quarkus REST, `@Context` and `@Inject` are interchangeable for context types:

```java
// Both work identically:
@Context UriInfo uriInfo;
@Inject UriInfo uriInfo;
```

### Request-Scoped Contexts

Context objects like UriInfo, HttpHeaders, and SecurityContext are request-scoped, ensuring each request has its own instance.

### Resource Lifecycle

REST resource classes can use any CDI scope:

- `@ApplicationScoped` - Singleton, shared across requests
- `@RequestScoped` - New instance per request
- `@Dependent` - New instance per injection point (default)

## Reactive Layer

Quarkus REST is built on reactive principles using Smallrye Mutiny.

### Reactive Return Types

Methods can return reactive types:

```java
@GET
public Uni<Book> getAsync() {
    return bookService.findAsync();
}

@GET
public Multi<Book> stream() {
    return bookService.streamAll();
}

@GET
public CompletionStage<Book> getCompletionStage() {
    return bookService.findAsyncJava();
}
```

### Non-Blocking Security

Security checks return `Uni<SecurityIdentity>`:

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.security;

@Singleton
class EagerSecurityContext {
    Uni<SecurityIdentity> getDeferredIdentity();
    Uni<SecurityIdentity> getPermissionCheck(
        ResteasyReactiveRequestContext requestContext,
        SecurityIdentity identity
    );
}
```

### Reactive Exception Mappers

Exception mappers can return reactive responses:

```java
@ServerExceptionMapper
public Uni<Response> handleException(CustomException ex) {
    return logService.logAsync(ex)
        .onItem().transform(logId ->
            Response.status(500)
                .entity(new ErrorResponse(logId))
                .build()
        );
}
```

### Event Loop Execution

By default, REST methods execute on the Vert.x event loop (non-blocking). Methods can opt into worker threads:

```java
@GET
@Blocking  // Execute on worker thread
public String blockingOperation() {
    return performBlockingIO();
}

@GET
@RunOnVirtualThread  // Execute on virtual thread (JDK 21+)
public String virtualThreadOperation() {
    return performBlockingIO();
}
```

## Vert.x Foundation

Quarkus REST is built on Vert.x for the HTTP layer.

### Vert.x Types

Direct access to Vert.x types:

```java { .api }
package io.vertx.core.http;

interface HttpServerRequest {
    String uri();
    String path();
    HttpMethod method();
    MultiMap headers();
    SocketAddress remoteAddress();
}

interface HttpServerResponse {
    HttpServerResponse setStatusCode(int statusCode);
    HttpServerResponse putHeader(String name, String value);
    void end();
}
```

```java { .api }
package io.vertx.ext.web;

interface RoutingContext {
    HttpServerRequest request();
    HttpServerResponse response();
    Map<String, Object> data();
}
```

### Vert.x Integration

REST resources can inject Vert.x types:

```java
@GET
public Response get(@Context HttpServerRequest request) {
    String remoteIp = request.remoteAddress().host();
    return Response.ok().build();
}

@GET
public void customResponse(@Context HttpServerResponse response) {
    response.setStatusCode(200);
    response.putHeader("X-Custom", "value");
    response.end("Custom response");
}
```

### WebSocket Support

WebSocket endpoints use Vert.x ServerWebSocket:

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.websocket;

class VertxWebSocketParamExtractor implements ParameterExtractor {
    Object extractParameter(ResteasyReactiveRequestContext requestContext);
}
```

## Build-Time Processing

Quarkus REST performs extensive build-time processing for optimal runtime performance.

### Recorder Pattern

Build-time logic uses the Recorder pattern:

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

class ResteasyReactiveRecorder {
    RuntimeValue<Deployment> createDeployment(...);
    RuntimeValue<RestInitialHandler> restInitialHandler(...);
    Handler<RoutingContext> handler(...);
    void registerExceptionMapper(...);
}
```

Recorders execute at build time to generate optimized runtime structures.

### Static Initialization

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

interface ResteasyReactiveInitialiser {
    void init(Deployment deployment);
}
```

Generated initialization code runs at build time.

### Zero Reflection

All reflection is eliminated at build time:

- Method invocation through generated invokers
- Parameter extraction through generated extractors
- Serialization through registered providers

This enables native image compilation and improves performance.

## Performance Optimizations

### Monomorphic Dispatch

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

class QuarkusResteasyReactiveRequestContext extends ResteasyReactiveRequestContext {
    protected void invokeHandler(int pos);
}
```

Handler invocation uses monomorphic instance checks instead of polymorphic calls for better JIT compilation.

### Eager Security

Security checks happen early in the handler chain, before method invocation, avoiding repeated checks:

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

abstract class StandardSecurityCheckInterceptor {
    @AroundInvoke
    Object intercept(InvocationContext ic);
}
```

CDI interceptors detect if security already checked and skip redundant work.

### Deferred Identity Resolution

Security identity is resolved lazily only when needed:

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.security;

@Singleton
class EagerSecurityContext {
    Uni<SecurityIdentity> getDeferredIdentity();
}
```

### Virtual Thread Support

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

class ResteasyReactiveRecorder {
    static Supplier<Executor> VTHREAD_EXECUTOR_SUPPLIER;
}
```

JDK 21+ virtual threads provide scalable blocking operations.

## Extension Points

Quarkus REST provides multiple extension points:

### Handler Chain Customizers

Add custom handlers:

```java
@ApplicationScoped
public class CustomHandlerChainCustomizer implements HandlerChainCustomizer {
    public List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass, ServerResourceMethod resourceMethod) {
        // Return custom handlers
    }
}
```

### Exception Mappers

Custom exception handling:

```java
@ServerExceptionMapper
public Response handleException(CustomException ex) {
    return Response.status(500).build();
}
```

### Message Body Readers/Writers

Custom serialization:

```java
@Provider
public class CustomMessageBodyWriter implements MessageBodyWriter<CustomType> {
    // Serialization logic
}
```

### Parameter Extractors

Custom parameter extraction:

```java
public class CustomParamExtractor implements ParameterExtractor {
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        // Extraction logic
    }
}
```

## Thread Model

### Event Loop Threads

Default execution happens on Vert.x event loop threads (non-blocking):

- Number of event loops = CPU cores × 2 (default)
- All I/O operations must be non-blocking
- Use reactive types (Uni, Multi, CompletionStage)

### Worker Threads

Blocking operations use worker threads:

```java
@Blocking
@GET
public String blockingOp() {
    return performBlockingIO();
}
```

Worker pool size configurable via `quarkus.thread-pool.max-threads`.

### Virtual Threads

JDK 21+ enables virtual threads for scalable blocking:

```java
@RunOnVirtualThread
@GET
public String virtualThreadOp() {
    return performBlockingIO();
}
```

## Native Image Support

Quarkus REST is fully compatible with GraalVM native images:

- Zero reflection at runtime
- All metadata generated at build time
- Serialization providers registered at build time
- Native executable with fast startup and low memory usage

## Complete Request Flow Diagram

```
┌─────────────────────────────────────────────────────┐
│ 1. HTTP Request arrives at Netty                    │
└────────────────┬────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────┐
│ 2. Vert.x Event Loop Thread                         │
└────────────────┬────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────┐
│ 3. RestInitialHandler - Create Request Context      │
└────────────────┬────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────┐
│ 4. Activate Request Scope + Security Setup          │
└────────────────┬────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────┐
│ 5. AFTER_PRE_MATCH Handler Chain                    │
│    - Request logging                                │
│    - Early filtering                                │
└────────────────┬────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────┐
│ 6. Resource Method Matching                         │
└────────────────┬────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────┐
│ 7. AFTER_MATCH Handler Chain                        │
│    - EagerSecurityHandler (security checks)         │
│    - ObservabilityHandler (metrics/tracing)         │
│    - ResteasyReactiveCompressionHandler             │
└────────────────┬────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────┐
│ 8. Parameter Extraction                             │
│    - Path/Query/Header params                       │
│    - Request body deserialization                   │
│    - Context injection                              │
└────────────────┬────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────┐
│ 9. Resource Method Invocation                       │
│    (on event loop or worker/virtual thread)         │
└────────────────┬────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────┐
│ 10. AFTER_METHOD_INVOKE Handler Chain               │
│     - Response modification                         │
│     - WebSocket upgrade                             │
└────────────────┬────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────┐
│ 11. Response Serialization                          │
│     - MessageBodyWriter selection                   │
│     - JSON/XML/etc serialization                    │
└────────────────┬────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────┐
│ 12. HTTP Response Transmission                      │
│     - Compression (if enabled)                      │
│     - Vert.x socket write                           │
└────────────────┬────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────────────────┐
│ 13. Request Scope Deactivation                      │
│     - Closer cleanup                                │
│     - CDI @Disposes                                 │
└─────────────────────────────────────────────────────┘
```

This architecture delivers high performance, low memory usage, and excellent scalability while maintaining compatibility with Jakarta REST standards.
