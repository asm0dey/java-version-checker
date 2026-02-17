# REST Links

Quarkus REST provides support for generating and injecting web links (HATEOAS-style hypermedia links) into HTTP responses. These links help clients discover and navigate related resources.

## Capabilities

### REST Link Definition

Define web links that should be included in HTTP responses.

```java { .api }
package io.quarkus.resteasy.reactive.links;

/**
 * Defines a web link that will be included in HTTP response headers or entity.
 * Supports both type-level (non-instance) and instance-based links.
 * Links follow the Web Linking RFC 5988 standard.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@interface RestLink {
    /**
     * The link relation type (rel attribute).
     * If empty, uses the method name as the relation.
     * Common values: "self", "next", "prev", "edit", "delete", etc.
     */
    String rel() default "";

    /**
     * The entity type this link applies to.
     * Used to determine which links to include for specific entity types.
     * Default Object.class means the link applies to all types.
     */
    Class<?> entityType() default Object.class;
}
```

**Usage Examples:**

```java
import io.quarkus.resteasy.reactive.links.RestLink;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

@Path("/books")
public class BookResource {

    // Self link - rel defaults to method name "self"
    @GET
    @Path("/{id}")
    @RestLink(entityType = Book.class)
    public Book getBook(@PathParam("id") Long id) {
        return findBook(id);
    }

    // Explicit rel attribute
    @GET
    @Path("/{id}")
    @RestLink(rel = "self", entityType = Book.class)
    public Book getSelf(@PathParam("id") Long id) {
        return findBook(id);
    }

    // Edit link
    @PUT
    @Path("/{id}")
    @RestLink(rel = "edit", entityType = Book.class)
    public Book updateBook(@PathParam("id") Long id, Book book) {
        return update(id, book);
    }

    // Delete link
    @DELETE
    @Path("/{id}")
    @RestLink(rel = "delete", entityType = Book.class)
    public Response deleteBook(@PathParam("id") Long id) {
        delete(id);
        return Response.noContent().build();
    }

    // List link (non-instance, type-level)
    @GET
    @RestLink(rel = "list", entityType = Book.class)
    public List<Book> listBooks() {
        return findAllBooks();
    }

    // Related resource links
    @GET
    @Path("/{id}/author")
    @RestLink(rel = "author", entityType = Book.class)
    public Author getAuthor(@PathParam("id") Long id) {
        return findAuthor(id);
    }

    @GET
    @Path("/{id}/reviews")
    @RestLink(rel = "reviews", entityType = Book.class)
    public List<Review> getReviews(@PathParam("id") Long id) {
        return findReviews(id);
    }

    private Book findBook(Long id) { return new Book(); }
    private Book update(Long id, Book book) { return book; }
    private void delete(Long id) {}
    private List<Book> findAllBooks() { return List.of(); }
    private Author findAuthor(Long id) { return new Author(); }
    private List<Review> findReviews(Long id) { return List.of(); }
}

class Book {
    public Long id;
    public String title;
}
class Author {}
class Review {}
```

### Inject REST Links

Control how REST links are injected into responses.

```java { .api }
package io.quarkus.resteasy.reactive.links;

/**
 * Injects REST links into HTTP response headers or entity.
 * Applied to resource methods or entity types.
 * Determines which links to include based on the RestLinkType.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@interface InjectRestLinks {
    /**
     * The type of links to inject.
     * TYPE = only type-level links (non-instance)
     * INSTANCE = all links including instance-based links
     */
    RestLinkType value() default RestLinkType.TYPE;
}
```

**Usage Examples:**

```java
import io.quarkus.resteasy.reactive.links.InjectRestLinks;
import io.quarkus.resteasy.reactive.links.RestLink;
import io.quarkus.resteasy.reactive.links.RestLinkType;
import jakarta.ws.rs.*;

@Path("/books")
public class BookResource {

    // Inject only type-level links
    @GET
    @Path("/{id}")
    @InjectRestLinks(RestLinkType.TYPE)
    public Book getBookWithTypeLinks(@PathParam("id") Long id) {
        return findBook(id);
    }

    // Inject all links including instance-based
    @GET
    @Path("/{id}/full")
    @InjectRestLinks(RestLinkType.INSTANCE)
    public Book getBookWithAllLinks(@PathParam("id") Long id) {
        return findBook(id);
    }

    // Applied to entity class
    @GET
    public List<FullBook> getAllBooks() {
        return findAllFullBooks();
    }

    private Book findBook(Long id) { return new Book(); }
    private List<FullBook> findAllFullBooks() { return List.of(); }
}

// Links injected into entity
@InjectRestLinks(RestLinkType.INSTANCE)
class FullBook {
    public Long id;
    public String title;
    // Links will be added to this object
}

class Book {
    public Long id;
    public String title;
}
```

### REST Link Type Enum

Specifies which types of links to inject.

```java { .api }
package io.quarkus.resteasy.reactive.links;

/**
 * Enum defining the scope of REST links to inject.
 */
enum RestLinkType {
    /**
     * Only type-level (non-instance) links.
     * These are links that don't require instance data (like "list all" endpoints).
     */
    TYPE,

    /**
     * All links including instance-based links.
     * Includes both type-level links and links specific to resource instances
     * (like "self", "edit", "delete" for a specific resource).
     */
    INSTANCE
}
```

### REST Link ID

Mark fields or methods as providing the identifier for generating instance-based links.

```java { .api }
package io.quarkus.resteasy.reactive.links;

/**
 * Marks a field or method as providing the ID for REST link generation.
 * Used to extract the identifier from an entity for building URIs.
 * Applied to entity classes to indicate which property is the ID.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@interface RestLinkId {
}
```

**Usage Examples:**

```java
import io.quarkus.resteasy.reactive.links.RestLinkId;
import io.quarkus.resteasy.reactive.links.InjectRestLinks;
import io.quarkus.resteasy.reactive.links.RestLinkType;

@InjectRestLinks(RestLinkType.INSTANCE)
public class Book {
    // Mark the ID field
    @RestLinkId
    public Long id;

    public String title;
    public String author;
}

// Using getter method
@InjectRestLinks(RestLinkType.INSTANCE)
public class Article {
    private Long articleId;
    public String title;

    // Mark the ID getter
    @RestLinkId
    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long id) {
        this.articleId = id;
    }
}

// Composite ID
@InjectRestLinks(RestLinkType.INSTANCE)
public class OrderItem {
    @RestLinkId
    public Long orderId;

    @RestLinkId
    public Long itemId;

    public int quantity;
}
```

### REST Links Provider Interface

Programmatically access and manage REST links.

```java { .api }
package io.quarkus.resteasy.reactive.links;

/**
 * Interface for programmatically accessing REST links.
 * Can be injected into resource classes to retrieve link information.
 */
interface RestLinksProvider {
    /**
     * Get links for a given entity.
     *
     * @param entity The entity to get links for
     * @return Collection of links for the entity
     */
    Collection<LinkInfo> getLinks(Object entity);

    /**
     * Get links for a given entity type.
     *
     * @param entityClass The entity class
     * @return Collection of links for the type
     */
    Collection<LinkInfo> getLinks(Class<?> entityClass);
}
```

**Usage Example:**

```java
import io.quarkus.resteasy.reactive.links.RestLinksProvider;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;

@Path("/books")
public class BookResource {

    @Inject
    RestLinksProvider linksProvider;

    @GET
    @Path("/{id}/links")
    public Response getBookLinks(@PathParam("id") Long id) {
        Book book = findBook(id);

        // Programmatically get links for this book
        Collection<LinkInfo> links = linksProvider.getLinks(book);

        return Response.ok(links).build();
    }

    @GET
    @Path("/schema/links")
    public Response getBookTypeLinks() {
        // Get type-level links for Book class
        Collection<LinkInfo> links = linksProvider.getLinks(Book.class);

        return Response.ok(links).build();
    }

    private Book findBook(Long id) { return new Book(); }
}

class Book {
    public Long id;
    public String title;
}

class LinkInfo {
    public String rel;
    public String href;
}
```

## Link Response Format

REST links can be injected in two ways:

### HTTP Link Headers

Links are added to the `Link` HTTP response header following RFC 5988:

```http
Link: <http://api.example.com/books/1>; rel="self"
Link: <http://api.example.com/books/1>; rel="edit"
Link: <http://api.example.com/books/1>; rel="delete"
Link: <http://api.example.com/books/1/author>; rel="author"
```

### Entity Links

Links can be embedded in JSON responses when using `@InjectRestLinks` on entity classes:

```json
{
  "id": 1,
  "title": "The Great Book",
  "author": "John Doe",
  "_links": {
    "self": { "href": "http://api.example.com/books/1" },
    "edit": { "href": "http://api.example.com/books/1" },
    "delete": { "href": "http://api.example.com/books/1" },
    "author": { "href": "http://api.example.com/books/1/author" }
  }
}
```

## Complete HATEOAS Example

```java
import io.quarkus.resteasy.reactive.links.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;

@Path("/api/books")
public class BookResource {

    // List endpoint - type-level link
    @GET
    @RestLink(rel = "list", entityType = Book.class)
    public List<Book> list() {
        return bookService.findAll();
    }

    // Get single book with all links
    @GET
    @Path("/{id}")
    @RestLink(rel = "self", entityType = Book.class)
    @InjectRestLinks(RestLinkType.INSTANCE)
    public Book get(@PathParam("id") Long id) {
        return bookService.findById(id);
    }

    // Create book
    @POST
    @RestLink(rel = "create", entityType = Book.class)
    public Response create(Book book) {
        Book created = bookService.create(book);
        URI location = UriBuilder.fromResource(BookResource.class)
            .path("/{id}")
            .build(created.id);
        return Response.created(location).entity(created).build();
    }

    // Update book
    @PUT
    @Path("/{id}")
    @RestLink(rel = "edit", entityType = Book.class)
    public Book update(@PathParam("id") Long id, Book book) {
        return bookService.update(id, book);
    }

    // Delete book
    @DELETE
    @Path("/{id}")
    @RestLink(rel = "delete", entityType = Book.class)
    public Response delete(@PathParam("id") Long id) {
        bookService.delete(id);
        return Response.noContent().build();
    }

    // Related resources
    @GET
    @Path("/{id}/reviews")
    @RestLink(rel = "reviews", entityType = Book.class)
    public List<Review> getReviews(@PathParam("id") Long id) {
        return reviewService.findByBookId(id);
    }

    @Inject
    BookService bookService;

    @Inject
    ReviewService reviewService;
}

@InjectRestLinks(RestLinkType.INSTANCE)
class Book {
    @RestLinkId
    public Long id;

    public String title;
    public String isbn;
    public String author;
}

class Review {
    public Long id;
    public int rating;
    public String comment;
}

interface BookService {
    List<Book> findAll();
    Book findById(Long id);
    Book create(Book book);
    Book update(Long id, Book book);
    void delete(Long id);
}

interface ReviewService {
    List<Review> findByBookId(Long bookId);
}
```

## Integration with JAX-RS

REST Links work seamlessly with JAX-RS response types:

```java
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;

@GET
@Path("/{id}")
public Response getBookWithManualLinks(@PathParam("id") Long id) {
    Book book = findBook(id);

    // Manual link creation
    Link selfLink = Link.fromUri("/books/" + id).rel("self").build();
    Link editLink = Link.fromUri("/books/" + id).rel("edit").build();

    return Response.ok(book)
        .links(selfLink, editLink)
        .build();
}
```

However, using `@RestLink` and `@InjectRestLinks` is preferred as it automates link generation and maintains consistency across the API.
