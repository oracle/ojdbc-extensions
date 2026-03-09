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

package oracle.jdbc.provider.hashicorp.hcpvaultdedicated.resource;

import oracle.jdbc.provider.hashicorp.hcpvaultdedicated.secrets.DedicatedVaultSecretsManagerFactory;
import oracle.jdbc.provider.hashicorp.util.JsonUtil;
import oracle.jdbc.provider.resource.ResourceParameter;
import oracle.jdbc.provider.util.ResourceParameterUtils;
import oracle.sql.json.OracleJsonObject;

import java.util.Map;
import java.util.stream.Stream;

import static oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication.DedicatedVaultParameters.*;

/**
 * <p>
 * A provider of secrets from HashiCorp Vault Dedicated. This class is designed
 * for inheritance by subclasses that implement an
 * {@link oracle.jdbc.spi.OracleResourceProvider} SPI defined by the Oracle JDBC
 * driver. This class defines parameters that configure the Secret path,
 * and optional field selection for extracting values from JSON-based secrets.
 * </p>
 */
public class HcpVaultDedicatedSecretProvider extends HcpVaultDedicatedResourceProvider {

  private static final ResourceParameter[] PARAMETERS = {
    new ResourceParameter(HcpVaultDedicatedResourceParameterNames.SECRET_PATH,
      SECRET_PATH),
    new ResourceParameter(HcpVaultDedicatedResourceParameterNames.FIELD_NAME,
      FIELD_NAME),
  };

  protected HcpVaultDedicatedSecretProvider(String valueType) {
    super(valueType, PARAMETERS);
  }

  protected HcpVaultDedicatedSecretProvider(String valueType, ResourceParameter[] additionalParameters) {
    super(valueType, ResourceParameterUtils.combineParameters(PARAMETERS, additionalParameters));
  }

  /**
   * <p>
   * Retrieves a secret from HashiCorp Vault Dedicated based on parameters
   * provided in {@code parameterValues}. This method centralizes secret
   * retrieval logic and is used by subclasses implementing
   * the {@link oracle.jdbc.spi.OracleResourceProvider} SPI.
   * </p>
   * <p>
   * If the secret is stored in JSON format with multiple fields, a specific field
   * can be extracted using the "fieldName" parameter. If no field name is provided
   * and the JSON contains a single key, that key’s value is returned. If
   * multiple keys exist and no field name is specified, an exception is thrown.
   * </p>
   *
   * @param parameterValues A map of parameter names and their corresponding
   * -values required for secret retrieval. Must not be null.
   * @return The extracted secret value as a {@code String}.
   */
  protected final String getSecret(Map<Parameter, CharSequence> parameterValues) {
    Map<Parameter, CharSequence> resolvedValues =
      resolveMissingParameters(parameterValues, HcpVaultDedicatedResourceProvider.PARAMETERS);
    String secretJson = getResource(
      DedicatedVaultSecretsManagerFactory.getInstance(), resolvedValues);
    OracleJsonObject secretJsonObj = JsonUtil.convertJsonToOracleJsonObject(secretJson);
    ResourceParameter fieldNameParam = Stream.of(PARAMETERS)
      .filter(param -> param.name().equals(HcpVaultDedicatedResourceParameterNames.FIELD_NAME))
      .findFirst()
      .orElse(null);

    String fieldName = parameterValues.containsKey(fieldNameParam) ?
      parameterValues.get(fieldNameParam).toString() : null;
    return JsonUtil.extractSecret(secretJsonObj, fieldName);
  }

}
