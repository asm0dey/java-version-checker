# Observability

Quarkus REST provides built-in observability features for metrics and tracing, including endpoint scoring, URL template path tracking, and integration with monitoring backends.

## ObservabilityHandler

The observability handler sets URL template paths on requests for accurate metrics and tracing.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.observability;

class ObservabilityHandler implements ServerRestHandler {
    // Template path management
    String getTemplatePath();
    void setTemplatePath(String templatePath);

    // Sub-resource handling
    boolean isSubResource();
    void setSubResource(boolean subResource);

    // Handler execution
    void handle(ResteasyReactiveRequestContext requestContext);
}
```

The handler normalizes slashes in template paths and sets the `UrlPathTemplate` routing context data for use by metrics and tracing systems.

**Usage**:

```java
// Automatically applied by Quarkus REST
// For path: /api/users/{id}
// Sets template: /api/users/{id} (not /api/users/123)
```

## ObservabilityCustomizer

Handler chain customizer that adds observability handlers to the request processing pipeline.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.observability;

class ObservabilityCustomizer implements HandlerChainCustomizer {
    List<ServerRestHandler> handlers(
        Phase phase,
        ResourceClass resourceClass,
        ServerResourceMethod resourceMethod
    );
}
```

The customizer adds ObservabilityHandler during the AFTER_MATCH phase, after the resource method has been matched but before it executes.

## URL Template Paths

URL template paths provide consistent metric and trace names regardless of path parameter values.

**Example Paths**:

```java
@Path("/api/users")
public class UserResource {

    @GET
    @Path("/{id}")
    public User getUser(@PathParam("id") Long id) {
        // Template path: /api/users/{id}
        // Not: /api/users/123, /api/users/456, etc.
        return userService.findById(id);
    }

    @GET
    @Path("/{userId}/orders/{orderId}")
    public Order getUserOrder(
        @PathParam("userId") Long userId,
        @PathParam("orderId") Long orderId
    ) {
        // Template path: /api/users/{userId}/orders/{orderId}
        return orderService.findByUserAndId(userId, orderId);
    }
}
```

This ensures metrics are aggregated by endpoint, not split across individual parameter values.

## Endpoint Scoring

Endpoint scoring tracks method invocation statistics for diagnostics and monitoring.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

class EndpointScoresSupplier implements Supplier<ScoreSystem.EndpointScores> {
    ScoreSystem.EndpointScores get();
}
```

### Accessing Endpoint Scores

Endpoint scores are available through the Dev UI JSON-RPC service.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.devui;

class ResteasyReactiveJsonRPCService {
    JsonObject getEndpointScores();  // Non-blocking
    JsonArray getExceptionMappers(); // Non-blocking
    JsonArray getParamConverterProviders(); // Non-blocking
}
```

**Endpoint Scores Format**:

```json
{
  "GET /api/users/{id}": {
    "invocations": 1523,
    "avgDuration": 45.3,
    "maxDuration": 342.1,
    "minDuration": 12.5
  },
  "POST /api/users": {
    "invocations": 234,
    "avgDuration": 123.7,
    "maxDuration": 567.2,
    "minDuration": 45.3
  }
}
```

## Metrics Integration

URL template paths integrate with Micrometer metrics for accurate endpoint tracking.

**Configuration**:

```properties
# Enable metrics
quarkus.micrometer.enabled=true
quarkus.micrometer.export.prometheus.enabled=true

# HTTP server metrics automatically include URL templates
quarkus.micrometer.binder.http-server.enabled=true
```

**Generated Metrics**:

```
# HTTP request duration with template path
http_server_requests_seconds_count{method="GET",uri="/api/users/{id}",status="200"} 1523
http_server_requests_seconds_sum{method="GET",uri="/api/users/{id}",status="200"} 68.9

# Not split across parameter values:
# NOT: uri="/api/users/123", uri="/api/users/456", etc.
```

## Tracing Integration

URL template paths are used for span names in distributed tracing.

**Configuration**:

```properties
# Enable OpenTelemetry tracing
quarkus.otel.traces.enabled=true
quarkus.otel.exporter.otlp.traces.endpoint=http://jaeger:4317
```

**Trace Span Names**:

```
Span: GET /api/users/{id}
  Duration: 45ms
  Attributes:
    http.method: GET
    http.route: /api/users/{id}
    http.status_code: 200
```

Template paths prevent span explosion and enable proper trace aggregation.

## ObservabilityIntegrationRecorder

Recorder for observability integration, especially for authentication failure handling.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.observability;

class ObservabilityIntegrationRecorder {
    Handler<RoutingContext> preAuthFailureHandler(RuntimeValue<Deployment> deployment);
    static void setTemplatePath(RoutingContext context, Deployment deployment);
}
```

The pre-auth failure handler sets template paths even when authentication fails, ensuring auth failures are properly tracked in metrics.

## Sub-Resource Methods

Sub-resource methods are handled specially for accurate path templates.

**Usage**:

```java
@Path("/api/departments")
public class DepartmentResource {

    @Path("/{deptId}/employees")
    public EmployeeSubResource getEmployeeSubResource(@PathParam("deptId") Long deptId) {
        // Returns sub-resource
        return new EmployeeSubResource(deptId);
    }
}

public class EmployeeSubResource {

    private final Long departmentId;

    public EmployeeSubResource(Long departmentId) {
        this.departmentId = departmentId;
    }

    @GET
    @Path("/{empId}")
    public Employee getEmployee(@PathParam("empId") Long empId) {
        // Template path: /api/departments/{deptId}/employees/{empId}
        return employeeService.findByDepartmentAndId(departmentId, empId);
    }
}
```

The ObservabilityHandler detects sub-resources and constructs the full template path.

## Programmatic Observability

Access observability data programmatically for custom monitoring.

**Usage**:

```java
import io.quarkus.resteasy.reactive.server.runtime.EndpointScoresSupplier;
import jakarta.inject.Inject;

@ApplicationScoped
public class MonitoringService {

    @Inject
    EndpointScoresSupplier scoresSupplier;

    public Map<String, EndpointStats> getEndpointStatistics() {
        ScoreSystem.EndpointScores scores = scoresSupplier.get();
        // Process and return statistics
        return convertToStats(scores);
    }

    public List<String> getSlowEndpoints(double thresholdMs) {
        ScoreSystem.EndpointScores scores = scoresSupplier.get();
        // Filter endpoints exceeding threshold
        return findSlow(scores, thresholdMs);
    }
}
```

## Health Checks

Integrate REST endpoint health into Quarkus health checks.

**Usage**:

```java
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class EndpointHealthCheck implements HealthCheck {

    @Inject
    EndpointScoresSupplier scoresSupplier;

    @Override
    public HealthCheckResponse call() {
        ScoreSystem.EndpointScores scores = scoresSupplier.get();

        // Check if any endpoints are failing
        boolean healthy = checkEndpointHealth(scores);

        return HealthCheckResponse.named("rest-endpoints")
            .status(healthy)
            .withData("totalEndpoints", scores.size())
            .build();
    }
}
```

## Custom Metrics

Add custom metrics to REST endpoints.

**Usage**:

```java
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Path("/api/data")
public class DataResource {

    @Inject
    MeterRegistry registry;

    @GET
    @Path("/{id}")
    public Response getData(@PathParam("id") Long id, @Context UriInfo uriInfo) {
        Timer.Sample sample = Timer.start(registry);

        try {
            Data data = dataService.findById(id);

            sample.stop(Timer.builder("data.fetch")
                .tag("method", "GET")
                .tag("endpoint", uriInfo.getPath())
                .tag("status", "success")
                .register(registry));

            return Response.ok(data).build();
        } catch (Exception e) {
            sample.stop(Timer.builder("data.fetch")
                .tag("status", "error")
                .register(registry));
            throw e;
        }
    }
}
```

## Tracing Context Propagation

Propagate tracing context to downstream services.

**Usage**:

```java
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;

@Path("/api/orchestrator")
public class OrchestratorResource {

    @Inject
    Tracer tracer;

    @Inject
    DownstreamService downstreamService;

    @GET
    @Path("/process")
    public Response process() {
        // Current span is automatically created by framework
        Span currentSpan = Span.current();
        currentSpan.setAttribute("custom.attribute", "value");

        // Call downstream service (context propagated automatically)
        String result = downstreamService.call();

        return Response.ok(result).build();
    }

    @GET
    @Path("/custom-span")
    public Response processWithCustomSpan() {
        Span span = tracer.spanBuilder("custom-operation").startSpan();
        try (var scope = span.makeCurrent()) {
            // Custom tracing logic
            String result = performOperation();
            span.setAttribute("result.size", result.length());
            return Response.ok(result).build();
        } finally {
            span.end();
        }
    }
}
```

## Logging Integration

Integrate request logging with observability.

**Usage**:

```java
import org.jboss.logging.Logger;
import io.vertx.ext.web.RoutingContext;

@Path("/api/logged")
public class LoggedResource {

    private static final Logger LOG = Logger.getLogger(LoggedResource.class);

    @GET
    @Path("/{id}")
    public Response get(
        @PathParam("id") Long id,
        @Context RoutingContext context
    ) {
        // Log with template path for correlation
        String templatePath = (String) context.data().get("UrlPathTemplate");

        LOG.infof("Processing request: %s, id: %d", templatePath, id);

        try {
            Data data = dataService.findById(id);
            LOG.infof("Successfully retrieved data for id: %d", id);
            return Response.ok(data).build();
        } catch (Exception e) {
            LOG.errorf(e, "Error processing request: %s, id: %d", templatePath, id);
            throw e;
        }
    }
}
```

## Performance Monitoring

Monitor endpoint performance and identify bottlenecks.

**Configuration**:

```properties
# Enable detailed metrics
quarkus.micrometer.binder.http-server.enabled=true
quarkus.micrometer.binder.http-server.max-uri-tags=100
quarkus.micrometer.binder.http-server.ignore-patterns=/health,/metrics

# Enable percentile histograms for latency analysis
quarkus.micrometer.binder.http-server.enable-percentile-histograms=true
```

**Monitoring Dashboard Example**:

```
Endpoint: GET /api/users/{id}
  Requests: 1523
  Success Rate: 99.3%
  Avg Duration: 45ms
  P50: 38ms
  P95: 120ms
  P99: 250ms
  Max: 342ms
```

## Alerting

Set up alerts based on endpoint metrics.

**Example Alert Rules** (Prometheus):

```yaml
groups:
  - name: rest_endpoint_alerts
    rules:
      - alert: HighErrorRate
        expr: |
          rate(http_server_requests_seconds_count{status=~"5.."}[5m])
          / rate(http_server_requests_seconds_count[5m]) > 0.05
        for: 5m
        annotations:
          summary: "High error rate on {{ $labels.uri }}"

      - alert: SlowEndpoint
        expr: |
          histogram_quantile(0.95,
            rate(http_server_requests_seconds_bucket[5m])
          ) > 1.0
        for: 5m
        annotations:
          summary: "Slow endpoint: {{ $labels.uri }} (P95 > 1s)"
```

## Dev UI Integration

Access observability data through the Quarkus Dev UI.

**Dev UI Features**:

- Real-time endpoint scores
- Exception mapper listing
- Parameter converter providers
- Request/response metrics

Access at `http://localhost:8080/q/dev` during development.

## Testing Observability

Verify observability features in tests.

**Usage**:

```java
import io.restassured.RestAssured;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ObservabilityTest {

    @Inject
    MeterRegistry registry;

    @Test
    public void testMetricsRecorded() {
        // Make request
        RestAssured.given()
            .when().get("/api/users/123")
            .then().statusCode(200);

        // Verify metric recorded with template path
        Timer timer = registry.find("http.server.requests")
            .tag("uri", "/api/users/{id}")
            .timer();

        assertNotNull(timer);
        assertTrue(timer.count() > 0);
    }
}
```
