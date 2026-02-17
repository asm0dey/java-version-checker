# REST Data Panache

REST Data Panache automatically generates RESTful CRUD endpoints for Panache entities, eliminating boilerplate code for standard entity operations. It provides both blocking and reactive implementations with customizable paths, security, and pagination.

## Capabilities

### Blocking REST Resources

Template interface for generating synchronous REST endpoints with standard CRUD operations on Panache entities.

```java { .api }
package io.quarkus.rest.data.panache;

/**
 * Base interface for generating REST resources for Panache entities.
 * Implementations automatically provide standard CRUD endpoints.
 *
 * @param <Entity> The Panache entity type
 * @param <ID> The entity's ID type
 */
public interface RestDataResource<Entity, ID> {

    /**
     * List all entities with optional pagination and sorting.
     * Endpoint: GET /{resource-name}
     *
     * @param page Pagination information
     * @param sort Sorting criteria
     * @return List of entities
     */
    default List<Entity> list(Page page, Sort sort) {
        throw new UnsupportedOperationException();
    }

    /**
     * Count all entities.
     * Endpoint: GET /{resource-name}/count
     *
     * @return Total count of entities
     */
    default long count() {
        throw new UnsupportedOperationException();
    }

    /**
     * Get a single entity by ID.
     * Endpoint: GET /{resource-name}/{id}
     *
     * @param id The entity ID
     * @return The entity or 404 if not found
     */
    default Entity get(ID id) {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a new entity.
     * Endpoint: POST /{resource-name}
     *
     * @param entity The entity to create
     * @return The created entity with generated ID
     */
    default Entity add(Entity entity) {
        throw new UnsupportedOperationException();
    }

    /**
     * Update an existing entity.
     * Endpoint: PUT /{resource-name}/{id}
     *
     * @param id The entity ID
     * @param entity The updated entity data
     * @return The updated entity
     */
    default Entity update(ID id, Entity entity) {
        throw new UnsupportedOperationException();
    }

    /**
     * Delete an entity by ID.
     * Endpoint: DELETE /{resource-name}/{id}
     *
     * @param id The entity ID
     * @return true if deleted, false if not found
     */
    default boolean delete(ID id) {
        throw new UnsupportedOperationException();
    }
}
```

**Usage Example:**

```java
import io.quarkus.rest.data.panache.RestDataResource;
import io.quarkus.rest.data.panache.ResourceProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
public class Product extends PanacheEntityBase {
    @Id
    @GeneratedValue
    public Long id;
    public String name;
    public Double price;
}

// Automatically generates CRUD endpoints at /products
@ResourceProperties(path = "products")
public interface ProductResource extends RestDataResource<Product, Long> {
    // All CRUD methods are automatically implemented
    // No additional code required!
}

// Available endpoints:
// GET    /products          - List all products (with pagination)
// GET    /products/count    - Count products
// GET    /products/{id}     - Get product by ID
// POST   /products          - Create new product
// PUT    /products/{id}     - Update product
// DELETE /products/{id}     - Delete product
```

### Reactive REST Resources

Template interface for generating reactive REST endpoints with non-blocking CRUD operations using Mutiny.

```java { .api }
package io.quarkus.rest.data.panache;

/**
 * Base interface for generating reactive REST resources for Panache entities.
 * All operations return Uni for non-blocking execution.
 *
 * @param <Entity> The Panache entity type
 * @param <ID> The entity's ID type
 */
public interface ReactiveRestDataResource<Entity, ID> {

    /**
     * List all entities reactively with optional pagination and sorting.
     * Endpoint: GET /{resource-name}
     *
     * @param page Pagination information
     * @param sort Sorting criteria
     * @return Uni containing list of entities
     */
    default Uni<List<Entity>> list(Page page, Sort sort) {
        throw new UnsupportedOperationException();
    }

    /**
     * Count all entities reactively.
     * Endpoint: GET /{resource-name}/count
     *
     * @return Uni containing total count
     */
    default Uni<Long> count() {
        throw new UnsupportedOperationException();
    }

    /**
     * Get a single entity by ID reactively.
     * Endpoint: GET /{resource-name}/{id}
     *
     * @param id The entity ID
     * @return Uni containing the entity or 404 if not found
     */
    default Uni<Entity> get(ID id) {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a new entity reactively.
     * Endpoint: POST /{resource-name}
     *
     * @param entity The entity to create
     * @return Uni containing the created entity
     */
    default Uni<Entity> add(Entity entity) {
        throw new UnsupportedOperationException();
    }

    /**
     * Update an existing entity reactively.
     * Endpoint: PUT /{resource-name}/{id}
     *
     * @param id The entity ID
     * @param entity The updated entity data
     * @return Uni containing the updated entity
     */
    default Uni<Entity> update(ID id, Entity entity) {
        throw new UnsupportedOperationException();
    }

    /**
     * Delete an entity by ID reactively.
     * Endpoint: DELETE /{resource-name}/{id}
     *
     * @param id The entity ID
     * @return Uni containing true if deleted, false if not found
     */
    default Uni<Boolean> delete(ID id) {
        throw new UnsupportedOperationException();
    }
}
```

**Usage Example:**

```java
import io.quarkus.rest.data.panache.ReactiveRestDataResource;
import io.quarkus.rest.data.panache.ResourceProperties;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;

@Entity
public class Order extends PanacheEntityBase {
    @Id
    @GeneratedValue
    public Long id;
    public String customerName;
    public Double total;
}

// Reactive CRUD endpoints at /orders
@ResourceProperties(path = "orders", paged = true)
public interface OrderResource extends ReactiveRestDataResource<Order, Long> {
    // All reactive CRUD methods are automatically implemented

    // Can override to add custom logic
    @Override
    default Uni<Order> add(Order order) {
        // Custom validation before creation
        if (order.total < 0) {
            return Uni.createFrom().failure(
                new IllegalArgumentException("Total must be positive")
            );
        }
        return ReactiveRestDataResource.super.add(order);
    }
}
```

### Resource Customization

Configure REST Data Panache resource behavior including paths, pagination, HAL support, and security.

```java { .api }
package io.quarkus.rest.data.panache;

/**
 * Customizes the generated REST resource.
 * Applied at interface or class level.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface ResourceProperties {

    /**
     * Whether to expose this resource (default: true).
     * Set to false to disable endpoint generation.
     */
    boolean exposed() default true;

    /**
     * Base path for the resource (default: pluralized entity name).
     * Example: "products", "api/users"
     */
    String path() default "";

    /**
     * Whether to enable pagination for list operations (default: true).
     * When enabled, accepts page and size query parameters.
     */
    boolean paged() default true;

    /**
     * Whether to enable HAL (Hypertext Application Language) responses (default: false).
     * When enabled, responses include _links for navigation.
     */
    boolean hal() default false;

    /**
     * HAL collection name (default: entity name).
     * Used as the key for embedded collections in HAL responses.
     */
    String halCollectionName() default "";

    /**
     * Security roles allowed to access this resource.
     * When specified, all endpoints require one of these roles.
     * Example: {"admin", "user"}
     */
    String[] rolesAllowed() default {};
}
```

**Usage Examples:**

```java
import io.quarkus.rest.data.panache.ResourceProperties;
import io.quarkus.rest.data.panache.RestDataResource;

// Custom path and pagination disabled
@ResourceProperties(path = "api/v1/products", paged = false)
public interface ProductResource extends RestDataResource<Product, Long> {}

// HAL responses with custom collection name
@ResourceProperties(
    path = "customers",
    hal = true,
    halCollectionName = "customerList"
)
public interface CustomerResource extends RestDataResource<Customer, Long> {}

// Security restrictions
@ResourceProperties(
    path = "admin/users",
    rolesAllowed = {"admin", "super-admin"}
)
public interface UserResource extends RestDataResource<User, Long> {}

// Disabled resource (no endpoints generated)
@ResourceProperties(exposed = false)
public interface InternalResource extends RestDataResource<Internal, Long> {}

// Combined settings
@ResourceProperties(
    path = "orders",
    paged = true,
    hal = true,
    halCollectionName = "orders",
    rolesAllowed = {"user", "admin"}
)
public interface OrderResource extends ReactiveRestDataResource<Order, Long> {}
```

### Method-Level Customization

Fine-grained control over individual CRUD operations including exposure, paths, and security.

```java { .api }
package io.quarkus.rest.data.panache;

/**
 * Customizes individual methods in the generated REST resource.
 * Applied to method declarations in the resource interface.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface MethodProperties {

    /**
     * Whether to expose this method (default: true).
     * Set to false to disable this specific endpoint.
     */
    boolean exposed() default true;

    /**
     * Custom path for this method (default: standard REST path).
     * Relative to the resource path.
     * Example: "active", "by-status/{status}"
     */
    String path() default "";

    /**
     * Security roles allowed for this specific method.
     * Overrides resource-level rolesAllowed for this method.
     * Example: {"admin"}
     */
    String[] rolesAllowed() default {};
}
```

**Usage Examples:**

```java
import io.quarkus.rest.data.panache.ResourceProperties;
import io.quarkus.rest.data.panache.MethodProperties;
import io.quarkus.rest.data.panache.RestDataResource;

@ResourceProperties(path = "products", rolesAllowed = {"user"})
public interface ProductResource extends RestDataResource<Product, Long> {

    // Disable delete operation
    @Override
    @MethodProperties(exposed = false)
    boolean delete(Long id);

    // Only admins can create
    @Override
    @MethodProperties(rolesAllowed = {"admin"})
    Product add(Product product);

    // Custom path for list operation
    @Override
    @MethodProperties(path = "all")
    List<Product> list(Page page, Sort sort);
    // Endpoint: GET /products/all

    // Multiple customizations
    @Override
    @MethodProperties(
        path = "item/{id}",
        rolesAllowed = {"user", "guest"}
    )
    Product get(Long id);
    // Endpoint: GET /products/item/{id}
}

@ResourceProperties(path = "users")
public interface UserResource extends ReactiveRestDataResource<User, Long> {

    // Only super-admins can delete users
    @Override
    @MethodProperties(rolesAllowed = {"super-admin"})
    Uni<Boolean> delete(Long id);

    // Disable update (use custom endpoint instead)
    @Override
    @MethodProperties(exposed = false)
    Uni<User> update(Long id, User user);

    // Custom count endpoint path
    @Override
    @MethodProperties(path = "total")
    Uni<Long> count();
    // Endpoint: GET /users/total
}
```

### REST Data Panache Exception

Exception thrown by REST Data Panache when errors occur during CRUD operations.

```java { .api }
package io.quarkus.rest.data.panache;

/**
 * Runtime exception thrown by REST Data Panache implementations.
 * Typically indicates issues with entity operations or configuration.
 */
public class RestDataPanacheException extends RuntimeException {

    /**
     * Constructs exception with message and cause.
     *
     * @param message Error description
     * @param cause Underlying cause
     */
    public RestDataPanacheException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

## Pagination and Sorting

REST Data Panache supports pagination and sorting through query parameters:

**Pagination Query Parameters:**
- `page` - Page number (0-indexed, default: 0)
- `size` - Page size (default: 20)

**Sorting Query Parameters:**
- `sort` - Sort criteria in format: `field,direction`
  - Direction: `asc` (ascending) or `desc` (descending)
  - Multiple sort fields: `sort=name,asc&sort=price,desc`

**Example Requests:**

```bash
# List first 10 products
GET /products?page=0&size=10

# List second page with 50 items
GET /products?page=1&size=50

# Sort by name ascending
GET /products?sort=name,asc

# Sort by price descending, then name ascending
GET /products?sort=price,desc&sort=name,asc

# Combined pagination and sorting
GET /products?page=2&size=25&sort=price,desc
```

## HAL Responses

When `hal = true` in `@ResourceProperties`, responses follow HAL (Hypertext Application Language) format with embedded navigation links:

```json
{
  "_embedded": {
    "products": [
      {
        "id": 1,
        "name": "Widget",
        "price": 29.99
      }
    ]
  },
  "_links": {
    "self": {
      "href": "/products?page=0&size=20"
    },
    "first": {
      "href": "/products?page=0&size=20"
    },
    "last": {
      "href": "/products?page=4&size=20"
    },
    "next": {
      "href": "/products?page=1&size=20"
    }
  }
}
```

## Integration with Panache

REST Data Panache works with both Hibernate ORM Panache and Hibernate Reactive Panache entities:

**Hibernate ORM (Blocking):**
```java
import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Book extends PanacheEntity {
    public String title;
    public String author;
}

@ResourceProperties(path = "books")
public interface BookResource extends RestDataResource<Book, Long> {}
```

**Hibernate Reactive (Non-blocking):**
```java
import io.quarkus.hibernate.reactive.panache.PanacheEntity;

@Entity
public class Article extends PanacheEntity {
    public String title;
    public String content;
}

@ResourceProperties(path = "articles")
public interface ArticleResource extends ReactiveRestDataResource<Article, Long> {}
```

## Types

```java { .api }
// Pagination support
package io.quarkus.panache.common;

interface Page {
    int index();
    int size();

    static Page of(int index, int size);
}

interface Sort {
    List<Sort.Column> getColumns();

    static Sort by(String... columns);
    static Sort ascending(String... columns);
    static Sort descending(String... columns);

    interface Column {
        String getName();
        Sort.Direction getDirection();
    }

    enum Direction {
        Ascending, Descending
    }
}
```
