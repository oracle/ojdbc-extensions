package com.example.annotation;

import java.lang.annotation.*;

/**
 * Sample annotation to execute a method with a temporary Authentication that includes:
 *  - original authorities
 *  - one authority for each role in dataRoles(), prefixed by "ORACLE_DATA_ROLE_"
 *
 * This is a simplified variant of {@link RunWithGrantedAuthorities} that only supports data roles
 * (no context attributes).
 *
 * Requirements and behavior notes (proxy-based, like @Transactional):
 * 1) The target must be a Spring-managed bean so calls go through a Spring AOP proxy.
 *    - Typically annotate a method on a @Service/@Repository/@Component managed by the container.
 *    - Self-invocation (this.method()) is not advised; the call must pass through the proxy.
 * 2) Annotation attributes must be compile-time constants (Java limitation for annotations).
 *    - Provide literal values for dataRoles (e.g., string/array literals).
 * 3) The annotation infrastructure must be registered.
 *    - This project uses a test-only auto-configuration (via test META-INF AutoConfiguration.imports)
 *      to register the advisor/interceptor, so no manual @EnableAspectJAutoProxy/@Import is required.
 *
 * Examples:
 *  - Method-level with multiple roles:
 *    {@code
 *    @RunWithDataRoles(
 *      dataRoles = {"HR_DYNAMIC_ROLE","HCM_APP_DATA_ROLE","HCM_APP_DATA_PERMISSION"}
 *    )
 *    public List<Employee> findAll(...) { ... }
 *    }
 *
 *  - Class-level (applies to all public methods in the bean):
 *    {@code
 *    @RunWithDataRoles(
 *      dataRoles = {"ROLE_A","ROLE_B"}
 *    )
 *    @Service
 *    public class MyService { ... }
 *    }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RunWithDataRoles {

  /**
   * Logical data roles (e.g. "HCM_APP_DATA_ROLE").
   * Each role will be added as a GrantedAuthority with prefix "ORACLE_DATA_ROLE_".
   */
  String[] dataRoles() default {};
}
