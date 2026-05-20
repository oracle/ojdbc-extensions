package com.example.annotation;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;

/**
 * Pointcut that matches:
 *  - methods annotated with @RunWithDataRoles
 *  - methods declared in a type annotated with @RunWithDataRoles (supports meta-annotations)
 */
public class RunWithDataRolesPointcut extends StaticMethodMatcherPointcut {

  @Override
  public boolean matches(Method method, @Nullable Class<?> targetClass) {
    if (method == null) {
      return false;
    }
    // Direct method annotation (covers CGLIB case where method is the implementation)
    if (AnnotatedElementUtils.hasAnnotation(method, RunWithDataRoles.class)) {
      return true;
    }
    // For JDK proxies, resolve the most specific method on the target class and check annotation there
    Class<?> type = (targetClass != null ? targetClass : method.getDeclaringClass());
    Method specific = ClassUtils.getMostSpecificMethod(method, type);
    if (specific != null && specific != method &&
        AnnotatedElementUtils.hasAnnotation(specific, RunWithDataRoles.class)) {
      return true;
    }
    // Fallback: type-level annotation
    return AnnotatedElementUtils.hasAnnotation(type, RunWithDataRoles.class);
  }
}
