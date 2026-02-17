# HTTP Caching

Quarkus REST provides declarative annotations for setting HTTP caching headers automatically on responses. These annotations simplify cache control configuration without manual header manipulation.

## Capabilities

### Cache Control Annotation

Automatically set Cache-Control headers with fine-grained control over caching directives.

```java { .api }
package org.jboss.resteasy.reactive;

/**
 * Declarative annotation for setting Cache-Control headers.
 * Applied at class or method level to automatically add caching directives.
 *
 * Generates Cache-Control header based on specified parameters.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@interface Cache {
    /** Max age in seconds. Default: -1 (not set) */
    int maxAge() default -1;

    /** Shared max age in seconds (for shared caches). Default: -1 (not set) */
    int sMaxAge() default -1;

    /** Whether response must not be stored. Default: false */
    boolean noStore() default false;

    /** Whether transformations are disallowed. Default: false */
    boolean noTransform() default false;

    /** Whether revalidation is required. Default: false */
    boolean mustRevalidate() default false;

    /** Whether proxy revalidation is required. Default: false */
    boolean proxyRevalidate() default false;

    /** Whether cache is private (not shared). Default: false */
    boolean isPrivate() default false;

    /** Whether cache should not be used without revalidation. Default: false */
    boolean noCache() default false;
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.Cache;
import jakarta.ws.rs.*;

@Path("/api")
public class CachingResource {

    // Cache for 1 hour
    @GET
    @Path("/static-data")
    @Cache(maxAge = 3600)
    public Data getStaticData() {
        return loadStaticData();
    }

    // Cache for 30 minutes, must revalidate
    @GET
    @Path("/products")
    @Cache(maxAge = 1800, mustRevalidate = true)
    public List<Product> getProducts() {
        return loadProducts();
    }

    // Private cache (not shared between users)
    @GET
    @Path("/user-data")
    @Cache(maxAge = 600, isPrivate = true)
    public UserData getUserData() {
        return loadUserData();
    }

    // Shared cache with different max-age for proxies
    @GET
    @Path("/public-content")
    @Cache(maxAge = 3600, sMaxAge = 7200)
    public Content getPublicContent() {
        return loadPublicContent();
    }

    // No transformation allowed
    @GET
    @Path("/images/{id}")
    @Cache(maxAge = 86400, noTransform = true)
    public byte[] getImage(@PathParam("id") String id) {
        return loadImage(id);
    }

    // Proxy must revalidate
    @GET
    @Path("/api-data")
    @Cache(maxAge = 300, proxyRevalidate = true)
    public ApiData getApiData() {
        return loadApiData();
    }

    // Caching with no-cache directive
    @GET
    @Path("/user-profile")
    @Cache(maxAge = 600, noCache = true)
    public Profile getProfile() {
        return loadProfile();
    }

    // Do not store in cache at all
    @GET
    @Path("/sensitive")
    @Cache(noStore = true)
    public SensitiveData getSensitiveData() {
        return loadSensitiveData();
    }

    // Complex caching policy
    @GET
    @Path("/articles/{id}")
    @Cache(maxAge = 1800, sMaxAge = 3600, mustRevalidate = true, noTransform = true)
    public Article getArticle(@PathParam("id") String id) {
        return loadArticle(id);
    }

    private Data loadStaticData() { return new Data(); }
    private List<Product> loadProducts() { return List.of(); }
    private UserData loadUserData() { return new UserData(); }
    private Content loadPublicContent() { return new Content(); }
    private byte[] loadImage(String id) { return new byte[0]; }
    private ApiData loadApiData() { return new ApiData(); }
    private Profile loadProfile() { return new Profile(); }
    private SensitiveData loadSensitiveData() { return new SensitiveData(); }
    private Article loadArticle(String id) { return new Article(); }
}

class Data {}
class Product {}
class UserData {}
class Content {}
class ApiData {}
class Profile {}
class SensitiveData {}
class Article {}
```

### Class-Level Caching

Apply caching policy to all methods in a resource class.

```java
import org.jboss.resteasy.reactive.Cache;
import jakarta.ws.rs.*;

// All methods inherit this cache policy
@Path("/catalog")
@Cache(maxAge = 3600, mustRevalidate = true)
public class CatalogResource {

    // Inherits class-level cache: max-age=3600, must-revalidate
    @GET
    @Path("/categories")
    public List<Category> getCategories() {
        return loadCategories();
    }

    // Override class-level cache for specific method
    @GET
    @Path("/featured")
    @Cache(maxAge = 600)
    public List<Product> getFeatured() {
        return loadFeatured();
    }

    // Disable caching for specific method
    @GET
    @Path("/live-inventory")
    @Cache(noStore = true)
    public Inventory getLiveInventory() {
        return getCurrentInventory();
    }

    private List<Category> loadCategories() { return List.of(); }
    private List<Product> loadFeatured() { return List.of(); }
    private Inventory getCurrentInventory() { return new Inventory(); }
}

class Category {}
class Inventory {}
```

### No-Cache Annotation

Simplified annotation for disabling caching entirely.

```java { .api }
package org.jboss.resteasy.reactive;

/**
 * Annotation to disable caching.
 * Sets Cache-Control: no-cache header.
 *
 * Can optionally specify fields that should not be cached.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@interface NoCache {
    /** Specific fields to mark as no-cache. Default: all fields */
    String[] fields() default {};
}
```

**Usage Examples:**

```java
import org.jboss.resteasy.reactive.NoCache;
import jakarta.ws.rs.*;

@Path("/dynamic")
public class DynamicResource {

    // Completely disable caching
    @GET
    @Path("/real-time-data")
    @NoCache
    public RealTimeData getData() {
        return getCurrentData();
    }

    // No-cache for specific fields
    @GET
    @Path("/user-status")
    @NoCache(fields = {"Set-Cookie", "Authorization"})
    public UserStatus getStatus() {
        return loadUserStatus();
    }

    // Class-level no-cache
    @Path("/admin")
    @NoCache
    public static class AdminResource {

        @GET
        @Path("/dashboard")
        public Dashboard getDashboard() {
            return loadDashboard();
        }

        @GET
        @Path("/stats")
        public Stats getStats() {
            return loadStats();
        }
    }

    private RealTimeData getCurrentData() { return new RealTimeData(); }
    private UserStatus loadUserStatus() { return new UserStatus(); }
    private static Dashboard loadDashboard() { return new Dashboard(); }
    private static Stats loadStats() { return new Stats(); }
}

class RealTimeData {}
class UserStatus {}
class Dashboard {}
class Stats {}
```

## Caching Strategies

### Time-Based Caching

Cache responses for specific durations based on data volatility.

```java
@Path("/content")
public class ContentResource {

    // Static content: cache for 1 day
    @GET
    @Path("/logo")
    @Cache(maxAge = 86400)
    public byte[] getLogo() {
        return loadLogo();
    }

    // Semi-static content: cache for 1 hour
    @GET
    @Path("/menu")
    @Cache(maxAge = 3600)
    public Menu getMenu() {
        return loadMenu();
    }

    // Dynamic content: cache for 5 minutes
    @GET
    @Path("/news")
    @Cache(maxAge = 300)
    public List<NewsItem> getNews() {
        return loadNews();
    }

    // Real-time content: no caching
    @GET
    @Path("/stock-prices")
    @NoCache
    public StockPrices getPrices() {
        return getCurrentPrices();
    }

    private byte[] loadLogo() { return new byte[0]; }
    private Menu loadMenu() { return new Menu(); }
    private List<NewsItem> loadNews() { return List.of(); }
    private StockPrices getCurrentPrices() { return new StockPrices(); }
}

class Menu {}
class NewsItem {}
class StockPrices {}
```

### User-Specific vs Public Caching

Control whether responses can be cached in shared caches (CDNs, proxies).

```java
@Path("/data")
public class DataResource {

    // Public data: can be cached by CDN/proxies
    @GET
    @Path("/public-articles")
    @Cache(maxAge = 3600, sMaxAge = 7200)
    public List<Article> getPublicArticles() {
        return loadPublicArticles();
    }

    // User-specific data: private cache only
    @GET
    @Path("/my-articles")
    @Cache(maxAge = 600, isPrivate = true)
    public List<Article> getMyArticles(@Context SecurityContext sec) {
        return loadUserArticles(sec.getUserPrincipal().getName());
    }

    // Authenticated data: no caching in shared caches
    @GET
    @Path("/dashboard")
    @Cache(maxAge = 300, isPrivate = true, noTransform = true)
    public Dashboard getDashboard(@Context SecurityContext sec) {
        return loadDashboard(sec.getUserPrincipal().getName());
    }

    private List<Article> loadPublicArticles() { return List.of(); }
    private List<Article> loadUserArticles(String user) { return List.of(); }
    private Dashboard loadDashboard(String user) { return new Dashboard(); }
}
```

### Revalidation Requirements

Force clients or proxies to revalidate cached content before using it.

```java
@Path("/resources")
public class RevalidationResource {

    // Must revalidate with origin server
    @GET
    @Path("/critical-data")
    @Cache(maxAge = 600, mustRevalidate = true)
    public CriticalData getCriticalData() {
        return loadCriticalData();
    }

    // Proxies must revalidate
    @GET
    @Path("/api-response")
    @Cache(maxAge = 1800, sMaxAge = 3600, proxyRevalidate = true)
    public ApiResponse getApiResponse() {
        return loadApiResponse();
    }

    // Both client and proxy must revalidate
    @GET
    @Path("/secure-content")
    @Cache(maxAge = 300, mustRevalidate = true, proxyRevalidate = true, isPrivate = true)
    public SecureContent getSecureContent() {
        return loadSecureContent();
    }

    private CriticalData loadCriticalData() { return new CriticalData(); }
    private ApiResponse loadApiResponse() { return new ApiResponse(); }
    private SecureContent loadSecureContent() { return new SecureContent(); }
}

class CriticalData {}
class ApiResponse {}
class SecureContent {}
```

## Integration with Conditional Requests

Caching annotations work with ETags and Last-Modified headers for conditional requests.

```java
import jakarta.ws.rs.core.*;

@Path("/documents")
public class ConditionalCachingResource {

    @GET
    @Path("/{id}")
    @Cache(maxAge = 3600, mustRevalidate = true)
    public Response getDocument(
            @PathParam("id") String id,
            @Context Request request) {

        Document doc = loadDocument(id);
        EntityTag etag = new EntityTag(computeETag(doc));
        Date lastModified = doc.getLastModified();

        // Check if client has current version
        Response.ResponseBuilder builder = request.evaluatePreconditions(lastModified, etag);

        // Return 304 Not Modified if client cache is current
        if (builder != null) {
            return builder
                .cacheControl(createCacheControl())
                .build();
        }

        // Return full document with cache headers
        return Response.ok(doc)
            .tag(etag)
            .lastModified(lastModified)
            .cacheControl(createCacheControl())
            .build();
    }

    private Document loadDocument(String id) {
        return new Document();
    }

    private String computeETag(Document doc) {
        return String.valueOf(doc.hashCode());
    }

    private CacheControl createCacheControl() {
        CacheControl cc = new CacheControl();
        cc.setMaxAge(3600);
        cc.setMustRevalidate(true);
        return cc;
    }
}

class Document {
    Date getLastModified() { return new Date(); }
}
```

## Generated Cache-Control Headers

The annotations generate standard HTTP Cache-Control headers:

```java
// @Cache(maxAge = 3600)
// Generates: Cache-Control: max-age=3600

// @Cache(maxAge = 1800, mustRevalidate = true)
// Generates: Cache-Control: max-age=1800, must-revalidate

// @Cache(maxAge = 3600, sMaxAge = 7200, isPrivate = true)
// Generates: Cache-Control: private, max-age=3600, s-maxage=7200

// @Cache(noStore = true)
// Generates: Cache-Control: no-store

// @NoCache
// Generates: Cache-Control: no-cache

// @NoCache(fields = {"Set-Cookie"})
// Generates: Cache-Control: no-cache="Set-Cookie"

// @Cache(noCache = true)
// Generates: Cache-Control: no-cache
```

## Best Practices

1. **Static Assets**: Use long max-age (days/months) for immutable content
2. **API Responses**: Use shorter max-age (minutes/hours) with must-revalidate
3. **User-Specific Data**: Always use isPrivate=true
4. **Sensitive Data**: Use noStore=true or @NoCache
5. **CDN Content**: Set both maxAge (client) and sMaxAge (CDN) appropriately
6. **Real-time Data**: Use @NoCache to prevent any caching
7. **Critical Data**: Use mustRevalidate to ensure freshness checks

```java
@Path("/best-practices")
public class BestPracticesResource {

    // Immutable static asset
    @GET
    @Path("/assets/{hash}/bundle.js")
    @Cache(maxAge = 31536000) // 1 year
    public byte[] getHashedAsset(@PathParam("hash") String hash) {
        return loadAsset(hash);
    }

    // API with CDN
    @GET
    @Path("/public-api/data")
    @Cache(maxAge = 300, sMaxAge = 600, mustRevalidate = true)
    public ApiData getPublicApiData() {
        return loadPublicApiData();
    }

    // User-specific API
    @GET
    @Path("/user/settings")
    @Cache(maxAge = 300, isPrivate = true, mustRevalidate = true)
    public Settings getUserSettings() {
        return loadUserSettings();
    }

    // No caching for sensitive operations
    @POST
    @Path("/payments")
    @NoCache
    public PaymentResult processPayment(PaymentRequest req) {
        return process(req);
    }

    private byte[] loadAsset(String hash) { return new byte[0]; }
    private ApiData loadPublicApiData() { return new ApiData(); }
    private Settings loadUserSettings() { return new Settings(); }
    private PaymentResult process(PaymentRequest req) { return new PaymentResult(); }
}

class Settings {}
class PaymentRequest {}
class PaymentResult {}
```
