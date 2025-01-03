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

package oracle.jdbc.provider.hashicorp.dedicated.configuration;

import oracle.jdbc.provider.configuration.JsonSecretUtil;
import oracle.jdbc.provider.hashicorp.dedicated.secrets.DedicatedVaultSecretsManagerFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.spi.OracleConfigurationJsonSecretProvider;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static oracle.jdbc.provider.hashicorp.dedicated.configuration.DedicatedVaultSecretsManagerConfigurationProvider.PARAMETER_SET_PARSER;
import static oracle.jdbc.provider.hashicorp.dedicated.secrets.DedicatedVaultSecretsManagerFactory.FIELD_NAME;

/**
 * <p>
 * Implementation of {@link OracleConfigurationJsonSecretProvider} for
 * Dedicated HashiCorp Vault. This provider retrieves secrets stored in the
 * Vault and optionally extracts a specific field if specified.
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
 * <p>
 * The secret path specified in the JSON is used to query the Vault and fetch
 * the desired secret. If {@code FIELD_NAME} is provided, the corresponding
 * field is extracted from the Vault's JSON response.
 * </p>
 */
public class DedicatedVaultJsonSecretProvider implements OracleConfigurationJsonSecretProvider {

  @Override
  public char[] getSecret(OracleJsonObject jsonObject) {
    ParameterSet parameterSet =
      PARAMETER_SET_PARSER.parseNamedValues(
        JsonSecretUtil.toNamedValues(jsonObject)
      );

    String secretString = DedicatedVaultSecretsManagerFactory
      .getInstance()
      .request(parameterSet)
      .getContent();

    ByteArrayInputStream inputStream = new ByteArrayInputStream(
      secretString.getBytes(StandardCharsets.UTF_8));

    OracleJsonObject secretJsonObj =
      new OracleJsonFactory()
        .createJsonTextValue(inputStream)
        .asJsonObject();

    String fieldName = parameterSet.getOptional(FIELD_NAME);
    String extractedPassword = String.valueOf(secretJsonObj.get(fieldName));

    return Base64.getEncoder()
            .encodeToString(extractedPassword.getBytes())
            .toCharArray();
  }

  @Override
  public String getSecretType() {
    return "hcpdedicatedvault";
  }
}
