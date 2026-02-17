# Main Method Testing

Quarkus JUnit 5 provides comprehensive support for testing command-line applications that use Quarkus main method execution.

## Main Method Test Annotations

### @QuarkusMainTest

Tests the main method within the same JVM as the test, creating a new in-memory Quarkus application that runs to completion.

```java { .api }
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(QuarkusMainTestExtension.class)
public @interface QuarkusMainTest {
}
```

### @QuarkusMainIntegrationTest

Tests the main method against built artifacts (JAR, native image, or container), running in a separate process.

```java { .api }
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(QuarkusMainIntegrationTestExtension.class)
public @interface QuarkusMainIntegrationTest {
}
```

## Launch Annotations and Interfaces

### @Launch Annotation

Annotation for launching command-line applications with specified arguments and expected exit codes.

```java { .api }
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Launch {
    /**
     * The program arguments to launch with
     */
    String[] value() default "";
    
    /**
     * Expected return code
     */
    int exitCode() default 0;
}
```

### QuarkusMainLauncher Interface

Interface for programmatically launching command-line applications with arbitrary parameters.

```java { .api }
public interface QuarkusMainLauncher {
    /**
     * Launch the command line application with the given parameters.
     */
    LaunchResult launch(String... args);
}
```

### LaunchResult Interface

Contains information about a command-line application execution run.

```java { .api }
public interface LaunchResult {
    /**
     * Get the command line application standard output as a single string.
     */
    default String getOutput() {
        return String.join("\n", getOutputStream());
    }
    
    /**
     * Get the command line application error output as a single string.
     */
    default String getErrorOutput() {
        return String.join("\n", getErrorStream());
    }
    
    /**
     * Echo the command line application standard output to the console.
     */
    default void echoSystemOut() {
        System.out.println(getOutput());
        System.out.println();
    }
    
    /**
     * Get the command line application standard output as a list of strings.
     * Each line of output correspond to a string in the list.
     */
    List<String> getOutputStream();
    
    /**
     * Get the command line application error output as a list of strings.
     * Each line of output correspond to a string in the list.
     */
    List<String> getErrorStream();
    
    /**
     * Get the exit code of the application.
     */
    int exitCode();
}
```

## Basic Usage Examples

### Simple Main Method Test

```java
import io.quarkus.test.junit.main.QuarkusMainTest;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusMainTest
class CalculatorMainTest {
    
    @Test
    @Launch({"add", "5", "3"})
    void testAddCommand(LaunchResult result) {
        assertEquals(0, result.exitCode());
        assertTrue(result.getOutput().contains("Result: 8"));
    }
    
    @Test
    @Launch(value = {"divide", "10", "0"}, exitCode = 1)
    void testDivisionByZero(LaunchResult result) {
        assertEquals(1, result.exitCode());
        assertTrue(result.getErrorOutput().contains("Division by zero"));
    }
    
    @Test
    @Launch({"--help"})
    void testHelpOutput(LaunchResult result) {
        assertEquals(0, result.exitCode());
        String output = result.getOutput();
        assertTrue(output.contains("Usage:"));
        assertTrue(output.contains("Commands:"));
    }
}
```

### Programmatic Launch Testing

```java
import io.quarkus.test.junit.main.QuarkusMainTest;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.Test;

@QuarkusMainTest
class FileProcessorMainTest {
    
    @Test
    void testFileProcessing(QuarkusMainLauncher launcher) {
        // Test successful file processing
        LaunchResult result = launcher.launch("process", "/tmp/input.txt", "/tmp/output.txt");
        assertEquals(0, result.exitCode());
        assertTrue(result.getOutput().contains("Processing completed"));
        
        // Test missing file
        LaunchResult errorResult = launcher.launch("process", "/nonexistent/file.txt");
        assertEquals(2, errorResult.exitCode());
        assertTrue(errorResult.getErrorOutput().contains("File not found"));
    }
    
    @Test
    void testValidationMode(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch("validate", "--strict", "/tmp/data.xml");
        
        if (result.exitCode() == 0) {
            assertTrue(result.getOutput().contains("Validation passed"));
        } else {
            assertTrue(result.getErrorOutput().contains("Validation failed"));
        }
    }
}
```

### Integration Testing

```java
import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.Test;

@QuarkusMainIntegrationTest
class DatabaseMigratorIT {
    
    @Test
    @Launch({"migrate", "--url", "jdbc:h2:mem:test"})
    void testDatabaseMigration(LaunchResult result) {
        assertEquals(0, result.exitCode());
        
        List<String> outputLines = result.getOutputStream();
        assertTrue(outputLines.stream().anyMatch(line -> 
            line.contains("Migration completed successfully")));
    }
    
    @Test
    @Launch(value = {"rollback", "--version", "invalid"}, exitCode = 1)
    void testInvalidRollback(LaunchResult result) {
        assertEquals(1, result.exitCode());
        assertTrue(result.getErrorOutput().contains("Invalid version"));
    }
}
```

## Advanced Testing Patterns

### Configuration-Dependent Testing

```java
@QuarkusMainTest
class ConfigurableAppTest {
    
    @Test
    void testWithDifferentConfigs(QuarkusMainLauncher launcher) {
        // Test with development config
        LaunchResult devResult = launcher.launch("--profile", "dev", "start");
        assertEquals(0, devResult.exitCode());
        
        // Test with production config  
        LaunchResult prodResult = launcher.launch("--profile", "prod", "start");
        assertEquals(0, prodResult.exitCode());
        
        // Verify different behaviors
        assertNotEquals(devResult.getOutput(), prodResult.getOutput());
    }
}
```

### Multi-Command Application Testing

```java
@QuarkusMainTest
class CliToolTest {
    
    @Test
    void testAllCommands(QuarkusMainLauncher launcher) {
        // Test create command
        LaunchResult createResult = launcher.launch("create", "project", "my-app");
        assertEquals(0, createResult.exitCode());
        assertTrue(createResult.getOutput().contains("Project created"));
        
        // Test list command
        LaunchResult listResult = launcher.launch("list", "projects");
        assertEquals(0, listResult.exitCode());
        assertTrue(listResult.getOutput().contains("my-app"));
        
        // Test delete command
        LaunchResult deleteResult = launcher.launch("delete", "project", "my-app");
        assertEquals(0, deleteResult.exitCode());
        assertTrue(deleteResult.getOutput().contains("Project deleted"));
    }
}
```

### Output Parsing and Validation

```java
@QuarkusMainTest
class DataAnalyzerTest {
    
    @Test
    @Launch({"analyze", "--format", "json", "/tmp/data.csv"})
    void testJsonOutput(LaunchResult result) {
        assertEquals(0, result.exitCode());
        
        String jsonOutput = result.getOutput();
        
        // Parse and validate JSON structure
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(jsonOutput);
        
        assertTrue(json.has("summary"));
        assertTrue(json.has("results"));
        assertTrue(json.get("results").isArray());
    }
    
    @Test
    @Launch({"analyze", "--format", "csv", "/tmp/data.csv"})
    void testCsvOutput(LaunchResult result) {
        assertEquals(0, result.exitCode());
        
        List<String> lines = result.getOutputStream();
        
        // Validate CSV format
        assertTrue(lines.get(0).contains("column1,column2,column3")); // Header
        assertTrue(lines.size() > 1); // Has data rows
        
        // Validate each data row
        lines.stream().skip(1).forEach(line -> {
            String[] columns = line.split(",");
            assertEquals(3, columns.length);
        });
    }
}
```

### Error Handling and Exit Codes

```java
@QuarkusMainTest
class ErrorHandlingTest {
    
    @Test
    void testErrorConditions(QuarkusMainLauncher launcher) {
        // Test invalid command
        LaunchResult invalidCmd = launcher.launch("invalid-command");
        assertEquals(1, invalidCmd.exitCode());
        assertTrue(invalidCmd.getErrorOutput().contains("Unknown command"));
        
        // Test missing required argument
        LaunchResult missingArg = launcher.launch("process");
        assertEquals(2, missingArg.exitCode());
        assertTrue(missingArg.getErrorOutput().contains("Missing required"));
        
        // Test invalid argument value
        LaunchResult invalidArg = launcher.launch("process", "--threads", "invalid");
        assertEquals(3, invalidArg.exitCode());
        assertTrue(invalidArg.getErrorOutput().contains("Invalid number"));
    }
}
```

## Testing Best Practices

### Test Organization

```java
// Base test class for shared test logic
@QuarkusMainTest
class BaseMainTest {
    
    protected void assertSuccessfulExecution(LaunchResult result) {
        assertEquals(0, result.exitCode());
        assertFalse(result.getOutput().trim().isEmpty());
    }
    
    protected void assertErrorExecution(LaunchResult result, String expectedError) {
        assertNotEquals(0, result.exitCode());
        assertTrue(result.getErrorOutput().contains(expectedError));
    }
}

// Integration test extends base test
@QuarkusMainIntegrationTest
class MainIntegrationTest extends BaseMainTest {
    // Same test methods run against built artifact
}
```

### Resource Management

```java
@QuarkusMainTest
class ResourceManagedTest {
    
    private Path tempDir;
    
    @BeforeEach
    void setupTempDirectory() throws IOException {
        tempDir = Files.createTempDirectory("test");
    }
    
    @AfterEach
    void cleanupTempDirectory() throws IOException {
        Files.walk(tempDir)
             .sorted(Comparator.reverseOrder())
             .map(Path::toFile)
             .forEach(File::delete);
    }
    
    @Test
    void testFileOperation(QuarkusMainLauncher launcher) {
        Path inputFile = tempDir.resolve("input.txt");
        Files.write(inputFile, "test data".getBytes());
        
        LaunchResult result = launcher.launch("process", inputFile.toString());
        assertEquals(0, result.exitCode());
    }
}
```

### Performance Testing

```java
@QuarkusMainTest
class PerformanceTest {
    
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testPerformanceRequirement(QuarkusMainLauncher launcher) {
        long startTime = System.currentTimeMillis();
        
        LaunchResult result = launcher.launch("heavy-operation", "--size", "1000");
        assertEquals(0, result.exitCode());
        
        long duration = System.currentTimeMillis() - startTime;
        assertTrue(duration < 30000, "Operation took too long: " + duration + "ms");
    }
}
```

## Limitations and Considerations

### CDI Injection Limitation
```java
@QuarkusMainTest
class MainTestLimitations {
    
    // ❌ Not supported in main method tests
    // @Inject
    // SomeService service;
    
    @Test
    void testWithoutInjection(QuarkusMainLauncher launcher) {
        // Must test through main method execution only
        LaunchResult result = launcher.launch("command");
        assertEquals(0, result.exitCode());
    }
}
```

### Application Lifecycle
- Each test method starts a new application instance
- Application runs to completion before test method continues
- Cannot test long-running applications directly (use integration tests instead)

### Native Image Testing
```java
// Works with both JVM and native image builds
@QuarkusMainIntegrationTest
class NativeCompatibleTest {
    
    @Test
    @Launch({"quick-command"})
    void testNativeCompatibility(LaunchResult result) {
        // Same test works for both JVM and native image
        assertEquals(0, result.exitCode());
    }
}
```