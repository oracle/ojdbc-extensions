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

package oracle.jdbc.provider.hashicorp.hcpvaultsecret.resource;

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
 * file stored in HCP Vault Secrets for use with an Oracle Autonomous Database.
 * The tnsnames.ora file can be stored as a base64-encoded secret or as plain
 * text. The provider automatically detects the format and processes the
 * content accordingly to extract connection strings by alias.
 * </p>
 * <p>
 * This class implements the {@link ConnectionStringProvider} SPI defined by
 * Oracle JDBC and is designed to be instantiated via {@link java.util.ServiceLoader}.
 * </p>
 */
public class HcpVaultSecretConnectionStringProvider
        extends HcpVaultSecretProvider
        implements ConnectionStringProvider {

  private static final ResourceParameter[] TNS_NAMES_PARAMETERS = {
    new ResourceParameter(HcpVaultSecretResourceParameterNames.TNS_ALIAS, TNS_ALIAS)
  };

  /**
   * A public no-arg constructor used by {@link java.util.ServiceLoader} to
   * construct an instance of this provider.
   */
  public HcpVaultSecretConnectionStringProvider() {
    super("tnsnames", TNS_NAMES_PARAMETERS);
  }

  @Override
  public String getConnectionString(Map<Parameter, CharSequence> parameterValues) {
    String alias = parseParameterValues(parameterValues).getRequired(TNS_ALIAS);
    byte[] fileBytes = decodeIfBase64(getSecret(parameterValues).getBytes());

    TNSNames tnsNames;
    try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
      tnsNames = TNSNames.read(inputStream);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read tnsnames.ora content", e);
    }

    String connectionString = tnsNames.getConnectionStringByAlias(alias);
    if (connectionString == null) {
      throw new IllegalArgumentException(
              "Alias specified does not exist in tnsnames.ora: " + alias);
    }
    return connectionString;
  }
}
