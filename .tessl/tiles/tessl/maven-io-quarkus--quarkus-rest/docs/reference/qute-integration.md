# Qute Template Integration

Quarkus REST integrates with Qute, Quarkus's type-safe template engine, providing convenient utilities for returning template-based HTML responses from REST endpoints.

**Note:** This feature requires the `quarkus-resteasy-reactive-qute` extension.

## Capabilities

### RestTemplate Utility

Static utility class for accessing Qute templates in REST resource methods. Automatically resolves templates based on resource class and method names.

```java { .api }
package io.quarkus.resteasy.reactive.qute;

import io.quarkus.qute.TemplateInstance;

/**
 * Utility class for accessing Qute templates from REST resource methods.
 * Templates are automatically resolved based on the resource class and method names.
 *
 * Template location convention: src/main/resources/templates/{ClassName}/{methodName}.html
 * For example, a method named "hello" in class "GreetingResource"
 * would resolve to templates/GreetingResource/hello.html
 */
public final class RestTemplate {

    /**
     * Get template instance for current resource method and add data with a key.
     *
     * @param name The data key
     * @param value The data value to pass to the template
     * @return TemplateInstance ready to be rendered
     */
    public static TemplateInstance data(String name, Object value);

    /**
     * Get template instance for current resource method and add data object.
     * The object's properties will be accessible directly in the template.
     *
     * @param data The data object to pass to the template
     * @return TemplateInstance ready to be rendered
     */
    public static TemplateInstance data(Object data);
}
```

## Usage

### Basic Template Rendering

```java
import io.quarkus.resteasy.reactive.qute.RestTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingResource {

    // Returns template from: templates/GreetingResource/hello.html
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance hello(@QueryParam("name") String name) {
        return RestTemplate.data("name", name != null ? name : "World");
    }

    // Returns template from: templates/GreetingResource/goodbye.html
    @GET
    @Path("/goodbye")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance goodbye() {
        return RestTemplate.data("message", "See you later!");
    }
}
```

**Template file** (`src/main/resources/templates/GreetingResource/hello.html`):

```html
<!DOCTYPE html>
<html>
<head>
    <title>Greeting</title>
</head>
<body>
    <h1>Hello, {name}!</h1>
    <p>Welcome to Quarkus REST with Qute templates.</p>
</body>
</html>
```

### Passing Data Objects

```java
import io.quarkus.resteasy.reactive.qute.RestTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/users")
public class UserResource {

    @Inject
    UserService userService;

    // Returns template from: templates/UserResource/list.html
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        List<User> users = userService.findAll();
        // Pass entire object - properties accessible in template
        return RestTemplate.data(users);
    }

    // Returns template from: templates/UserResource/details.html
    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance details(@PathParam("id") long id) {
        User user = userService.findById(id);
        return RestTemplate.data(user);
    }

    // Returns template from: templates/UserResource/profile.html
    @GET
    @Path("/{id}/profile")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance profile(@PathParam("id") long id) {
        User user = userService.findById(id);
        UserProfile profile = new UserProfile(user, userService.getStats(id));
        return RestTemplate.data(profile);
    }
}

class User {
    public String name;
    public String email;
}

class UserProfile {
    public User user;
    public UserStats stats;

    public UserProfile(User user, UserStats stats) {
        this.user = user;
        this.stats = stats;
    }
}

class UserStats {
    public int loginCount;
    public String lastLogin;
}

interface UserService {
    List<User> findAll();
    User findById(long id);
    UserStats getStats(long id);
}
```

**Template file** (`src/main/resources/templates/UserResource/list.html`):

```html
<!DOCTYPE html>
<html>
<head>
    <title>Users</title>
</head>
<body>
    <h1>User List</h1>
    <ul>
    {#for user in data}
        <li>{user.name} - {user.email}</li>
    {/for}
    </ul>
</body>
</html>
```

**Template file** (`src/main/resources/templates/UserResource/details.html`):

```html
<!DOCTYPE html>
<html>
<head>
    <title>User Details</title>
</head>
<body>
    <h1>{data.name}</h1>
    <p>Email: {data.email}</p>
</body>
</html>
```

**Template file** (`src/main/resources/templates/UserResource/profile.html`):

```html
<!DOCTYPE html>
<html>
<head>
    <title>User Profile</title>
</head>
<body>
    <h1>{data.user.name}'s Profile</h1>
    <p>Email: {data.user.email}</p>
    <h2>Statistics</h2>
    <p>Login count: {data.stats.loginCount}</p>
    <p>Last login: {data.stats.lastLogin}</p>
</body>
</html>
```

### Multiple Data Parameters

```java
import io.quarkus.resteasy.reactive.qute.RestTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/dashboard")
public class DashboardResource {

    @Inject
    DashboardService dashboardService;

    // Returns template from: templates/DashboardResource/index.html
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        // Pass multiple data values with keys
        return RestTemplate.data("users", dashboardService.getUserCount())
                           .data("orders", dashboardService.getOrderCount())
                           .data("revenue", dashboardService.getRevenue())
                           .data("date", java.time.LocalDate.now());
    }

    // Returns template from: templates/DashboardResource/report.html
    @GET
    @Path("/report")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance report(@QueryParam("month") String month) {
        DashboardData data = dashboardService.getMonthlyData(month);

        // Chain multiple data() calls
        return RestTemplate.data("month", month)
                           .data("summary", data.summary)
                           .data("details", data.details)
                           .data("charts", data.charts);
    }
}

interface DashboardService {
    int getUserCount();
    int getOrderCount();
    double getRevenue();
    DashboardData getMonthlyData(String month);
}

class DashboardData {
    public String summary;
    public Object details;
    public Object charts;
}
```

**Template file** (`src/main/resources/templates/DashboardResource/index.html`):

```html
<!DOCTYPE html>
<html>
<head>
    <title>Dashboard</title>
</head>
<body>
    <h1>Dashboard</h1>
    <div class="stats">
        <div>Users: {users}</div>
        <div>Orders: {orders}</div>
        <div>Revenue: ${revenue}</div>
        <div>Date: {date}</div>
    </div>
</body>
</html>
```

### Reactive Templates with Uni

```java
import io.quarkus.resteasy.reactive.qute.RestTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/async")
public class AsyncTemplateResource {

    @Inject
    AsyncDataService dataService;

    // Reactive template rendering
    @GET
    @Path("/data")
    @Produces(MediaType.TEXT_HTML)
    public Uni<TemplateInstance> asyncData() {
        return dataService.fetchDataAsync()
            .onItem().transform(data ->
                RestTemplate.data("items", data)
            );
    }

    // Combine multiple async operations
    @GET
    @Path("/combined")
    @Produces(MediaType.TEXT_HTML)
    public Uni<TemplateInstance> combined() {
        Uni<String> titleUni = dataService.fetchTitle();
        Uni<List<String>> itemsUni = dataService.fetchItems();

        return Uni.combine().all().unis(titleUni, itemsUni)
            .asTuple()
            .onItem().transform(tuple ->
                RestTemplate.data("title", tuple.getItem1())
                           .data("items", tuple.getItem2())
            );
    }
}

interface AsyncDataService {
    Uni<List<String>> fetchDataAsync();
    Uni<String> fetchTitle();
    Uni<List<String>> fetchItems();
}
```

## Template Naming Convention

Templates are resolved using the following pattern:

```
src/main/resources/templates/{ResourceClassName}/{methodName}.html
```

Examples:
- `GreetingResource.hello()` → `templates/GreetingResource/hello.html`
- `UserResource.details()` → `templates/UserResource/details.html`
- `DashboardResource.index()` → `templates/DashboardResource/index.html`

**Note:** The file extension (.html) can be changed based on your Qute configuration. Other formats (e.g., .txt, .xml) are supported.

## Requirements

This feature requires the `quarkus-resteasy-reactive-qute` extension:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-resteasy-reactive-qute</artifactId>
</dependency>
```

## Integration with Qute Features

`RestTemplate` integrates with all standard Qute features:

- **Type-safe templates**: Full IDE support for template expressions
- **Template extensions**: Custom methods and formatters
- **Template fragments**: Reusable template components
- **Template variants**: Locale-specific or content-type specific variants
- **Template caching**: Automatic caching and hot-reload in dev mode

See the [Quarkus Qute documentation](https://quarkus.io/guides/qute) for complete template syntax and features.

## Content Negotiation

Combine with `@Produces` for different content types:

```java
@Path("/content")
public class ContentResource {

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance html() {
        return RestTemplate.data("format", "HTML");
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public TemplateInstance text() {
        return RestTemplate.data("format", "Plain Text");
    }
}
```
