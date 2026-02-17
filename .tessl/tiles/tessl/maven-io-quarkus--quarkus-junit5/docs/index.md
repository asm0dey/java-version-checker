# Quarkus JUnit 5

Quarkus JUnit 5 is a comprehensive testing extension that provides full JUnit 5 integration for Quarkus applications. It enables developers to write unit tests, integration tests, and native image tests with complete CDI support, dependency injection, mocking capabilities, and application lifecycle management. The extension supports multiple testing modes including in-JVM testing, external artifact testing, and command-line application testing.

## Package Information

- **Package Name**: quarkus-junit5
- **Package Type**: maven
- **Group ID**: io.quarkus
- **Artifact ID**: quarkus-junit5
- **Language**: Java
- **Installation**: Add to Maven dependencies:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-junit5</artifactId>
    <scope>test</scope>
</dependency>
```

For Gradle:

```gradle
testImplementation 'io.quarkus:quarkus-junit5'
```

## Core Imports

```java
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.QuarkusTestProfile;
```

For main method testing:

```java
import io.quarkus.test.junit.main.QuarkusMainTest;
import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.LaunchResult;
```

For conditional testing:

```java
import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.DisableIfBuiltWithGraalVMNewerThan;
import io.quarkus.test.junit.DisableIfBuiltWithGraalVMOlderThan;
import io.quarkus.test.junit.GraalVMVersion;
```

For test callbacks:

```java
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestAfterAllCallback;
import io.quarkus.test.junit.callback.QuarkusTestContext;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
```

## Basic Usage

### Unit Testing with CDI Injection

```java
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class UserServiceTest {
    
    @Inject
    UserService userService;
    
    @Test
    void testCreateUser() {
        User user = userService.createUser("John", "john@example.com");
        assertEquals("John", user.getName());
    }
}
```

### Integration Testing

```java
import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
class UserServiceIntegrationTest {
    
    @Test
    void testUserEndpoint() {
        // Test against built JAR/native image/container
        given()
            .when().get("/users")
            .then()
            .statusCode(200);
    }
}
```

### Mocking CDI Beans

```java
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest 
class UserServiceWithMockTest {
    
    @Inject
    UserService userService;
    
    @Inject 
    UserRepository userRepository;
    
    @BeforeEach
    void setup() {
        UserRepository mockRepo = Mockito.mock(UserRepository.class);
        QuarkusMock.installMockForInstance(mockRepo, userRepository);
    }
    
    @Test
    void testWithMock() {
        // Test with mocked repository
    }
}
```

## Architecture

Quarkus JUnit 5 is built around several key components:

- **Test Extensions**: JUnit 5 extensions that manage Quarkus application lifecycle during tests
- **Annotation System**: Declarative annotations for different testing modes and configurations
- **CDI Integration**: Full dependency injection support in test classes
- **Mock Support**: Runtime bean replacement for testing isolation
- **Profile System**: Test-specific configuration and bean management
- **Launcher Providers**: Support for testing different artifact types (JAR, native, container)
- **Callback System**: Service provider interfaces for test lifecycle customization

## Capabilities

### Core Test Annotations

Primary annotations for different testing modes including unit tests with CDI injection, integration tests against built artifacts, and main method testing.

```java { .api }
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(QuarkusTestExtension.class)
public @interface QuarkusTest {
}

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith({DisabledOnIntegrationTestCondition.class, QuarkusIntegrationTestExtension.class})
public @interface QuarkusIntegrationTest {
    @Deprecated
    interface Context extends DevServicesContext {
    }
}
```

[Core Testing](./core-testing.md)

### Test Configuration and Profiles

Test profile system for managing different configurations, CDI alternatives, and test resources across different test scenarios.

```java { .api }
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TestProfile {
    Class<? extends QuarkusTestProfile> value();
}

public interface QuarkusTestProfile {
    default Map<String, String> getConfigOverrides() { return Collections.emptyMap(); }
    default Set<Class<?>> getEnabledAlternatives() { return Collections.emptySet(); }
    default String getConfigProfile() { return null; }
    default List<TestResourceEntry> testResources() { return Collections.emptyList(); }
    default boolean disableGlobalTestResources() { return false; }
    default Set<String> tags() { return Collections.emptySet(); }
    default String[] commandLineParameters() { return new String[0]; }
    default boolean runMainMethod() { return false; }
    default boolean disableApplicationLifecycleObservers() { return false; }
}
```

[Test Profiles](./test-profiles.md)

### Mocking and Bean Replacement

Runtime mock installation for CDI normal scoped beans, supporting both instance-based and type-based mocking with qualifier support.

```java { .api }
public class QuarkusMock {
    public static <T> void installMockForInstance(T mock, T instance);
    public static <T> void installMockForType(T mock, Class<? super T> instance, Annotation... qualifiers);
    public static <T> void installMockForType(T mock, TypeLiteral<? super T> typeLiteral, Annotation... qualifiers);
}
```

[Mocking Support](./mocking.md)

### Main Method Testing

Testing framework for command-line applications with support for parameter passing, exit code validation, and output capture.

```java { .api }
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(QuarkusMainTestExtension.class)
public @interface QuarkusMainTest {
}

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(QuarkusMainIntegrationTestExtension.class)
public @interface QuarkusMainIntegrationTest {
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Launch {
    String[] value() default "";
    int exitCode() default 0;
}

public interface QuarkusMainLauncher {
    LaunchResult launch(String... args);
}
```

[Main Method Testing](./main-method-testing.md)

### Conditional Test Execution

Annotations for conditionally enabling or disabling tests based on integration test mode, GraalVM versions, and artifact types.

```java { .api }
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DisabledOnIntegrationTest {
    String value() default "";
    ArtifactType[] forArtifactTypes() default {ArtifactType.ALL};
    
    enum ArtifactType {
        ALL, JAR, CONTAINER, NATIVE_BINARY
    }
}

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisableIfBuiltWithGraalVMNewerThanCondition.class)
public @interface DisableIfBuiltWithGraalVMNewerThan {
    GraalVMVersion value();
}

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisableIfBuiltWithGraalVMOlderThanCondition.class)
public @interface DisableIfBuiltWithGraalVMOlderThan {
    GraalVMVersion value();
}
```

[Conditional Testing](./conditional-testing.md)

### Test Lifecycle Callbacks

Service provider interfaces for customizing test execution lifecycle with callbacks for various test phases including construction, setup, execution, and teardown.

```java { .api }
public interface QuarkusTestBeforeEachCallback {
    void beforeEach(QuarkusTestMethodContext context);
}

public interface QuarkusTestAfterEachCallback {
    void afterEach(QuarkusTestMethodContext context);
}

public interface QuarkusTestAfterAllCallback {
    void afterAll(QuarkusTestContext context);
}

public interface QuarkusTestBeforeClassCallback {
    void beforeClass(Class<?> testClass);
}

public interface QuarkusTestAfterConstructCallback {
    void afterConstruct(Object testInstance);
}

public interface QuarkusTestBeforeTestExecutionCallback {
    void beforeTestExecution(QuarkusTestMethodContext context);
}

public interface QuarkusTestAfterTestExecutionCallback {
    void afterTestExecution(QuarkusTestMethodContext context);
}
```

[Test Callbacks](./test-callbacks.md)

## Types

### Core Test Context Types

```java { .api }
// Package: io.quarkus.test.junit.callback
public class QuarkusTestContext {
    public Object getTestInstance();
    public List<Object> getOuterInstances();
    public TestStatus getTestStatus();
}

public final class QuarkusTestMethodContext extends QuarkusTestContext {
    public Method getTestMethod();
}
```

### Launch Result Types

```java { .api }
public interface LaunchResult {
    default String getOutput() { return String.join("\n", getOutputStream()); }
    default String getErrorOutput() { return String.join("\n", getErrorStream()); }
    default void echoSystemOut() { System.out.println(getOutput()); }
    List<String> getOutputStream();
    List<String> getErrorStream();
    int exitCode();
}
```

### Test Resource Configuration

```java { .api }
// Inner class of QuarkusTestProfile
public static final class TestResourceEntry {
    public TestResourceEntry(Class<? extends QuarkusTestResourceLifecycleManager> clazz);
    public TestResourceEntry(Class<? extends QuarkusTestResourceLifecycleManager> clazz, Map<String, String> args);
    public TestResourceEntry(Class<? extends QuarkusTestResourceLifecycleManager> clazz, Map<String, String> args, boolean parallel);
    
    public Class<? extends QuarkusTestResourceLifecycleManager> getClazz();
    public Map<String, String> getArgs();
    public boolean isParallel();
}
```

### GraalVM Version Enumeration

```java { .api }
public enum GraalVMVersion {
    GRAALVM_23_1_0, GRAALVM_24_0_0, GRAALVM_24_0_999, 
    GRAALVM_24_1_0, GRAALVM_24_1_999, GRAALVM_24_2_0;
    
    public GraalVM.Version getVersion();
    public String toString();
}
```