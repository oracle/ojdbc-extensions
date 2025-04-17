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

package oracle.jdbc.provider.hashicorp.util;

import oracle.jdbc.provider.resource.AbstractResourceProvider;
import oracle.jdbc.provider.resource.ResourceParameter;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract class that encapsulates common behavior for resolving parameters
 * from system properties or environment variables.
 */
public abstract class AbstractVaultResourceProvider extends AbstractResourceProvider {

  protected AbstractVaultResourceProvider(String providerType, String resourceType, ResourceParameter[] parameters) {
    super(providerType, resourceType, parameters);
  }

  /**
   * Resolves missing parameters using system properties or environment variables.
   *
   * @param parameterValues The original parameter map.
   * @param parameters The parameters to check.
   * @return A map with resolved parameters.
   */
  protected Map<Parameter, CharSequence> resolveMissingParameters(
    Map<Parameter, CharSequence> parameterValues, ResourceParameter[] parameters) {

    Map<Parameter, CharSequence> resolved = new HashMap<>(parameterValues);
    for (ResourceParameter param : parameters) {
      resolveParameter(resolved, param);
    }
    return resolved;
  }

  private void resolveParameter(Map<Parameter, CharSequence> parameterValues, ResourceParameter parameter) {
    if (!parameterValues.containsKey(parameter)) {
      String envKey = getEnvVariableForParameter(parameter.name());
      String value = System.getProperty(envKey, System.getenv(envKey));
      if (value != null) {
        parameterValues.put(parameter, value);
      }
    }
  }

  /**
   * Subclasses must define how parameter names map to env vars or sys props.
   *
   * @param paramName The parameter name
   * @return Corresponding environment variable or system property key
   */
  protected abstract String getEnvVariableForParameter(String paramName);
}
