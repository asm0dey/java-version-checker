# Mocking Support

QuarkusMock provides runtime mock installation for CDI normal scoped beans, enabling test isolation and controlled behavior during testing.

## QuarkusMock Class

Utility class for installing mocks for CDI normal scoped beans including `@ApplicationScoped` and `@RequestScoped` beans.

```java { .api }
public class QuarkusMock {
    
    /**
     * Installs a mock for a CDI normal scoped bean
     *
     * @param mock The mock object
     * @param instance The CDI normal scoped bean that was injected into your test
     * @param <T> The bean type
     */
    public static <T> void installMockForInstance(T mock, T instance);
    
    /**
     * Installs a mock for a CDI normal scoped bean
     *
     * @param mock The mock object  
     * @param instance The type of the CDI normal scoped bean to replace
     * @param qualifiers The CDI qualifiers of the bean to mock
     * @param <T> The bean type
     */
    public static <T> void installMockForType(T mock, Class<? super T> instance, Annotation... qualifiers);
    
    /**
     * Installs a mock for a CDI normal scoped bean by typeLiteral and qualifiers
     *
     * @param mock The mock object
     * @param typeLiteral TypeLiteral representing the required type
     * @param qualifiers The CDI qualifiers of the bean to mock
     * @param <T> The bean type
     */
    public static <T> void installMockForType(T mock, TypeLiteral<? super T> typeLiteral, Annotation... qualifiers);
}
```

## Basic Mock Installation

### Mock by Instance

```java
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
class UserServiceTest {
    
    @Inject
    UserService userService;
    
    @Inject
    EmailService emailService; // Real CDI bean injected
    
    @BeforeEach
    void setup() {
        // Create mock and install it for the injected instance
        EmailService mockEmailService = Mockito.mock(EmailService.class);
        QuarkusMock.installMockForInstance(mockEmailService, emailService);
        
        // Configure mock behavior
        Mockito.when(mockEmailService.sendWelcomeEmail(Mockito.anyString()))
               .thenReturn(true);
    }
    
    @Test
    void testUserRegistration() {
        User user = userService.registerUser("john@example.com", "password");
        
        // Verify mock was called
        Mockito.verify(emailService).sendWelcomeEmail("john@example.com");
        assertNotNull(user);
    }
}
```

### Mock by Type

```java
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
class PaymentProcessorTest {
    
    @BeforeEach
    void setup() {
        // Mock by type without needing injection
        PaymentGateway mockGateway = Mockito.mock(PaymentGateway.class);
        QuarkusMock.installMockForType(mockGateway, PaymentGateway.class);
        
        Mockito.when(mockGateway.processPayment(Mockito.any()))
               .thenReturn(new PaymentResult(true, "SUCCESS"));
    }
    
    @Test
    void testPaymentProcessing() {
        // Test logic that uses PaymentGateway
    }
}
```

## Advanced Mocking Patterns

### Qualified Bean Mocking

```java
@ApplicationScoped
@Named("primary")
public class PrimaryEmailService implements EmailService { }

@ApplicationScoped  
@Named("backup")
public class BackupEmailService implements EmailService { }

@QuarkusTest
class QualifiedMockTest {
    
    @BeforeEach
    void setup() {
        EmailService mockPrimary = Mockito.mock(EmailService.class);
        EmailService mockBackup = Mockito.mock(EmailService.class);
        
        // Mock qualified beans
        QuarkusMock.installMockForType(mockPrimary, EmailService.class, 
                                     NamedLiteral.of("primary"));
        QuarkusMock.installMockForType(mockBackup, EmailService.class,
                                     NamedLiteral.of("backup"));
    }
}
```

### Generic Type Mocking

```java
@ApplicationScoped
public class GenericRepository<T> {
    public List<T> findAll() { return List.of(); }
}

@QuarkusTest
class GenericMockTest {
    
    @BeforeEach  
    void setup() {
        @SuppressWarnings("unchecked")
        GenericRepository<User> mockRepo = Mockito.mock(GenericRepository.class);
        
        // Mock using TypeLiteral for generic types
        TypeLiteral<GenericRepository<User>> typeLiteral = 
            new TypeLiteral<GenericRepository<User>>() {};
            
        QuarkusMock.installMockForType(mockRepo, typeLiteral);
        
        Mockito.when(mockRepo.findAll())
               .thenReturn(List.of(new User("test")));
    }
}
```

### Mock Lifecycle Management

```java
@QuarkusTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MockLifecycleTest {
    
    private EmailService globalMock;
    
    @BeforeAll
    void setupGlobalMock() {
        // Global mocks persist across all tests
        globalMock = Mockito.mock(EmailService.class);
        QuarkusMock.installMockForType(globalMock, EmailService.class);
    }
    
    @BeforeEach
    void setupPerTestMock() {
        // Per-test mocks are cleared after each test
        NotificationService perTestMock = Mockito.mock(NotificationService.class);
        QuarkusMock.installMockForType(perTestMock, NotificationService.class);
    }
    
    @Test
    void testWithBothMocks() {
        // Both global and per-test mocks are active
    }
}
```

## Mock Configuration Patterns

### Stubbing and Verification

```java
@QuarkusTest
class PaymentServiceTest {
    
    @Inject
    PaymentService paymentService;
    
    @Inject
    AuditService auditService;
    
    @BeforeEach
    void setup() {
        AuditService mockAudit = Mockito.mock(AuditService.class);
        QuarkusMock.installMockForInstance(mockAudit, auditService);
        
        // Stub void methods
        Mockito.doNothing().when(mockAudit).logPayment(Mockito.any());
        
        // Stub methods with return values
        Mockito.when(mockAudit.isAuditEnabled()).thenReturn(true);
    }
    
    @Test
    void testPaymentWithAudit() {
        paymentService.processPayment(new Payment(100.0));
        
        // Verify interactions
        Mockito.verify(auditService).logPayment(Mockito.any(Payment.class));
        Mockito.verify(auditService, Mockito.times(1)).isAuditEnabled();
    }
}
```

### Exception Simulation

```java
@QuarkusTest
class ErrorHandlingTest {
    
    @BeforeEach
    void setup() {
        DatabaseService mockDb = Mockito.mock(DatabaseService.class);
        QuarkusMock.installMockForType(mockDb, DatabaseService.class);
        
        // Simulate database connection failure
        Mockito.when(mockDb.save(Mockito.any()))
               .thenThrow(new DatabaseException("Connection failed"));
    }
    
    @Test
    void testDatabaseErrorHandling() {
        assertThrows(ServiceException.class, () -> {
            userService.createUser("test@example.com");
        });
    }
}
```

### Complex Mock Scenarios

```java
@QuarkusTest
class ComplexMockingTest {
    
    @BeforeEach
    void setupComplexMocks() {
        // Mock with conditional responses
        ExternalApiService mockApi = Mockito.mock(ExternalApiService.class);
        
        Mockito.when(mockApi.getUserData("valid-id"))
               .thenReturn(new UserData("John Doe"));
        Mockito.when(mockApi.getUserData("invalid-id"))
               .thenThrow(new ApiException("User not found"));
        
        QuarkusMock.installMockForType(mockApi, ExternalApiService.class);
        
        // Mock with sequential responses
        CacheService mockCache = Mockito.mock(CacheService.class);
        Mockito.when(mockCache.get("key"))
               .thenReturn(null)          // First call: cache miss
               .thenReturn("cached-value"); // Second call: cache hit
               
        QuarkusMock.installMockForType(mockCache, CacheService.class);
    }
}
```

## Integration with Test Profiles

```java
// Test profile that provides mock configuration
public class MockedServicesProfile implements QuarkusTestProfile {
    
    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(MockEmailService.class, MockPaymentGateway.class);
    }
    
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "external.api.enabled", "false",
            "cache.enabled", "false"
        );
    }
}

@QuarkusTest
@TestProfile(MockedServicesProfile.class)
class IntegratedMockTest {
    
    @BeforeEach
    void setupRuntimeMocks() {
        // Combine profile alternatives with runtime mocks
        DatabaseService mockDb = Mockito.mock(DatabaseService.class);
        QuarkusMock.installMockForType(mockDb, DatabaseService.class);
    }
}
```

## Best Practices and Limitations

### Supported Bean Scopes
- ✅ `@ApplicationScoped` - Fully supported
- ✅ `@RequestScoped` - Fully supported  
- ❌ `@Singleton` - Not supported (not a normal scoped bean)
- ❌ `@Dependent` - Not supported (not a normal scoped bean)

### Thread Safety Considerations
```java
@QuarkusTest
class ThreadSafetyTest {
    
    @Test
    void testConcurrentAccess() {
        // Note: Mocks are global, so concurrent test execution
        // can cause race conditions. Avoid parallel execution
        // when using QuarkusMock.
    }
}
```

### Mock Cleanup
```java
@QuarkusTest
class MockCleanupTest {
    
    @Test
    void testOne() {
        // Install mock for this test
        Service mock = Mockito.mock(Service.class);
        QuarkusMock.installMockForType(mock, Service.class);
        
        // Mock is automatically cleared after test
    }
    
    @Test  
    void testTwo() {
        // Fresh application context without previous test's mock
    }
}
```

### Error Scenarios

```java
// This will throw RuntimeException
Service invalidMock = new Service() { /* implementation */ };
QuarkusMock.installMockForType(invalidMock, WrongType.class); 
// Error: invalidMock is not assignable to WrongType
```