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

package oracle.jdbc.provider.oci.configuration;

import java.util.Map;

import oracle.jdbc.provider.oci.vault.SecretFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.spi.OracleConfigurationSecretProvider;

/**
 * A provider of Secret values from OCI Vault.
 */
public final class OciVaultSecretProvider
    implements OracleConfigurationSecretProvider {


  /**
   * {@inheritDoc}
   *
   * @return secret type. Not null.
   */
  @Override
  public String getSecretType() {
    return "ocivault";
  }

  /**
   * <p>
   *   Returns the password of the Secret that is retrieved from OCI Vault.
   * </p><p>
   *   The JSON object has the following form:
   * </p><pre>{@code
   *   "password": {
   *       "type": "ocivault",
   *       "value": "ocid1.vaultsecret.oc1.phx.amaaaaaad...",
   *       "authentication": {
   *           "method": "OCI_DEFAULT"
   *       }
   *   }
   * }</pre>
   *
   * @param secretProperties a map containing the flattened key/value pairs of 
   * the JSON object.
   * @return encoded char array in base64 format that represents the retrieved
   *         Secret.
   */
  @Override
  public char[] getSecret(Map<String, String> secretProperties) {
    ParameterSet parameters =
      OciConfigurationParameters.getParser()
        .parseNamedValues(secretProperties);

    return SecretFactory.getInstance()
      .request(parameters)
      .getContent()
      .getBase64Secret()
      .toCharArray();
  }
}
