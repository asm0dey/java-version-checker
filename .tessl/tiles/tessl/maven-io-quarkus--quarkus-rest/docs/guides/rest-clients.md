# REST Clients Guide

This guide shows how to build REST clients to call external APIs.

## Add REST Client Extension

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-client-reactive</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-client-reactive-jackson</artifactId>
</dependency>
```

## Create Client Interface

```java
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/users")
@RegisterRestClient(configKey = "user-api")
public interface UserApiClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<User> getAll();

    @GET
    @Path("/{id}")
    User getById(@PathParam("id") Long id);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    User create(User user);

    @PUT
    @Path("/{id}")
    User update(@PathParam("id") Long id, User user);

    @DELETE
    @Path("/{id}")
    void delete(@PathParam("id") Long id);
}
```

## Configure Client

`application.properties`:

```properties
quarkus.rest-client.user-api.url=https://api.example.com
quarkus.rest-client.user-api.scope=jakarta.inject.Singleton
```

## Use the Client

```java
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/proxy")
public class ProxyResource {

    @RestClient
    UserApiClient userApi;

    @GET
    @Path("/users")
    public List<User> getAllUsers() {
        return userApi.getAll();
    }

    @GET
    @Path("/users/{id}")
    public User getUser(@PathParam("id") Long id) {
        return userApi.getById(id);
    }
}
```

## Reactive Clients

Return `Uni` for async operations:

```java
@Path("/api/users")
@RegisterRestClient(configKey = "user-api")
public interface UserApiClient {

    @GET
    Uni<List<User>> getAll();

    @GET
    @Path("/{id}")
    Uni<User> getById(@PathParam("id") Long id);

    @POST
    Uni<User> create(User user);
}
```

Usage:

```java
@Path("/proxy")
public class ProxyResource {

    @RestClient
    UserApiClient userApi;

    @GET
    @Path("/users/{id}")
    public Uni<User> getUser(@PathParam("id") Long id) {
        return userApi.getById(id);
    }
}
```

## Query Parameters

```java
@GET
@Path("/search")
List<User> search(
    @QueryParam("name") String name,
    @QueryParam("age") Integer age
);
```

## Headers

### Static Headers

```java
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;

@GET
@ClientHeaderParam(name = "User-Agent", value = "MyApp/1.0")
@ClientHeaderParam(name = "Accept", value = "application/json")
List<User> getAll();
```

### Dynamic Headers

```java
@GET
@ClientHeaderParam(
    name = "Authorization",
    value = "{generateToken}"
)
List<User> getAll();

default String generateToken() {
    return "Bearer " + tokenService.getToken();
}
```

### Header Injection

```java
@GET
List<User> getAll(@HeaderParam("Authorization") String token);
```

## Authentication

### Basic Auth

```java
import io.quarkus.rest.client.reactive.ClientBasicAuth;

@GET
@ClientBasicAuth(username = "${api.username}", password = "${api.password}")
List<User> getAll();
```

### Bearer Token

```properties
quarkus.rest-client.user-api.url=https://api.example.com
quarkus.rest-client.user-api.headers.Authorization=Bearer ${api.token}
```

## Error Handling

### Exception Mapper

```java
import io.quarkus.rest.client.reactive.ClientExceptionMapper;

@Path("/api/users")
@RegisterRestClient
public interface UserApiClient {

    @GET
    @Path("/{id}")
    User getById(@PathParam("id") Long id);

    @ClientExceptionMapper
    static RuntimeException toException(Response response) {
        if (response.getStatus() == 404) {
            return new NotFoundException("User not found");
        }
        return new RuntimeException("API error: " + response.getStatus());
    }
}
```

### Reactive Error Handling

```java
@GET
@Path("/users/{id}")
public Uni<User> getUser(@PathParam("id") Long id) {
    return userApi.getById(id)
        .onFailure().recoverWithItem(User.getDefaultUser());
}
```

## Timeouts

```properties
quarkus.rest-client.user-api.connect-timeout=5000
quarkus.rest-client.user-api.read-timeout=10000
```

## Retry

```java
import org.eclipse.microprofile.faulttolerance.Retry;

@Path("/api/users")
@RegisterRestClient
public interface UserApiClient {

    @GET
    @Path("/{id}")
    @Retry(maxRetries = 3, delay = 1000)
    User getById(@PathParam("id") Long id);
}
```

## Circuit Breaker

```java
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

@GET
@Path("/{id}")
@CircuitBreaker(
    requestVolumeThreshold = 4,
    failureRatio = 0.5,
    delay = 5000
)
User getById(@PathParam("id") Long id);
```

## Fallback

```java
import org.eclipse.microprofile.faulttolerance.Fallback;

@GET
@Path("/{id}")
@Fallback(fallbackMethod = "getDefaultUser")
User getById(@PathParam("id") Long id);

default User getDefaultUser(Long id) {
    return new User(id, "Default User");
}
```

## Multipart Form Data

```java
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@POST
@Path("/upload")
@Consumes(MediaType.MULTIPART_FORM_DATA)
String upload(
    @RestForm @PartType(MediaType.APPLICATION_OCTET_STREAM) FileUpload file,
    @RestForm String description
);
```

## SSL/TLS Configuration

```properties
quarkus.rest-client.user-api.url=https://api.example.com
quarkus.rest-client.user-api.trust-store=/path/to/truststore.jks
quarkus.rest-client.user-api.trust-store-password=changeit
quarkus.rest-client.user-api.key-store=/path/to/keystore.jks
quarkus.rest-client.user-api.key-store-password=changeit
```

## Programmatic Client

Create clients programmatically:

```java
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;

UserApiClient client = QuarkusRestClientBuilder.newBuilder()
    .baseUri(URI.create("https://api.example.com"))
    .build(UserApiClient.class);

List<User> users = client.getAll();
```

## Logging

Enable request/response logging:

```properties
quarkus.rest-client.user-api.logging.scope=request-response
quarkus.rest-client.user-api.logging.body-limit=1024
```

## Testing

### Mock with @InjectMock

```java
@QuarkusTest
public class ProxyResourceTest {

    @InjectMock
    @RestClient
    UserApiClient userApi;

    @Test
    public void testGetUser() {
        User mockUser = new User(1L, "Alice");
        when(userApi.getById(1L)).thenReturn(mockUser);

        given()
            .when().get("/proxy/users/1")
            .then()
            .statusCode(200)
            .body("name", is("Alice"));
    }
}
```

## Next Steps

- [Common Scenarios](../examples/common-scenarios.md) - Client examples
- [Advanced Patterns](../examples/advanced-patterns.md) - Complex integrations
- [Security Setup](security-setup.md) - Secure client calls

## See Also

- [REST Client Reference](../reference/rest-client.md)
- [Reactive Programming](../reference/reactive-programming.md)
- [Exception Mapping](../reference/exception-mapping.md)
