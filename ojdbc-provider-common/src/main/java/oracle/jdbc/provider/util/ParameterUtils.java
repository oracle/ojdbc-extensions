/*
 ** Copyright (c) 2026 Oracle and/or its affiliates.
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

/**
 * Utility class for parameter resolution with fallback support.
 * <p>
 * This class provides methods to retrieve parameter values from multiple sources
 * with a priority order: ParameterSet → System Properties → Environment Variables.
 * </p>
 */
public final class ParameterUtils {

  private ParameterUtils() {
    // Prevent instantiation.
  }

  /**
   * Returns the fallback value for the given key by checking system properties first,
   * then environment variables.
   *
   * @param key the key to look up
   * @return the fallback value, or null if not found
   */
  public static String getFallback(String key) {
    return System.getProperty(key, System.getenv(key));
  }

  /**
   * Retrieves a parameter value with cascading fallback to system property and environment variable.
   * <p>
   * The lookup order is:
   * <ol>
   *   <li>ParameterSet</li>
   *   <li>System property</li>
   *   <li>Environment variable</li>
   * </ol>
   *
   * @param parameter The Parameter object to retrieve from ParameterSet
   * @param sysPropKey The system property key to check as fallback
   * @param envVarKey The environment variable key to check as fallback
   * @param paramSet The ParameterSet to check first
   * @return The parameter value from the first available source
   * @throws IllegalArgumentException if the parameter is not found in any source
   */
  public static String getParameterWithFallback(Parameter<String> parameter,
                                           String sysPropKey, String envVarKey, ParameterSet paramSet) {
    String value = paramSet.getOptional(parameter);
    if (value == null) {
      value = System.getProperty(sysPropKey);
      if (value == null) {
        value = System.getenv(envVarKey);
        if (value == null) {
          throw new IllegalArgumentException(
            String.format("Parameter '%s' is required and not found in ParameterSet, system property '%s', or environment variable '%s'",
              paramSet.getName(parameter), sysPropKey, envVarKey));
        }
      }
    }
    return value;
  }
}