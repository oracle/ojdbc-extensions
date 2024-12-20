/*
 ** Copyright (c) 2024 Oracle and/or its affiliates.
 **
 ** The Universal Permissive License (UPL), Version 1.0
 **
 ** Subject to the condition set forth below, permission is hereby granted to any
 ** person obtaining a copy of this software, associated documentation and/or data
 ** (collectively the "Software"), free of charge and under any and all copyright
 ** rights in the Software, and any and all patent rights owned or freely
 ** licensable by each licensor hereunder covering either (i) the unmodified
 ** Software as contributed to or provided by such licensor, or (ii) the Larger
 ** Works (as defined below), to deal in both
 **
 ** (a) the Software, and
 ** (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 ** one is included with the Software (each a "Larger Work" to which the Software
 ** is contributed by such licensors),
 **
 ** without restriction, including without limitation the rights to copy, create
 ** derivative works of, display, perform, and distribute the Software and make,
 ** use, sell, offer for sale, import, export, have made, and have sold the
 ** Software and the Larger Work(s), and to sublicense the foregoing rights on
 ** either these or other terms.
 **
 ** This license is subject to the following condition:
 ** The above copyright notice and either this complete permission notice or at
 ** a minimum a reference to the UPL must be included in all copies or
 ** substantial portions of the Software.
 **
 ** THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 ** IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 ** FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 ** AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 ** LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 ** OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 ** SOFTWARE.
 */

package oracle.jdbc.provider.oson;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;

import java.io.Serializable;
import java.util.List;

/**
 * Utility class providing helper methods for identifying and handling
 * serializable types and Java wrapper types. This class is designed to assist
 * in determining if a class or interface implements {@link Serializable}, and
 * whether a type falls under standard Java types that are inherently serializable.
 */
public class Util {

  /**
   * Checks if a given class belongs to the standard Java packages or is an array,
   * which are typically considered inherently serializable.
   *
   * @param clazz the class to check.
   * @return {@code true} if the class is from a known Java package or is an array;
   *         {@code false} otherwise.
   */
  public static boolean isJavaSerializableType(Class<?> clazz) {
    String packageName = clazz.getPackage().getName();

    return packageName.startsWith("java.lang")
              || packageName.startsWith("java.util")
              || packageName.startsWith("java.sql")
              || packageName.startsWith("java.time")
              || packageName.startsWith("java.math")
              || packageName.startsWith("java.security")
              || packageName.startsWith("java.net")
              || clazz.isArray();
  }

  /**
   * Checks if any of the provided interfaces implement {@link Serializable}.
   *
   * @param interfaces the list of {@link JavaType} interfaces to check.
   * @return {@code true} if at least one interface implements {@link Serializable};
   *         {@code false} otherwise.
   */
  public static boolean implementsSerializable(List<JavaType> interfaces) {
    boolean result = false;
    for (JavaType javaType : interfaces) {
      if(Serializable.class.isAssignableFrom(javaType.getRawClass())){
        result = true;
        break;
      }
    }
    return result;
  }

  /**
   * Determines if a given field represented by a {@link BeanPropertyWriter} is
   * inherently serializable as a Java wrapper type.
   *
   * @param writer the {@link BeanPropertyWriter} representing the field to check.
   * @return {@code true} if the field type is inherently serializable; {@code false} otherwise.
   */
  public static boolean isJavaWrapperSerializable(BeanPropertyWriter writer) {
    JavaType type = writer.getType();
    return isJavaWrapperSerializable(type);
  }

  /**
   * Checks if a given {@link JavaType} represents a Java wrapper type that is inherently serializable.
   *
   * @param type the {@link JavaType} to check.
   * @return {@code true} if the type is inherently serializable; {@code false} otherwise.
   */
  public static boolean isJavaWrapperSerializable(JavaType type) {
    Class<?> rawType = type.getRawClass();
    return isJavaSerializableType(rawType);
  }
}
