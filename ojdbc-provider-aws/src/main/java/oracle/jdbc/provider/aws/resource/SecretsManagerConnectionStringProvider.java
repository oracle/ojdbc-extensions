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

import oracle.jdbc.spi.ConnectionStringProvider;
import oracle.jdbc.provider.resource.ResourceParameter;
import oracle.jdbc.provider.util.TNSNames;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import static oracle.jdbc.provider.util.CommonParameters.TNS_ALIAS;
import static oracle.jdbc.provider.util.FileUtils.isBase64Encoded;

/**
 * <p>
 * A provider for securely retrieving the connection string from a tnsnames.ora
 * file stored in AWS Secrets Manager for use with an Oracle Autonomous Database.
 * The tnsnames.ora file is stored as a base64-encoded or plain-text secret in
 * AWS Secrets Manager, and is decoded and parsed to select connection string
 * based on specified alias.
 * </p>
 * <p>
 * This class implements the {@link ConnectionStringProvider} SPI defined by
 * Oracle JDBC. It is designed to be instantiated via {@link java.util.ServiceLoader}.
 * </p>
 */
public class SecretsManagerConnectionStringProvider
  extends SecretsManagerSecretProvider
  implements ConnectionStringProvider {

  private static final ResourceParameter[] PARAMETERS = {
    new ResourceParameter(AwsSecretsManagerResourceParameterNames.TNS_ALIAS,
      TNS_ALIAS)
  };

  /**
   * A public no-arg constructor used by {@link java.util.ServiceLoader} to
   * construct an instance of this provider.
   */
  public SecretsManagerConnectionStringProvider() {
    super("secrets-manager-tnsnames", PARAMETERS);
  }

  /**
   * Retrieves a database connection string from the tnsnames.ora file stored
   * in AWS Secrets Manager.
   * <p>
   * This method accesses the file stored as a secret in AWS Secrets Manager,
   * attempts to decode it if it is base64-encoded, and then parses the
   * tnsnames.ora content. It returns the connection string associated with
   * the specified alias from the tnsnames.ora file.
   * </p>
   *
   * @param parameterValues The parameters required to access the tnsnames.ora
   * file in AWS Secrets Manager, including the secret name and the tnsAlias.
   * @return The connection string associated with the specified alias
   * in the tnsnames.ora file.
   * @throws IllegalStateException If there is an error reading the tnsnames.ora
   * file or accessing AWS Secrets Manager.
   * @throws IllegalArgumentException If the specified alias is invalid or
   * does not exist in the tnsnames.ora file.
   */
  @Override
  public String getConnectionString(Map<Parameter, CharSequence> parameterValues) {
    String alias = parseParameterValues(parameterValues).getRequired(TNS_ALIAS);
    byte[] secretBytes = getSecret(parameterValues).getBytes();

    byte[] fileBytes = isBase64Encoded(secretBytes)
      ? Base64.getDecoder().decode(secretBytes) : secretBytes;

    try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
      TNSNames tnsNames = TNSNames.read(inputStream);
      String connectionString = tnsNames.getConnectionStringByAlias(alias);
      if (connectionString == null) {
        throw new IllegalArgumentException("Alias specified does not exist in tnsnames.ora: " + alias);
      }
      return connectionString;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read tnsnames.ora content", e);
    }
  }

}
