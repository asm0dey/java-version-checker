# Security Features

Quarkus REST provides comprehensive security features including role-based access control (RBAC), permission-based authorization, authentication integration, and automatic security context management.

## Security Annotations

### Role-Based Access Control

Control access to endpoints based on user roles.

```java { .api }
package jakarta.annotation.security;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@interface RolesAllowed {
    String[] value();
}

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@interface PermitAll {}

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@interface DenyAll {}
```

**Usage**:

```java
@Path("/admin")
@RolesAllowed("admin")
public class AdminResource {

    @GET
    @Path("/users")
    public List<User> getAllUsers() {
        return userService.findAll();
    }

    @DELETE
    @Path("/user/{id}")
    @RolesAllowed({"admin", "superuser"})  // Multiple roles
    public Response deleteUser(@PathParam("id") Long id) {
        userService.delete(id);
        return Response.noContent().build();
    }
}

@Path("/public")
public class PublicResource {

    @GET
    @Path("/info")
    @PermitAll  // Explicitly allow all users
    public String getInfo() {
        return "Public information";
    }

    @GET
    @Path("/restricted")
    @DenyAll  // Deny all access
    public String getRestricted() {
        return "Never accessible";
    }
}
```

### Authentication Required

Require authenticated users without specific role checks.

```java { .api }
package io.quarkus.security;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@interface Authenticated {}
```

**Usage**:

```java
@Path("/profile")
@Authenticated
public class ProfileResource {

    @GET
    public Profile getUserProfile(@Context SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal().getName();
        return profileService.findByUsername(username);
    }
}
```

### Permission-Based Access Control

Fine-grained permission-based authorization using Quarkus security.

```java { .api }
package io.quarkus.security;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@interface PermissionsAllowed {
    String[] value();
    PermissionsAllowed.LogicalOperation logical() default LogicalOperation.AND;

    enum LogicalOperation {
        AND, OR
    }
}
```

**Usage**:

```java
@Path("/documents")
public class DocumentResource {

    @GET
    @Path("/{id}")
    @PermissionsAllowed("document:read")
    public Document getDocument(@PathParam("id") Long id) {
        return documentService.findById(id);
    }

    @PUT
    @Path("/{id}")
    @PermissionsAllowed({"document:write", "document:update"})
    public Response updateDocument(@PathParam("id") Long id, Document doc) {
        documentService.update(id, doc);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    @PermissionsAllowed(value = {"document:delete", "admin:all"}, logical = PermissionsAllowed.LogicalOperation.OR)
    public Response deleteDocument(@PathParam("id") Long id) {
        documentService.delete(id);
        return Response.noContent().build();
    }
}
```

## SecurityContext

Access security information about the current request.

```java { .api }
package jakarta.ws.rs.core;

interface SecurityContext {
    Principal getUserPrincipal();
    boolean isUserInRole(String role);
    boolean isSecure();
    String getAuthenticationScheme();

    String BASIC_AUTH = "BASIC";
    String CLIENT_CERT_AUTH = "CLIENT_CERT";
    String DIGEST_AUTH = "DIGEST";
    String FORM_AUTH = "FORM";
}
```

### Quarkus REST SecurityContext Implementation

The Quarkus REST implementation provides additional features:

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

class ResteasyReactiveSecurityContext implements SecurityContext {
    Principal getUserPrincipal();
    boolean isUserInRole(String role);  // Supports "**" wildcard for any authenticated user
    boolean isSecure();
    String getAuthenticationScheme();
}
```

**Usage**:

```java
@GET
@Path("/user-info")
public UserInfo getUserInfo(@Context SecurityContext securityContext) {
    Principal principal = securityContext.getUserPrincipal();
    String username = principal != null ? principal.getName() : "anonymous";
    boolean isAdmin = securityContext.isUserInRole("admin");
    boolean isSecure = securityContext.isSecure();
    String authScheme = securityContext.getAuthenticationScheme();

    return new UserInfo(username, isAdmin, isSecure, authScheme);
}

@GET
@Path("/authenticated-user")
public String getAuthenticatedUser(@Context SecurityContext securityContext) {
    // Check if user is authenticated (any role)
    if (securityContext.isUserInRole("**")) {
        return "User is authenticated: " + securityContext.getUserPrincipal().getName();
    }
    return "Anonymous user";
}
```

## Security Exception Mappers

Quarkus REST provides built-in exception mappers for common security exceptions.

### Authentication Completion Exception

Maps authentication completion failures to 401 Unauthorized.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.exceptionmappers;

class AuthenticationCompletionExceptionMapper
    implements ExceptionMapper<AuthenticationCompletionException> {
    Response toResponse(AuthenticationCompletionException ex);
}
```

### Authentication Failed Exception

Maps authentication failures to HTTP responses with WWW-Authenticate challenge.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.exceptionmappers;

class AuthenticationFailedExceptionMapper {
    @ServerExceptionMapper(value = AuthenticationFailedException.class, priority = Priorities.USER + 1)
    Uni<Response> handle(RoutingContext routingContext);
}
```

### Authentication Redirect Exception

Maps authentication redirects to HTTP 302 responses with Location header and cache control.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.exceptionmappers;

class AuthenticationRedirectExceptionMapper
    implements ExceptionMapper<AuthenticationRedirectException> {
    Response toResponse(AuthenticationRedirectException ex);
}
```

### Forbidden Exception

Maps authorization failures to 403 Forbidden.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.exceptionmappers;

class ForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {
    Response toResponse(ForbiddenException exception);
}
```

### Unauthorized Exception

Maps unauthorized access to HTTP responses with WWW-Authenticate challenge.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.exceptionmappers;

class UnauthorizedExceptionMapper {
    @ServerExceptionMapper(value = UnauthorizedException.class, priority = Priorities.USER + 1)
    Uni<Response> handle(RoutingContext routingContext);
}
```

## Eager Security

Quarkus REST performs security checks early in the request processing pipeline for better performance.

### Eager Security Context

Manages eager security checks and deferred identity resolution.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.security;

@Singleton
class EagerSecurityContext {
    static EagerSecurityContext instance;

    Uni<SecurityIdentity> getDeferredIdentity();
    Uni<SecurityIdentity> getPermissionCheck(
        ResteasyReactiveRequestContext requestContext,
        SecurityIdentity identity
    );
}
```

### Security Handlers

Process security annotations and perform authorization checks.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.security;

class EagerSecurityHandler implements ServerRestHandler {
    void handle(ResteasyReactiveRequestContext requestContext);

    abstract class Customizer implements HandlerChainCustomizer {
        static HandlerChainCustomizer newInstance(boolean onlyCheckForHttpPermissions);
    }
}

class EagerSecurityInterceptorHandler implements ServerRestHandler {
    void handle(ResteasyReactiveRequestContext requestContext);

    class Customizer implements HandlerChainCustomizer {
        static HandlerChainCustomizer newInstance();
    }
}

class SecurityContextOverrideHandler implements ServerRestHandler {
    void handle(ResteasyReactiveRequestContext requestContext);
}

record ResourceMethodDescription(
    MethodDescription invokedMethodDesc,
    MethodDescription fallbackMethodDesc
) {
    static ResourceMethodDescription of(ServerResourceMethod method);
}
```

## Reactive Security

Security operations integrate with reactive programming using Mutiny.

**Usage**:

```java
@GET
@Path("/secure-async")
@RolesAllowed("user")
public Uni<Data> getSecureDataAsync() {
    // Security check happens before this method executes
    return dataService.fetchAsync();
}

@POST
@Path("/secure-create")
@PermissionsAllowed("data:create")
public Uni<Response> createSecureDataAsync(Data data) {
    // Permission check happens before method execution
    return dataService.persistAsync(data)
        .onItem().transform(created ->
            Response.created(URI.create("/data/" + created.getId()))
                .entity(created)
                .build()
        );
}
```


## Programmatic Security

Inject SecurityIdentity for programmatic security checks.

```java
import io.quarkus.security.identity.SecurityIdentity;

@Path("/secure")
public class SecureResource {

    @Inject
    SecurityIdentity securityIdentity;

    @GET
    @Path("/check")
    public Response checkPermissions() {
        if (securityIdentity.isAnonymous()) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        if (securityIdentity.hasRole("admin")) {
            // Admin-specific logic
            return Response.ok("Admin access").build();
        }

        // Regular user logic
        return Response.ok("User access").build();
    }

    @GET
    @Path("/custom-check")
    public Response customSecurityCheck() {
        // Check for specific permission
        boolean hasPermission = securityIdentity.checkPermission(
            new CustomPermission("document:read")
        ).await().indefinitely();

        if (!hasPermission) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        return Response.ok().build();
    }
}
```

## Method-Level Security

Apply different security constraints to methods within the same resource class.

```java
@Path("/mixed")
public class MixedSecurityResource {

    @GET
    @Path("/public")
    @PermitAll
    public String publicEndpoint() {
        return "Public data";
    }

    @GET
    @Path("/authenticated")
    @Authenticated
    public String authenticatedEndpoint(@Context SecurityContext ctx) {
        return "Hello " + ctx.getUserPrincipal().getName();
    }

    @GET
    @Path("/user")
    @RolesAllowed("user")
    public String userEndpoint() {
        return "User data";
    }

    @GET
    @Path("/admin")
    @RolesAllowed("admin")
    public String adminEndpoint() {
        return "Admin data";
    }
}
```

## Class-Level Security

Apply security constraints to entire resource classes with method-level overrides.

```java
@Path("/admin")
@RolesAllowed("admin")  // Default for all methods
public class AdminResource {

    @GET
    @Path("/dashboard")
    public String dashboard() {
        // Inherits @RolesAllowed("admin")
        return "Admin dashboard";
    }

    @GET
    @Path("/settings")
    @RolesAllowed({"admin", "superuser"})  // Override class-level
    public String settings() {
        return "Settings";
    }

    @GET
    @Path("/public-info")
    @PermitAll  // Override to allow all
    public String publicInfo() {
        return "Public admin info";
    }
}
```

## HTTP Security Policies

Quarkus REST integrates with Quarkus HTTP security policies for path-based authorization.

Configure in `application.properties`:

```properties
# Require authentication for all /api/* paths
quarkus.http.auth.policy.api-policy.roles-allowed=user

# Apply policy to path patterns
quarkus.http.auth.permission.api.paths=/api/*
quarkus.http.auth.permission.api.policy=api-policy

# Public paths
quarkus.http.auth.permission.public.paths=/public/*
quarkus.http.auth.permission.public.policy=permit
```

## Security Interceptors

Standard security check interceptors prevent repeated security checks when eager security is enabled.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime;

abstract class StandardSecurityCheckInterceptor {
    static final String STANDARD_SECURITY_CHECK_INTERCEPTOR = "io.quarkus.resteasy.reactive.server.runtime.StandardSecurityCheckInterceptor";

    @AroundInvoke
    Object intercept(InvocationContext ic);
}

@Interceptor
class RolesAllowedInterceptor extends StandardSecurityCheckInterceptor {}

@Interceptor
class PermissionsAllowedInterceptor extends StandardSecurityCheckInterceptor {}

@Interceptor
class PermitAllInterceptor extends StandardSecurityCheckInterceptor {}

@Interceptor
class AuthenticatedInterceptor extends StandardSecurityCheckInterceptor {}
```

## Proactive vs Lazy Authentication

Quarkus supports two authentication modes:

**Proactive Authentication** (default): Authentication happens before routing, for all requests.

```properties
quarkus.http.auth.proactive=true
```

**Lazy Authentication**: Authentication happens only when security annotations are encountered.

```properties
quarkus.http.auth.proactive=false
```

With lazy authentication, use `@Authenticated` or security annotations to trigger authentication:

```java
@Path("/lazy")
public class LazyAuthResource {

    @GET
    @Path("/public")
    public String publicEndpoint() {
        // No authentication triggered
        return "Public";
    }

    @GET
    @Path("/secure")
    @Authenticated  // Triggers authentication
    public String secureEndpoint(@Context SecurityContext ctx) {
        return "Hello " + ctx.getUserPrincipal().getName();
    }
}
```

## Integration with Authentication Mechanisms

Quarkus REST works with various authentication mechanisms:

- **Basic Authentication**: `quarkus-elytron-security-properties-file`
- **OIDC/OAuth2**: `quarkus-oidc`
- **JWT**: `quarkus-smallrye-jwt`
- **Mutual TLS**: `quarkus-elytron-security`
- **Custom**: Implement `IdentityProvider`

Example with JWT:

```java
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/jwt")
public class JwtResource {

    @Inject
    JsonWebToken jwt;

    @GET
    @RolesAllowed("user")
    public Response getUserInfo() {
        String username = jwt.getName();
        Set<String> groups = jwt.getGroups();

        return Response.ok()
            .entity(new UserInfo(username, groups))
            .build();
    }
}
```
