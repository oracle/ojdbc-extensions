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

package oracle.jdbc.provider.oci.resource;

import oracle.jdbc.provider.resource.ResourceParameter;
import oracle.jdbc.provider.util.TNSNames;
import oracle.jdbc.spi.ConnectionStringProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Map;

import static oracle.jdbc.provider.oci.vault.SecretFactory.OCID;
import static oracle.jdbc.provider.util.CommonParameters.TNS_ALIAS;

/**
 * <p>
 * A provider for securely retrieving the connection string from a tnsnames.ora
 * file stored in OCI Vault for use with an Oracle Autonomous Database.
 * The tnsnames.ora file can be stored as plain text or base64-encoded content
 * in OCI Vault. This class decodes and parses the content to select
 * connection strings based on specified aliases.
 * </p>
 * <p>
 * This class implements the {@link ConnectionStringProvider} SPI defined by
 * Oracle JDBC.
 * It is designed to be instantiated via {@link java.util.ServiceLoader}.
 * </p>
 */
public class VaultConnectionStringProvider
        extends OciResourceProvider
        implements ConnectionStringProvider {

  private static final ResourceParameter[] PARAMETERS = {
          new ResourceParameter("ocid", OCID),
          new ResourceParameter("tnsAlias", TNS_ALIAS)
  };

  /**
   * A public no-arg constructor used by {@link java.util.ServiceLoader} to
   * construct an instance of this provider.
   */
  public VaultConnectionStringProvider() {
    super("vault-tnsnames", PARAMETERS);
  }

  /**
   * Retrieves a database connection string from the tnsnames.ora file stored
   * in OCI Vault.
   * <p>
   * This method accesses the file in OCI Vault, decodes it if it is
   * base64-encoded, and parses the `tnsnames.ora` content. The method
   * returns the connection string associated with the specified alias from
   * the `tnsnames.ora` file.
   * The file can either be stored as plain text or as base64-encoded content
   * in the vault.
   * </p>
   *
   * @param parameterValues The parameters required to access the `tnsnames.ora`
   * file in OCI Vault, including the vault OCID and the tns-alias.
   * @return The connection string associated with the specified alias
   * in the `tnsnames.ora` file.
   * @throws IllegalStateException If there is an error reading the `tnsnames.ora`
   * file, decoding its content, or accessing the OCI Vault.
   * @throws IllegalArgumentException If the specified alias is invalid or
   * does not exist in the `tnsnames.ora` file.
   */
  @Override
  public String getConnectionString(Map<Parameter, CharSequence> parameterValues) {

    String alias;
    try {
      alias = parseParameterValues(parameterValues).getRequired(TNS_ALIAS);
    } catch (IllegalStateException e) {
      throw new IllegalArgumentException(
              "Required parameter 'tnAlias' is missing", e
      );
    }

    // Retrieve the secret containing tnsnames.ora content from OCI Vault
    String secretValue = getVaultSecret(parameterValues)
                           .getBase64Secret();

    byte[] fileBytes = Base64.getDecoder().decode(secretValue);

    TNSNames tnsNames;
    try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
      tnsNames = TNSNames.read(inputStream);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read tnsnames.ora content", e);
    }

    String connectionString = tnsNames.getConnectionStringByAlias(alias);
    if (connectionString == null) {
      throw new IllegalArgumentException(
              "Alias specified does not exist in tnsnames.ora: " + alias
      );
    }
    return connectionString;
  }
}