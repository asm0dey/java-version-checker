# Security Setup Guide

This guide shows how to secure your Quarkus REST endpoints with authentication and authorization.

## Add Security Extension

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-security</artifactId>
</dependency>
```

## Role-Based Access Control (RBAC)

### Secure Endpoints

```java
import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.DenyAll;

@Path("/api")
public class SecureResource {

    @GET
    @Path("/public")
    @PermitAll
    public String publicEndpoint() {
        return "Public data";
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

    @GET
    @Path("/restricted")
    @DenyAll
    public String restrictedEndpoint() {
        return "Never accessible";
    }
}
```

### Multiple Roles

```java
@DELETE
@Path("/users/{id}")
@RolesAllowed({"admin", "superuser"})
public Response deleteUser(@PathParam("id") Long id) {
    userService.delete(id);
    return Response.noContent().build();
}
```

### Class-Level Security

```java
@Path("/admin")
@RolesAllowed("admin")  // Applies to all methods
public class AdminResource {

    @GET
    @Path("/dashboard")
    public String dashboard() {
        return "Admin dashboard";
    }

    @GET
    @Path("/public-info")
    @PermitAll  // Override class-level
    public String publicInfo() {
        return "Public admin info";
    }
}
```

## Basic Authentication

### Add Extension

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-elytron-security-properties-file</artifactId>
</dependency>
```

### Configure Users

`src/main/resources/application.properties`:

```properties
quarkus.security.users.embedded.enabled=true
quarkus.security.users.embedded.plain-text=true
quarkus.security.users.embedded.users.alice=alice123
quarkus.security.users.embedded.users.bob=bob456
quarkus.security.users.embedded.roles.alice=user
quarkus.security.users.embedded.roles.bob=admin
```

### Test

```bash
curl -u alice:alice123 http://localhost:8080/api/user
curl -u bob:bob456 http://localhost:8080/api/admin
```

## JWT Authentication

### Add Extension

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-jwt</artifactId>
</dependency>
```

### Configure

```properties
mp.jwt.verify.publickey.location=META-INF/resources/publicKey.pem
mp.jwt.verify.issuer=https://your-issuer.com
```

### Use JWT Claims

```java
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/secure")
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

## OIDC/OAuth2

### Add Extension

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-oidc</artifactId>
</dependency>
```

### Configure

```properties
quarkus.oidc.auth-server-url=https://your-oidc-provider.com/realms/your-realm
quarkus.oidc.client-id=your-client-id
quarkus.oidc.credentials.secret=your-client-secret
```

### Secure Endpoints

```java
@Path("/protected")
@Authenticated  // Requires any authenticated user
public class ProtectedResource {

    @Inject
    SecurityIdentity securityIdentity;

    @GET
    public Response getInfo() {
        String username = securityIdentity.getPrincipal().getName();
        Set<String> roles = securityIdentity.getRoles();
        
        return Response.ok()
            .entity(new UserInfo(username, roles))
            .build();
    }
}
```

## Permission-Based Access

```java
import io.quarkus.security.PermissionsAllowed;

@Path("/documents")
public class DocumentResource {

    @GET
    @Path("/{id}")
    @PermissionsAllowed("document:read")
    public Document get(@PathParam("id") Long id) {
        return documentService.findById(id);
    }

    @PUT
    @Path("/{id}")
    @PermissionsAllowed({"document:write", "document:update"})
    public Response update(@PathParam("id") Long id, Document doc) {
        documentService.update(id, doc);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    @PermissionsAllowed(
        value = {"document:delete", "admin:all"},
        logical = PermissionsAllowed.LogicalOperation.OR
    )
    public Response delete(@PathParam("id") Long id) {
        documentService.delete(id);
        return Response.noContent().build();
    }
}
```

## Access Security Context

```java
import jakarta.ws.rs.core.SecurityContext;

@Path("/user")
public class UserResource {

    @GET
    @Path("/info")
    public Response getUserInfo(@Context SecurityContext securityContext) {
        Principal principal = securityContext.getUserPrincipal();
        String username = principal != null ? principal.getName() : "anonymous";
        boolean isAdmin = securityContext.isUserInRole("admin");
        boolean isSecure = securityContext.isSecure();
        
        return Response.ok()
            .entity(new UserInfo(username, isAdmin, isSecure))
            .build();
    }
}
```

## Programmatic Security

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
            return Response.status(401).build();
        }

        if (securityIdentity.hasRole("admin")) {
            return Response.ok("Admin access").build();
        }

        return Response.ok("User access").build();
    }
}
```

## HTTP Security Policies

Configure path-based security in `application.properties`:

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

## CORS Configuration

```properties
quarkus.http.cors=true
quarkus.http.cors.origins=https://example.com
quarkus.http.cors.methods=GET,POST,PUT,DELETE
quarkus.http.cors.headers=accept,authorization,content-type
quarkus.http.cors.exposed-headers=Content-Disposition
quarkus.http.cors.access-control-max-age=24H
```

## Proactive vs Lazy Authentication

```properties
# Proactive (default): Authenticate all requests
quarkus.http.auth.proactive=true

# Lazy: Authenticate only when security annotations present
quarkus.http.auth.proactive=false
```

## Next Steps

- [Reactive Programming](reactive-programming.md) - Secure reactive endpoints
- [REST Clients](rest-clients.md) - Secure client calls
- [Common Scenarios](../examples/common-scenarios.md) - Security examples

## See Also

- [Security Reference](../reference/security.md)
- [Filters Reference](../reference/filters.md)
- [Exception Mapping](../reference/exception-mapping.md)
