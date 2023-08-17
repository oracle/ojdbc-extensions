/*
 ** Copyright (c) 2023 Oracle and/or its affiliates.
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
package oracle.jdbc.provider.resource;

import oracle.jdbc.spi.OracleResourceProvider;
import oracle.jdbc.spi.OracleResourceProvider.Parameter;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Common functionality for tests that verify {@link OracleResourceProvider}.
 */
public class ResourceProviderTestUtil {

  /**
   * Creates a mapping of {@code Parameter} objects to values, where the names
   * of the parameters match the names used as keys in the given
   * {@code testParameters} map.
   * @param provider Provider that declares parameters.
   * @param testParameters Parameter values mapped to parameter names.
   * @return Parameter values mapped to {@code Parameter}s of the
   * {@code provider}.
   */
  public static Map<Parameter, CharSequence> createParameterValues(
    OracleResourceProvider provider,
    Map<String, ? extends CharSequence> testParameters) {

    Map<Parameter, CharSequence> parameterValues = new HashMap<>();

    for (Map.Entry<String, ? extends CharSequence> entry
      : testParameters.entrySet()) {
      String name = entry.getKey();
      Parameter parameter = getParameter(provider, name);

      parameterValues.put(parameter, entry.getValue());
    }


    return parameterValues;
  }

  /**
   * @param provider Provider that declares a {@code Parameter}
   * @param name Name of a parameter
   * @return The {@code Parameter} with a matching {@code name}.
   */
  public static Parameter getParameter(
    OracleResourceProvider provider, String name) {
    return provider.getParameters()
      .stream()
      .filter(parameter -> name.equalsIgnoreCase(parameter.name()))
      .findFirst()
      .orElseThrow(() ->
        new AssertionError("No parameter named: " + name));

  }

  /**
   * @param name Name of a provider
   * @return The provider with a matching {@code name}
   * @throws IllegalStateException If no provider with a matching name is found.
   */
  public static <T extends OracleResourceProvider> T findProvider(
    Class<T> providerClass, String name) {

    ServiceLoader<T> serviceLoader = ServiceLoader.load(providerClass);

    for (T provider : serviceLoader) {
      if (name.equalsIgnoreCase(provider.getName())) {
        return provider;
      }
    }

    throw new IllegalStateException("No provider found with name: " + name);
  }

}
