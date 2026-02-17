# WebSocket Support

Quarkus REST provides WebSocket support through Vert.x integration, allowing REST endpoints to upgrade to WebSocket connections with automatic parameter extraction.

## WebSocket Parameter Extraction

Inject Vert.x ServerWebSocket into REST resource methods for WebSocket handling.

### VertxWebSocketParamExtractor

Parameter extractor for Vert.x ServerWebSocket parameters.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.websocket;

class VertxWebSocketParamExtractor implements ParameterExtractor {
    Object extractParameter(ResteasyReactiveRequestContext requestContext);
}
```

The extractor handles asynchronous WebSocket extraction from the request context.

### ServerWebSocket Interface

The Vert.x ServerWebSocket interface provides WebSocket functionality.

```java { .api }
package io.vertx.core.http;

interface ServerWebSocket extends WebSocketBase {
    String uri();
    String path();
    String query();
    MultiMap headers();
    SocketAddress remoteAddress();

    ServerWebSocket handler(Handler<Buffer> handler);
    ServerWebSocket endHandler(Handler<Void> endHandler);
    ServerWebSocket exceptionHandler(Handler<Throwable> handler);
    ServerWebSocket closeHandler(Handler<Void> handler);

    Future<Void> writeTextMessage(String text);
    Future<Void> writeBinaryMessage(Buffer data);
    Future<Void> writePing(Buffer data);
    Future<Void> writePong(Buffer data);

    Future<Void> close();
    Future<Void> close(short statusCode);
    Future<Void> close(short statusCode, String reason);
}
```

## WebSocket Endpoints

Create WebSocket endpoints by injecting ServerWebSocket parameters.

**Usage**:

```java
import io.vertx.core.http.ServerWebSocket;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/websocket")
public class WebSocketResource {

    @GET
    @Path("/echo")
    public void echo(ServerWebSocket webSocket) {
        // Handle incoming messages
        webSocket.handler(buffer -> {
            String message = buffer.toString();
            System.out.println("Received: " + message);

            // Echo message back
            webSocket.writeTextMessage("Echo: " + message);
        });

        // Handle connection close
        webSocket.closeHandler(v -> {
            System.out.println("WebSocket closed");
        });

        // Handle errors
        webSocket.exceptionHandler(err -> {
            System.err.println("WebSocket error: " + err.getMessage());
        });
    }
}
```

## WebSocket Handler Chain

WebSocket endpoints use a specialized handler chain.

### VertxWebSocketRestHandler

Handler chain customizer for WebSocket endpoint handling.

```java { .api }
package io.quarkus.resteasy.reactive.server.runtime.websocket;

class VertxWebSocketRestHandler implements HandlerChainCustomizer {
    List<ServerRestHandler> handlers(
        Phase phase,
        ResourceClass resourceClass,
        ServerResourceMethod resourceMethod
    );
}
```

The customizer adds WebSocket-specific handlers during the AFTER_METHOD_INVOKE phase.

## WebSocket with Path Parameters

WebSocket endpoints support standard JAX-RS path parameters.

**Usage**:

```java
@Path("/websocket/chat")
public class ChatWebSocketResource {

    @GET
    @Path("/{room}")
    public void joinRoom(
        @PathParam("room") String room,
        ServerWebSocket webSocket
    ) {
        System.out.println("Client joined room: " + room);

        webSocket.handler(buffer -> {
            String message = buffer.toString();
            // Broadcast to room
            broadcastToRoom(room, message);
        });

        webSocket.closeHandler(v -> {
            System.out.println("Client left room: " + room);
        });
    }

    private void broadcastToRoom(String room, String message) {
        // Implementation for broadcasting
    }
}
```

## WebSocket Query Parameters

Access query parameters from the WebSocket upgrade request.

**Usage**:

```java
@Path("/websocket/stream")
public class StreamWebSocketResource {

    @GET
    public void stream(
        @QueryParam("token") String token,
        ServerWebSocket webSocket
    ) {
        // Validate token
        if (!isValidToken(token)) {
            webSocket.close((short) 401, "Invalid token");
            return;
        }

        // Start streaming data
        startStreaming(webSocket);
    }

    private void startStreaming(ServerWebSocket webSocket) {
        // Stream data to client
        vertx.setPeriodic(1000, timerId -> {
            if (!webSocket.isClosed()) {
                webSocket.writeTextMessage("Data at " + System.currentTimeMillis());
            } else {
                vertx.cancelTimer(timerId);
            }
        });
    }
}
```

## WebSocket Headers

Access HTTP headers from the WebSocket upgrade request.

**Usage**:

```java
@Path("/websocket/secure")
public class SecureWebSocketResource {

    @GET
    public void connect(ServerWebSocket webSocket) {
        // Access headers
        MultiMap headers = webSocket.headers();
        String authHeader = headers.get("Authorization");
        String userAgent = headers.get("User-Agent");

        System.out.println("Authorization: " + authHeader);
        System.out.println("User-Agent: " + userAgent);

        if (authHeader == null || !validateAuth(authHeader)) {
            webSocket.close((short) 401, "Unauthorized");
            return;
        }

        // Handle authenticated connection
        handleConnection(webSocket);
    }
}
```

## Binary WebSocket Messages

Handle binary data over WebSocket connections.

**Usage**:

```java
import io.vertx.core.buffer.Buffer;

@Path("/websocket/binary")
public class BinaryWebSocketResource {

    @GET
    public void binary(ServerWebSocket webSocket) {
        webSocket.handler(buffer -> {
            // Receive binary data
            byte[] bytes = buffer.getBytes();
            System.out.println("Received " + bytes.length + " bytes");

            // Process binary data
            Buffer response = processData(buffer);

            // Send binary response
            webSocket.writeBinaryMessage(response);
        });
    }

    private Buffer processData(Buffer input) {
        // Process binary data
        return Buffer.buffer(input.getBytes());
    }
}
```

## WebSocket Ping/Pong

Implement keep-alive using ping/pong frames.

**Usage**:

```java
@Path("/websocket/keepalive")
public class KeepAliveWebSocketResource {

    @Inject
    Vertx vertx;

    @GET
    public void keepAlive(ServerWebSocket webSocket) {
        // Send periodic pings
        long timerId = vertx.setPeriodic(30000, id -> {
            if (!webSocket.isClosed()) {
                webSocket.writePing(Buffer.buffer("ping"))
                    .onFailure(err -> {
                        System.err.println("Ping failed: " + err.getMessage());
                        vertx.cancelTimer(id);
                    });
            } else {
                vertx.cancelTimer(id);
            }
        });

        webSocket.pongHandler(buffer -> {
            System.out.println("Pong received");
        });

        webSocket.closeHandler(v -> {
            vertx.cancelTimer(timerId);
        });
    }
}
```

## WebSocket with CDI Integration

Inject CDI beans into WebSocket endpoints.

**Usage**:

```java
import jakarta.inject.Inject;

@Path("/websocket/service")
public class ServiceWebSocketResource {

    @Inject
    MessageService messageService;

    @Inject
    UserService userService;

    @GET
    public void connect(@QueryParam("userId") Long userId, ServerWebSocket webSocket) {
        User user = userService.findById(userId);
        if (user == null) {
            webSocket.close((short) 404, "User not found");
            return;
        }

        webSocket.handler(buffer -> {
            String message = buffer.toString();

            // Use injected service
            String processed = messageService.process(user, message);

            webSocket.writeTextMessage(processed);
        });
    }
}
```

## WebSocket Connection Management

Manage multiple WebSocket connections.

**Usage**:

```java
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class WebSocketConnectionManager {

    private final Map<String, ServerWebSocket> connections = new ConcurrentHashMap<>();

    public void register(String clientId, ServerWebSocket webSocket) {
        connections.put(clientId, webSocket);

        webSocket.closeHandler(v -> {
            connections.remove(clientId);
        });
    }

    public void broadcast(String message) {
        connections.values().forEach(ws -> {
            if (!ws.isClosed()) {
                ws.writeTextMessage(message);
            }
        });
    }

    public void sendTo(String clientId, String message) {
        ServerWebSocket ws = connections.get(clientId);
        if (ws != null && !ws.isClosed()) {
            ws.writeTextMessage(message);
        }
    }
}

@Path("/websocket/managed")
public class ManagedWebSocketResource {

    @Inject
    WebSocketConnectionManager manager;

    @GET
    public void connect(@QueryParam("clientId") String clientId, ServerWebSocket webSocket) {
        manager.register(clientId, webSocket);

        webSocket.handler(buffer -> {
            String message = buffer.toString();
            // Broadcast to all clients
            manager.broadcast(clientId + ": " + message);
        });
    }
}
```

## WebSocket with Security

Integrate WebSocket endpoints with Quarkus security.

**Usage**:

```java
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;

@Path("/websocket/admin")
public class AdminWebSocketResource {

    @Inject
    SecurityIdentity securityIdentity;

    @GET
    @RolesAllowed("admin")
    public void adminConnect(ServerWebSocket webSocket) {
        String username = securityIdentity.getPrincipal().getName();
        System.out.println("Admin connected: " + username);

        webSocket.handler(buffer -> {
            String message = buffer.toString();
            // Handle admin messages
            handleAdminMessage(username, message, webSocket);
        });
    }

    private void handleAdminMessage(String admin, String message, ServerWebSocket webSocket) {
        // Admin-specific logic
        webSocket.writeTextMessage("Admin " + admin + " processed: " + message);
    }
}
```

## WebSocket Error Handling

Handle errors and close connections gracefully.

**Usage**:

```java
@Path("/websocket/robust")
public class RobustWebSocketResource {

    private static final Logger LOG = Logger.getLogger(RobustWebSocketResource.class);

    @GET
    public void connect(ServerWebSocket webSocket) {
        webSocket.handler(buffer -> {
            try {
                String message = buffer.toString();
                processMessage(message, webSocket);
            } catch (Exception e) {
                LOG.error("Error processing message", e);
                webSocket.writeTextMessage("Error: " + e.getMessage());
            }
        });

        webSocket.exceptionHandler(err -> {
            LOG.error("WebSocket exception", err);
            if (!webSocket.isClosed()) {
                webSocket.close((short) 1011, "Server error");
            }
        });

        webSocket.closeHandler(v -> {
            LOG.info("WebSocket closed normally");
            cleanup();
        });
    }

    private void processMessage(String message, ServerWebSocket webSocket) {
        // Message processing logic
    }

    private void cleanup() {
        // Cleanup resources
    }
}
```

## WebSocket Close Codes

Use standard WebSocket close codes for proper connection termination.

**Common Close Codes**:

```java
@Path("/websocket/codes")
public class CloseCodeWebSocketResource {

    @GET
    public void connect(ServerWebSocket webSocket) {
        webSocket.handler(buffer -> {
            String message = buffer.toString();

            if (message.equals("quit")) {
                // Normal closure
                webSocket.close((short) 1000, "Normal closure");
            } else if (message.equals("error")) {
                // Server error
                webSocket.close((short) 1011, "Server error");
            } else if (message.contains("invalid")) {
                // Invalid data
                webSocket.close((short) 1003, "Invalid data");
            }
        });
    }
}

// Standard close codes:
// 1000 - Normal closure
// 1001 - Going away
// 1002 - Protocol error
// 1003 - Unsupported data
// 1006 - Abnormal closure
// 1007 - Invalid payload
// 1008 - Policy violation
// 1009 - Message too big
// 1011 - Server error
```

## WebSocket Subprotocol Negotiation

Negotiate WebSocket subprotocols.

**Usage**:

```java
@Path("/websocket/protocol")
public class SubprotocolWebSocketResource {

    @GET
    public void connect(ServerWebSocket webSocket) {
        // Check requested subprotocol
        String subprotocol = webSocket.headers().get("Sec-WebSocket-Protocol");

        if ("chat".equals(subprotocol)) {
            // Accept chat subprotocol
            handleChatProtocol(webSocket);
        } else if ("binary".equals(subprotocol)) {
            // Accept binary subprotocol
            handleBinaryProtocol(webSocket);
        } else {
            // Reject if no supported subprotocol
            webSocket.close((short) 1002, "Unsupported protocol");
        }
    }

    private void handleChatProtocol(ServerWebSocket webSocket) {
        // Handle chat protocol
    }

    private void handleBinaryProtocol(ServerWebSocket webSocket) {
        // Handle binary protocol
    }
}
```

## Testing WebSocket Endpoints

Test WebSocket functionality.

**Usage**:

```java
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocket;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WebSocketTest {

    @TestHTTPResource("/websocket/echo")
    URI echoUri;

    @Inject
    Vertx vertx;

    @Test
    public void testEcho() throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();

        vertx.createHttpClient()
            .webSocket(echoUri.getPort(), echoUri.getHost(), echoUri.getPath())
            .onComplete(ar -> {
                if (ar.succeeded()) {
                    WebSocket ws = ar.result();

                    ws.handler(buffer -> {
                        future.complete(buffer.toString());
                    });

                    ws.writeTextMessage("Hello");
                } else {
                    future.completeExceptionally(ar.cause());
                }
            });

        String response = future.get(5, TimeUnit.SECONDS);
        assertEquals("Echo: Hello", response);
    }
}
```
