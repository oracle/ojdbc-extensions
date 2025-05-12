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

package oracle.jdbc.provider.aws.resource;

import oracle.jdbc.provider.aws.secrets.AwsSecretExtractor;
import oracle.jdbc.provider.aws.secrets.SecretsManagerFactory;
import oracle.jdbc.provider.resource.ResourceParameter;
import oracle.jdbc.provider.util.ResourceParameterUtils;

import java.util.Map;
import java.util.stream.Stream;

import static oracle.jdbc.provider.aws.configuration.AwsConfigurationParameters.FIELD_NAME;
import static oracle.jdbc.provider.aws.secrets.SecretsManagerFactory.SECRET_NAME;

/**
 * <p>
 * Base class for all providers that retrieve secrets from AWS Secrets Manager.
 * This class defines shared parameters and secret retrieval logic.
 * </p>
 */
public class SecretsManagerSecretProvider extends AwsResourceProvider {

  private static final ResourceParameter[] PARAMETERS = {
    new ResourceParameter("secretName", SECRET_NAME),
    new ResourceParameter("fieldName", FIELD_NAME)
  };

  protected SecretsManagerSecretProvider(String valueType) {
    super(valueType, PARAMETERS);
  }

  protected SecretsManagerSecretProvider(String valueType, ResourceParameter[] additionalParameters) {
    super(valueType, ResourceParameterUtils.combineParameters(PARAMETERS, additionalParameters));
  }

  /**
   * <p>
   * Retrieves a secret from AWS Secrets Manager based on parameters provided
   * in {@code parameterValues}. This method centralizes secret retrieval logic
   * and is intended to be used by subclasses implementing the
   * {@link oracle.jdbc.spi.OracleResourceProvider} SPI.
   * </p><p>
   * The method uses the {@code getResource} method to parse the
   * {@code parameterValues} and request the secret from AWS Secrets Manager via
   * the {@link oracle.jdbc.provider.aws.secrets.SecretsManagerFactory} instance.
   * The secret value is returned as a {@code String}.
   * </p>
   *
   * @param parameterValues A map of parameter names and their corresponding
   * values required for secret retrieval.
   * @return The secret value as a {@code String}.
   * @throws IllegalStateException If secret retrieval fails or returns null.
   */
  protected final String getSecret(Map<Parameter, CharSequence> parameterValues) {
    String secretJson = getResource(SecretsManagerFactory.getInstance(),
      parameterValues);
    ResourceParameter fieldNameParam = Stream.of(PARAMETERS)
      .filter(param -> param.name().equals("fieldName"))
      .findFirst()
      .orElse(null);
    String fieldName = parameterValues.containsKey(fieldNameParam) ?
      parameterValues.get(fieldNameParam).toString() : null;
    return AwsSecretExtractor.extractSecret(secretJson, fieldName);
  }
}
