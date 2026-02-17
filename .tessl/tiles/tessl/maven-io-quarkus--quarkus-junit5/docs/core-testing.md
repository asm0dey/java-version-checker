# Core Testing

The core testing capabilities provide the primary annotations for different testing modes in Quarkus applications.

## Unit Testing with @QuarkusTest

The `@QuarkusTest` annotation enables full CDI support in unit tests, running within the same JVM as the test with complete application context.

```java { .api }
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(QuarkusTestExtension.class)
@Tag("io.quarkus.test.junit.QuarkusTest")
public @interface QuarkusTest {
}
```

### Usage Example

```java
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ProductServiceTest {
    
    @Inject
    ProductService productService;
    
    @Inject
    EntityManager entityManager;
    
    @Test
    void testCreateProduct() {
        Product product = productService.createProduct("Laptop", 999.99);
        assertNotNull(product.getId());
        assertEquals("Laptop", product.getName());
    }
    
    @Test
    @Transactional
    void testProductPersistence() {
        Product product = new Product("Phone", 599.99);
        entityManager.persist(product);
        entityManager.flush();
        
        Product found = entityManager.find(Product.class, product.getId());
        assertEquals("Phone", found.getName());
    }
}
```

**Key Features:**
- Full CDI dependency injection with `@Inject`
- Transaction support with `@Transactional`  
- Configuration property injection with `@ConfigProperty`
- Dev services integration (automatic test databases, etc.)
- Mock support via `QuarkusMock`

## Integration Testing with @QuarkusIntegrationTest

The `@QuarkusIntegrationTest` annotation runs tests against built application artifacts (JAR files, native images, or containers) in separate processes.

```java { .api }
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith({DisabledOnIntegrationTestCondition.class, QuarkusIntegrationTestExtension.class})
public @interface QuarkusIntegrationTest {
    
    /**
     * @deprecated Use DevServicesContext instead
     */
    @Deprecated
    interface Context extends DevServicesContext {
    }
}
```

### Usage Example

```java
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusIntegrationTest
class ProductEndpointIT {
    
    @Test
    void testGetProducts() {
        given()
            .when().get("/api/products")
            .then()
                .statusCode(200)
                .body("size()", is(0));
    }
    
    @Test
    void testCreateProduct() {
        given()
            .contentType("application/json")
            .body("{\"name\":\"Tablet\",\"price\":299.99}")
            .when().post("/api/products")
            .then()
                .statusCode(201);
    }
    
    @Test
    void testHealthCheck() {
        given()
            .when().get("/q/health")
            .then()
                .statusCode(200)
                .body("status", is("UP"));
    }
}
```

**Key Features:**
- Tests against production-like artifacts
- Supports JAR, native image, and container testing
- HTTP-based testing with REST Assured
- No CDI injection (tests run in separate process)
- Automatic artifact detection and launching
- Dev services integration for external dependencies

## Test Execution Modes

### JVM Mode vs Native Mode
- **JVM Mode**: Tests run against standard JVM bytecode
- **Native Mode**: Tests run against GraalVM native images  
- **Container Mode**: Tests run against containerized applications

### Artifact Type Detection
The framework automatically detects and launches the appropriate artifact type:

1. **Native Binary**: If `target/[app-name]-runner` exists
2. **Container**: If container image was built during build process
3. **JAR**: Falls back to `java -jar target/quarkus-app/quarkus-run.jar`

### Test Isolation
- `@QuarkusTest`: Tests share application instance (faster, but less isolation)
- `@QuarkusIntegrationTest`: Each test class gets fresh application instance (slower, but better isolation)

## Error Handling

Common exceptions and error scenarios:

```java
// Configuration errors
@ConfigProperty(name = "nonexistent.property")
String value; // Throws DeploymentException if property not found

// CDI injection errors  
@Inject
NonExistentBean bean; // Throws UnsatisfiedResolutionException

// Test resource errors
@QuarkusTestResource(DatabaseResource.class)
class MyTest {
    // Throws TestResourceException if resource fails to start
}
```

## Testing Best Practices

### Test Class Organization
```java
// Base test class with @QuarkusTest
@QuarkusTest
class UserServiceTest {
    // Unit test methods
}

// Integration test extends base class
@QuarkusIntegrationTest  
class UserServiceIT extends UserServiceTest {
    // Same test methods run against built artifact
}
```

### Resource Management
```java
@QuarkusTest
class DatabaseTest {
    
    @TestTransaction  // Automatic rollback
    @Test
    void testDataModification() {
        // Changes are rolled back after test
    }
    
    @Test
    void testReadOnlyOperation() {
        // No transaction needed for read-only tests
    }
}
```