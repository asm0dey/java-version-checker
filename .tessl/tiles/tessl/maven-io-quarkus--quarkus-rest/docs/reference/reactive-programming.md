# Reactive Programming

Quarkus REST provides built-in integration with Smallrye Mutiny for reactive, non-blocking endpoint execution. Methods returning reactive types automatically run on I/O threads (event loop) for optimal performance.

## Reactive Return Types

### Uni - Single Async Value

Return `Uni<T>` for operations that produce a single value asynchronously.

```java { .api }
package io.smallrye.mutiny;

class Uni<T> {
    // Creation
    static UniCreate createFrom();

    interface UniCreate {
        Uni<T> item(T item);
        Uni<T> nullItem();
        Uni<T> completionStage(CompletionStage<T> stage);
        Uni<T> future(Supplier<CompletionStage<T>> supplier);
        Uni<T> emitter(Consumer<UniEmitter<? super T>> consumer);
        Uni<T> failure(Throwable failure);
    }

    // Transformation
    UniOnItem<T> onItem();

    interface UniOnItem<T> {
        <R> Uni<R> transform(Function<? super T, ? extends R> mapper);
        <R> Uni<R> transformToUni(Function<? super T, Uni<? extends R>> mapper);
        Uni<T> invoke(Consumer<? super T> callback);
        Uni<T> delayIt().by(Duration duration);
    }

    // Error handling
    UniOnFailure<T> onFailure();

    interface UniOnFailure<T> {
        UniOnFailure<T> onlyIf(Predicate<? super Throwable> predicate);
        Uni<T> recoverWithItem(T fallback);
        Uni<T> recoverWithItem(Function<? super Throwable, ? extends T> supplier);
        Uni<T> recoverWithUni(Function<? super Throwable, Uni<? extends T>> supplier);
        Uni<T> retry().atMost(long maxAttempts);
    }

    // Subscription
    Cancellable subscribe().with(Consumer<? super T> onItem);
    Cancellable subscribe().with(Consumer<? super T> onItem, Consumer<? super Throwable> onFailure);

    // Blocking (avoid in production)
    T await().indefinitely();
    T await().atMost(Duration duration);
}
```

**Usage**:

```java
@GET
@Path("/book/{id}")
public Uni<Book> getBook(@PathParam("id") Long id) {
    return bookRepository.findById(id);  // Reactive repository
}

@GET
@Path("/book/{id}/details")
public Uni<BookDetails> getBookDetails(@PathParam("id") Long id) {
    return bookRepository.findById(id)
        .onItem().transform(book -> new BookDetails(book))
        .onFailure().recoverWithItem(new BookDetails());
}

@POST
@Path("/book")
public Uni<Response> createBook(Book book) {
    return bookRepository.persist(book)
        .onItem().transform(created ->
            Response.created(URI.create("/book/" + created.getId()))
                .entity(created)
                .build()
        );
}
```

### Multi - Stream of Values

Return `Multi<T>` for operations that produce multiple values (streaming).

```java { .api }
package io.smallrye.mutiny;

class Multi<T> {
    // Creation
    static MultiCreate createFrom();

    interface MultiCreate {
        Multi<T> items(T... items);
        Multi<T> iterable(Iterable<T> iterable);
        Multi<T> emitter(Consumer<MultiEmitter<? super T>> consumer);
        Multi<T> publisher(Publisher<T> publisher);
    }

    // Transformation
    MultiOnItem<T> onItem();

    interface MultiOnItem<T> {
        <R> Multi<R> transform(Function<? super T, ? extends R> mapper);
        <R> Multi<R> transformToUniAndConcatenate(Function<? super T, Uni<? extends R>> mapper);
        Multi<T> invoke(Consumer<? super T> callback);
    }

    // Filtering
    Multi<T> select().where(Predicate<? super T> predicate);
    Multi<T> select().first(long count);

    // Error handling
    MultiOnFailure<T> onFailure();

    interface MultiOnFailure<T> {
        Multi<T> recoverWithItem(T fallback);
        Multi<T> recoverWithMulti(Function<? super Throwable, Multi<? extends T>> supplier);
        Multi<T> retry().atMost(long maxAttempts);
    }

    // Collection
    Uni<List<T>> collect().asList();

    // Subscription
    Cancellable subscribe().with(Consumer<? super T> onItem);
    Cancellable subscribe().with(
        Consumer<? super T> onItem,
        Consumer<? super Throwable> onFailure,
        Runnable onCompletion
    );
}
```

**Usage**:

```java
@GET
@Path("/books/stream")
@Produces(MediaType.APPLICATION_JSON)
public Multi<Book> streamBooks() {
    return bookRepository.streamAll();  // Streams all books
}

@GET
@Path("/books/recent")
public Multi<Book> getRecentBooks() {
    return bookRepository.streamAll()
        .select().where(book -> book.getYear() >= 2020)
        .select().first(10);
}

@GET
@Path("/books/names")
@Produces(MediaType.APPLICATION_JSON)
public Multi<String> getBookNames() {
    return bookRepository.streamAll()
        .onItem().transform(Book::getTitle);
}
```

## Server-Sent Events (SSE)

Stream data to clients using Server-Sent Events.

```java { .api }
package jakarta.ws.rs.core;

class MediaType {
    static final String SERVER_SENT_EVENTS = "text/event-stream";
    static final MediaType SERVER_SENT_EVENTS_TYPE;
}
```

**Usage**:

```java
@GET
@Path("/events")
@Produces(MediaType.SERVER_SENT_EVENTS)
public Multi<String> streamEvents() {
    return Multi.createFrom().ticks().every(Duration.ofSeconds(1))
        .onItem().transform(tick -> "Event " + tick);
}

@GET
@Path("/book-updates")
@Produces(MediaType.SERVER_SENT_EVENTS)
public Multi<Book> streamBookUpdates() {
    return bookUpdatePublisher.getUpdates();  // Real-time book updates
}
```

## Threading Model

Reactive endpoints automatically run on I/O threads (event loop) for optimal performance.

### Automatic Thread Selection

```java
// Runs on I/O thread automatically
@GET
@Path("/async")
public Uni<String> asyncEndpoint() {
    return Uni.createFrom().item("Async response");
}

// Runs on worker thread automatically
@GET
@Path("/sync")
public String syncEndpoint() {
    return "Sync response";
}
```

### Explicit Thread Control

Force execution on specific thread pools using `@Blocking` or `@NonBlocking`.

```java { .api }
package io.smallrye.common.annotation;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@interface Blocking {}

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@interface NonBlocking {}
```

**Usage**:

```java
@GET
@Path("/blocking-uni")
@Blocking  // Force worker thread even with Uni
public Uni<String> blockingUni() {
    // Runs on worker thread despite returning Uni
    return Uni.createFrom().item(() -> expensiveBlockingOperation());
}

@GET
@Path("/non-blocking")
@NonBlocking  // Force I/O thread
public String nonBlocking() {
    // Runs on I/O thread despite returning String
    return cachedValue;
}
```

## CompletionStage Support

Standard Java `CompletionStage` is also supported for async operations.

```java { .api }
package java.util.concurrent;

interface CompletionStage<T> {
    <U> CompletionStage<U> thenApply(Function<? super T, ? extends U> fn);
    <U> CompletionStage<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn);
    CompletionStage<T> exceptionally(Function<Throwable, ? extends T> fn);
    CompletionStage<Void> thenAccept(Consumer<? super T> action);
}
```

**Usage**:

```java
@GET
@Path("/book/{id}/async")
public CompletionStage<Book> getBookAsync(@PathParam("id") Long id) {
    return bookRepository.findByIdAsync(id);
}

@POST
@Path("/book/async")
public CompletionStage<Response> createBookAsync(Book book) {
    return bookRepository.persistAsync(book)
        .thenApply(created ->
            Response.created(URI.create("/book/" + created.getId()))
                .entity(created)
                .build()
        );
}
```

## Reactive Exception Handling

Handle failures in reactive pipelines.

### Uni Error Handling

```java
@GET
@Path("/book/{id}/safe")
public Uni<Book> getBookSafe(@PathParam("id") Long id) {
    return bookRepository.findById(id)
        .onFailure().recoverWithItem(() -> Book.getDefaultBook())
        .onFailure().invoke(throwable ->
            log.error("Failed to fetch book: " + id, throwable)
        );
}

@GET
@Path("/book/{id}/retry")
public Uni<Book> getBookWithRetry(@PathParam("id") Long id) {
    return bookRepository.findById(id)
        .onFailure().retry().atMost(3)
        .onFailure().recoverWithItem(new Book());
}
```

### Multi Error Handling

```java
@GET
@Path("/books/safe-stream")
@Produces(MediaType.APPLICATION_JSON)
public Multi<Book> streamBooksSafe() {
    return bookRepository.streamAll()
        .onFailure().recoverWithMulti(error -> {
            log.error("Stream failed", error);
            return Multi.createFrom().empty();
        });
}
```

## Reactive Context Integration

Combine reactive types with context injection.

```java
@GET
@Path("/user/books")
public Uni<List<Book>> getUserBooks(@Context SecurityContext securityContext) {
    String username = securityContext.getUserPrincipal().getName();
    return bookRepository.findByOwner(username)
        .collect().asList();
}

@POST
@Path("/book")
public Uni<Response> createBook(Book book, @Context UriInfo uriInfo) {
    return bookRepository.persist(book)
        .onItem().transform(created -> {
            URI location = uriInfo.getAbsolutePathBuilder()
                .path(created.getId().toString())
                .build();
            return Response.created(location).entity(created).build();
        });
}
```

## Combining Multiple Reactive Operations

### Combining Uni instances

```java
@GET
@Path("/book/{id}/full-details")
public Uni<BookFullDetails> getFullDetails(@PathParam("id") Long id) {
    Uni<Book> bookUni = bookRepository.findById(id);
    Uni<Author> authorUni = bookUni
        .onItem().transformToUni(book -> authorRepository.findById(book.getAuthorId()));
    Uni<List<Review>> reviewsUni = reviewRepository.findByBookId(id)
        .collect().asList();

    return Uni.combine().all().unis(bookUni, authorUni, reviewsUni)
        .asTuple()
        .onItem().transform(tuple ->
            new BookFullDetails(tuple.getItem1(), tuple.getItem2(), tuple.getItem3())
        );
}
```

### Sequential vs Parallel

```java
// Sequential - operations execute one after another
@GET
@Path("/sequential")
public Uni<Result> sequential() {
    return operation1()
        .onItem().transformToUni(result1 -> operation2(result1))
        .onItem().transformToUni(result2 -> operation3(result2));
}

// Parallel - operations execute concurrently
@GET
@Path("/parallel")
public Uni<Result> parallel() {
    Uni<Data1> uni1 = operation1();
    Uni<Data2> uni2 = operation2();
    Uni<Data3> uni3 = operation3();

    return Uni.combine().all().unis(uni1, uni2, uni3)
        .asTuple()
        .onItem().transform(tuple ->
            combineResults(tuple.getItem1(), tuple.getItem2(), tuple.getItem3())
        );
}
```

## Backpressure

Multi provides built-in backpressure support through the Reactive Streams specification.

```java
@GET
@Path("/books/controlled-stream")
@Produces(MediaType.APPLICATION_JSON)
public Multi<Book> controlledStream() {
    return bookRepository.streamAll()
        .onBackPressure().drop()  // Drop items if consumer is slow
        .select().first(100);     // Limit total items
}
```

## Timeouts

Add timeouts to reactive operations.

```java
@GET
@Path("/book/{id}/timeout")
public Uni<Book> getBookWithTimeout(@PathParam("id") Long id) {
    return bookRepository.findById(id)
        .ifNoItem().after(Duration.ofSeconds(5))
        .fail()
        .onFailure().recoverWithItem(Book.getDefaultBook());
}
```

## Reactive Transaction Management

Integrate with reactive transaction APIs.

```java
import io.quarkus.hibernate.reactive.panache.Panache;

@POST
@Path("/book/transactional")
public Uni<Response> createBookTransactional(Book book) {
    return Panache.withTransaction(() ->
        bookRepository.persist(book)
    ).onItem().transform(created ->
        Response.created(URI.create("/book/" + created.getId()))
            .entity(created)
            .build()
    );
}
```

## Testing Reactive Endpoints

Convert reactive types to blocking for testing.

```java
// In test code
Uni<Book> bookUni = getBook(1L);
Book book = bookUni.await().indefinitely();

Multi<Book> booksMulti = streamBooks();
List<Book> books = booksMulti.collect().asList()
    .await().indefinitely();
```

**Note**: Avoid `.await()` in production code. It blocks the calling thread and defeats the purpose of reactive programming.
