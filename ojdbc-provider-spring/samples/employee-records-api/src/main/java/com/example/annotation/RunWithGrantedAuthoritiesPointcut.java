package com.example.annotation;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.aop.support.StaticMethodMatcherPointcut;

import java.lang.reflect.Method;
import org.springframework.util.ClassUtils;

/**
 * Pointcut that matches:
 *  - methods annotated with @RunWithGrantedAuthorities
 *  - methods declared in a type annotated with @RunWithGrantedAuthorities (supports meta-annotations)
 */
public class RunWithGrantedAuthoritiesPointcut extends StaticMethodMatcherPointcut {

  @Override
  public boolean matches(Method method, @Nullable Class<?> targetClass) {
    if (method == null) {
      return false;
    }
    // Direct method annotation (covers CGLIB case where method is the implementation)
    if (AnnotatedElementUtils.hasAnnotation(method, RunWithGrantedAuthorities.class)) {
      return true;
    }
    // For JDK proxies, resolve the most specific method on the target class and check annotation there
    Class<?> type = (targetClass != null ? targetClass : method.getDeclaringClass());
    Method specific = ClassUtils.getMostSpecificMethod(method, type);
    if (specific != null && specific != method &&
        AnnotatedElementUtils.hasAnnotation(specific, RunWithGrantedAuthorities.class)) {
      return true;
    }
    // Fallback: type-level annotation
    return AnnotatedElementUtils.hasAnnotation(type, RunWithGrantedAuthorities.class);
  }
}
