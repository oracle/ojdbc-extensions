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
