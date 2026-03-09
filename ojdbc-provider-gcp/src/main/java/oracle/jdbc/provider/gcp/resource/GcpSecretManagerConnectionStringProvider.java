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

package oracle.jdbc.provider.gcp.resource;

import oracle.jdbc.provider.resource.ResourceParameter;
import oracle.jdbc.provider.util.TNSNames;
import oracle.jdbc.spi.ConnectionStringProvider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static oracle.jdbc.provider.util.CommonParameters.TNS_ALIAS;
import static oracle.jdbc.provider.util.FileUtils.decodeIfBase64;


/**
 * <p>
 * A provider for securely retrieving the connection string from a tnsnames.ora
 * file stored in GCP Secret Manager for use with an Oracle Autonomous Database.
 * The tnsnames.ora file can be stored either as a base64-encoded secret or in
 * raw binary format in GCP Secret Manager. This provider automatically
 * detects and decodes base64-encoded content as needed before parsing the
 * file to retrieve connection strings based on specified aliases.
 * </p>
 * <p>
 * This class implements the {@link ConnectionStringProvider} SPI defined by
 * Oracle JDBC. It is designed to be instantiated via {@link java.util.ServiceLoader}.
 * </p>
 */
public class GcpSecretManagerConnectionStringProvider
        extends GcpSecretManagerProvider
        implements ConnectionStringProvider {

  private static final ResourceParameter[] TNS_NAMES_PARAMETERS = {
          new ResourceParameter("tnsAlias", TNS_ALIAS)
  };

  /**
   * A public no-arg constructor used by {@link java.util.ServiceLoader} to
   * construct an instance of this provider.
   */
  public GcpSecretManagerConnectionStringProvider() {
    super("secretmanager-tnsnames", TNS_NAMES_PARAMETERS);
  }

  /**
   * Retrieves a database connection string from the tnsnames.ora file stored
   * in GCP Secret Manager.
   * <p>
   * This method accesses the file in GCP Secret Manager, detects if the content
   * is base64-encoded, and decodes it if necessary. The tnsnames.ora content is
   * then parsed to locate the connection string associated with the specified
   * alias.
   * </p>
   *
   * @param parameterValues The parameters required to access the tnsnames.ora
   * file in GCP Secret Manager, including the secret name and the tns-alias.
   * @return The connection string associated with the specified alias
   * in the tnsnames.ora file.
   * @throws IllegalStateException If there is an error reading the tnsnames.ora
   * file or accessing the GCP Secret Manager.
   * @throws IllegalArgumentException If the specified alias is invalid or
   * does not exist in the tnsnames.ora file.
   */
  @Override
  public String getConnectionString(Map<Parameter, CharSequence> parameterValues) {
    byte[] fileBytes = decodeIfBase64(getSecret(parameterValues).toByteArray());

    TNSNames tnsNames;
    try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
      tnsNames = TNSNames.read(inputStream);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read tnsnames.ora content", e);
    }

    String alias;
    try {
      alias = parseParameterValues(parameterValues).getRequired(TNS_ALIAS);
    } catch (IllegalStateException e) {
      throw new IllegalArgumentException(
              "Required parameter 'tnsAlias' is missing", e
      );
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