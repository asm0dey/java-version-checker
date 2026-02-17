# Jackson Serialization Customization

Quarkus REST provides Jackson-specific annotations for per-method serialization/deserialization configuration and role-based field filtering without affecting global Jackson configuration.

**Note:** These features require the `quarkus-rest-jackson` extension and are marked as experimental.

## Capabilities

### Custom Serialization

Configure Jackson serialization per resource method without modifying global ObjectMapper configuration.

```java { .api }
package io.quarkus.resteasy.reactive.jackson;

import java.lang.reflect.Type;
import java.util.function.BiFunction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * Annotation for per-method Jackson serialization configuration.
 * Allows customizing JSON serialization for specific REST endpoints
 * without affecting global Jackson configuration.
 *
 * Applied at method or type level.
 * Marked as @Experimental - API may change in future releases.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Experimental
@interface CustomSerialization {
    /**
     * BiFunction that creates custom ObjectWriter from global ObjectMapper and target type.
     *
     * Requirements:
     * - MUST have a no-args constructor
     * - Should be stateless or only use state initialized in constructor
     * - MUST NOT modify the ObjectMapper instance (it's the global instance)
     * - Cached per resource method after first creation
     *
     * @return BiFunction class that produces custom ObjectWriter
     */
    Class<? extends BiFunction<ObjectMapper, Type, ObjectWriter>> value();
}
```

**Usage Examples:**

```java
import io.quarkus.resteasy.reactive.jackson.CustomSerialization;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import jakarta.ws.rs.*;
import java.lang.reflect.Type;
import java.util.function.BiFunction;

@Path("/users")
public class UserResource {

    // Custom serialization for sensitive data
    @GET
    @Path("/{id}")
    @Produces("application/json")
    @CustomSerialization(SensitiveDataWriter.class)
    public User getUser(@PathParam("id") long id) {
        return userService.findById(id);
    }

    // Different serialization for admin endpoint
    @GET
    @Path("/{id}/full")
    @Produces("application/json")
    @CustomSerialization(FullDataWriter.class)
    public User getUserFull(@PathParam("id") long id) {
        return userService.findById(id);
    }

    // Custom date format serialization
    @GET
    @Path("/created-today")
    @Produces("application/json")
    @CustomSerialization(CustomDateFormatWriter.class)
    public List<User> getUsersCreatedToday() {
        return userService.findCreatedToday();
    }
}

// Filter sensitive fields for regular users
public class SensitiveDataWriter implements BiFunction<ObjectMapper, Type, ObjectWriter> {

    @Override
    public ObjectWriter apply(ObjectMapper mapper, Type type) {
        // Create filter that excludes sensitive fields
        SimpleBeanPropertyFilter filter = SimpleBeanPropertyFilter
            .serializeAllExcept("password", "ssn", "creditCard");

        FilterProvider filters = new SimpleFilterProvider()
            .addFilter("sensitiveFilter", filter);

        return mapper.writer(filters);
    }
}

// Include all fields for admin view
public class FullDataWriter implements BiFunction<ObjectMapper, Type, ObjectWriter> {

    @Override
    public ObjectWriter apply(ObjectMapper mapper, Type type) {
        // Return writer with all features enabled
        return mapper.writer()
            .withDefaultPrettyPrinter();
    }
}

// Custom date formatting
public class CustomDateFormatWriter implements BiFunction<ObjectMapper, Type, ObjectWriter> {

    @Override
    public ObjectWriter apply(ObjectMapper mapper, Type type) {
        // Create writer with custom date format
        return mapper.writer()
            .with(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    }
}

// Type-level custom serialization (applies to all methods)
@Path("/reports")
@CustomSerialization(ReportWriter.class)
public class ReportResource {

    @GET
    @Path("/daily")
    public Report getDailyReport() {
        return reportService.getDaily();
    }

    @GET
    @Path("/monthly")
    public Report getMonthlyReport() {
        return reportService.getMonthly();
    }
}

public class ReportWriter implements BiFunction<ObjectMapper, Type, ObjectWriter> {

    @Override
    public ObjectWriter apply(ObjectMapper mapper, Type type) {
        return mapper.writer()
            .withView(ReportViews.Summary.class);
    }
}

class User {}
class Report {}
class ReportViews {
    static class Summary {}
}
interface UserService {
    User findById(long id);
    List<User> findCreatedToday();
}
interface ReportService {
    Report getDaily();
    Report getMonthly();
}
```

### Custom Deserialization

Configure Jackson deserialization per resource method without modifying global ObjectMapper configuration.

```java { .api }
package io.quarkus.resteasy.reactive.jackson;

import java.lang.reflect.Type;
import java.util.function.BiFunction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

/**
 * Annotation for per-method Jackson deserialization configuration.
 * Allows customizing JSON deserialization for specific REST endpoints
 * without affecting global Jackson configuration.
 *
 * Applied at method or type level.
 * Marked as @Experimental - API may change in future releases.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Experimental
@interface CustomDeserialization {
    /**
     * BiFunction that creates custom ObjectReader from global ObjectMapper and target type.
     *
     * Requirements:
     * - MUST have a no-args constructor
     * - Should be stateless or only use state initialized in constructor
     * - MUST NOT modify the ObjectMapper instance (it's the global instance)
     * - Cached per resource method after first creation
     *
     * @return BiFunction class that produces custom ObjectReader
     */
    Class<? extends BiFunction<ObjectMapper, Type, ObjectReader>> value();
}
```

**Usage Examples:**

```java
import io.quarkus.resteasy.reactive.jackson.CustomDeserialization;
import com.fasterxml.jackson.databind.*;
import jakarta.ws.rs.*;
import java.lang.reflect.Type;
import java.util.function.BiFunction;

@Path("/data")
public class DataResource {

    // Custom deserialization for lenient parsing
    @POST
    @Path("/import")
    @Consumes("application/json")
    @CustomDeserialization(LenientReader.class)
    public Response importData(DataImport data) {
        processImport(data);
        return Response.ok().build();
    }

    // Strict deserialization for validation
    @POST
    @Path("/validate")
    @Consumes("application/json")
    @CustomDeserialization(StrictReader.class)
    public Response validateData(DataImport data) {
        return Response.ok("Valid").build();
    }

    // Custom date parsing
    @POST
    @Path("/legacy")
    @Consumes("application/json")
    @CustomDeserialization(LegacyFormatReader.class)
    public Response importLegacy(LegacyData data) {
        processLegacy(data);
        return Response.ok().build();
    }

    private void processImport(DataImport data) {}
    private void processLegacy(LegacyData data) {}
}

// Lenient reader - ignores unknown properties
public class LenientReader implements BiFunction<ObjectMapper, Type, ObjectReader> {

    @Override
    public ObjectReader apply(ObjectMapper mapper, Type type) {
        return mapper.readerFor(mapper.constructType(type))
            .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .with(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
    }
}

// Strict reader - fails on any unknown property
public class StrictReader implements BiFunction<ObjectMapper, Type, ObjectReader> {

    @Override
    public ObjectReader apply(ObjectMapper mapper, Type type) {
        return mapper.readerFor(mapper.constructType(type))
            .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .with(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .with(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
    }
}

// Legacy date format reader
public class LegacyFormatReader implements BiFunction<ObjectMapper, Type, ObjectReader> {

    @Override
    public ObjectReader apply(ObjectMapper mapper, Type type) {
        return mapper.readerFor(mapper.constructType(type))
            .with(new java.text.SimpleDateFormat("dd/MM/yyyy"));
    }
}

class DataImport {}
class LegacyData {}
```

### Role-Based Field Filtering

Automatically filter JSON fields based on user roles using security annotations.

```java { .api }
package io.quarkus.resteasy.reactive.jackson;

/**
 * Annotation for role-based field filtering in JSON serialization.
 * Fields annotated with @SecureField will only be included in JSON
 * if the current user has one of the specified roles.
 *
 * Applied to fields or getters of POJOs returned by REST methods.
 * Works with Quarkus security integration.
 *
 * Warning: Does not work with jakarta.ws.rs.core.Response return types.
 * Use org.jboss.resteasy.reactive.RestResponse instead.
 *
 * Marked as @Experimental - API may change in future releases.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
@Experimental
@interface SecureField {
    /**
     * Roles allowed to see this field in serialized JSON.
     * Field will be omitted if current user doesn't have any of these roles.
     */
    String[] rolesAllowed();
}

/**
 * Enables secure serialization for a method or class.
 * When applied, @SecureField annotations on response types are processed,
 * even if the class is annotated with @DisableSecureSerialization.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Experimental
@interface EnableSecureSerialization {
}

/**
 * Disables secure serialization for a method or class.
 * When applied, all @SecureField annotations are ignored
 * and serialization proceeds normally.
 *
 * At class level: applies to all methods in the class.
 * At method level: applies only to that method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Experimental
@interface DisableSecureSerialization {
}
```

**Usage Examples:**

```java
import io.quarkus.resteasy.reactive.jackson.SecureField;
import io.quarkus.resteasy.reactive.jackson.EnableSecureSerialization;
import io.quarkus.resteasy.reactive.jackson.DisableSecureSerialization;
import org.jboss.resteasy.reactive.RestResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;

// POJO with role-based field filtering
public class UserAccount {
    public String username;
    public String email;

    // Only visible to admin role
    @SecureField(rolesAllowed = "admin")
    public String ssn;

    // Only visible to admin and manager roles
    @SecureField(rolesAllowed = {"admin", "manager"})
    public Double salary;

    // Only visible to admin role
    @SecureField(rolesAllowed = "admin")
    public String internalNotes;

    // Always visible (no @SecureField)
    public String department;
}

@Path("/accounts")
public class AccountResource {

    // Regular users see: username, email, department
    // Admins see all fields including ssn, salary, internalNotes
    @GET
    @Path("/{id}")
    public UserAccount getAccount(@PathParam("id") long id) {
        return accountService.findById(id);
    }

    // Using RestResponse (required for @SecureField to work)
    @GET
    @Path("/{id}/details")
    public RestResponse<UserAccount> getAccountDetails(@PathParam("id") long id) {
        UserAccount account = accountService.findById(id);
        return RestResponse.ok(account);
    }

    // Wrong: Using Response - @SecureField won't work!
    // Use RestResponse instead
    @GET
    @Path("/{id}/wrong")
    public Response getAccountWrong(@PathParam("id") long id) {
        UserAccount account = accountService.findById(id);
        return Response.ok(account).build();  // @SecureField ignored!
    }

    @Inject
    AccountService accountService;
}

// Method-level control
@Path("/reports")
public class ReportResource {

    // Secure field filtering enabled (default)
    @GET
    @Path("/employee/{id}")
    public EmployeeReport getEmployeeReport(@PathParam("id") long id) {
        return reportService.getEmployee(id);
    }

    // Disable secure filtering for this method - all fields included
    @GET
    @Path("/export/{id}")
    @DisableSecureSerialization
    @RolesAllowed("admin")
    public EmployeeReport exportAllData(@PathParam("id") long id) {
        // All fields included regardless of @SecureField
        return reportService.getEmployee(id);
    }

    @Inject
    ReportService reportService;
}

// Class-level control
@Path("/public")
@DisableSecureSerialization  // Disable for all methods in this class
public class PublicResource {

    @GET
    @Path("/profile/{id}")
    public UserProfile getPublicProfile(@PathParam("id") long id) {
        // @SecureField annotations ignored
        return profileService.getPublic(id);
    }

    // Re-enable for specific method
    @GET
    @Path("/restricted/{id}")
    @EnableSecureSerialization
    @RolesAllowed("member")
    public UserProfile getRestrictedProfile(@PathParam("id") long id) {
        // @SecureField annotations active again
        return profileService.getRestricted(id);
    }

    @Inject
    ProfileService profileService;
}

// Complex example with nested objects
public class CompanyData {
    public String name;
    public String address;

    @SecureField(rolesAllowed = {"admin", "finance"})
    public FinancialInfo financials;

    @SecureField(rolesAllowed = "admin")
    public List<UserAccount> employees;
}

public class FinancialInfo {
    public Double revenue;
    public Double expenses;

    @SecureField(rolesAllowed = "admin")
    public String taxId;
}

interface AccountService {
    UserAccount findById(long id);
}

interface ReportService {
    EmployeeReport getEmployee(long id);
}

interface ProfileService {
    UserProfile getPublic(long id);
    UserProfile getRestricted(long id);
}

class EmployeeReport {}
class UserProfile {}
```

**Key Points:**
- `@SecureField` filters fields based on current user's roles from Quarkus security context
- Must use `RestResponse<T>` instead of `Response` for filtering to work
- Nested objects respect their own `@SecureField` annotations
- `@EnableSecureSerialization` / `@DisableSecureSerialization` provide fine-grained control
- Integrates with `@RolesAllowed` and other Quarkus security annotations
- All features are experimental and may change in future releases

## Requirements

All Jackson customization features require the `quarkus-rest-jackson` extension:

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-jackson</artifactId>
</dependency>
```

## Experimental Status

These APIs are marked as `@Experimental` and may change in future Quarkus releases. The API design is still being evaluated for optimal user experience.
