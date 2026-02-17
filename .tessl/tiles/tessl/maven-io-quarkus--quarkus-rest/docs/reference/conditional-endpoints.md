# Conditional Endpoint Enablement

Dynamically enable or disable REST endpoints based on runtime configuration properties using the `@EndpointDisabled` annotation.

## EndpointDisabled Annotation

```java { .api }
package io.quarkus.resteasy.reactive.server;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@interface EndpointDisabled {
    /**
     * Name of the configuration property to check
     */
    String name();

    /**
     * Expected string value of the property for the endpoint to be disabled
     * If the property value matches this string, the endpoint is disabled
     */
    String stringValue() default "";

    /**
     * If true, the endpoint is disabled when the property is not set
     * If false (default), the endpoint is enabled when the property is not set
     */
    boolean disableIfMissing() default false;
}
```

## Basic Usage

Disable an endpoint when a configuration property has a specific value:

```java
@Path("/experimental")
@EndpointDisabled(name = "features.experimental", stringValue = "false")
public class ExperimentalResource {

    @GET
    @Path("/feature")
    public String experimentalFeature() {
        return "Experimental feature";
    }
}
```

In `application.properties`:

```properties
# When set to "false", the endpoint is disabled
features.experimental=false

# When set to "true" or any other value, the endpoint is enabled
features.experimental=true
```

## Disable When Property Missing

Disable an endpoint when a configuration property is not defined:

```java
@Path("/premium")
@EndpointDisabled(name = "license.premium", disableIfMissing = true)
public class PremiumResource {

    @GET
    @Path("/features")
    public List<String> getPremiumFeatures() {
        return List.of("Feature 1", "Feature 2");
    }
}
```

This endpoint will only be enabled when `license.premium` is explicitly set in configuration.

## Environment-Specific Endpoints

Enable different endpoints based on the environment:

```java
@Path("/debug")
@EndpointDisabled(name = "quarkus.profile", stringValue = "prod")
public class DebugResource {

    @GET
    @Path("/info")
    public String debugInfo() {
        return "Debug information";
    }
}
```

The debug endpoint is disabled in production but available in dev and test profiles.

## Feature Flags

Use configuration properties as feature flags:

```java
@Path("/beta")
@EndpointDisabled(name = "features.beta.enabled", stringValue = "false")
public class BetaFeaturesResource {

    @GET
    @Path("/new-api")
    public Response newApi() {
        return Response.ok("Beta API").build();
    }
}
```

In `application.properties`:

```properties
# Feature flag: enable or disable beta features
features.beta.enabled=true
```

## Multiple Conditional Endpoints

Different resources can be conditionally enabled based on different properties:

```java
@Path("/feature-a")
@EndpointDisabled(name = "features.a.enabled", stringValue = "false")
public class FeatureAResource {
    @GET
    public String featureA() {
        return "Feature A";
    }
}

@Path("/feature-b")
@EndpointDisabled(name = "features.b.enabled", stringValue = "false")
public class FeatureBResource {
    @GET
    public String featureB() {
        return "Feature B";
    }
}
```

In `application.properties`:

```properties
features.a.enabled=true
features.b.enabled=false
```

## License-Based Endpoints

Control access to premium features based on license configuration:

```java
@Path("/enterprise")
@EndpointDisabled(name = "license.type", stringValue = "basic")
public class EnterpriseResource {

    @GET
    @Path("/analytics")
    public Response getAnalytics() {
        return Response.ok("Enterprise analytics").build();
    }
}
```

In `application.properties`:

```properties
# License type controls endpoint availability
license.type=enterprise  # Enables enterprise endpoints
# license.type=basic      # Disables enterprise endpoints
```

## Tenant-Specific Endpoints

Enable or disable endpoints based on tenant configuration:

```java
@Path("/tenant-specific")
@EndpointDisabled(name = "tenant.custom-features", stringValue = "disabled")
public class TenantSpecificResource {

    @GET
    @Path("/custom")
    public Response customFeature() {
        return Response.ok("Tenant custom feature").build();
    }
}
```

## Important Notes

1. **Class-Level Only**: `@EndpointDisabled` can only be applied to classes (resource classes), not individual methods

2. **String Comparison**: The comparison is always done using string values. Boolean and numeric properties should be compared as strings:

```java
// Correct
@EndpointDisabled(name = "feature.enabled", stringValue = "false")

// Incorrect - won't work as expected
@EndpointDisabled(name = "feature.enabled", stringValue = false)  // Compilation error
```

3. **Runtime Evaluation**: The endpoint availability is determined at application startup based on the configuration. Changing configuration at runtime requires application restart.

4. **Default Behavior**: If `stringValue` is not specified, the default is an empty string:

```java
@EndpointDisabled(name = "feature.enabled")  // Disabled when property equals ""
```

5. **Property Sources**: The annotation works with any Quarkus configuration source (application.properties, environment variables, system properties, etc.)

## Environment Variables

Configuration properties can be set via environment variables:

```java
@Path("/cloud-only")
@EndpointDisabled(name = "deployment.cloud", stringValue = "false")
public class CloudOnlyResource {
    @GET
    public String cloudFeature() {
        return "Cloud feature";
    }
}
```

Set via environment variable:

```bash
DEPLOYMENT_CLOUD=true java -jar application.jar
```

## Profile-Specific Configuration

Combine with Quarkus profiles for environment-specific endpoint availability:

In `application.properties`:

```properties
# Development profile - all features enabled
%dev.features.experimental=true
%dev.features.debug=true

# Production profile - only stable features
%prod.features.experimental=false
%prod.features.debug=false
```

```java
@Path("/experimental")
@EndpointDisabled(name = "features.experimental", stringValue = "false")
public class ExperimentalResource {
    // Enabled in dev, disabled in prod
}
```

## Combining with Security

Conditional endpoints work alongside security annotations:

```java
@Path("/admin-beta")
@RolesAllowed("admin")
@EndpointDisabled(name = "features.admin-beta", stringValue = "false")
public class AdminBetaResource {

    @GET
    public String betaFeature() {
        // Requires both admin role AND feature flag enabled
        return "Admin beta feature";
    }
}
```

## Build-Time vs Runtime

`@EndpointDisabled` is evaluated at application startup (build time in native mode, startup time in JVM mode). The endpoint is either included or excluded from the application based on configuration at that time.

For truly dynamic runtime enabling/disabling, implement custom logic in the resource methods:

```java
@Path("/dynamic")
public class DynamicResource {

    @ConfigProperty(name = "feature.dynamic.enabled")
    boolean featureEnabled;

    @GET
    public Response dynamicFeature() {
        if (!featureEnabled) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok("Dynamic feature").build();
    }
}
```
