# Test Profiles

Test profiles enable different configurations and CDI alternatives for different test scenarios, allowing tests to run with customized application behavior.

## @TestProfile Annotation

Associates a test class with a specific test profile implementation.

```java { .api }
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestProfile {
    /**
     * The test profile to use. If subsequent tests use the same
     * profile then Quarkus will not be restarted between tests,
     * giving faster execution.
     */
    Class<? extends QuarkusTestProfile> value();
}
```

### Usage Example

```java
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(DatabaseTestProfile.class)
class UserServiceDatabaseTest {
    
    @Inject
    UserService userService;
    
    @Test
    void testWithH2Database() {
        // Test runs with H2 database configuration
    }
}
```

## QuarkusTestProfile Interface

Defines the configuration and behavior for a test profile.

```java { .api }
public interface QuarkusTestProfile {
    
    /**
     * Returns additional config to be applied to the test. This
     * will override any existing config (including in application.properties).
     */
    default Map<String, String> getConfigOverrides() {
        return Collections.emptyMap();
    }
    
    /**
     * Returns enabled alternatives. This has the same effect as setting 
     * the 'quarkus.arc.selected-alternatives' config key.
     */
    default Set<Class<?>> getEnabledAlternatives() {
        return Collections.emptySet();
    }
    
    /**
     * Allows the default config profile to be overridden.
     */
    default String getConfigProfile() {
        return null;
    }
    
    /**
     * Additional QuarkusTestResourceLifecycleManager classes to be used 
     * from this specific test profile.
     */
    default List<TestResourceEntry> testResources() {
        return Collections.emptyList();
    }
    
    /**
     * If this returns true then only the test resources returned from 
     * testResources() will be started, global annotated test resources 
     * will be ignored.
     */
    default boolean disableGlobalTestResources() {
        return false;
    }
    
    /**
     * The tags this profile is associated with.
     */
    default Set<String> tags() {
        return Collections.emptySet();
    }
    
    /**
     * The command line parameters that are passed to the main method on startup.
     */
    default String[] commandLineParameters() {
        return new String[0];
    }
    
    /**
     * If the main method should be run.
     */
    default boolean runMainMethod() {
        return false;
    }
    
    /**
     * If this method returns true then all StartupEvent and ShutdownEvent 
     * observers declared on application beans should be disabled.
     */
    default boolean disableApplicationLifecycleObservers() {
        return false;
    }
}
```

## TestResourceEntry Class

Configuration for test resources within a profile.

```java { .api }
public final class TestResourceEntry {
    
    public TestResourceEntry(Class<? extends QuarkusTestResourceLifecycleManager> clazz);
    
    public TestResourceEntry(Class<? extends QuarkusTestResourceLifecycleManager> clazz, 
                           Map<String, String> args);
    
    public TestResourceEntry(Class<? extends QuarkusTestResourceLifecycleManager> clazz, 
                           Map<String, String> args, boolean parallel);
    
    public Class<? extends QuarkusTestResourceLifecycleManager> getClazz();
    public Map<String, String> getArgs();
    public boolean isParallel();
}
```

## Common Test Profile Patterns

### Database Configuration Profile

```java
import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class H2DatabaseProfile implements QuarkusTestProfile {
    
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "quarkus.datasource.db-kind", "h2",
            "quarkus.datasource.jdbc.url", "jdbc:h2:mem:testdb",
            "quarkus.hibernate-orm.database.generation", "drop-and-create"
        );
    }
}

@QuarkusTest
@TestProfile(H2DatabaseProfile.class)
class DatabaseIntegrationTest {
    // Tests run with H2 in-memory database
}
```

### Alternative Bean Profile

```java
// Production implementation
@ApplicationScoped
public class EmailServiceImpl implements EmailService {
    public void sendEmail(String to, String subject, String body) {
        // Real email sending logic
    }
}

// Test alternative
@Alternative
@ApplicationScoped
public class MockEmailService implements EmailService {
    public void sendEmail(String to, String subject, String body) {
        // Mock implementation - no actual email sent
    }
}

// Test profile enabling the alternative
public class MockEmailProfile implements QuarkusTestProfile {
    
    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(MockEmailService.class);
    }
}

@QuarkusTest
@TestProfile(MockEmailProfile.class)
class UserRegistrationTest {
    
    @Inject
    EmailService emailService; // Injects MockEmailService
    
    @Test
    void testUserRegistration() {
        // Test runs with mock email service
    }
}
```

### Test Resources Profile

```java
// Custom test resource
public class RedisTestResource implements QuarkusTestResourceLifecycleManager {
    
    @Override
    public Map<String, String> start() {
        // Start Redis container
        return Map.of(
            "quarkus.redis.hosts", "redis://localhost:6379"
        );
    }
    
    @Override
    public void stop() {
        // Stop Redis container
    }
}

// Profile with test resource
public class RedisProfile implements QuarkusTestProfile {
    
    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(
            new TestResourceEntry(RedisTestResource.class)
        );
    }
}

@QuarkusTest
@TestProfile(RedisProfile.class)
class CacheServiceTest {
    // Test runs with Redis test container
}
```

### Development Mode Profile

```java
public class DevModeProfile implements QuarkusTestProfile {
    
    @Override
    public String getConfigProfile() {
        return "dev";
    }
    
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "quarkus.log.level", "DEBUG",
            "quarkus.hibernate-orm.log.sql", "true"
        );
    }
    
    @Override
    public boolean runMainMethod() {
        return true; // Start application main method
    }
}
```

### Tagged Profile System

```java
public class IntegrationTestProfile implements QuarkusTestProfile {
    
    @Override
    public Set<String> tags() {
        return Set.of("integration", "database");
    }
    
    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(
            new TestResourceEntry(PostgresTestResource.class),
            new TestResourceEntry(RedisTestResource.class)
        );
    }
}

// Run only tests with specific tags:
// mvn test -Dquarkus.test.profile.tags=integration
```

## Profile Performance Optimization

### Profile Reuse
```java
// Tests with same profile run consecutively without Quarkus restart
@QuarkusTest
@TestProfile(DatabaseProfile.class)
class UserServiceTest { }

@QuarkusTest  
@TestProfile(DatabaseProfile.class) // Same profile - no restart
class ProductServiceTest { }

@QuarkusTest
@TestProfile(CacheProfile.class) // Different profile - restart required
class CacheServiceTest { }
```

### Resource Lifecycle Management
```java
public class OptimizedProfile implements QuarkusTestProfile {
    
    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(
            // Parallel resource startup
            new TestResourceEntry(DatabaseResource.class, Map.of(), true),
            new TestResourceEntry(MessageQueueResource.class, Map.of(), true)
        );
    }
    
    @Override
    public boolean disableGlobalTestResources() {
        return true; // Only use profile-specific resources
    }
}
```

## Error Handling

### Profile Configuration Errors
```java
public class InvalidProfile implements QuarkusTestProfile {
    
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "invalid.property", "value" // May cause deployment error
        );
    }
}
```

### Alternative Resolution Errors
```java
// Missing @Alternative annotation will cause deployment error
@ApplicationScoped // Should be @Alternative @ApplicationScoped
public class TestServiceImpl implements TestService { }

public class BrokenProfile implements QuarkusTestProfile {
    
    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(TestServiceImpl.class); // Deployment will fail
    }
}
```