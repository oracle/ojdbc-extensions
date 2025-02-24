/*
 ** Copyright (c) 2025 Oracle and/or its affiliates.
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

package oracle.jdbc.provider.util;

import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;

import java.util.Optional;

/**
 * Utility class for handling parameters and environment variables.
 */
public final class ParameterUtil {

  private ParameterUtil() {
  }

  /**
   * Fetches a value from system properties first, then falls back to
   * environment variables.
   *
   * @param key the name of the property or environment variable
   * @return the value of the property or environment variable, or null if not found
   */
  public static String getPropertyOrEnv(String key) {
    return System.getProperty(key, System.getenv(key));
  }

  /**
   * Fetches a parameter from the ParameterSet or falls back to system
   * properties, and then environment variables.
   *
   * @param parameterSet the ParameterSet to search for the parameter
   * @param parameter    the Parameter to fetch from the ParameterSet
   * @param envKey       the environment/system property key to use as fallback
   * @return the parameter value
   * @throws IllegalStateException if neither the parameter nor
   * the environment/system property is found
   */
  public static String getRequiredOrFallback(
          ParameterSet parameterSet, Parameter<String> parameter, String envKey) {
    String value = Optional.ofNullable(parameterSet.getOptional(parameter))
                           .orElse(getPropertyOrEnv(envKey));

    if (value == null || value.isEmpty()) {
      throw new IllegalStateException(
              "Required configuration '" + envKey + "' not found in ParameterSet, system properties, or environment variables.");
    }
    return value;
  }

  /**
   * Fetches a parameter from the ParameterSet or falls back to system properties,
   * then environment variables. Returns the default value if no value is found.
   *
   * @param parameterSet the ParameterSet to search for the parameter
   * @param parameter the Parameter to fetch from the ParameterSet
   * @param envKey the environment/system property key to use as fallback
   * @param defaultValue the default value to return if no value is found
   * @return the parameter value, environment/system property value, or
   * the default value
   */
  public static String getOptionalOrFallback(
          ParameterSet parameterSet, Parameter<String> parameter,
          String envKey, String defaultValue) {
    return Optional.ofNullable(parameterSet.getOptional(parameter))
            .orElse(Optional.ofNullable(getPropertyOrEnv(envKey))
                    .orElse(defaultValue));
  }

}
