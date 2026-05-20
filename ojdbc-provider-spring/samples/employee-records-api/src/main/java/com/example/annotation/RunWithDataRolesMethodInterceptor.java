package com.example.annotation;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.security.access.intercept.RunAsUserToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Simplified annotation interceptor that augments Authentication with data roles only.
 * Mirrors RunWithGrantedAuthoritiesMethodInterceptor but without context attributes.
 */
public class RunWithDataRolesMethodInterceptor implements MethodInterceptor {

  private static final String RUN_AS_KEY = "runas-key";
  private static final String ROLE_PREFIX = "ORACLE_DATA_ROLE_";

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    RunWithDataRoles ann = resolveAnnotation(invocation);
    if (ann == null) {
      return invocation.proceed();
    }

    final Authentication originalAuth = SecurityContextHolder.getContext().getAuthentication();
    if (originalAuth == null) {
      return invocation.proceed();
    }

    final Collection<GrantedAuthority> mergedAuthorities = new ArrayList<>(
        originalAuth.getAuthorities() != null ? originalAuth.getAuthorities() : List.of()
    );

    if (ann.dataRoles() != null) {
      for (String role : ann.dataRoles()) {
        if (role != null && !role.isBlank()) {
          mergedAuthorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + role.trim()));
        }
      }
    }

    Authentication augmentedAuth;
    if (originalAuth instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      JwtAuthenticationToken withExtra =
          new JwtAuthenticationToken(jwt, mergedAuthorities, jwtAuth.getName());
      withExtra.setDetails(jwtAuth.getDetails());
      augmentedAuth = withExtra;
    } else {
      augmentedAuth = new RunAsUserToken(
          RUN_AS_KEY,
          originalAuth.getPrincipal(),
          originalAuth.getCredentials(),
          mergedAuthorities,
          originalAuth.getClass()
      );
    }

    try {
      SecurityContextHolder.getContext().setAuthentication(augmentedAuth);
      return invocation.proceed();
    } finally {
      SecurityContextHolder.getContext().setAuthentication(originalAuth);
    }
  }

  @Nullable
  private RunWithDataRoles resolveAnnotation(MethodInvocation invocation) {
    Method method = invocation.getMethod();
    RunWithDataRoles ann =
        AnnotatedElementUtils.findMergedAnnotation(method, RunWithDataRoles.class);
    if (ann == null) {
      Class<?> targetClass = invocation.getThis() != null ? invocation.getThis().getClass() : method.getDeclaringClass();
      ann = AnnotatedElementUtils.findMergedAnnotation(targetClass, RunWithDataRoles.class);
    }
    return ann;
  }
}
