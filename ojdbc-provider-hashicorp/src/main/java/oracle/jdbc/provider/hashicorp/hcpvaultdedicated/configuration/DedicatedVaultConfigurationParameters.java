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

package oracle.jdbc.provider.hashicorp.hcpvaultdedicated.configuration;

import oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication.DedicatedVaultAuthenticationMethod;
import oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication.DedicatedVaultParameters;
import oracle.jdbc.provider.parameter.ParameterSetParser;

/**
 * <p>
 * A class for defining and parsing configuration parameters for
 * Dedicated HashiCorp Vault.
 * </p><p>
 * This class provides methods to build and parse {@link ParameterSetParser}
 * objects, which are used to interpret configuration parameters passed to
 * the Vault.
 * </p>
 */
public final class DedicatedVaultConfigurationParameters {

  /**
   * Private constructor that should never be called.
   */
  private DedicatedVaultConfigurationParameters() {}

  /**
   * Configures a {@link ParameterSetParser.Builder} with parameters used for
   * Dedicated Vault authentication.
   *
   * @param builder the builder to configure. Must not be null.
   * @return the configured builder. Not null.
   */
  public static ParameterSetParser.Builder configureBuilder(ParameterSetParser.Builder builder) {
    return builder.addParameter(
            // The parameter name is "AUTHENTICATION"
            "AUTHENTICATION",
            // Tied to HashicorpCredentialsFactory.AUTHENTICATION_METHOD
            DedicatedVaultParameters.AUTHENTICATION_METHOD,
            // Default value if none is specified:
            DedicatedVaultAuthenticationMethod.AUTO_DETECT,
            DedicatedVaultConfigurationParameters::parseAuthentication)
            ;
  }

  /**
   * Parses the authentication method from a string value.
   * @param value the string value representing the authentication method. Must
   * not be null.
   * @return the parsed {@link DedicatedVaultAuthenticationMethod}.
   * @throws IllegalArgumentException if the value is unrecognized.
   */
  private static DedicatedVaultAuthenticationMethod parseAuthentication(String value) {
    try {
      return DedicatedVaultAuthenticationMethod.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
              "Unrecognized Hashicorp authentication value: " + value, e);
    }
  }
}
