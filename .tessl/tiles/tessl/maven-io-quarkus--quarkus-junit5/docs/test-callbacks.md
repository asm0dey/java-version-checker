# Test Callbacks

Quarkus JUnit 5 provides a comprehensive callback system through service provider interfaces that allow customization of test execution lifecycle at various phases.

## Callback Interfaces

### QuarkusTestBeforeEachCallback

Called before each test method in a `@QuarkusTest`.

```java { .api }
public interface QuarkusTestBeforeEachCallback {
    void beforeEach(QuarkusTestMethodContext context);
}
```

### QuarkusTestAfterEachCallback

Called after each test method in a `@QuarkusTest`.

```java { .api }
public interface QuarkusTestAfterEachCallback {
    void afterEach(QuarkusTestMethodContext context);
}
```

### QuarkusTestBeforeClassCallback

Called before the test class is executed.

```java { .api }
public interface QuarkusTestBeforeClassCallback {
    void beforeClass(Class<?> testClass);
}
```

### QuarkusTestAfterAllCallback

Called after all tests in a test class have been executed.

```java { .api }
public interface QuarkusTestAfterAllCallback {
    void afterAll(QuarkusTestContext context);
}
```

### QuarkusTestAfterConstructCallback

Called after the test instance has been constructed.

```java { .api }
public interface QuarkusTestAfterConstructCallback {
    void afterConstruct(Object testInstance);
}
```

### QuarkusTestBeforeTestExecutionCallback

Called immediately before test method execution.

```java { .api }
public interface QuarkusTestBeforeTestExecutionCallback {
    void beforeTestExecution(QuarkusTestMethodContext context);
}
```

### QuarkusTestAfterTestExecutionCallback

Called immediately after test method execution.

```java { .api }
public interface QuarkusTestAfterTestExecutionCallback {
    void afterTestExecution(QuarkusTestMethodContext context);
}
```

## Context Classes

### QuarkusTestContext

Context object passed to test callbacks containing test execution information.

```java { .api }
public class QuarkusTestContext {
    
    /**
     * Get the test instance
     */
    public Object getTestInstance();
    
    /**
     * Get outer test instances (for nested tests)
     */
    public List<Object> getOuterInstances();
    
    /**
     * Get the test status including failure information
     */
    public TestStatus getTestStatus();
}
```

### QuarkusTestMethodContext

Extended context for method-level callbacks, providing access to the test method.

```java { .api }
public final class QuarkusTestMethodContext extends QuarkusTestContext {
    
    /**
     * Get the test method being executed
     */
    public Method getTestMethod();
}
```

## Service Provider Registration

Callback implementations must be registered as service providers in `META-INF/services/` files.

### Example Service Registration

Create file: `META-INF/services/io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback`

```text
com.example.MyTestSetupCallback
com.example.DatabaseCleanupCallback
```

## Callback Implementation Examples

### Database Cleanup Callback

```java
import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

public class DatabaseCleanupCallback implements QuarkusTestAfterEachCallback {
    
    @Override
    public void afterEach(QuarkusTestMethodContext context) {
        // Clean up database after each test
        EntityManager em = CDI.current().select(EntityManager.class).get();
        
        cleanupTestData(em);
    }
    
    @Transactional
    private void cleanupTestData(EntityManager em) {
        em.createQuery("DELETE FROM TestEntity").executeUpdate();
        em.createQuery("DELETE FROM UserEntity WHERE email LIKE '%test%'").executeUpdate();
    }
}
```

### Test Metrics Collector

```java
import io.quarkus.test.junit.callback.QuarkusTestBeforeTestExecutionCallback;
import io.quarkus.test.junit.callback.QuarkusTestAfterTestExecutionCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class TestMetricsCallback implements 
    QuarkusTestBeforeTestExecutionCallback, 
    QuarkusTestAfterTestExecutionCallback {
    
    private final Map<String, Long> testStartTimes = new ConcurrentHashMap<>();
    private final Map<String, Long> testDurations = new ConcurrentHashMap<>();
    
    @Override
    public void beforeTestExecution(QuarkusTestMethodContext context) {
        String testKey = getTestKey(context);
        testStartTimes.put(testKey, System.currentTimeMillis());
        System.out.println("Starting test: " + testKey);
    }
    
    @Override
    public void afterTestExecution(QuarkusTestMethodContext context) {
        String testKey = getTestKey(context);
        Long startTime = testStartTimes.remove(testKey);
        
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            testDurations.put(testKey, duration);
            System.out.println("Test " + testKey + " took " + duration + "ms");
        }
    }
    
    private String getTestKey(QuarkusTestMethodContext context) {
        return context.getTestInstance().getClass().getSimpleName() + 
               "." + context.getTestMethod().getName();
    }
}
```

### Mock Installation Callback

```java
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import io.quarkus.test.junit.QuarkusMock;
import org.mockito.Mockito;
import java.lang.reflect.Field;

public class AutoMockCallback implements QuarkusTestBeforeEachCallback {
    
    @Override
    public void beforeEach(QuarkusTestMethodContext context) {
        Object testInstance = context.getTestInstance();
        Class<?> testClass = testInstance.getClass();
        
        // Find fields annotated with custom @MockBean annotation
        for (Field field : testClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(MockBean.class)) {
                installMockForField(field, testInstance);
            }
        }
    }
    
    private void installMockForField(Field field, Object testInstance) {
        try {
            field.setAccessible(true);
            Object realBean = field.get(testInstance);
            
            if (realBean != null) {
                Object mock = Mockito.mock(field.getType());
                QuarkusMock.installMockForInstance(mock, realBean);
                
                // Replace field value with mock for easy access in tests
                field.set(testInstance, mock);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to install mock for field: " + field.getName(), e);
        }
    }
}

// Custom annotation
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MockBean {
}
```

### Test Data Setup Callback

```java
import io.quarkus.test.junit.callback.QuarkusTestAfterConstructCallback;
import io.quarkus.test.junit.callback.QuarkusTestBeforeClassCallback;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

public class TestDataSetupCallback implements 
    QuarkusTestAfterConstructCallback,
    QuarkusTestBeforeClassCallback {
    
    @Override
    public void beforeClass(Class<?> testClass) {
        if (testClass.isAnnotationPresent(WithTestData.class)) {
            setupTestData(testClass.getAnnotation(WithTestData.class));
        }
    }
    
    @Override
    public void afterConstruct(Object testInstance) {
        Class<?> testClass = testInstance.getClass();
        if (testClass.isAnnotationPresent(WithMethodTestData.class)) {
            setupMethodSpecificData(testInstance);
        }
    }
    
    @Transactional
    void setupTestData(WithTestData annotation) {
        EntityManager em = CDI.current().select(EntityManager.class).get();
        
        for (String dataset : annotation.datasets()) {
            loadDataset(em, dataset);
        }
    }
    
    private void loadDataset(EntityManager em, String dataset) {
        switch (dataset) {
            case "users":
                em.persist(new User("test1@example.com", "Test User 1"));
                em.persist(new User("test2@example.com", "Test User 2"));
                break;
            case "products":
                em.persist(new Product("Laptop", 999.99));
                em.persist(new Product("Mouse", 29.99));
                break;
        }
    }
    
    private void setupMethodSpecificData(Object testInstance) {
        // Setup data specific to individual test methods
    }
}

// Custom annotations
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WithTestData {
    String[] datasets() default {};
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WithMethodTestData {
    String value() default "";
}
```

## Error Handling in Callbacks

### Safe Callback Implementation

```java
import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SafeCleanupCallback implements QuarkusTestAfterEachCallback {
    
    private static final Logger logger = LoggerFactory.getLogger(SafeCleanupCallback.class);
    
    @Override
    public void afterEach(QuarkusTestMethodContext context) {
        try {
            performCleanup(context);
        } catch (Exception e) {
            // Log error but don't fail the test
            logger.error("Cleanup failed for test: " + 
                        context.getTestMethod().getName(), e);
        }
    }
    
    private void performCleanup(QuarkusTestMethodContext context) {
        // Cleanup logic that might fail
        TestStatus status = context.getTestStatus();
        if (status.getTestErrorCause() != null) {
            // Special cleanup for failed tests
            logger.info("Test failed, performing error cleanup");
        }
    }
}
```

### Context Information Usage

```java
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import java.lang.reflect.Method;

public class ContextAwareCallback implements QuarkusTestBeforeEachCallback {
    
    @Override
    public void beforeEach(QuarkusTestMethodContext context) {
        Method testMethod = context.getTestMethod();
        Object testInstance = context.getTestInstance();
        
        // Check for custom test annotations
        if (testMethod.isAnnotationPresent(SlowTest.class)) {
            System.out.println("Starting slow test: " + testMethod.getName());
            // Maybe increase timeout or adjust configuration
        }
        
        // Access test class information
        Class<?> testClass = testInstance.getClass();
        if (testClass.isAnnotationPresent(DatabaseTest.class)) {
            // Setup database-specific configuration
            setupDatabaseForTest();
        }
        
        // Check outer instances for nested tests
        if (!context.getOuterInstances().isEmpty()) {
            System.out.println("Running nested test in: " + 
                             context.getOuterInstances().get(0).getClass().getSimpleName());
        }
    }
    
    private void setupDatabaseForTest() {
        // Database setup logic
    }
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface SlowTest {
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface DatabaseTest {
}
```

## Advanced Callback Patterns

### Conditional Callback Execution

```java
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;

public class ConditionalCallback implements QuarkusTestBeforeEachCallback {
    
    @Override
    public void beforeEach(QuarkusTestMethodContext context) {
        // Only execute for certain test classes
        if (shouldExecuteForTest(context)) {
            performSetup(context);
        }
    }
    
    private boolean shouldExecuteForTest(QuarkusTestMethodContext context) {
        Class<?> testClass = context.getTestInstance().getClass();
        
        // Execute only for integration tests
        return testClass.getPackage().getName().contains("integration");
    }
    
    private void performSetup(QuarkusTestMethodContext context) {
        // Setup logic
    }
}
```

### Resource Management Callback

```java
import io.quarkus.test.junit.callback.QuarkusTestBeforeClassCallback;
import io.quarkus.test.junit.callback.QuarkusTestAfterAllCallback;
import io.quarkus.test.junit.callback.QuarkusTestContext;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class ResourceManagerCallback implements 
    QuarkusTestBeforeClassCallback,
    QuarkusTestAfterAllCallback {
    
    private final Map<Class<?>, AutoCloseable> classResources = new ConcurrentHashMap<>();
    
    @Override
    public void beforeClass(Class<?> testClass) {
        if (testClass.isAnnotationPresent(RequiresExternalService.class)) {
            try {
                ExternalServiceMock service = new ExternalServiceMock();
                service.start();
                classResources.put(testClass, service);
            } catch (Exception e) {
                throw new RuntimeException("Failed to start external service", e);
            }
        }
    }
    
    @Override
    public void afterAll(QuarkusTestContext context) {
        Class<?> testClass = context.getTestInstance().getClass();
        AutoCloseable resource = classResources.remove(testClass);
        
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception e) {
                System.err.println("Failed to close resource: " + e.getMessage());
            }
        }
    }
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface RequiresExternalService {
}
```

## Best Practices

### Service Provider Declaration

Always declare your callback implementations in the appropriate service files:

```text
# META-INF/services/io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback
com.example.DatabaseCleanupCallback
com.example.TestMetricsCallback

# META-INF/services/io.quarkus.test.junit.callback.QuarkusTestAfterConstructCallback  
com.example.AutoMockCallback
com.example.TestDataSetupCallback
```

### Error Handling

```java
public class RobustCallback implements QuarkusTestAfterEachCallback {
    
    @Override
    public void afterEach(QuarkusTestMethodContext context) {
        try {
            // Callback logic
            performCleanup();
        } catch (Exception e) {
            // Never let callback exceptions fail tests
            logError("Callback failed", e);
        }
    }
    
    private void logError(String message, Exception e) {
        // Use appropriate logging framework
        System.err.println(message + ": " + e.getMessage());
    }
}
```

### Performance Considerations

```java
public class EfficientCallback implements QuarkusTestBeforeEachCallback {
    
    // Cache expensive resources
    private static final Map<String, Object> cache = new ConcurrentHashMap<>();
    
    @Override
    public void beforeEach(QuarkusTestMethodContext context) {
        String cacheKey = context.getTestMethod().getName();
        
        // Use cached resources when possible
        Object resource = cache.computeIfAbsent(cacheKey, this::createResource);
        
        // Apply resource to test
        applyResource(context, resource);
    }
    
    private Object createResource(String key) {
        // Create expensive resource only once per test method
        return new ExpensiveResource();
    }
    
    private void applyResource(QuarkusTestMethodContext context, Object resource) {
        // Apply resource to test execution
    }
}
```