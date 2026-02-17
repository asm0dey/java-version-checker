# Advanced Patterns

Complex implementation patterns and advanced use cases.

## Custom Handler Chain

Extend the request processing pipeline:

```java
import org.jboss.resteasy.reactive.server.spi.*;

@ApplicationScoped
public class CustomHandlerProvider implements HandlerChainCustomizer {

    @Override
    public List<ServerRestHandler> handlers(
            Phase phase,
            ResourceClass resourceClass,
            ServerResourceMethod resourceMethod) {
        
        if (phase == Phase.AFTER_MATCH) {
            return List.of(new RateLimitHandler());
        }
        return Collections.emptyList();
    }
}

public class RateLimitHandler implements ServerRestHandler {

    @Override
    public void handle(ResteasyReactiveRequestContext ctx) {
        String clientId = ctx.getHeader("X-Client-ID");
        
        if (!rateLimiter.allowRequest(clientId)) {
            ctx.abortWith(Response.status(429)
                .entity("Rate limit exceeded")
                .build());
            return;
        }
        
        ctx.resume();
    }
}
```

## Streaming Large Files

```java
@GET
@Path("/download/{id}")
@Produces(MediaType.APPLICATION_OCTET_STREAM)
public Response downloadLargeFile(@PathParam("id") String id) {
    File file = fileService.getFile(id);
    
    StreamingOutput stream = output -> {
        try (InputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }
    };
    
    return Response.ok(stream)
        .header("Content-Disposition", 
            "attachment; filename=\"" + file.getName() + "\"")
        .header("Content-Length", file.length())
        .build();
}
```

## Reactive Batch Processing

```java
@POST
@Path("/batch")
public Uni<BatchResult> processBatch(List<Item> items) {
    return Multi.createFrom().iterable(items)
        .onItem().transformToUniAndMerge(item ->
            processItem(item)
                .onFailure().recoverWithItem(error -> {
                    LOG.error("Failed to process item: " + item.id, error);
                    return new ProcessResult(item.id, false, error.getMessage());
                })
        )
        .collect().asList()
        .onItem().transform(results -> {
            long successful = results.stream()
                .filter(r -> r.success)
                .count();
            return new BatchResult(successful, results.size() - successful);
        });
}

private Uni<ProcessResult> processItem(Item item) {
    return repository.persist(item)
        .onItem().transform(saved -> 
            new ProcessResult(saved.id, true, null)
        );
}
```

## GraphQL-Style Field Selection

```java
@GET
@Path("/{id}")
public Response get(
        @PathParam("id") Long id,
        @QueryParam("fields") String fields) {
    
    User user = userService.findById(id);
    if (user == null) {
        return Response.status(404).build();
    }
    
    if (fields != null) {
        Set<String> fieldSet = Set.of(fields.split(","));
        Map<String, Object> filtered = new HashMap<>();
        
        if (fieldSet.contains("id")) filtered.put("id", user.id);
        if (fieldSet.contains("name")) filtered.put("name", user.name);
        if (fieldSet.contains("email")) filtered.put("email", user.email);
        
        return Response.ok(filtered).build();
    }
    
    return Response.ok(user).build();
}
```

## Conditional Endpoint Activation

```java
import io.quarkus.resteasy.reactive.server.EndpointDisabled;

@Path("/admin")
@EndpointDisabled(
    name = "admin.endpoints.enabled",
    disableIfMissing = true
)
public class AdminResource {

    @GET
    @Path("/stats")
    public Stats getStats() {
        return statsService.getStats();
    }
}
```

`application.properties`:

```properties
admin.endpoints.enabled=true
```

## Custom Jackson Serialization

```java
import io.quarkus.resteasy.reactive.jackson.CustomSerialization;
import com.fasterxml.jackson.databind.*;

@GET
@Path("/users/{id}")
@CustomSerialization(UserSerializer.class)
public User get(@PathParam("id") Long id) {
    return userService.findById(id);
}

public class UserSerializer 
        implements BiFunction<ObjectMapper, Type, ObjectWriter> {
    
    @Override
    public ObjectWriter apply(ObjectMapper mapper, Type type) {
        return mapper.writerWithView(PublicView.class);
    }
}
```

## Role-Based Field Filtering

```java
import io.quarkus.resteasy.reactive.jackson.SecureField;
import io.quarkus.resteasy.reactive.jackson.EnableSecureSerialization;

public class User {
    public Long id;
    public String name;
    
    @SecureField(rolesAllowed = {"admin"})
    public String email;
    
    @SecureField(rolesAllowed = {"admin", "manager"})
    public BigDecimal salary;
}

@Path("/users")
public class UserResource {

    @GET
    @Path("/{id}")
    @EnableSecureSerialization
    public User get(@PathParam("id") Long id) {
        return userService.findById(id);
    }
}
```

## Server-Side Multipart Generation

```java
import org.jboss.resteasy.reactive.server.multipart.*;

@GET
@Path("/export/{id}")
@Produces(MediaType.MULTIPART_FORM_DATA)
public MultipartFormDataOutput export(@PathParam("id") Long id) {
    Report report = reportService.generate(id);
    
    MultipartFormDataOutput output = new MultipartFormDataOutput();
    
    output.addFormData("metadata", report.metadata, 
        MediaType.APPLICATION_JSON_TYPE);
    
    output.addFormData("data", report.data, 
        MediaType.APPLICATION_OCTET_STREAM_TYPE, 
        "report.csv");
    
    output.addFormData("summary", report.summary, 
        MediaType.TEXT_PLAIN_TYPE);
    
    return output;
}
```

## Reactive Circuit Breaker Pattern

```java
import io.smallrye.faulttolerance.api.CircuitBreakerName;

@Path("/external")
public class ExternalApiResource {

    @RestClient
    ExternalApiClient externalApi;

    @Inject
    CacheService cache;

    @GET
    @Path("/data")
    @CircuitBreaker(
        requestVolumeThreshold = 4,
        failureRatio = 0.5,
        delay = 10000
    )
    @CircuitBreakerName("external-api")
    public Uni<Data> getData() {
        return externalApi.fetchData()
            .onFailure().recoverWithUni(() -> {
                LOG.warn("External API failed, using cache");
                return cache.getData();
            });
    }

    @GET
    @Path("/circuit-status")
    public CircuitBreakerStatus getCircuitStatus() {
        return circuitBreakerRegistry.getStatus("external-api");
    }
}
```

## Parallel Async Operations with Timeout

```java
@GET
@Path("/dashboard")
public Uni<Dashboard> getDashboard() {
    Uni<UserStats> stats = statsService.getUserStats()
        .ifNoItem().after(Duration.ofSeconds(5))
        .fail();
    
    Uni<List<Notification>> notifications = notificationService.getRecent()
        .ifNoItem().after(Duration.ofSeconds(3))
        .recoverWithItem(Collections.emptyList());
    
    Uni<List<Activity>> activities = activityService.getRecent()
        .ifNoItem().after(Duration.ofSeconds(3))
        .recoverWithItem(Collections.emptyList());
    
    return Uni.combine().all()
        .unis(stats, notifications, activities)
        .asTuple()
        .onItem().transform(tuple ->
            new Dashboard(
                tuple.getItem1(),
                tuple.getItem2(),
                tuple.getItem3()
            )
        );
}
```

## Custom Message Body Reader/Writer

```java
import org.jboss.resteasy.reactive.server.spi.*;

@Provider
@Consumes("application/x-custom")
public class CustomReader implements ServerMessageBodyReader<CustomData> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
            ResteasyReactiveResourceInfo lazyMethod, MediaType mediaType) {
        return type == CustomData.class;
    }

    @Override
    public CustomData readFrom(Class<CustomData> type, Type genericType,
            MediaType mediaType, ServerRequestContext context) 
            throws IOException {
        InputStream input = context.getInputStream();
        // Custom deserialization logic
        return parseCustomFormat(input);
    }
}

@Provider
@Produces("application/x-custom")
public class CustomWriter implements ServerMessageBodyWriter<CustomData> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
            ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return type == CustomData.class;
    }

    @Override
    public void writeResponse(CustomData data, Type genericType,
            ServerRequestContext context) throws IOException {
        OutputStream output = context.getOutputStream();
        // Custom serialization logic
        writeCustomFormat(data, output);
    }
}
```

## REST Data Panache with Custom Methods

```java
import io.quarkus.rest.data.panache.PanacheRepositoryResource;
import io.quarkus.rest.data.panache.ResourceProperties;
import io.quarkus.rest.data.panache.MethodProperties;

@ResourceProperties(hal = true, paged = true)
public interface ProductResource 
        extends PanacheRepositoryResource<ProductRepository, Product, Long> {

    @MethodProperties(path = "active")
    @GET
    default List<Product> listActive() {
        return ProductRepository.find("active", true).list();
    }

    @MethodProperties(path = "search")
    @GET
    default List<Product> search(@QueryParam("q") String query) {
        return ProductRepository.find(
            "name like ?1 or description like ?1",
            "%" + query + "%"
        ).list();
    }
}
```

## WebSocket Integration

```java
import io.vertx.core.http.ServerWebSocket;

@Path("/ws")
public class WebSocketResource {

    @GET
    @Path("/connect")
    public void connect(@Context ServerWebSocket webSocket) {
        webSocket.textMessageHandler(message -> {
            // Process message
            String response = processMessage(message);
            webSocket.writeTextMessage(response);
        });
        
        webSocket.closeHandler(v -> {
            LOG.info("WebSocket closed");
        });
    }
}
```

## See Also

- [Common Scenarios](common-scenarios.md) - Basic patterns
- [Integration Examples](integration-examples.md) - Integration patterns
- [Handler Chain Reference](../reference/handler-chain.md)
- [Server SPI Reference](../reference/server-spi.md)
