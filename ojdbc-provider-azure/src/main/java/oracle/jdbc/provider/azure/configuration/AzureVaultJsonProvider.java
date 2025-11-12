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

package oracle.jdbc.provider.azure.configuration;

import oracle.jdbc.driver.configuration.OracleConfigurationParsableProvider;
import oracle.jdbc.provider.azure.keyvault.KeyVaultSecretFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.parameter.ParameterSetParser;
import oracle.jdbc.util.OracleConfigurationCache;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A provider for JSON payload which contains configuration from Azure Vault.
 * See {@link #getInputStream(String)} for the spec of the JSON payload.
 */
public class AzureVaultJsonProvider extends OracleConfigurationParsableProvider {

  /**
   * Parser that recognizes the "key" and "value" field,
   * and parses the "value" field as a Key Vault secret URL.
   * {@link AzureConfigurationParameters#configureBuilder(ParameterSetParser.Builder)}
   * configures the parser to recognize fields of the nested JSON object named
   * "authentication".
   */
  private static final Parameter<String> KEY = Parameter.create();

  private static final OracleConfigurationCache CACHE = OracleConfigurationCache.create(100);

  static final ParameterSetParser PARAMETER_SET_PARSER =
    AzureConfigurationParameters.configureBuilder(
      ParameterSetParser.builder()
        .addParameter("key", KEY, "")
        .addParameter("value", AzureVaultURLParser::parseVaultSecretUri))
        .build();

  /**
   * {@inheritDoc}
   * <p>
   * Returns the JSON payload stored in Azure Vault Secret.
   * </p><p>The {@code secretIdentifier} is an identifier of Vault Secret which
   * can be acquired on the Azure Web Console. The Json payload is stored in
   * the Secret Value of Vault Secret.
   * </p>
   * @param secretIdentifier the identifier of secret used by this
   *                         provider to retrieve JSON payload from Azure
   * @return JSON payload
   **/
  @Override
  public InputStream getInputStream(String secretIdentifier) {
    final String valueFieldName = "value";
    Map<String, String> optionsWithSecret = new HashMap<>(options);
    optionsWithSecret.put(valueFieldName, secretIdentifier);

    ParameterSet parameters = PARAMETER_SET_PARSER
        .parseNamedValues(optionsWithSecret);

    String secretContent = KeyVaultSecretFactory.getInstance()
      .request(parameters)
      .getContent()
      .getValue();

    InputStream inputStream =
      new ByteArrayInputStream(secretContent.getBytes());

    return inputStream;
  }

  /**
   * {@inheritDoc}
   * Returns type of this provider, which is a unique identifier for the
   * Service Provider Interface.
   *
   * @return type of this provider
   */
  @Override
  public String getType() {
    return "azurevault";
  }

  @Override
  public String getParserType(String arg0) {
    return "json";
  }

  /**
   * {@inheritDoc}
   * @return cache of this provider which is used to store configuration
   */
  @Override
  public OracleConfigurationCache getCache() {
    return CACHE;
  }
}
