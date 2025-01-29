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

package oracle.jdbc.provider.hashicorp.hcpvaultsecret.configuration;

import oracle.jdbc.provider.hashicorp.hcpvaultsecret.authentication.HcpVaultAuthenticationMethod;
import oracle.jdbc.provider.hashicorp.hcpvaultsecret.authentication.HcpVaultTokenFactory;
import oracle.jdbc.provider.parameter.ParameterSetParser;

/**
 * Defines the parameters for configuring HCP Vault Secrets in the JDBC URL
 * or property sets.
 * <p>
 * This class provides utilities to parse and validate HCP Vault-specific
 * parameters used for authentication and interaction with the HCP Vault
 * Secrets Manager.
 * </p>
 */
public final class HcpVaultConfigurationParameters {

  private HcpVaultConfigurationParameters() {}

  public static ParameterSetParser.Builder configureBuilder(ParameterSetParser.Builder builder) {
    return builder
      .addParameter(
         "AUTHENTICATION",
         HcpVaultTokenFactory.AUTHENTICATION_METHOD,
         HcpVaultAuthenticationMethod.CLIENT_CREDENTIALS,
         HcpVaultConfigurationParameters::parseAuthMethod)
      .addParameter(
         "HCP_CLIENT_ID",
         HcpVaultTokenFactory.HCP_CLIENT_ID)
      .addParameter(
         "HCP_CLIENT_SECRET",
         HcpVaultTokenFactory.HCP_CLIENT_SECRET);
  }

  /**
   * Parses the authentication method from a string value.
   *
   * @param value the string value representing the authentication method. Must
   * not be null.
   * @return the parsed {@link HcpVaultAuthenticationMethod}.
   * @throws IllegalArgumentException if the value is unrecognized.
   */
  private static HcpVaultAuthenticationMethod parseAuthMethod(String value) {
    switch (value.toUpperCase()) {
      case "CLIENT_CREDENTIALS":
        return HcpVaultAuthenticationMethod.CLIENT_CREDENTIALS;
      default:
        throw new IllegalArgumentException("Unrecognized HCP auth method: " + value);
    }
  }
}
