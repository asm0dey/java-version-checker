# Common Scenarios

Real-world examples of using Quarkus REST in typical scenarios.

## CRUD API with Validation

```java
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.jboss.resteasy.reactive.RestResponse;

public class Product {
    public Long id;
    
    @NotBlank
    public String name;
    
    @NotNull
    @DecimalMin("0.01")
    public BigDecimal price;
    
    @Min(0)
    public int stock;
}

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    @Inject
    ProductService service;

    @GET
    public List<Product> list(
            @QueryParam("category") String category,
            @QueryParam("minPrice") BigDecimal minPrice,
            @QueryParam("maxPrice") BigDecimal maxPrice) {
        return service.find(category, minPrice, maxPrice);
    }

    @GET
    @Path("/{id}")
    public RestResponse<Product> get(@PathParam("id") Long id) {
        Product product = service.findById(id);
        return product != null 
            ? RestResponse.ok(product)
            : RestResponse.notFound();
    }

    @POST
    public RestResponse<Product> create(@Valid Product product) {
        Product created = service.create(product);
        URI location = URI.create("/products/" + created.id);
        return RestResponse.created(location);
    }

    @PUT
    @Path("/{id}")
    public RestResponse<Product> update(
            @PathParam("id") Long id,
            @Valid Product product) {
        Product updated = service.update(id, product);
        return updated != null
            ? RestResponse.ok(updated)
            : RestResponse.notFound();
    }

    @DELETE
    @Path("/{id}")
    public RestResponse<Void> delete(@PathParam("id") Long id) {
        boolean deleted = service.delete(id);
        return deleted
            ? RestResponse.noContent()
            : RestResponse.notFound();
    }
}
```

## Paginated API

```java
@GET
public Response list(
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("20") int size,
        @QueryParam("sort") @DefaultValue("name") String sortField) {
    
    Page<Product> result = service.findAll(page, size, sortField);
    
    return Response.ok(result.content)
        .header("X-Total-Count", result.totalElements)
        .header("X-Total-Pages", result.totalPages)
        .header("X-Page-Number", result.pageNumber)
        .header("X-Page-Size", result.pageSize)
        .build();
}
```

## File Upload

```java
import org.jboss.resteasy.reactive.multipart.FileUpload;

@POST
@Path("/upload")
@Consumes(MediaType.MULTIPART_FORM_DATA)
@Produces(MediaType.APPLICATION_JSON)
public RestResponse<UploadResult> upload(
        @RestForm FileUpload file,
        @RestForm String description) {
    
    if (file == null || file.size() == 0) {
        return RestResponse.status(400, 
            new UploadResult("No file provided"));
    }
    
    if (file.size() > 10_000_000) {
        return RestResponse.status(413, 
            new UploadResult("File too large"));
    }
    
    String storedPath = fileService.store(file, description);
    return RestResponse.ok(new UploadResult(storedPath));
}
```

## Reactive Database Query

```java
import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.Panache;

@Path("/orders")
public class OrderResource {

    @Inject
    OrderRepository orderRepo;

    @Inject
    CustomerRepository customerRepo;

    @GET
    @Path("/{id}")
    public Uni<OrderDTO> get(@PathParam("id") Long id) {
        return orderRepo.findById(id)
            .onItem().ifNull().failWith(NotFoundException::new)
            .onItem().transformToUni(order ->
                customerRepo.findById(order.customerId)
                    .onItem().transform(customer ->
                        new OrderDTO(order, customer)
                    )
            );
    }

    @POST
    public Uni<RestResponse<Order>> create(OrderRequest request) {
        return Panache.withTransaction(() -> {
            Order order = new Order();
            order.customerId = request.customerId;
            order.items = request.items;
            order.total = calculateTotal(request.items);
            return orderRepo.persist(order);
        }).onItem().transform(created ->
            RestResponse.created(URI.create("/orders/" + created.id))
        );
    }
}
```

## Secured API with JWT

```java
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/account")
@Authenticated
public class AccountResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    AccountService accountService;

    @GET
    @Path("/profile")
    public Uni<Profile> getProfile() {
        String userId = jwt.getSubject();
        return accountService.getProfile(userId);
    }

    @PUT
    @Path("/profile")
    public Uni<Profile> updateProfile(ProfileUpdate update) {
        String userId = jwt.getSubject();
        return accountService.updateProfile(userId, update);
    }

    @GET
    @Path("/orders")
    public Uni<List<Order>> getOrders() {
        String userId = jwt.getSubject();
        return accountService.getOrders(userId);
    }
}
```

## REST Client with Fallback

```java
@Path("/api/weather")
@RegisterRestClient(configKey = "weather-api")
public interface WeatherApiClient {

    @GET
    @Path("/{city}")
    @Retry(maxRetries = 3, delay = 1000)
    @Fallback(fallbackMethod = "getDefaultWeather")
    @Timeout(5000)
    Uni<Weather> getWeather(@PathParam("city") String city);

    default Uni<Weather> getDefaultWeather(String city) {
        return Uni.createFrom().item(
            new Weather(city, "Unknown", 20.0)
        );
    }
}

@Path("/weather")
public class WeatherResource {

    @RestClient
    WeatherApiClient weatherApi;

    @GET
    @Path("/{city}")
    public Uni<Weather> getWeather(@PathParam("city") String city) {
        return weatherApi.getWeather(city);
    }
}
```

## Server-Sent Events

```java
@Path("/notifications")
public class NotificationResource {

    @Inject
    NotificationService notificationService;

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<Notification> stream(@Context SecurityContext ctx) {
        String userId = ctx.getUserPrincipal().getName();
        return notificationService.streamForUser(userId);
    }

    @GET
    @Path("/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<String> events() {
        return Multi.createFrom().ticks().every(Duration.ofSeconds(5))
            .onItem().transform(tick -> 
                "Heartbeat at " + Instant.now()
            );
    }
}
```

## Request/Response Filtering

```java
@ApplicationScoped
public class LoggingFilters {

    private static final Logger LOG = Logger.getLogger(LoggingFilters.class);

    @ServerRequestFilter
    public void logRequest(UriInfo uriInfo, HttpHeaders headers) {
        LOG.infof("Request: %s %s from %s",
            uriInfo.getRequestUri().getPath(),
            headers.getHeaderString("User-Agent"),
            headers.getHeaderString("X-Forwarded-For")
        );
    }

    @ServerResponseFilter
    public void addHeaders(ContainerResponseContext response) {
        response.getHeaders().add("X-Powered-By", "Quarkus");
        response.getHeaders().add("X-Request-ID", UUID.randomUUID().toString());
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> handleValidation(
            ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .collect(Collectors.toList());
        
        return RestResponse.status(422, 
            new ErrorResponse("Validation failed", errors)
        );
    }
}
```

## Caching with ETags

```java
@Path("/articles")
public class ArticleResource {

    @Inject
    ArticleService service;

    @GET
    @Path("/{id}")
    public Response get(
            @PathParam("id") Long id,
            @HeaderParam("If-None-Match") String ifNoneMatch) {
        
        Article article = service.findById(id);
        if (article == null) {
            return Response.status(404).build();
        }
        
        String etag = computeETag(article);
        
        if (etag.equals(ifNoneMatch)) {
            return Response.notModified()
                .tag(etag)
                .build();
        }
        
        return Response.ok(article)
            .tag(etag)
            .cacheControl(CacheControl.valueOf("max-age=3600"))
            .build();
    }

    private String computeETag(Article article) {
        return "\"" + article.id + "-" + article.updatedAt.getTime() + "\"";
    }
}
```

## Multipart Form Processing

```java
@POST
@Path("/submit")
@Consumes(MediaType.MULTIPART_FORM_DATA)
public RestResponse<SubmissionResult> submit(
        @RestForm String title,
        @RestForm String description,
        @RestForm FileUpload document,
        @RestForm FileUpload thumbnail) {
    
    if (document == null) {
        return RestResponse.status(400, 
            new SubmissionResult("Document is required"));
    }
    
    String docPath = fileService.store(document);
    String thumbPath = thumbnail != null 
        ? fileService.store(thumbnail)
        : null;
    
    Submission submission = new Submission();
    submission.title = title;
    submission.description = description;
    submission.documentPath = docPath;
    submission.thumbnailPath = thumbPath;
    
    submissionService.save(submission);
    
    return RestResponse.ok(new SubmissionResult(submission.id));
}
```

## See Also

- [Advanced Patterns](advanced-patterns.md) - Complex scenarios
- [Integration Examples](integration-examples.md) - Integration patterns
- [Quick Start Guide](../guides/quick-start.md)
- [Building REST APIs](../guides/building-rest-apis.md)
