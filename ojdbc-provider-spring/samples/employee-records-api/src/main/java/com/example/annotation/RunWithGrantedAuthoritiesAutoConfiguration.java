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
