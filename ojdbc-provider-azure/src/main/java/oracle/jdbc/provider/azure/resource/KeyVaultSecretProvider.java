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

package oracle.jdbc.provider.azure.resource;

import oracle.jdbc.provider.azure.keyvault.KeyVaultSecretFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.resource.ResourceParameter;
import oracle.jdbc.provider.util.ResourceParameterUtils;

import java.util.Map;

/**
 * <p>
 * A provider of secrets from Azure's Key Vault service. This class is designed
 * for inheritance by subclasses that implement an
 * {@link oracle.jdbc.spi.OracleResourceProvider} SPI defined by the Oracle JDBC
 * driver. This class defines defines parameters that configure a Key
 * Vault URL and the name of a secret.
 * </p>
 */
class KeyVaultSecretProvider extends AzureResourceProvider {

  private static final ResourceParameter[] PARAMETERS = {
    new ResourceParameter("vaultUrl", KeyVaultSecretFactory.VAULT_URL),
    new ResourceParameter("secretName", KeyVaultSecretFactory.SECRET_NAME)
  };

  protected KeyVaultSecretProvider(String valueType) {
    super(valueType, PARAMETERS);
  }

  public KeyVaultSecretProvider(String valueType, ResourceParameter[] additionalParameters) {
    super(valueType, ResourceParameterUtils
            .combineParameters(PARAMETERS, additionalParameters));
  }

  /**
   * <p>
   * Retrieves a secret from Azure Key Vault based on parameters provided
   * in {@code parameterValues}. This method centralizes secret retrieval
   * logic and can be used by subclasses implementing
   * the {@link oracle.jdbc.spi.OracleResourceProvider} SPI.
   * </p><p>
   * The method uses the {@code getResource} method to parse the
   * {@code parameterValues} and request the secret from Azure Key Vault via
   * the {@link KeyVaultSecretFactory} instance.
   * The secret value is returned as a {@code String}.
   * </p>
   *
   * @param parameterValues A map of parameter names and their corresponding
   * values required for secret retrieval. Must not be null.
   * @return The secret value as a {@code String}. Not null.
   */
  protected final String getSecret(
          Map<Parameter, CharSequence> parameterValues) {
    return getResource(KeyVaultSecretFactory.getInstance(), parameterValues)
            .getValue();
  }

}
