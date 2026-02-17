# Streaming Responses

Quarkus REST provides `RestMulti` for streaming responses with full control over HTTP status codes and headers, built on top of Mutiny's `Multi` reactive type.

## Capabilities

### RestMulti Class

Type-safe wrapper around Mutiny `Multi` for streaming HTTP responses with status codes and headers.

```java { .api }
package org.jboss.resteasy.reactive;

/**
 * Wrapper around Mutiny Multi for streaming responses with HTTP metadata.
 * Allows setting status code and headers for streaming endpoints.
 * Provides builder pattern for configuration.
 *
 * @param <T> The type of elements in the stream
 */
class RestMulti<T> {
    /**
     * Get the HTTP status code for this response.
     * @return Status code or null if not set
     */
    Integer getStatus();

    /**
     * Get the HTTP headers for this response.
     * @return Map of header names to values
     */
    Map<String, List<String>> getHeaders();

    /**
     * Create a RestMulti from a Multi data source.
     * Returns a builder for configuring the streaming response.
     *
     * @param data The Multi data source
     * @return Builder for configuring RestMulti
     */
    static <T> SyncRestMulti.Builder<T> fromMultiData(Multi<T> data);

    /**
     * Create a RestMulti from a Uni that produces a Multi.
     * Useful for async initialization of streaming responses.
     *
     * @param asyncResponse Uni that produces the entity
     * @param mapper Function to extract Multi from entity
     * @return RestMulti streaming the data
     */
    static <T, R> RestMulti<R> fromUniResponse(
        Uni<T> asyncResponse,
        Function<T, Multi<R>> mapper
    );

    /**
     * Create a RestMulti from a Uni with custom status and headers.
     *
     * @param asyncResponse Uni that produces the entity
     * @param mapper Function to extract Multi from entity
     * @param headersMapper Function to extract headers from entity
     * @param statusMapper Function to extract status from entity
     * @return RestMulti streaming the data with metadata
     */
    static <T, R> RestMulti<R> fromUniResponse(
        Uni<T> asyncResponse,
        Function<T, Multi<R>> mapper,
        Function<T, Map<String, List<String>>> headersMapper,
        Function<T, Integer> statusMapper
    );

    /**
     * Inner builder class for constructing RestMulti instances.
     */
    class SyncRestMulti {
        static class Builder<T> {
            /**
             * Set the initial demand for the stream (default: 1).
             * Controls backpressure and concurrency:
             * - demand = 1: Serial processing (one item at a time)
             * - demand > 1: Concurrent processing (multiple items at once)
             *
             * @param demand Initial demand count
             * @return This builder
             */
            Builder<T> withDemand(long demand);

            /**
             * Set whether to encode the stream as a JSON array (default: true).
             * When true, wraps stream elements in JSON array brackets [].
             * When false, sends elements as newline-delimited JSON.
             *
             * @param encodeAsJsonArray Whether to encode as JSON array
             * @return This builder
             */
            Builder<T> encodeAsJsonArray(boolean encodeAsJsonArray);

            /**
             * Set the HTTP status code for the response.
             *
             * @param status HTTP status code
             * @return This builder
             */
            Builder<T> status(int status);

            /**
             * Add an HTTP header to the response.
             *
             * @param name Header name
             * @param value Header value
             * @return This builder
             */
            Builder<T> header(String name, String value);

            /**
             * Build the RestMulti instance.
             *
             * @return Configured RestMulti
             */
            RestMulti<T> build();
        }
    }
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.RestMulti;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.time.Duration;
import java.util.List;

@Path("/stream")
public class StreamingResource {

    // Basic streaming with default settings
    @GET
    @Path("/numbers")
    @Produces(MediaType.APPLICATION_JSON)
    public RestMulti<Integer> streamNumbers() {
        Multi<Integer> numbers = Multi.createFrom().range(1, 11);
        return RestMulti.fromMultiData(numbers).build();
    }

    // Streaming with custom status code
    @GET
    @Path("/data")
    @Produces(MediaType.APPLICATION_JSON)
    public RestMulti<Data> streamData() {
        Multi<Data> dataStream = dataService.streamAll();

        return RestMulti.fromMultiData(dataStream)
            .status(200)
            .build();
    }

    // Streaming with custom headers
    @GET
    @Path("/events")
    @Produces(MediaType.APPLICATION_JSON)
    public RestMulti<Event> streamEvents() {
        Multi<Event> events = eventService.streamEvents();

        return RestMulti.fromMultiData(events)
            .status(200)
            .header("X-Stream-Type", "events")
            .header("X-Stream-Version", "1.0")
            .header("Cache-Control", "no-cache")
            .build();
    }

    // Streaming with backpressure control
    @GET
    @Path("/large-data")
    @Produces(MediaType.APPLICATION_JSON)
    public RestMulti<LargeObject> streamLargeData() {
        Multi<LargeObject> data = dataService.streamLargeObjects();

        return RestMulti.fromMultiData(data)
            .withDemand(5)  // Request 5 items initially
            .status(200)
            .build();
    }

    // Newline-delimited JSON (not JSON array)
    @GET
    @Path("/ndjson")
    @Produces(MediaType.APPLICATION_JSON)
    public RestMulti<Record> streamNDJSON() {
        Multi<Record> records = recordService.streamAll();

        return RestMulti.fromMultiData(records)
            .encodeAsJsonArray(false)  // Send as newline-delimited JSON
            .build();
    }

    // JSON array encoding (default)
    @GET
    @Path("/json-array")
    @Produces(MediaType.APPLICATION_JSON)
    public RestMulti<Item> streamJSONArray() {
        Multi<Item> items = itemService.streamAll();

        return RestMulti.fromMultiData(items)
            .encodeAsJsonArray(true)  // Wrap in JSON array []
            .build();
    }

    // Async initialization with fromUniResponse
    @GET
    @Path("/async-init")
    @Produces(MediaType.APPLICATION_JSON)
    public RestMulti<Message> streamWithAsyncInit() {
        // Uni that resolves to a container with data
        Uni<MessageContainer> containerUni = messageService.getContainer();

        // Extract Multi from container
        return RestMulti.fromUniResponse(
            containerUni,
            container -> container.getMessages()
        );
    }

    // Async with custom status and headers
    @GET
    @Path("/async-full")
    @Produces(MediaType.APPLICATION_JSON)
    public RestMulti<Result> streamWithFullAsyncConfig() {
        Uni<ResultSet> resultSetUni = queryService.executeAsync();

        return RestMulti.fromUniResponse(
            resultSetUni,
            // Extract Multi from ResultSet
            resultSet -> resultSet.getResults(),
            // Extract headers from ResultSet
            resultSet -> Map.of(
                "X-Total-Count", List.of(String.valueOf(resultSet.getTotalCount())),
                "X-Query-Time", List.of(String.valueOf(resultSet.getQueryTimeMs()))
            ),
            // Extract status from ResultSet
            resultSet -> resultSet.hasResults() ? 200 : 204
        );
    }

    // Timed streaming
    @GET
    @Path("/ticks")
    @Produces(MediaType.APPLICATION_JSON)
    public RestMulti<Long> streamTicks() {
        Multi<Long> ticks = Multi.createFrom().ticks()
            .every(Duration.ofSeconds(1))
            .select().first(10);

        return RestMulti.fromMultiData(ticks)
            .header("X-Tick-Interval", "1s")
            .build();
    }

    // Conditional streaming based on query params
    @GET
    @Path("/conditional")
    @Produces(MediaType.APPLICATION_JSON)
    public RestMulti<Book> streamConditional(
            @QueryParam("format") @DefaultValue("array") String format,
            @QueryParam("limit") @DefaultValue("100") int limit) {

        Multi<Book> books = bookService.streamBooks()
            .select().first(limit);

        return RestMulti.fromMultiData(books)
            .encodeAsJsonArray("array".equals(format))
            .header("X-Format", format)
            .header("X-Limit", String.valueOf(limit))
            .build();
    }

    @Inject
    DataService dataService;

    @Inject
    EventService eventService;

    @Inject
    RecordService recordService;

    @Inject
    ItemService itemService;

    @Inject
    MessageService messageService;

    @Inject
    QueryService queryService;

    @Inject
    BookService bookService;
}

class Data {
    public String value;
}

class Event {
    public String type;
    public long timestamp;
}

class LargeObject {
    public byte[] data;
}

class Record {
    public int id;
    public String name;
}

class Item {
    public String sku;
    public double price;
}

class Message {
    public String text;
}

class MessageContainer {
    public Multi<Message> getMessages() {
        return Multi.createFrom().empty();
    }
}

class Result {
    public String data;
}

class ResultSet {
    public Multi<Result> getResults() {
        return Multi.createFrom().empty();
    }
    public int getTotalCount() { return 0; }
    public long getQueryTimeMs() { return 0; }
    public boolean hasResults() { return true; }
}

class Book {
    public String title;
}

interface DataService {
    Multi<Data> streamAll();
    Multi<LargeObject> streamLargeObjects();
}

interface EventService {
    Multi<Event> streamEvents();
}

interface RecordService {
    Multi<Record> streamAll();
}

interface ItemService {
    Multi<Item> streamAll();
}

interface MessageService {
    Uni<MessageContainer> getContainer();
}

interface QueryService {
    Uni<ResultSet> executeAsync();
}

interface BookService {
    Multi<Book> streamBooks();
}
```

## Integration with Server-Sent Events (SSE)

`RestMulti` works with SSE for real-time event streaming:

```java
import org.jboss.resteasy.reactive.RestMulti;
import org.jboss.resteasy.reactive.RestStreamElementType;
import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.time.Duration;

@Path("/sse")
public class SSEResource {

    // SSE with RestMulti
    @GET
    @Path("/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public RestMulti<Event> streamSSE() {
        Multi<Event> events = Multi.createFrom().ticks()
            .every(Duration.ofSeconds(2))
            .onItem().transform(tick -> new Event("event-" + tick, System.currentTimeMillis()));

        return RestMulti.fromMultiData(events)
            .header("X-SSE-Retry", "5000")
            .build();
    }

    // SSE with custom event metadata
    @GET
    @Path("/notifications")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public RestMulti<Notification> streamNotifications() {
        Multi<Notification> notifications = notificationService.stream();

        return RestMulti.fromMultiData(notifications)
            .withDemand(1)  // Process notifications one at a time
            .header("X-Stream-Id", generateStreamId())
            .build();
    }

    private String generateStreamId() {
        return java.util.UUID.randomUUID().toString();
    }

    @Inject
    NotificationService notificationService;
}

class Event {
    public String id;
    public long timestamp;

    public Event(String id, long timestamp) {
        this.id = id;
        this.timestamp = timestamp;
    }
}

class Notification {
    public String message;
    public String severity;
}

interface NotificationService {
    Multi<Notification> stream();
}
```

## Comparison with Plain Multi

| Feature | `Multi<T>` | `RestMulti<T>` |
|---------|------------|----------------|
| Streaming data | ✓ | ✓ |
| Custom status code | ✗ | ✓ |
| Custom headers | ✗ | ✓ |
| Backpressure control | ✓ | ✓ (via `withDemand`) |
| JSON array encoding | Fixed | Configurable |
| Async initialization | Manual | Built-in (`fromUniResponse`) |

**When to use `RestMulti`:**
- Need custom HTTP status codes for streaming responses
- Want to add custom headers to streaming responses
- Need fine-grained control over JSON encoding format
- Require async initialization of stream sources

**When to use plain `Multi`:**
- Simple streaming without custom metadata
- Default JSON array encoding is sufficient
- No need for custom status or headers

## Error Handling in Streams

Handle errors in RestMulti streams:

```java
@GET
@Path("/safe-stream")
@Produces(MediaType.APPLICATION_JSON)
public RestMulti<Data> safeStream() {
    Multi<Data> data = dataService.streamAll()
        .onFailure().recoverWithMulti(error -> {
            log.error("Stream failed", error);
            // Return empty stream or fallback data
            return Multi.createFrom().items(Data.fallback());
        })
        .onFailure().invoke(error -> {
            // Log but continue
            log.warn("Item processing failed", error);
        });

    return RestMulti.fromMultiData(data)
        .status(200)
        .header("X-Error-Handling", "fallback")
        .build();
}
```

### StreamingOutputStream

Specialized `ByteArrayOutputStream` that allows `MessageBodyWriter` implementations to detect streaming context.

```java { .api }
package org.jboss.resteasy.reactive.server;

import java.io.ByteArrayOutputStream;

/**
 * A specialized ByteArrayOutputStream used to give MessageBodyWriter classes
 * the ability to tell if they are being called in a streaming context.
 *
 * This class extends ByteArrayOutputStream without adding any new methods or fields.
 * The type itself serves as a marker to indicate streaming context.
 */
class StreamingOutputStream extends ByteArrayOutputStream {
    // Inherits all methods from ByteArrayOutputStream:
    // - write(int b)
    // - write(byte[] b, int off, int len)
    // - toByteArray()
    // - size()
    // - reset()
    // - toString()
    // etc.
}
```

**Usage Notes:**
- Marker class pattern - the type provides semantic information rather than additional functionality
- `MessageBodyWriter` implementations can check `outputStream instanceof StreamingOutputStream`
- Allows writers to optimize behavior based on streaming vs non-streaming context
- Inherits all standard `ByteArrayOutputStream` methods

**Example MessageBodyWriter Implementation:**

```java
import org.jboss.resteasy.reactive.server.StreamingOutputStream;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import java.io.OutputStream;
import java.io.IOException;

@Provider
public class CustomDataWriter implements MessageBodyWriter<CustomData> {

    @Override
    public void writeTo(CustomData data, Class<?> type, Type genericType,
                       Annotation[] annotations, MediaType mediaType,
                       MultivaluedMap<String, Object> httpHeaders,
                       OutputStream outputStream) throws IOException {

        // Check if we're in a streaming context
        if (outputStream instanceof StreamingOutputStream) {
            // Use streaming-optimized serialization
            writeStreaming(data, outputStream);
        } else {
            // Use standard serialization
            writeStandard(data, outputStream);
        }
    }

    private void writeStreaming(CustomData data, OutputStream out) throws IOException {
        // Stream data incrementally without buffering entire content
        for (DataChunk chunk : data.getChunks()) {
            out.write(chunk.getBytes());
            out.flush();  // Flush each chunk immediately
        }
    }

    private void writeStandard(CustomData data, OutputStream out) throws IOException {
        // Buffer entire content before writing
        byte[] allData = data.toByteArray();
        out.write(allData);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
                              Annotation[] annotations, MediaType mediaType) {
        return CustomData.class.isAssignableFrom(type);
    }
}

class CustomData {
    public List<DataChunk> getChunks() { return List.of(); }
    public byte[] toByteArray() { return new byte[0]; }
}

class DataChunk {
    public byte[] getBytes() { return new byte[0]; }
}
```

## Performance Considerations

**Backpressure**: Use `withDemand()` to control memory usage with large streams:

```java
// Good for large datasets
RestMulti.fromMultiData(largeDataStream)
    .withDemand(10)  // Request 10 items at a time
    .build();
```

**JSON Encoding**: Choose encoding based on client requirements:
- `encodeAsJsonArray(true)`: Standard JSON array, good for small datasets
- `encodeAsJsonArray(false)`: Newline-delimited JSON (NDJSON), better for large streams

**Async Initialization**: Use `fromUniResponse` to avoid blocking during setup:

```java
// Non-blocking initialization
RestMulti.fromUniResponse(
    dataService.initializeAsync(),  // Non-blocking setup
    container -> container.stream()
);
```
