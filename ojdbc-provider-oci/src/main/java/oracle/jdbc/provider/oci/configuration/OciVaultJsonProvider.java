/*
 ** Copyright (c) 2026 Oracle and/or its affiliates.
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

import oracle.jdbc.driver.configuration.OracleConfigurationParsableProvider;
import oracle.jdbc.provider.oci.vault.SecretFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.util.configuration.OracleConfigurationCache;
import oracle.jdbc.util.configuration.OracleConfiguration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * A provider for configuration payloads stored in OCI Vault. Payloads are parsed
 * by {@link OracleConfigurationParsableProvider}; JSON is used by default, and
 * other parser types such as Pkl may be selected with the {@code parser} option.
 **/
public class OciVaultJsonProvider extends OracleConfigurationParsableProvider {

  private static final OracleConfigurationCache<String, OracleConfiguration> CACHE = OracleConfigurationCache.create(100);

  /**
   * {@inheritDoc}
   * <p>
   * Returns the configuration payload stored in OCI Vault Secret.
   * </p><p>The {@code secretOcid} is an OCID of Vault Secret which can be
   * acquired on the OCI Web Console. The payload is stored in the Secret
   * Contents of Vault Secret.
   * </p>
   * @param secretOcid the OCID of secret used by this provider to retrieve
   *                   the payload from OCI
   * @return configuration payload
   **/
  @Override
  public InputStream getInputStream(String secretOcid) {
    final String valueFieldName = "value";
    Map<String, String> optionsWithOcid = new HashMap<>(options);
    // "parser" is consumed by OracleConfigurationParsableProvider, not OCI.
    optionsWithOcid.remove("parser");
    optionsWithOcid.put(valueFieldName, secretOcid);

    ParameterSet parameters =
      OciConfigurationParameters.getParser()
        .parseNamedValues(optionsWithOcid);

    String secretContent = SecretFactory.getInstance()
      .request(parameters)
      .getContent()
      .getBase64Secret();

    InputStream inputStream = new ByteArrayInputStream(
        Base64.getDecoder().decode(secretContent));
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
    return "ocivault";
  }

  /**
   * {@inheritDoc}
   * @return cache of this provider which is used to store configuration
   */
  @Override
  public OracleConfigurationCache<String, OracleConfiguration> getCache() {
    return CACHE;
  }
}
