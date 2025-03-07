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

package oracle.jdbc.provider.hashicorp.hcpvaultsecret.authentication;

import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.parameter.ParameterSetImpl;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Base class for HCP Vault Secrets authentication strategies.
 * <p>
 * Subclasses must implement methods to generate an access token and a cache key.
 * </p>
 */
public abstract class AbstractHcpVaultAuthentication {

  /**
   * Generates an HCP Vault Secrets token based on the provided parameters.
   *
   * @param parameterSet the parameters for the authentication request.
   * @return the generated {@link HcpVaultSecretToken}.
   */
  public abstract HcpVaultSecretToken generateToken(ParameterSet parameterSet);

  /**
   * Generates a cache key for the authentication request.
   *
   * @param parameterSet the parameters for the authentication request.
   * @return a {@link ParameterSet} to be used as a cache key.
   */
  public abstract Map<String, Object> generateCacheKey(ParameterSet parameterSet);

  /**
   * Filters the parameters from the {@link ParameterSet} based on the provided relevant keys.
   *
   * This utility method extracts only the parameters relevant to a specific authentication method,
   * ensuring that the generated cache key includes only necessary data.
   *
   * @param parameterSet the set of parameters to filter. Must not be null.
   * @param relevantKeys an array of parameter keys relevant to the authentication method.
   * @return a map containing only the filtered parameters.
   */
  protected static Map<String, Object> filterParameters(ParameterSet parameterSet, String[] relevantKeys) {
    Map<String, Object> allParameters = ((ParameterSetImpl) parameterSet).getParameterKeyValuePairs();

    return allParameters.entrySet().stream()
            .filter(entry -> Arrays.asList(relevantKeys).contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

}
