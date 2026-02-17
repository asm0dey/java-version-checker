# Conditional Testing

Quarkus JUnit 5 provides annotations for conditionally enabling or disabling tests based on various criteria including integration test mode, GraalVM versions, and artifact types.

## Integration Test Conditional Execution

### @DisabledOnIntegrationTest

Disables tests when running as `@QuarkusIntegrationTest`, useful for tests that require CDI injection or only make sense in unit test context.

```java { .api }
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DisabledOnIntegrationTest {
    /**
     * Reason for disabling this test
     */
    String value() default "";
    
    /**
     * The types of Quarkus application produced by the build for which this test is disabled.
     */
    ArtifactType[] forArtifactTypes() default {ArtifactType.ALL};
    
    enum ArtifactType {
        ALL, JAR, CONTAINER, NATIVE_BINARY
    }
}
```

### Usage Examples

#### Class-Level Disabling

```java
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.DisabledOnIntegrationTest;
import jakarta.inject.Inject;

@QuarkusTest
@DisabledOnIntegrationTest("CDI injection not available in integration tests")
class UnitTestOnlyTest {
    
    @Inject
    UserService userService; // Only works in @QuarkusTest
    
    @Test
    void testCdiInjection() {
        assertNotNull(userService);
        User user = userService.createUser("test@example.com");
        assertNotNull(user.getId());
    }
}

// Integration test without CDI-dependent tests
@QuarkusIntegrationTest  
class UserServiceIT extends UnitTestOnlyTest {
    // Inherits tests but @DisabledOnIntegrationTest tests are skipped
    
    @Test
    void testHttpEndpoint() {
        given()
            .when().get("/api/users")
            .then()
            .statusCode(200);
    }
}
```

#### Method-Level Disabling

```java
@QuarkusTest
class MixedTestModes {
    
    @Test
    @DisabledOnIntegrationTest("Requires direct database access")
    void testDatabaseDirectly() {
        // Test that directly accesses database - not available in integration tests
    }
    
    @Test
    void testHttpApi() {
        // Test that works in both unit and integration tests
        given()
            .when().get("/api/health")
            .then()
            .statusCode(200);
    }
}
```

#### Artifact-Specific Disabling

```java
@QuarkusTest
class ArtifactSpecificTest {
    
    @Test
    @DisabledOnIntegrationTest(
        value = "Container has different network setup",
        forArtifactTypes = {DisabledOnIntegrationTest.ArtifactType.CONTAINER}
    )
    void testNetworkConfiguration() {
        // Disabled only when running against container, enabled for JAR and native
    }
    
    @Test
    @DisabledOnIntegrationTest(
        value = "Native image specific test",
        forArtifactTypes = {
            DisabledOnIntegrationTest.ArtifactType.JAR,
            DisabledOnIntegrationTest.ArtifactType.CONTAINER
        }
    )
    void testNativeImageOptimization() {
        // Only runs against native binary
    }
}
```

## GraalVM Version Conditional Execution

### @DisableIfBuiltWithGraalVMNewerThan

Disables tests if the GraalVM version used to build the native image is newer than the specified version.

```java { .api }
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisableIfBuiltWithGraalVMNewerThanCondition.class)
public @interface DisableIfBuiltWithGraalVMNewerThan {
    GraalVMVersion value();
}
```

### @DisableIfBuiltWithGraalVMOlderThan

Disables tests if the GraalVM version used to build the native image is older than the specified version.

```java { .api }
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisableIfBuiltWithGraalVMOlderThanCondition.class)
public @interface DisableIfBuiltWithGraalVMOlderThan {
    GraalVMVersion value();
}
```

### GraalVMVersion Enum

```java { .api }
public enum GraalVMVersion {
    GRAALVM_23_1_0(GraalVM.Version.VERSION_23_1_0),
    GRAALVM_24_0_0(GraalVM.Version.VERSION_24_0_0),
    GRAALVM_24_0_999(GraalVM.Version.VERSION_24_0_999),
    GRAALVM_24_1_0(GraalVM.Version.VERSION_24_1_0),
    GRAALVM_24_1_999(GraalVM.Version.VERSION_24_1_999),
    GRAALVM_24_2_0(GraalVM.Version.VERSION_24_2_0);
    
    public GraalVM.Version getVersion();
    public String toString();
}
```

### GraalVM Version Usage Examples

#### Feature Compatibility Testing

```java
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.DisableIfBuiltWithGraalVMOlderThan;
import io.quarkus.test.junit.DisableIfBuiltWithGraalVMNewerThan;
import io.quarkus.test.junit.GraalVMVersion;

@QuarkusIntegrationTest
class GraalVMVersionTest {
    
    @Test
    @DisableIfBuiltWithGraalVMOlderThan(GraalVMVersion.GRAALVM_24_0_0)
    void testNewGraalVMFeature() {
        // Test feature that requires GraalVM 24.0.0 or newer
        given()
            .when().get("/api/advanced-feature")
            .then()
            .statusCode(200);
    }
    
    @Test
    @DisableIfBuiltWithGraalVMNewerThan(GraalVMVersion.GRAALVM_24_1_999)
    void testLegacyBehavior() {
        // Test behavior that changed in newer GraalVM versions
        given()
            .when().get("/api/legacy-endpoint")
            .then()
            .statusCode(200)
            .body("format", equalTo("legacy"));
    }
}
```

#### Class-Level Version Requirements

```java
@QuarkusIntegrationTest
@DisableIfBuiltWithGraalVMOlderThan(GraalVMVersion.GRAALVM_24_1_0)
class ModernGraalVMTest {
    
    @Test
    void testModernFeatureOne() {
        // All tests in this class require GraalVM 24.1.0+
    }
    
    @Test
    void testModernFeatureTwo() {
        // This test also requires GraalVM 24.1.0+
    }
}
```

#### Version Range Testing

```java
@QuarkusIntegrationTest
class VersionRangeTest {
    
    @Test
    @DisableIfBuiltWithGraalVMOlderThan(GraalVMVersion.GRAALVM_24_0_0)
    @DisableIfBuiltWithGraalVMNewerThan(GraalVMVersion.GRAALVM_24_1_999)
    void testSpecificVersionRange() {
        // Test only runs with GraalVM 24.0.0 through 24.1.999
        given()
            .when().get("/api/version-specific")
            .then()
            .statusCode(200);
    }
}
```

## Complex Conditional Scenarios

### Combining Multiple Conditions

```java
@QuarkusTest
class ComplexConditionalTest {
    
    @Test
    @DisabledOnIntegrationTest("Requires CDI injection")
    @EnabledIf("#{systemProperties['test.mode'] == 'full'}")
    void testComplexScenario() {
        // Only runs in unit tests AND when test.mode=full
    }
    
    @Test
    @DisabledOnIntegrationTest(
        value = "Container networking differs",
        forArtifactTypes = {DisabledOnIntegrationTest.ArtifactType.CONTAINER}
    )
    @DisableIfBuiltWithGraalVMOlderThan(GraalVMVersion.GRAALVM_24_0_0)
    void testModernNativeFeature() {
        // Disabled on containers and old GraalVM versions
    }
}
```

### Environment-Specific Testing

```java
@QuarkusIntegrationTest
class EnvironmentConditionalTest {
    
    @Test
    @DisabledOnIntegrationTest(
        forArtifactTypes = {DisabledOnIntegrationTest.ArtifactType.NATIVE_BINARY}
    )
    @EnabledIf("#{environment['CI'] == null}")
    void testLocalDevelopmentOnly() {
        // Only runs locally, not in CI, and not against native images
    }
    
    @Test
    @DisableIfBuiltWithGraalVMOlderThan(GraalVMVersion.GRAALVM_24_1_0)
    @EnabledIf("#{systemProperties['quarkus.native.enabled'] == 'true'}")
    void testNativeImageWithModernGraalVM() {
        // Only runs for native builds with GraalVM 24.1.0+
    }
}
```

## Testing Pattern Examples

### Base Test Class Pattern

```java
// Base test class with unit tests
@QuarkusTest
class BaseServiceTest {
    
    @Test
    @DisabledOnIntegrationTest("Requires CDI injection")
    void testServiceDependencyInjection() {
        // Unit test specific logic
    }
    
    @Test
    void testServiceHttpEndpoint() {
        // Works in both unit and integration tests
        given()
            .when().get("/api/service")
            .then()
            .statusCode(200);
    }
}

// Integration test inherits base tests
@QuarkusIntegrationTest
class ServiceIntegrationTest extends BaseServiceTest {
    // CDI-dependent tests are automatically skipped
    
    @Test
    @DisableIfBuiltWithGraalVMOlderThan(GraalVMVersion.GRAALVM_24_0_0)
    void testNativeSpecificBehavior() {
        // Integration test specific to newer GraalVM
    }
}
```

### Platform-Specific Testing

```java
@QuarkusIntegrationTest
class PlatformSpecificTest {
    
    @Test
    @DisabledOnIntegrationTest(
        value = "JVM heap analysis not available in containers",
        forArtifactTypes = {DisabledOnIntegrationTest.ArtifactType.CONTAINER}
    )
    void testMemoryUsage() {
        // Memory analysis test
    }
    
    @Test
    @DisabledOnIntegrationTest(
        forArtifactTypes = {
            DisabledOnIntegrationTest.ArtifactType.JAR,
            DisabledOnIntegrationTest.ArtifactType.NATIVE_BINARY
        }
    )
    void testContainerSpecificFeature() {
        // Test that only runs in container environment
    }
}
```

### Version Migration Testing

```java
@QuarkusIntegrationTest
class MigrationTest {
    
    @Test
    @DisableIfBuiltWithGraalVMNewerThan(GraalVMVersion.GRAALVM_23_1_0)
    void testLegacyCompatibility() {
        // Test legacy behavior for older versions
        given()
            .when().get("/api/legacy")
            .then()
            .statusCode(200)
            .body("version", equalTo("legacy"));
    }
    
    @Test
    @DisableIfBuiltWithGraalVMOlderThan(GraalVMVersion.GRAALVM_24_0_0)
    void testNewApiFeatures() {
        // Test new features available in modern versions
        given()
            .when().get("/api/modern")
            .then()
            .statusCode(200)
            .body("features", hasItems("new-feature-1", "new-feature-2"));
    }
}
```

## Best Practices

### Descriptive Disable Reasons

```java
@Test
@DisabledOnIntegrationTest("Test requires @Inject UserService which is not available in integration tests")
void testWithGoodReason() {
    // Clear explanation helps maintainers understand why test is disabled
}

@Test
@DisableIfBuiltWithGraalVMOlderThan(GraalVMVersion.GRAALVM_24_1_0)
void testRequiringNewGraalVM() {
    // Version requirements are self-documenting
}
```

### Minimal Disabling

```java
@QuarkusTest
class MinimalDisablingTest {
    
    @Inject
    UserService userService;
    
    @Test
    void testBusinessLogic() {
        // Test that works in both modes - no disabling needed
        User user = userService.createUser("test@example.com");
        assertNotNull(user);
    }
    
    @Test
    @DisabledOnIntegrationTest("Requires CDI injection")
    void testCdiSpecificBehavior() {
        // Only disable when absolutely necessary
        assertNotNull(userService);
    }
}
```

### Documentation and Comments

```java
@QuarkusIntegrationTest
class WellDocumentedTest {
    
    /**
     * This test verifies container-specific networking behavior.
     * It's disabled for JAR and native runs because they use
     * different network configurations.
     */
    @Test
    @DisabledOnIntegrationTest(
        value = "Container networking configuration differs from JAR/native",
        forArtifactTypes = {
            DisabledOnIntegrationTest.ArtifactType.JAR,
            DisabledOnIntegrationTest.ArtifactType.NATIVE_BINARY
        }
    )
    void testContainerNetworking() {
        // Test implementation
    }
}
```