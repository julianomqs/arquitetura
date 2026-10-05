package org.example.util;

import java.lang.reflect.Method;

public class ReflectionUtil {

  public static Method getMethod(Class<?> clazz, String methodName) {
    for (var method : clazz.getMethods()) {
      if (method.getName().equals(methodName)) {
        return method;
      }
    }

    return null;
  }
}
