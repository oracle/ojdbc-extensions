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
package oracle.jdbc.provider.aws.configuration;

import oracle.jdbc.provider.aws.secrets.AwsSecretExtractor;
import oracle.jdbc.provider.aws.secrets.SecretsManagerFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.spi.OracleConfigurationSecretProvider;

import java.util.Map;

import static oracle.jdbc.provider.aws.configuration.AwsConfigurationParameters.FIELD_NAME;
import static oracle.jdbc.provider.aws.configuration.AwsSecretsManagerConfigurationProvider.PARAMETER_SET_PARSER;
import static oracle.jdbc.provider.util.FileUtils.toBase64EncodedCharArray;

public class AwsJsonSecretsManagerProvider
    implements OracleConfigurationSecretProvider {

  /**
   * {@inheritDoc}
   * <p>
   * Returns the password of the Secret that is retrieved from AWS Secrets
   * Manager secret.
   * </p>
   * <p>
   * The {@code jsonObject} has the following form:
   * </p>
   *
   * <pre>{@code
   *   "password": {
   *       "type": "awssecretsmanager",
   *       "value": "<secret-name>",
   *       "field_name": "<field-name>"
   *   }
   * }</pre>
   *
   * <p>
   * The {@code field_name} parameter indicates the key whose value should
   * be extracted as the secret. When there are multiple key-value pairs
   * present, specifying this parameter is mandatory in order to
   * unambiguously select the desired secret value. If the secret contains
   * only a single entry and no {@code field_name} is provided, that sole
   * value will be used. In cases where the secret is plain text,
   * the {@code field_name} parameter is not required.
   * </p>
   *
   * @param map Map object to be parsed
   * @return encoded char array in base64 format that represents the retrieved
   *         Secret.
   */
  @Override
  public char[] getSecret(Map<String, String> map) {
    ParameterSet parameterSet = PARAMETER_SET_PARSER.parseNamedValues(map);
    String fieldName = parameterSet.getOptional(FIELD_NAME);

    String secretString = SecretsManagerFactory.getInstance()
        .request(parameterSet)
        .getContent();

    String extractedSecret = AwsSecretExtractor.extractSecret(secretString,
      fieldName);

    return toBase64EncodedCharArray(extractedSecret);
  }

  @Override
  public String getSecretType() {
    return "awssecretsmanager";
  }
}
