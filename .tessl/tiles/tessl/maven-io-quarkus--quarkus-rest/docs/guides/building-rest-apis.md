# Building REST APIs

This guide covers common patterns for building REST APIs with Quarkus REST.

## CRUD Operations

### Resource Class Structure

```java
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserService userService;

    @GET
    public List<User> list() {
        return userService.listAll();
    }

    @GET
    @Path("/{id}")
    public Response get(@PathParam("id") Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return Response.status(404).build();
        }
        return Response.ok(user).build();
    }

    @POST
    public Response create(User user, @Context UriInfo uriInfo) {
        User created = userService.create(user);
        URI location = uriInfo.getAbsolutePathBuilder()
            .path(created.id.toString())
            .build();
        return Response.created(location).entity(created).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, User user) {
        User updated = userService.update(id, user);
        if (updated == null) {
            return Response.status(404).build();
        }
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        boolean deleted = userService.delete(id);
        if (!deleted) {
            return Response.status(404).build();
        }
        return Response.noContent().build();
    }
}
```

## Using RestResponse

Type-safe alternative to Response:

```java
import org.jboss.resteasy.reactive.RestResponse;

@Path("/users")
public class UserResource {

    @GET
    @Path("/{id}")
    public RestResponse<User> get(@PathParam("id") Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return RestResponse.notFound();
        }
        return RestResponse.ok(user);
    }

    @POST
    public RestResponse<User> create(User user) {
        User created = userService.create(user);
        URI location = URI.create("/users/" + created.id);
        return RestResponse.created(location);
    }
}
```

## Validation

Add Bean Validation:

```java
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public class User {
    @NotBlank
    public String name;

    @Email
    public String email;

    @Min(18)
    public int age;
}

@Path("/users")
public class UserResource {

    @POST
    public RestResponse<User> create(@Valid User user) {
        // Validation happens automatically
        User created = userService.create(user);
        return RestResponse.ok(created);
    }
}
```

## Error Handling

### Exception Mapper

```java
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

@ApplicationScoped
public class ErrorMappers {

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> handleValidation(
            ValidationException ex) {
        return RestResponse.status(422, 
            new ErrorResponse("Validation failed", ex.getMessage()));
    }

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> handleNotFound(
            NotFoundException ex) {
        return RestResponse.status(404, 
            new ErrorResponse("Not found", ex.getMessage()));
    }
}
```

## Pagination

```java
@GET
public Response list(
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("20") int size) {
    
    List<User> users = userService.list(page, size);
    long total = userService.count();
    
    return Response.ok(users)
        .header("X-Total-Count", total)
        .header("X-Page", page)
        .header("X-Page-Size", size)
        .build();
}
```

## Filtering and Sorting

```java
@GET
public List<User> list(
        @QueryParam("name") String nameFilter,
        @QueryParam("sort") @DefaultValue("name") String sortField,
        @QueryParam("order") @DefaultValue("asc") String sortOrder) {
    
    return userService.list(nameFilter, sortField, sortOrder);
}
```

## Content Negotiation

```java
@GET
@Path("/{id}")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public User get(@PathParam("id") Long id) {
    return userService.findById(id);
}
```

Client specifies format:

```bash
curl -H "Accept: application/json" http://localhost:8080/users/1
curl -H "Accept: application/xml" http://localhost:8080/users/1
```

## Sub-Resources

```java
@Path("/users")
public class UserResource {

    @Path("/{userId}/posts")
    public PostResource posts(@PathParam("userId") Long userId) {
        return new PostResource(userId);
    }
}

public class PostResource {
    private final Long userId;

    public PostResource(Long userId) {
        this.userId = userId;
    }

    @GET
    public List<Post> list() {
        return postService.findByUser(userId);
    }

    @POST
    public RestResponse<Post> create(Post post) {
        post.userId = userId;
        return RestResponse.ok(postService.create(post));
    }
}
```

## Custom Headers

```java
@GET
@Path("/{id}")
public Response get(@PathParam("id") Long id) {
    User user = userService.findById(id);
    
    return Response.ok(user)
        .header("X-Custom-Header", "value")
        .header("Cache-Control", "max-age=3600")
        .build();
}
```

## ETags for Caching

```java
@GET
@Path("/{id}")
public Response get(
        @PathParam("id") Long id,
        @HeaderParam("If-None-Match") String ifNoneMatch) {
    
    User user = userService.findById(id);
    String etag = computeETag(user);
    
    if (etag.equals(ifNoneMatch)) {
        return Response.notModified().tag(etag).build();
    }
    
    return Response.ok(user).tag(etag).build();
}
```

## Async Operations

See [Reactive Programming Guide](reactive-programming.md) for async patterns.

## Next Steps

- [Reactive Programming](reactive-programming.md) - Add async support
- [Security Setup](security-setup.md) - Secure your API
- [REST Clients](rest-clients.md) - Call other APIs
- [Common Scenarios](../examples/common-scenarios.md) - More examples

## See Also

- [Jakarta REST Reference](../reference/jakarta-rest.md)
- [REST Response Reference](../reference/rest-response.md)
- [Exception Mapping](../reference/exception-mapping.md)
- [Filters](../reference/filters.md)
