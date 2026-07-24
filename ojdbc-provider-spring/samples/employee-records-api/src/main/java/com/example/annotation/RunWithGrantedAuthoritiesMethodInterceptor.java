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
 * Sample annotation interceptor
 */
public class RunWithGrantedAuthoritiesMethodInterceptor implements MethodInterceptor {

  private static final String RUN_AS_KEY = "runas-key";
  private static final String ROLE_PREFIX = "ORACLE_DATA_ROLE_";
  private static final String ATTR_PREFIX = "ORACLE_CONTEXT_ATTRIBUTE_";

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    RunWithGrantedAuthorities ann = resolveAnnotation(invocation);
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

    String attributesJson = ann.contextAttributes();
    if (attributesJson != null && !attributesJson.isBlank()) {
      mergedAuthorities.add(new SimpleGrantedAuthority(ATTR_PREFIX + attributesJson));
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
  private RunWithGrantedAuthorities resolveAnnotation(MethodInvocation invocation) {
    Method method = invocation.getMethod();
    RunWithGrantedAuthorities ann =
        AnnotatedElementUtils.findMergedAnnotation(method, RunWithGrantedAuthorities.class);
    if (ann == null) {
      Class<?> targetClass = invocation.getThis() != null ? invocation.getThis().getClass() : method.getDeclaringClass();
      ann = AnnotatedElementUtils.findMergedAnnotation(targetClass, RunWithGrantedAuthorities.class);
    }
    return ann;
  }
}
