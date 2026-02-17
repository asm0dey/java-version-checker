# Build-Time SPI

Quarkus REST provides extensive build-time Service Provider Interface (SPI) extension points for framework developers and extension authors. These build items allow customization of the REST framework during build-time processing.

## Overview

Build items are used during Quarkus build-time processing to customize REST framework behavior, register handlers, modify annotations, and extend functionality. These are advanced APIs primarily for extension developers.

**Package**: `io.quarkus.resteasy.reactive.server.spi`

## MethodScannerBuildItem

Register custom method scanners to analyze and process REST resource methods during build time.

```java { .api }
package io.quarkus.resteasy.reactive.server.spi;

class MethodScannerBuildItem extends MultiBuildItem {
    MethodScannerBuildItem(MethodScanner methodScanner);
    MethodScanner getMethodScanner();
}
```

**Purpose**: Allows extensions to scan and process resource methods with custom logic.

**Usage**:

```java
@BuildStep
MethodScannerBuildItem registerCustomScanner() {
    return new MethodScannerBuildItem(new CustomMethodScanner());
}

class CustomMethodScanner implements MethodScanner {
    @Override
    public List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass, Map<String, Object> methodContext) {
        // Analyze method and return customizers
        if (method.hasAnnotation("com.example.CustomAnnotation")) {
            return List.of(new CustomHandlerChainCustomizer());
        }
        return Collections.emptyList();
    }
}
```

## ContextTypeBuildItem

Register additional types that can be injected via `@Context` annotation in REST resource methods.

```java { .api }
package io.quarkus.resteasy.reactive.server.spi;

class ContextTypeBuildItem extends MultiBuildItem {
    ContextTypeBuildItem(DotName className);
    DotName getType();
}
```

**Purpose**: Extends the set of types available for `@Context` injection beyond standard JAX-RS types.

**Usage**:

```java
@BuildStep
ContextTypeBuildItem registerCustomContextType() {
    // Register custom type for @Context injection
    return new ContextTypeBuildItem(
        DotName.createSimple("com.example.CustomContext")
    );
}

// Usage in resource:
@Path("/api")
public class MyResource {
    @GET
    public Response get(@Context CustomContext customContext) {
        // customContext is injected automatically
        return Response.ok().build();
    }
}
```

## AnnotationsTransformerBuildItem

Transform annotations on classes, methods, and fields during build time.

```java { .api }
package io.quarkus.resteasy.reactive.server.spi;

class AnnotationsTransformerBuildItem extends MultiBuildItem {
    @Deprecated
    AnnotationsTransformerBuildItem(AnnotationsTransformer transformer);

    AnnotationsTransformerBuildItem(AnnotationTransformation transformation);

    @Deprecated
    AnnotationsTransformer getAnnotationsTransformer();

    AnnotationTransformation getAnnotationTransformation();
}
```

**Purpose**: Modify or add annotations programmatically during build time.

**Usage**:

```java
@BuildStep
AnnotationsTransformerBuildItem addSecurityAnnotations() {
    return new AnnotationsTransformerBuildItem(
        AnnotationTransformation.builder()
            .whenMethod(m -> m.hasAnnotation(MY_ANNOTATION))
            .transform(ctx -> {
                // Add @RolesAllowed("admin") to methods with custom annotation
                ctx.transform().add(RolesAllowed.class, "admin").done();
            })
    );
}
```

## HandlerConfigurationProviderBuildItem

Provide runtime configuration for handlers that need configuration data.

```java { .api }
package io.quarkus.resteasy.reactive.server.spi;

class HandlerConfigurationProviderBuildItem extends MultiBuildItem {
    HandlerConfigurationProviderBuildItem(Class configClass, Supplier valueSupplier);

    Class getConfigClass();
    Supplier getValueSupplier();
}
```

**Purpose**: Supply configuration to `GenericRuntimeConfigurableServerRestHandler` implementations.

**Usage**:

```java
@BuildStep
HandlerConfigurationProviderBuildItem provideHandlerConfig() {
    return new HandlerConfigurationProviderBuildItem(
        MyHandlerConfig.class,
        () -> new MyHandlerConfig(someValue)
    );
}
```

## PreExceptionMapperHandlerBuildItem

Register handlers that run before standard JAX-RS exception mapping.

```java { .api }
package io.quarkus.resteasy.reactive.server.spi;

class PreExceptionMapperHandlerBuildItem extends MultiBuildItem
    implements Comparable<PreExceptionMapperHandlerBuildItem> {

    PreExceptionMapperHandlerBuildItem(ServerRestHandler handler);
    PreExceptionMapperHandlerBuildItem(ServerRestHandler handler, int priority);

    ServerRestHandler getHandler();
    int getPriority();  // Default: Priorities.USER
    int compareTo(PreExceptionMapperHandlerBuildItem o);
}
```

**Purpose**: Intercept exceptions before they reach standard exception mappers.

**Usage**:

```java
@BuildStep
PreExceptionMapperHandlerBuildItem registerPreExceptionHandler() {
    return new PreExceptionMapperHandlerBuildItem(
        new CustomPreExceptionHandler(),
        Priorities.USER - 100  // Run before default handlers
    );
}

class CustomPreExceptionHandler implements ServerRestHandler {
    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) {
        Throwable throwable = requestContext.getThrowable();
        if (throwable instanceof SpecialException) {
            // Handle specially
            requestContext.setResult(Response.status(400).build());
        }
    }
}
```

## ResumeOn404BuildItem

Force RESTEasy Reactive to pass control to the next handler when returning 404 instead of immediately replying.

```java { .api }
package io.quarkus.resteasy.reactive.server.spi;

class ResumeOn404BuildItem extends MultiBuildItem {
    // Marker build item - presence indicates behavior
}
```

**Purpose**: Allows fallback to other handlers (e.g., static resources) when no REST endpoint matches.

**Usage**:

```java
@BuildStep
ResumeOn404BuildItem enableFallbackOn404() {
    // Return build item to enable 404 resume behavior
    return new ResumeOn404BuildItem();
}
```

## SubResourcesAsBeansBuildItem

Make JAX-RS sub-resources available as CDI beans for dependency injection.

```java { .api }
package io.quarkus.resteasy.reactive.server.spi;

class SubResourcesAsBeansBuildItem extends MultiBuildItem {
    // Marker build item - presence indicates behavior
}
```

**Purpose**: Enables CDI injection in sub-resource classes.

**Usage**:

```java
@BuildStep
SubResourcesAsBeansBuildItem enableSubResourceBeans() {
    // Return build item to make sub-resources CDI beans
    return new SubResourcesAsBeansBuildItem();
}

// Sub-resource can now use @Inject
public class UserSubResource {
    @Inject
    UserService userService;  // Works because of SubResourcesAsBeansBuildItem

    @GET
    @Path("/{id}")
    public User getUser(@PathParam("id") Long id) {
        return userService.findById(id);
    }
}
```

## UnwrappedExceptionBuildItem

Register exception types that should be unwrapped to find the cause for exception mapping.

```java { .api }
package io.quarkus.resteasy.reactive.server.spi;

class UnwrappedExceptionBuildItem extends MultiBuildItem {
    UnwrappedExceptionBuildItem(String throwableClassName);
    UnwrappedExceptionBuildItem(Class<? extends Throwable> throwableClass);

    @Deprecated
    Class<? extends Throwable> getThrowableClass();

    String getThrowableClassName();
}
```

**Purpose**: Unwrap wrapper exceptions to map the underlying cause.

**Usage**:

```java
@BuildStep
UnwrappedExceptionBuildItem unwrapExecutionException() {
    // Unwrap ExecutionException to find and map the cause
    return new UnwrappedExceptionBuildItem(ExecutionException.class);
}

// When ExecutionException is thrown, the cause is extracted
// and exception mappers run against the cause instead
```

## AllowNotRestParametersBuildItem

Allow non-REST parameters (without JAX-RS annotations) to coexist with REST parameters in resource methods.

```java { .api }
package io.quarkus.resteasy.reactive.server.spi;

class AllowNotRestParametersBuildItem extends SimpleBuildItem {
    // Marker build item - presence indicates behavior
}
```

**Purpose**: Enables mixing of JAX-RS parameters with custom parameter types in resource methods.

**Usage**:

```java
@BuildStep
AllowNotRestParametersBuildItem allowMixedParameters() {
    // Allow non-REST parameters alongside REST parameters
    return new AllowNotRestParametersBuildItem();
}

// With this build item, methods like this are allowed:
@GET
public Response get(
    @QueryParam("id") Long id,      // REST parameter
    CustomContext customContext      // Non-REST parameter
) {
    return Response.ok().build();
}
```

## Extension Development Patterns

### Complete Extension Example

Example of a complete Quarkus extension using REST SPI build items:

```java
package com.example.extension;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.resteasy.reactive.server.spi.*;

public class CustomRestExtensionProcessor {

    private static final String FEATURE = "custom-rest-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    ContextTypeBuildItem registerCustomContext() {
        // Register custom @Context type
        return new ContextTypeBuildItem(
            DotName.createSimple("com.example.RequestContext")
        );
    }

    @BuildStep
    MethodScannerBuildItem registerMethodScanner() {
        // Scan methods for custom annotations
        return new MethodScannerBuildItem(new CustomAnnotationScanner());
    }

    @BuildStep
    PreExceptionMapperHandlerBuildItem registerExceptionHandler() {
        // Add pre-exception mapping handler
        return new PreExceptionMapperHandlerBuildItem(
            new CustomExceptionHandler(),
            Priorities.USER
        );
    }

    @BuildStep
    UnwrappedExceptionBuildItem unwrapCustomExceptions() {
        // Unwrap custom wrapper exceptions
        return new UnwrappedExceptionBuildItem(
            "com.example.WrapperException"
        );
    }
}
```

### Method Scanner Implementation

```java
public class CustomAnnotationScanner implements MethodScanner {

    @Override
    public List<HandlerChainCustomizer> scan(
        MethodInfo method,
        ClassInfo actualEndpointClass,
        Map<String, Object> methodContext
    ) {
        // Check for custom annotation
        if (method.hasAnnotation(DotName.createSimple("com.example.Audited"))) {
            // Return handler chain customizer
            return Collections.singletonList(
                new AuditHandlerCustomizer(method.name())
            );
        }
        return Collections.emptyList();
    }
}

public class AuditHandlerCustomizer implements HandlerChainCustomizer {

    private final String methodName;

    public AuditHandlerCustomizer(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public List<ServerRestHandler> handlers(
        Phase phase,
        ResourceClass resourceClass,
        ServerResourceMethod resourceMethod
    ) {
        if (phase == Phase.AFTER_METHOD_INVOKE) {
            return Collections.singletonList(new AuditHandler(methodName));
        }
        return Collections.emptyList();
    }
}

public class AuditHandler implements ServerRestHandler {

    private final String methodName;

    public AuditHandler(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) {
        // Log audit information after method invocation
        System.out.println("Method " + methodName + " invoked");
        requestContext.resume();
    }
}
```

### Annotation Transformation Example

```java
@BuildStep
AnnotationsTransformerBuildItem addCorsHeaders() {
    return new AnnotationsTransformerBuildItem(
        AnnotationTransformation.builder()
            .whenClass(c -> c.hasAnnotation(MY_CORS_ANNOTATION))
            .transform(ctx -> {
                // Add CORS headers to all methods in annotated classes
                ctx.transform()
                    .add(ResponseHeader.class)
                        .value("name", "Access-Control-Allow-Origin")
                        .value("value", "*")
                    .done();
            })
    );
}
```

## Testing Extensions

Test build-time SPI extensions using Quarkus test framework:

```java
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CustomExtensionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
        .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
            .addClasses(TestResource.class, CustomExtensionProcessor.class));

    @Test
    public void testCustomExtension() {
        // Test that custom extension is working
        RestAssured.given()
            .when().get("/test")
            .then()
            .statusCode(200)
            .header("X-Custom-Header", "value");
    }
}
```

## Best Practices

1. **Use Appropriate Build Item Types**
   - MultiBuildItem: Can have multiple instances
   - SimpleBuildItem: Only one instance allowed

2. **Consider Build-Time Performance**
   - Minimize work in build steps
   - Cache computed results when possible
   - Use efficient indexing operations

3. **Document Extension Points**
   - Clearly document custom annotations
   - Provide usage examples
   - Explain interaction with other extensions

4. **Handle Edge Cases**
   - Check for null values
   - Handle missing annotations gracefully
   - Provide sensible defaults

5. **Test Thoroughly**
   - Test with and without the extension
   - Test interaction with other extensions
   - Test build-time failures

## Related Build Items

Build items in the deployment package (internal use):

- `ResteasyReactiveDeploymentBuildItem` - Main deployment descriptor
- `ResteasyReactiveDeploymentInfoBuildItem` - Deployment metadata
- `ServerSerialisersBuildItem` - Server serializers registration
- `RequestContextFactoryBuildItem` - Request context factory
- `ContextResolversBuildItem` - Context resolvers registration

These are primarily for internal framework use but may be relevant for advanced extension development.
