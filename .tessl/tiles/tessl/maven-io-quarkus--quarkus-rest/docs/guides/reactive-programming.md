# Reactive Programming Guide

This guide shows how to build reactive, non-blocking REST endpoints with Mutiny.

## Why Reactive?

Reactive programming allows handling many concurrent requests efficiently by:
- Not blocking threads while waiting for I/O
- Using event loops instead of thread pools
- Scaling better with limited resources

## Add Reactive Extensions

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-hibernate-reactive-panache</artifactId>
</dependency>
```

## Uni - Single Async Value

Return `Uni<T>` for operations that produce one value asynchronously:

```java
import io.smallrye.mutiny.Uni;

@Path("/users")
public class UserResource {

    @Inject
    UserRepository repository;

    @GET
    @Path("/{id}")
    public Uni<User> get(@PathParam("id") Long id) {
        return repository.findById(id);
    }

    @POST
    public Uni<Response> create(User user) {
        return repository.persist(user)
            .onItem().transform(created ->
                Response.created(URI.create("/users/" + created.id))
                    .entity(created)
                    .build()
            );
    }
}
```

## Multi - Stream of Values

Return `Multi<T>` for streaming multiple values:

```java
import io.smallrye.mutiny.Multi;

@GET
@Path("/stream")
@Produces(MediaType.APPLICATION_JSON)
public Multi<User> streamUsers() {
    return repository.streamAll();
}
```

## Transformation

Transform items in the reactive pipeline:

```java
@GET
@Path("/{id}/dto")
public Uni<UserDTO> getDTO(@PathParam("id") Long id) {
    return repository.findById(id)
        .onItem().transform(user -> new UserDTO(user));
}
```

## Error Handling

### Recover with Fallback

```java
@GET
@Path("/{id}")
public Uni<User> get(@PathParam("id") Long id) {
    return repository.findById(id)
        .onFailure().recoverWithItem(User.getDefaultUser());
}
```

### Retry on Failure

```java
@GET
@Path("/{id}")
public Uni<User> get(@PathParam("id") Long id) {
    return repository.findById(id)
        .onFailure().retry().atMost(3);
}
```

### Custom Error Handling

```java
@GET
@Path("/{id}")
public Uni<Response> get(@PathParam("id") Long id) {
    return repository.findById(id)
        .onItem().transform(user -> Response.ok(user).build())
        .onFailure().recoverWithItem(error ->
            Response.status(500)
                .entity("Error: " + error.getMessage())
                .build()
        );
}
```

## Combining Operations

### Sequential Operations

```java
@POST
@Path("/{id}/activate")
public Uni<User> activate(@PathParam("id") Long id) {
    return repository.findById(id)
        .onItem().transformToUni(user -> {
            user.active = true;
            return repository.persist(user);
        });
}
```

### Parallel Operations

```java
@GET
@Path("/{id}/full")
public Uni<UserFullDetails> getFullDetails(@PathParam("id") Long id) {
    Uni<User> userUni = userRepository.findById(id);
    Uni<List<Post>> postsUni = postRepository.findByUserId(id);
    Uni<List<Comment>> commentsUni = commentRepository.findByUserId(id);

    return Uni.combine().all()
        .unis(userUni, postsUni, commentsUni)
        .asTuple()
        .onItem().transform(tuple ->
            new UserFullDetails(
                tuple.getItem1(),
                tuple.getItem2(),
                tuple.getItem3()
            )
        );
}
```

## Server-Sent Events (SSE)

Stream events to clients:

```java
@GET
@Path("/events")
@Produces(MediaType.SERVER_SENT_EVENTS)
public Multi<String> streamEvents() {
    return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
        .onItem().transform(tick -> "Event " + tick);
}

@GET
@Path("/updates")
@Produces(MediaType.SERVER_SENT_EVENTS)
public Multi<User> streamUserUpdates() {
    return userUpdatePublisher.getUpdates();
}
```

## Timeouts

Add timeouts to prevent hanging:

```java
@GET
@Path("/{id}")
public Uni<User> get(@PathParam("id") Long id) {
    return repository.findById(id)
        .ifNoItem().after(Duration.ofSeconds(5))
        .fail()
        .onFailure().recoverWithItem(User.getDefaultUser());
}
```

## Backpressure

Control flow when consumer is slower than producer:

```java
@GET
@Path("/stream")
@Produces(MediaType.APPLICATION_JSON)
public Multi<User> streamUsers() {
    return repository.streamAll()
        .onBackPressure().drop()  // Drop items if consumer is slow
        .select().first(100);     // Limit total items
}
```

## Reactive Transactions

Use Panache transactions with reactive:

```java
import io.quarkus.hibernate.reactive.panache.Panache;

@POST
public Uni<Response> create(User user) {
    return Panache.withTransaction(() ->
        repository.persist(user)
    ).onItem().transform(created ->
        Response.created(URI.create("/users/" + created.id))
            .entity(created)
            .build()
    );
}
```

## Threading Control

### Automatic Threading

- `Uni`/`Multi` methods run on I/O threads (event loop)
- Regular return types run on worker threads

### Force Blocking

```java
@GET
@Path("/{id}")
@Blocking  // Force worker thread
public Uni<User> get(@PathParam("id") Long id) {
    return Uni.createFrom().item(() -> expensiveBlockingOperation(id));
}
```

### Force Non-Blocking

```java
@GET
@Path("/cached")
@NonBlocking  // Force I/O thread
public String getCached() {
    return cache.get("key");
}
```

## Reactive REST Client

Call other APIs reactively:

```java
@Path("/api")
@RegisterRestClient
public interface ExternalApi {

    @GET
    @Path("/data")
    Uni<Data> getData();
}

@Path("/proxy")
public class ProxyResource {

    @RestClient
    ExternalApi externalApi;

    @GET
    @Path("/data")
    public Uni<Data> proxyData() {
        return externalApi.getData();
    }
}
```

## Testing Reactive Code

Convert to blocking for tests:

```java
@Test
public void testGetUser() {
    Uni<User> userUni = resource.get(1L);
    User user = userUni.await().indefinitely();
    
    assertEquals("Alice", user.name);
}
```

**Note**: Never use `.await()` in production code!

## Common Patterns

### Load and Transform

```java
public Uni<UserDTO> getUserDTO(Long id) {
    return repository.findById(id)
        .onItem().transform(UserDTO::new);
}
```

### Load, Validate, Save

```java
public Uni<User> updateUser(Long id, UserUpdate update) {
    return repository.findById(id)
        .onItem().ifNull().failWith(NotFoundException::new)
        .onItem().transform(user -> {
            user.name = update.name;
            user.email = update.email;
            return user;
        })
        .onItem().transformToUni(repository::persist);
}
```

### Conditional Logic

```java
public Uni<Response> activate(Long id) {
    return repository.findById(id)
        .onItem().transformToUni(user -> {
            if (user.active) {
                return Uni.createFrom().item(
                    Response.status(409).entity("Already active").build()
                );
            }
            user.active = true;
            return repository.persist(user)
                .onItem().transform(u -> Response.ok(u).build());
        });
}
```

## Next Steps

- [REST Clients](rest-clients.md) - Build reactive REST clients
- [Common Scenarios](../examples/common-scenarios.md) - Reactive examples
- [Advanced Patterns](../examples/advanced-patterns.md) - Complex reactive flows

## See Also

- [Reactive Programming Reference](../reference/reactive-programming.md)
- [Streaming Reference](../reference/streaming.md)
- [Architecture](../reference/architecture.md)
