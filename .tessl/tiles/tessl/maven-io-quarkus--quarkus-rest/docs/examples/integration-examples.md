# Integration Examples

Examples of integrating Quarkus REST with other Quarkus features and external systems.

## Database Integration with Panache

### Reactive Panache

```java
import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;

@Entity
public class Book extends PanacheEntity {
    public String title;
    public String author;
    public BigDecimal price;
}

@ApplicationScoped
public class BookRepository implements PanacheRepository<Book> {
    
    public Uni<List<Book>> findByAuthor(String author) {
        return find("author", author).list();
    }
    
    public Uni<List<Book>> findExpensive() {
        return find("price > ?1", new BigDecimal("50")).list();
    }
}

@Path("/books")
public class BookResource {

    @Inject
    BookRepository repository;

    @GET
    public Uni<List<Book>> list() {
        return repository.listAll();
    }

    @GET
    @Path("/{id}")
    public Uni<RestResponse<Book>> get(@PathParam("id") Long id) {
        return repository.findById(id)
            .onItem().transform(book -> book != null
                ? RestResponse.ok(book)
                : RestResponse.notFound()
            );
    }

    @POST
    public Uni<RestResponse<Book>> create(Book book) {
        return Panache.withTransaction(() ->
            repository.persist(book)
        ).onItem().transform(created ->
            RestResponse.created(URI.create("/books/" + created.id))
        );
    }
}
```

## Qute Template Integration

```java
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;

@Path("/pages")
public class PageResource {

    @Inject
    Template book;  // Injects templates/book.html

    @Inject
    UserService userService;

    @GET
    @Path("/book/{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getBookPage(@PathParam("id") Long id) {
        Book book = bookService.findById(id);
        return book.data("book", book)
                   .data("reviews", reviewService.findByBook(id));
    }

    @GET
    @Path("/user/{id}")
    @Produces(MediaType.TEXT_HTML)
    public Uni<TemplateInstance> getUserPage(@PathParam("id") Long id) {
        return userService.findById(id)
            .onItem().transform(user ->
                book.data("user", user)
                    .data("books", user.books)
            );
    }
}
```

## Kafka Integration

```java
import org.eclipse.microprofile.reactive.messaging.*;

@Path("/orders")
public class OrderResource {

    @Inject
    @Channel("orders-out")
    Emitter<Order> orderEmitter;

    @POST
    public Uni<RestResponse<Order>> create(Order order) {
        return orderService.create(order)
            .onItem().invoke(created ->
                orderEmitter.send(created)
            )
            .onItem().transform(created ->
                RestResponse.created(URI.create("/orders/" + created.id))
            );
    }
}

@ApplicationScoped
public class OrderProcessor {

    @Incoming("orders-in")
    @Outgoing("notifications-out")
    public Uni<Notification> process(Order order) {
        return processOrder(order)
            .onItem().transform(result ->
                new Notification(order.customerId, 
                    "Order " + order.id + " processed")
            );
    }
}
```

## OpenAPI/Swagger Integration

```java
import org.eclipse.microprofile.openapi.annotations.*;
import org.eclipse.microprofile.openapi.annotations.parameters.*;
import org.eclipse.microprofile.openapi.annotations.responses.*;
import org.eclipse.microprofile.openapi.annotations.media.*;

@Path("/products")
@Tag(name = "Products", description = "Product management endpoints")
public class ProductResource {

    @GET
    @Operation(summary = "List all products", 
               description = "Returns a list of all products with optional filtering")
    @APIResponse(responseCode = "200", 
                 description = "Success",
                 content = @Content(schema = @Schema(implementation = Product[].class)))
    public List<Product> list(
            @Parameter(description = "Filter by category")
            @QueryParam("category") String category) {
        return productService.list(category);
    }

    @POST
    @Operation(summary = "Create a product")
    @APIResponse(responseCode = "201", description = "Product created")
    @APIResponse(responseCode = "400", description = "Invalid input")
    public RestResponse<Product> create(
            @RequestBody(description = "Product to create", required = true)
            Product product) {
        Product created = productService.create(product);
        return RestResponse.created(URI.create("/products/" + created.id));
    }
}
```

## Metrics Integration

```java
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Path("/api")
public class MetricsResource {

    @Inject
    MeterRegistry registry;

    @GET
    @Path("/data")
    public Response getData() {
        Timer.Sample sample = Timer.start(registry);
        
        try {
            Data data = dataService.fetch();
            
            sample.stop(registry.timer("api.data.fetch", 
                "status", "success"));
            
            return Response.ok(data).build();
        } catch (Exception e) {
            sample.stop(registry.timer("api.data.fetch", 
                "status", "error"));
            throw e;
        }
    }

    @GET
    @Path("/metrics")
    public Map<String, Object> getMetrics() {
        return Map.of(
            "requests", registry.counter("http.requests").count(),
            "errors", registry.counter("http.errors").count()
        );
    }
}
```

## Tracing Integration

```java
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Scope;

@Path("/traced")
public class TracedResource {

    @Inject
    Tracer tracer;

    @GET
    @Path("/operation")
    public Uni<Result> performOperation() {
        Span span = tracer.spanBuilder("custom-operation")
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("user.id", getCurrentUserId());
            
            return performAsyncOperation()
                .onItem().invoke(result -> {
                    span.setAttribute("result.size", result.size());
                    span.setStatus(StatusCode.OK);
                })
                .onFailure().invoke(error -> {
                    span.recordException(error);
                    span.setStatus(StatusCode.ERROR, error.getMessage());
                })
                .eventually(() -> span.end());
        }
    }
}
```

## Cache Integration

```java
import io.quarkus.cache.*;

@Path("/cached")
public class CachedResource {

    @Inject
    DataService dataService;

    @GET
    @Path("/data/{id}")
    @CacheResult(cacheName = "data-cache")
    public Data getData(@PathParam("id") Long id) {
        return dataService.fetch(id);
    }

    @PUT
    @Path("/data/{id}")
    @CacheInvalidate(cacheName = "data-cache")
    public Data updateData(@PathParam("id") Long id, Data data) {
        return dataService.update(id, data);
    }

    @DELETE
    @Path("/data/{id}")
    @CacheInvalidate(cacheName = "data-cache")
    public Response deleteData(@PathParam("id") Long id) {
        dataService.delete(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/cache/clear")
    @CacheInvalidateAll(cacheName = "data-cache")
    public Response clearCache() {
        return Response.ok().build();
    }
}
```

## Scheduler Integration

```java
import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class ScheduledTasks {

    @Inject
    DataService dataService;

    @Inject
    NotificationService notificationService;

    @Scheduled(every = "1h")
    public void cleanupOldData() {
        int deleted = dataService.deleteOld();
        LOG.info("Cleaned up " + deleted + " old records");
    }

    @Scheduled(cron = "0 0 9 * * ?")  // 9 AM daily
    public void sendDailyReport() {
        Report report = dataService.generateDailyReport();
        notificationService.sendReport(report);
    }
}

@Path("/tasks")
public class TaskResource {

    @Inject
    Scheduler scheduler;

    @POST
    @Path("/trigger/{taskName}")
    public Response triggerTask(@PathParam("taskName") String taskName) {
        scheduler.trigger(taskName);
        return Response.accepted().build();
    }
}
```

## Health Checks

```java
import org.eclipse.microprofile.health.*;

@Liveness
@ApplicationScoped
public class LivenessCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("Application is running");
    }
}

@Readiness
@ApplicationScoped
public class ReadinessCheck implements HealthCheck {

    @Inject
    DataSource dataSource;

    @Override
    public HealthCheckResponse call() {
        try {
            dataSource.getConnection().close();
            return HealthCheckResponse.up("Database connection OK");
        } catch (Exception e) {
            return HealthCheckResponse.down("Database connection failed");
        }
    }
}
```

## gRPC Integration

```java
import io.quarkus.grpc.GrpcClient;

@Path("/grpc-proxy")
public class GrpcProxyResource {

    @GrpcClient("hello")
    HelloGrpc.HelloBlockingStub helloClient;

    @GET
    @Path("/hello/{name}")
    public String hello(@PathParam("name") String name) {
        HelloRequest request = HelloRequest.newBuilder()
            .setName(name)
            .build();
        
        HelloReply reply = helloClient.sayHello(request);
        return reply.getMessage();
    }
}
```

## Redis Integration

```java
import io.quarkus.redis.datasource.ReactiveRedisDataSource;

@Path("/redis")
public class RedisResource {

    private final ReactiveRedisDataSource redis;

    public RedisResource(ReactiveRedisDataSource redis) {
        this.redis = redis;
    }

    @GET
    @Path("/get/{key}")
    public Uni<String> get(@PathParam("key") String key) {
        return redis.value(String.class).get(key);
    }

    @POST
    @Path("/set/{key}")
    public Uni<Void> set(
            @PathParam("key") String key,
            String value) {
        return redis.value(String.class).set(key, value);
    }

    @GET
    @Path("/counter/{key}")
    public Uni<Long> increment(@PathParam("key") String key) {
        return redis.value(Long.class).incr(key);
    }
}
```

## See Also

- [Common Scenarios](common-scenarios.md) - Basic examples
- [Advanced Patterns](advanced-patterns.md) - Complex patterns
- [Configuration Reference](../reference/configuration.md)
- [Architecture](../reference/architecture.md)
