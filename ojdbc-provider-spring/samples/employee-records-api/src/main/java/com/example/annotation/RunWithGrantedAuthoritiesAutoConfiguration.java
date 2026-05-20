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

import org.aopalliance.aop.Advice;
import org.springframework.aop.Advisor;
import org.springframework.aop.framework.autoproxy.InfrastructureAdvisorAutoProxyCreator;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import com.example.annotation.RunWithGrantedAuthoritiesPointcut;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.Ordered;

/**
 * Sample auto-configuration for the annotations
 *
 * Loaded via:
 *   src/test/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 */
@AutoConfiguration
public class RunWithGrantedAuthoritiesAutoConfiguration {

  /**
   * Register a proxy creator for infrastructure advisors (no @AspectJ required).
   */
  @Bean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public static InfrastructureAdvisorAutoProxyCreator infrastructureAdvisorAutoProxyCreator() {
    InfrastructureAdvisorAutoProxyCreator apc = new InfrastructureAdvisorAutoProxyCreator();
    apc.setOrder(Ordered.HIGHEST_PRECEDENCE);
    apc.setProxyTargetClass(true); // prefer CGLIB to ensure class methods are advised consistently
    return apc;
  }

  /**
   * The core interceptor that performs the temporary SecurityContext augmentation.
   */
  @Bean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public RunWithGrantedAuthoritiesMethodInterceptor runWithGrantedAuthoritiesMethodInterceptor() {
    return new RunWithGrantedAuthoritiesMethodInterceptor();
  }

  /**
   * Advisor that applies the interceptor to:
   *  - any method annotated with @RunWithGrantedAuthorities
   *  - any method within a class annotated with @RunWithGrantedAuthorities (supports @Inherited)
   */
  @Bean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public Advisor runWithGrantedAuthoritiesAdvisor(RunWithGrantedAuthoritiesMethodInterceptor interceptor) {
    RunWithGrantedAuthoritiesPointcut pointcut = new RunWithGrantedAuthoritiesPointcut();
    Advice advice = interceptor;
    return new DefaultPointcutAdvisor(pointcut, advice);
  }

  /**
   * The core interceptor that performs the temporary SecurityContext augmentation for data roles only.
   */
  @Bean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public RunWithDataRolesMethodInterceptor runWithDataRolesMethodInterceptor() {
    return new RunWithDataRolesMethodInterceptor();
  }

  /**
   * Advisor that applies the data-roles-only interceptor to:
   *  - any method annotated with @RunWithDataRoles
   *  - any method within a class annotated with @RunWithDataRoles (supports @Inherited)
   */
  @Bean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  public Advisor runWithDataRolesAdvisor(RunWithDataRolesMethodInterceptor interceptor) {
    RunWithDataRolesPointcut pointcut = new RunWithDataRolesPointcut();
    Advice advice = interceptor;
    return new DefaultPointcutAdvisor(pointcut, advice);
  }
}
