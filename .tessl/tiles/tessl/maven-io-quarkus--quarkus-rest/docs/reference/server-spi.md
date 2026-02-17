# Server SPI

Advanced server-side SPI (Service Provider Interface) for low-level request/response handling, custom filters, exception mappers, and message body readers/writers. These interfaces provide fine-grained control over the request processing pipeline for framework extensions and advanced use cases.

## Capabilities

### HTTP Request Interface

Low-level interface for accessing HTTP request details with full control over headers, parameters, body streaming, and connection management.

```java { .api }
package org.jboss.resteasy.reactive.server.spi;

/**
 * Server-side HTTP request abstraction providing low-level access
 * to request details and streaming capabilities.
 */
public interface ServerHttpRequest {

    // Header operations
    String getRequestHeader(CharSequence name);
    MultivaluedMap<String, String> getAllRequestHeaders();
    boolean containsRequestHeader(CharSequence name);

    // Request path and method
    String getRequestPath();
    String getRequestMethod();
    String getRequestNormalisedPath();
    URI getRequestAbsoluteUri();
    String getRequestScheme();
    String getRequestHost();

    // Query parameters
    String getQueryParam(String name);
    MultivaluedMap<String, String> getAllQueryParams();
    String query();
    Set<String> queryParamNames();

    // Body streaming
    ServerHttpRequest createInputStream(ByteBuffer buffer);
    InputStream createInputStream();

    // Flow control
    ServerHttpRequest pauseRequestInput();
    ServerHttpRequest resumeRequestInput();
    ServerHttpRequest setReadListener(ReadCallback callback);

    // State
    boolean isRequestEnded();
    boolean isOnIoThread();

    // Form data
    Uni<MultiValueMap<String, String>> getExistingParsedForm();

    // Connection management
    void closeConnection();

    // Type unwrapping
    <T> T unwrap(Class<T> target);
}
```

### HTTP Response Interface

Low-level interface for controlling HTTP responses including status, headers, body streaming, and lifecycle management.

```java { .api }
package org.jboss.resteasy.reactive.server.spi;

/**
 * Server-side HTTP response abstraction extending streaming capabilities
 * with comprehensive control over response headers, status, and body.
 */
public interface ServerHttpResponse extends StreamingResponse<ServerHttpResponse> {

    // Status
    int getStatusCode();
    ServerHttpResponse setStatusCode(int statusCode);

    // Headers
    ServerHttpResponse setResponseHeader(CharSequence name, CharSequence value);
    ServerHttpResponse setResponseHeader(CharSequence name, Iterable<CharSequence> values);
    ServerHttpResponse addResponseHeader(CharSequence name, CharSequence value);
    ServerHttpResponse removeResponseHeader(CharSequence name);
    String getResponseHeader(String name);
    Iterable<Map.Entry<String, String>> getAllResponseHeaders();

    // Body operations
    ServerHttpResponse end();
    ServerHttpResponse end(byte[] data);
    ServerHttpResponse end(String data, String encoding);
    boolean closed();
    boolean headWritten();
    ServerHttpResponse write(byte[] data);
    OutputStream createResponseOutputStream();
    ServerHttpResponse sendFile(Path path, long offset, long length);

    // Chunking
    ServerHttpResponse setChunked(boolean chunked);

    // Buffering and flow control
    boolean isWriteQueueFull();
    void addDrainHandler(Runnable onDrain);

    // Lifecycle
    void addCloseHandler(Runnable closeHandler);
    ServerHttpResponse setPreCommitListener(Runnable task);
}

/**
 * Base interface for streaming responses with type-safe builder pattern.
 */
public interface StreamingResponse<T> {
    T setStatusCode(int code);
    T setResponseHeader(CharSequence name, CharSequence value);
    T setResponseHeader(CharSequence name, Iterable<CharSequence> values);
}
```

### Server Request Context

Context object providing access to request/response, resource information, and request processing control during filter and handler execution.

```java { .api }
package org.jboss.resteasy.reactive.server.spi;

/**
 * Server request processing context available during filter and handler execution.
 * Provides access to request/response objects and processing control.
 */
public interface ServerRequestContext extends ResteasyReactiveCallbackContext {

    // Response access
    ServerHttpResponse serverResponse();
    OutputStream getOrCreateOutputStream();

    // Input/Content type
    InputStream getInputStream();
    MediaType getResponseContentType();
    MediaType getResponseMediaType();

    // Headers
    MultivaluedMap<String, String> getRequestHeaders();

    // Resource information
    ResteasyReactiveResourceInfo getResteasyReactiveResourceInfo();

    // Request control
    void abortWith(Response response);
}
```

### Reactive Container Filter Interfaces

SPI interfaces for implementing request and response filters with reactive support and full context access.

```java { .api }
package org.jboss.resteasy.reactive.server.spi;

/**
 * Request filter interface with reactive support.
 * Implemented by filters needing direct access to reactive request context.
 */
public interface ResteasyReactiveContainerRequestFilter {
    /**
     * Filter the incoming request.
     *
     * @param requestContext The reactive request context
     */
    void filter(ResteasyReactiveContainerRequestContext requestContext)
        throws IOException;
}

/**
 * Response filter interface with reactive support.
 * Implemented by filters needing to modify responses after endpoint execution.
 */
public interface ResteasyReactiveContainerResponseFilter {
    /**
     * Filter the response.
     *
     * @param requestContext The reactive request context
     * @param responseContext The standard response context
     */
    void filter(
        ResteasyReactiveContainerRequestContext requestContext,
        ContainerResponseContext responseContext
    ) throws IOException;
}

/**
 * Extended request context with reactive capabilities.
 * Extends standard JAX-RS ContainerRequestContext.
 */
public interface ResteasyReactiveContainerRequestContext
    extends ContainerRequestContext {
    // Inherits all JAX-RS ContainerRequestContext methods
    // Plus reactive-specific extensions
}
```

### Exception Mapper Interfaces

SPI interfaces for mapping exceptions to HTTP responses with reactive support and context access.

```java { .api }
package org.jboss.resteasy.reactive.server.spi;

/**
 * Exception mapper with access to server request context.
 * Provides more control than standard JAX-RS ExceptionMapper.
 *
 * @param <E> The exception type to handle
 */
public interface ResteasyReactiveExceptionMapper<E extends Throwable> {
    /**
     * Map exception to HTTP response with context access.
     *
     * @param exception The thrown exception
     * @param context Server request context
     * @return Response to send to client
     */
    Response toResponse(E exception, ServerRequestContext context);
}

/**
 * Async exception mapper returning reactive response.
 * Allows for async operations during exception handling.
 *
 * @param <E> The exception type to handle
 */
public interface ResteasyReactiveAsyncExceptionMapper<E extends Throwable> {
    /**
     * Handle exception asynchronously.
     *
     * @param exception The thrown exception
     * @param context Async exception mapper context
     */
    void asyncResponse(E exception, AsyncExceptionMapperContext context)
        throws Exception;

    /**
     * Synchronous mapping not supported - throws IllegalStateException.
     *
     * @param exception The thrown exception
     * @return Never returns normally
     * @throws IllegalStateException Always thrown
     */
    Response toResponse(E exception);
}

/**
 * Context for async exception mapping operations.
 * Provides methods to complete or fail the response asynchronously.
 */
public interface AsyncExceptionMapperContext {
    // Methods for async response completion
}
```

### Message Body Reader SPI

SPI interface for custom deserialization of request bodies with server context access.

```java { .api }
package org.jboss.resteasy.reactive.server.spi;

/**
 * Server-side message body reader with reactive context access.
 * Extends standard JAX-RS MessageBodyReader with server-specific capabilities.
 *
 * @param <T> The type this reader can deserialize
 */
public interface ServerMessageBodyReader<T> extends MessageBodyReader<T> {

    /**
     * Determine if this reader can handle the given type with server context.
     *
     * @param type The class of object to read
     * @param genericType The generic type
     * @param resourceInfo Resource method information
     * @param mediaType The media type
     * @return true if this reader can handle the type
     */
    boolean isReadable(
        Class<?> type,
        Type genericType,
        ResteasyReactiveResourceInfo resourceInfo,
        MediaType mediaType
    );

    /**
     * Read the entity from input stream with server context access.
     *
     * @param type The class of object to read
     * @param genericType The generic type
     * @param mediaType The media type
     * @param context Server request context
     * @return The deserialized entity
     */
    T readFrom(
        Class<T> type,
        Type genericType,
        MediaType mediaType,
        ServerRequestContext context
    ) throws WebApplicationException, IOException;
}
```

### Message Body Writer SPI

SPI interface for custom serialization of response bodies with server context access.

```java { .api }
package org.jboss.resteasy.reactive.server.spi;

/**
 * Server-side message body writer with reactive context access.
 * Extends standard JAX-RS MessageBodyWriter with server-specific capabilities.
 *
 * @param <T> The type this writer can serialize
 */
public interface ServerMessageBodyWriter<T> extends MessageBodyWriter<T> {

    /**
     * Determine if this writer can handle the given type with server context.
     *
     * @param type The class of object to write
     * @param genericType The generic type
     * @param resourceInfo Resource method information
     * @param mediaType The media type
     * @return true if this writer can handle the type
     */
    boolean isWriteable(
        Class<?> type,
        Type genericType,
        ResteasyReactiveResourceInfo resourceInfo,
        MediaType mediaType
    );

    /**
     * Write the entity to output with server context access.
     *
     * @param o The entity to write
     * @param genericType The generic type
     * @param context Server request context
     */
    void writeResponse(
        T o,
        Type genericType,
        ServerRequestContext context
    ) throws WebApplicationException, IOException;

    /**
     * Special writer that handles all types.
     * Used as catch-all for unhandled response types.
     */
    interface AllWriteableMessageBodyWriter extends ServerMessageBodyWriter<Object> {
        // Marker interface indicating this writer handles all types
    }
}
```

### Custom Handler Interface

SPI interface for implementing custom request handlers in the processing pipeline.

```java { .api }
package org.jboss.resteasy.reactive.server.spi;

/**
 * Custom handler in the server request processing pipeline.
 * Handlers can inspect, modify, or short-circuit request processing.
 */
public interface ServerRestHandler extends RestHandler<ResteasyReactiveRequestContext> {
    /**
     * Handle the request context.
     * Can modify context, perform operations, or abort processing.
     *
     * @param requestContext The request processing context
     * @throws Exception If handling fails
     */
    void handle(ResteasyReactiveRequestContext requestContext) throws Exception;
}

/**
 * Base interface for REST handlers.
 *
 * @param <T> The request context type
 */
public interface RestHandler<T> {
    void handle(T context) throws Exception;
}
```

### Resource Information

Interface providing metadata about the resource method being invoked.

```java { .api }
package org.jboss.resteasy.reactive.server.spi;

/**
 * Provides metadata about the resource method being invoked.
 * Available during filter and handler execution.
 */
public interface ResteasyReactiveResourceInfo extends ResourceInfo {

    // Method identification
    String getName();
    String getMethodId();
    Class<?>[] getParameterTypes();

    // Annotations
    Set<String> getClassAnnotationNames();
    Set<String> getMethodAnnotationNames();

    // Reflection access
    Method getMethod();
    Annotation[] getAnnotations();
    Annotation[] getParameterAnnotations(int index);
    Type getGenericReturnType();

    // Execution model
    boolean isNonBlocking();
}
```

### Runtime Configuration

Interface for accessing server runtime configuration.

```java { .api }
package org.jboss.resteasy.reactive.server.spi;

/**
 * Server runtime configuration accessible during request processing.
 */
public interface RuntimeConfiguration {

    /**
     * Request read timeout.
     *
     * @return Timeout duration
     */
    Duration readTimeout();

    /**
     * Request body configuration.
     */
    Body body();

    interface Body {
        boolean deleteUploadedFilesOnEnd();
        Optional<Path> uploadsDirectory();
        Charset defaultCharset();
        MultiPart multiPart();

        interface MultiPart {
            InputPart inputPart();

            interface InputPart {
                Charset defaultCharset();
            }
        }
    }

    /**
     * Request size limits.
     */
    Limits limits();

    interface Limits {
        Optional<Long> maxBodySize();
        long maxFormAttributeSize();
        int maxParameters();
    }
}
```

### Endpoint Invoker

Interface for invoking synthetic endpoint beans programmatically.

```java { .api }
package org.jboss.resteasy.reactive.server.spi;

/**
 * Invokes endpoint methods on synthetic beans.
 * Used internally by the framework for method invocation.
 */
public interface EndpointInvoker {
    /**
     * Invoke the endpoint method.
     *
     * @param instance The resource instance
     * @param parameters Method parameters
     * @return Method return value
     * @throws Exception If invocation fails
     */
    Object invoke(Object instance, Object[] parameters) throws Exception;
}

/**
 * Factory for creating endpoint invokers.
 */
public interface EndpointInvokerFactory {
    EndpointInvoker create();
}
```

### Configurable Handler Base Classes

Base classes for handlers that can be configured at runtime.

```java { .api }
package org.jboss.resteasy.reactive.server.spi;

/**
 * Base class for handlers that support runtime configuration.
 */
public abstract class RuntimeConfigurableServerRestHandler
    implements ServerRestHandler {

    /**
     * Configure this handler with runtime configuration.
     *
     * @param runtimeConfiguration Runtime configuration
     */
    public abstract void configure(RuntimeConfiguration runtimeConfiguration);
}

/**
 * Generic version supporting custom configuration types.
 *
 * @param <C> Configuration type
 */
public abstract class GenericRuntimeConfigurableServerRestHandler<C>
    extends RuntimeConfigurableServerRestHandler {

    /**
     * Configure with typed configuration.
     *
     * @param configuration Typed configuration object
     */
    public abstract void configure(C configuration);
}
```

## Usage Examples

### Custom Message Body Reader

```java
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;
import jakarta.ws.rs.ext.Provider;

@Provider
@Consumes("application/x-custom")
public class CustomFormatReader implements ServerMessageBodyReader<CustomData> {

    @Override
    public boolean isReadable(
        Class<?> type,
        Type genericType,
        ResteasyReactiveResourceInfo resourceInfo,
        MediaType mediaType
    ) {
        return type == CustomData.class &&
               "application/x-custom".equals(mediaType.toString());
    }

    @Override
    public CustomData readFrom(
        Class<CustomData> type,
        Type genericType,
        MediaType mediaType,
        ServerRequestContext context
    ) throws IOException {
        // Access server request for custom deserialization
        InputStream input = context.getInputStream();
        // Parse custom format
        return parseCustomFormat(input);
    }

    @Override
    public boolean isReadable(
        Class<?> type,
        Type genericType,
        Annotation[] annotations,
        MediaType mediaType
    ) {
        return isReadable(type, genericType, null, mediaType);
    }

    @Override
    public CustomData readFrom(
        Class<CustomData> type,
        Type genericType,
        Annotation[] annotations,
        MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders,
        InputStream entityStream
    ) throws IOException {
        return parseCustomFormat(entityStream);
    }

    private CustomData parseCustomFormat(InputStream input) throws IOException {
        // Implementation
        return new CustomData();
    }
}

class CustomData {}
```

### Custom Exception Mapper with Context

```java
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveExceptionMapper;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;
import jakarta.ws.rs.ext.Provider;

@Provider
public class DatabaseExceptionMapper
    implements ResteasyReactiveExceptionMapper<SQLException> {

    @Override
    public Response toResponse(SQLException exception, ServerRequestContext context) {
        // Access request details for error response
        String path = context.getResteasyReactiveResourceInfo().getName();

        // Build detailed error response
        ErrorResponse error = new ErrorResponse(
            "Database error in " + path,
            exception.getErrorCode(),
            exception.getSQLState()
        );

        return Response
            .status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(error)
            .build();
    }
}

class ErrorResponse {
    public String message;
    public int errorCode;
    public String sqlState;

    public ErrorResponse(String message, int errorCode, String sqlState) {
        this.message = message;
        this.errorCode = errorCode;
        this.sqlState = sqlState;
    }
}
```

### Custom Request Filter

```java
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestFilter;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveContainerRequestContext;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class CustomAuthFilter implements ResteasyReactiveContainerRequestFilter {

    @Override
    public void filter(ResteasyReactiveContainerRequestContext requestContext)
        throws IOException {

        // Access request headers
        String authHeader = requestContext.getHeaderString("Authorization");

        if (authHeader == null || !validateToken(authHeader)) {
            // Abort with 401
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid or missing authentication")
                    .build()
            );
        }
    }

    private boolean validateToken(String token) {
        // Validation logic
        return token != null && token.startsWith("Bearer ");
    }
}
```

### Custom Response Handler

```java
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import io.quarkus.resteasy.reactive.server.runtime.ResteasyReactiveRequestContext;

public class MetricsHandler implements ServerRestHandler {

    private final MetricsCollector metrics;

    public MetricsHandler(MetricsCollector metrics) {
        this.metrics = metrics;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        long startTime = System.currentTimeMillis();

        try {
            // Continue processing
            requestContext.resume();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            String path = requestContext.getResteasyReactiveResourceInfo().getName();
            metrics.recordRequest(path, duration);
        }
    }
}

interface MetricsCollector {
    void recordRequest(String path, long durationMs);
}
```

## Types

```java { .api }
// Supporting types referenced in SPI

package org.jboss.resteasy.reactive.server.spi;

interface ReadCallback {
    void done();
    void error(Throwable t);
}

interface ResteasyReactiveCallbackContext {
    // Base callback context
}

class ContentType {
    // Content type representation
}
```
