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

package com.example.employees.config;

import com.example.annotation.RunWithDataRoles;
import com.example.annotation.RunWithDataRolesMethodInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import com.example.annotation.RunWithDataRolesPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Wires the RunWithDataRolesMethodInterceptor via a Spring AOP Advisor.
 * This ensures methods/classes annotated with @RunWithDataRoles are intercepted
 * and augmented authorities are applied for the duration of the invocation.
 *
 * Notes:
 * - Package is under com.example.employees.* so it is picked up by Spring Boot's component scan
 *   (since the main app class is in com.example.employees).
 * - The annotation & interceptor are located in com.example.annotation.* (outside the main scan);
 *   referencing them here lets us register the advisor without moving those classes.
 */
@Configuration
@EnableAspectJAutoProxy
public class RunWithDataRolesAopConfig {

    @Bean
    public RunWithDataRolesMethodInterceptor runWithDataRolesMethodInterceptor() {
        return new RunWithDataRolesMethodInterceptor();
    }

    @Bean
    public Advisor runWithDataRolesAdvisor(RunWithDataRolesMethodInterceptor interceptor) {
        RunWithDataRolesPointcut pointcut = new RunWithDataRolesPointcut();
        return new DefaultPointcutAdvisor(pointcut, interceptor);
    }
}
