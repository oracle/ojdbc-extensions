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

import com.azure.core.util.UrlBuilder;
import oracle.jdbc.provider.azure.keyvault.KeyVaultSecretFactory;
import oracle.jdbc.provider.parameter.ParameterSetBuilder;
import oracle.jdbc.provider.parameter.ParameterSetParser;

/**
 * A URL parser used by {@link AzureVaultSecretProvider} and {@link AzureVaultJsonProvider}.
 */
public class AzureVaultURLParser {
  /**
   * Parser that recognizes the "value" field and parses it as a Key Vault
   * secret URL.
   * {@link AzureConfigurationParameters#configureBuilder(ParameterSetParser.Builder)}
   * configures the parser to recognize fields of the nested JSON object named
   * "authentication".
   */
  public static final ParameterSetParser PARAMETER_SET_PARSER =
    AzureConfigurationParameters.configureBuilder(ParameterSetParser.builder()
        .addParameter("value", AzureVaultURLParser::parseVaultSecretUri))
      .build();

  /**
   * Parses the "value" field of a JSON object as a vault URI with the path
   * of a named secret. An example URI is:
   * <pre>
   * https://mykeyvaultpfs.vault.azure.net/secrets/mySecret2
   * </pre>
   * This parser configures the given {@code builder} with two distinct
   * parameters accepted by {@link KeyVaultSecretFactory}: One parameter for the
   * vault URL, and another for the secret name.
   * @param vaultSecretUri Vault Secret URI which contains the path of a secret.
   *                      Not null.
   * @param builder Builder to configure with parsed parameters. Not null.
   */
  private static void parseVaultSecretUri(
    String vaultSecretUri, ParameterSetBuilder builder) {

    UrlBuilder urlBuilder = UrlBuilder.parse(vaultSecretUri);

    String vaultUrl = "https://" + urlBuilder.getHost();
    builder.add("value", KeyVaultSecretFactory.VAULT_URL, vaultUrl);

    String path = urlBuilder.getPath();

    if (!path.contains("/secrets"))
      throw new IllegalArgumentException("The Vault Secret URI should " +
        "contain \"/secrets\" following by the name of the Secret: " +
        vaultSecretUri);

    String secretName = path.replace("/secrets", "");
    builder.add("value", KeyVaultSecretFactory.SECRET_NAME, secretName);
  }
}
