# Quick Start Guide

This guide will help you create your first Quarkus REST application.

## Prerequisites

- JDK 11+ installed
- Maven 3.8+ or Gradle 7+
- IDE (IntelliJ IDEA, VS Code, or Eclipse)

## Create a New Project

### Using Maven

```bash
mvn io.quarkus:quarkus-maven-plugin:3.15.7:create \
    -DprojectGroupId=com.example \
    -DprojectArtifactId=rest-quickstart \
    -Dextensions="quarkus-rest,quarkus-rest-jackson"
cd rest-quickstart
```

### Using Gradle

```bash
mvn io.quarkus:quarkus-maven-plugin:3.15.7:create \
    -DprojectGroupId=com.example \
    -DprojectArtifactId=rest-quickstart \
    -Dextensions="quarkus-rest,quarkus-rest-jackson" \
    -DbuildTool=gradle
cd rest-quickstart
```

## Create Your First Endpoint

Create `src/main/java/com/example/GreetingResource.java`:

```java
package com.example;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus REST!";
    }
}
```

## Run the Application

### Development Mode

```bash
./mvnw quarkus:dev
```

Or with Gradle:

```bash
./gradlew quarkusDev
```

### Test the Endpoint

```bash
curl http://localhost:8080/hello
```

Output: `Hello from Quarkus REST!`

## Add Path Parameters

Update `GreetingResource.java`:

```java
import jakarta.ws.rs.PathParam;

@Path("/hello")
public class GreetingResource {

    @GET
    @Path("/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello(@PathParam("name") String name) {
        return "Hello " + name + "!";
    }
}
```

Test:

```bash
curl http://localhost:8080/hello/Alice
```

Output: `Hello Alice!`

## Return JSON

Create `src/main/java/com/example/Greeting.java`:

```java
package com.example;

public class Greeting {
    public String message;
    public long timestamp;

    public Greeting(String message) {
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
}
```

Update `GreetingResource.java`:

```java
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingResource {

    @GET
    @Path("/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Greeting hello(@PathParam("name") String name) {
        return new Greeting("Hello " + name + "!");
    }
}
```

Test:

```bash
curl http://localhost:8080/hello/Alice
```

Output: `{"message":"Hello Alice!","timestamp":1706000000000}`

## Handle POST Requests

Add to `GreetingResource.java`:

```java
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Consumes;

@POST
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Greeting create(Greeting greeting) {
    greeting.timestamp = System.currentTimeMillis();
    return greeting;
}
```

Test:

```bash
curl -X POST http://localhost:8080/hello \
  -H "Content-Type: application/json" \
  -d '{"message":"Custom message"}'
```

## Add Query Parameters

```java
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.DefaultValue;

@GET
@Produces(MediaType.APPLICATION_JSON)
public Greeting greet(
    @QueryParam("name") @DefaultValue("World") String name,
    @QueryParam("formal") @DefaultValue("false") boolean formal) {
    
    String message = formal 
        ? "Good day, " + name 
        : "Hello " + name + "!";
    
    return new Greeting(message);
}
```

Test:

```bash
curl "http://localhost:8080/hello?name=Alice&formal=true"
```

## Package and Run

### Build JAR

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

### Build Native Executable

```bash
./mvnw package -Dnative
./target/rest-quickstart-1.0.0-SNAPSHOT-runner
```

## Next Steps

- [Building REST APIs](building-rest-apis.md) - Learn more REST patterns
- [Reactive Programming](reactive-programming.md) - Add async/reactive support
- [Security Setup](security-setup.md) - Secure your endpoints
- [Common Scenarios](../examples/common-scenarios.md) - Real-world examples

## See Also

- [Jakarta REST Reference](../reference/jakarta-rest.md)
- [Configuration Reference](../reference/configuration.md)
- [REST Response API](../reference/rest-response.md)
