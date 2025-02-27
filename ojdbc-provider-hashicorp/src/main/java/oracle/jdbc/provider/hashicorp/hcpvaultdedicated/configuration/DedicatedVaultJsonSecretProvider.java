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

import oracle.jdbc.provider.hashicorp.JsonUtil;
import oracle.jdbc.provider.hashicorp.hcpvaultdedicated.secrets.DedicatedVaultSecretsManagerFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.spi.OracleConfigurationSecretProvider;
import oracle.sql.json.OracleJsonObject;

import java.util.Base64;
import java.util.Map;

import static oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication.DedicatedVaultParameters.FIELD_NAME;
import static oracle.jdbc.provider.hashicorp.hcpvaultdedicated.configuration.DedicatedVaultSecretsManagerConfigurationProvider.PARAMETER_SET_PARSER;

/**
 * <p>
 * Implementation of {@link OracleConfigurationSecretProvider} for
 * Dedicated HashiCorp Vault. This provider retrieves secrets stored in the HCP
 * Vault Dedicated and optionally extracts a specific field if specified.
 * </p>
 * <p>
 * The {@code jsonObject} must adhere to the following structure:
 * </p>
 *
 * <pre>{@code
 *   "password": {
 *       "type": "hcpdedicatedvault",
 *       "value": "<secret-path>",
 *       "FIELD_NAME": "<field-name>"
 *   }
 * }</pre>
 *
 * <h2>Behavior for Extracting the Secret</h2>
 * <ul>
 *   <li> If {@code field_name} is provided, the corresponding value is
 *   extracted.</li>
 *  <li>If the secret contains <b>only one key-value pair</b>, that value
 *  is automatically selected.</li>
 *  <li>If multiple keys exist and {@code field_name} is <b>not provided</b>,
 *  an error is thrown.</li>
 * </ul>
 * <p>
 * The secret path specified in the JSON is used to query the Vault and fetch
 * the desired secret.
 * </p>
 */
public class DedicatedVaultJsonSecretProvider implements OracleConfigurationSecretProvider {

  @Override
  public char[] getSecret(Map<String, String> map) {
    ParameterSet parameterSet = PARAMETER_SET_PARSER.parseNamedValues(map);
    String secretString = DedicatedVaultSecretsManagerFactory
      .getInstance()
      .request(parameterSet)
      .getContent();

    OracleJsonObject secretJsonObj = JsonUtil.convertJsonToOracleJsonObject(secretString);

    String fieldName = parameterSet.getOptional(FIELD_NAME);
    String extractedPassword;
    if (fieldName != null && secretJsonObj.containsKey(fieldName)) {
      extractedPassword = secretJsonObj.getString(fieldName);
    } else if (secretJsonObj.size() == 1) {
      extractedPassword = secretJsonObj.values().iterator().next().toString();
    } else {
      throw new IllegalStateException(
              "FIELD_NAME is required when multiple keys exist in the secret."
      );
    }

    return Base64.getEncoder()
            .encodeToString(extractedPassword.getBytes())
            .toCharArray();
  }

  @Override
  public String getSecretType() {
    return "hcpvaultdedicated";
  }
}
