/*
 *  Copyright (c) 2026 Oracle and/or its affiliates.
 *
 *  The Universal Permissive License (UPL), Version 1.0
 *
 *  Subject to the condition set forth below, permission is hereby granted to any
 *  person obtaining a copy of this software, associated documentation and/or data
 *  (collectively the "Software"), free of charge and under any and all copyright
 *  rights in the Software, and any and all patent rights owned or freely
 *  licensable by each licensor hereunder covering either (i) the unmodified
 *  Software as contributed to or provided by such licensor, or (ii) the Larger
 *  Works (as defined below), to deal in both
 *
 *  (a) the Software, and
 *  (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 *  one is included with the Software (each a "Larger Work" to which the Software
 *  is contributed by such licensors),
 *
 *  without restriction, including without limitation the rights to copy, create
 *  derivative works of, display, perform, and distribute the Software and make,
 *  use, sell, offer for sale, import, export, have made, and have sold the
 *  Software and the Larger Work(s), and to sublicense the foregoing rights on
 *  either these or other terms.
 *
 *  This license is subject to the following condition:
 *  The above copyright notice and either this complete permission notice or at
 *  a minimum a reference to the UPL must be included in all copies or
 *  substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

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
