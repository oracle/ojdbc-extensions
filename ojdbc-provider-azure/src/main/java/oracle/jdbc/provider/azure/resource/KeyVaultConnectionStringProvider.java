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

package oracle.jdbc.provider.azure.resource;

import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.resource.ResourceParameter;
import oracle.jdbc.provider.util.TNSNames;
import oracle.jdbc.spi.ConnectionStringProvider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

import static oracle.jdbc.provider.util.CommonParameters.CONSUMER_GROUP;


/**
 * <p>
 * A provider for securely retrieving the connection string from a tnsnames.ora
 * file stored in Azure Key Vault for use with an Oracle Autonomous Database.
 * The tnsnames.ora file is stored as a base64-encoded secret in Azure Key Vault
 * and is decoded, parsed, and used to select connection strings based on
 * predefined Consumer Groups (e.g., HIGH, MEDIUM, LOW, TRANSACTION_PROCESSING,
 * TRANSACTION_PROCESSING_URGENT).
 * </p>
 * <p>
 * This class implements the {@link ConnectionStringProvider} SPI defined by
 * Oracle JDBC.
 * It is designed to be instantiated via {@link java.util.ServiceLoader}.
 * </p>
 *
 */
public class KeyVaultConnectionStringProvider
        extends KeyVaultSecretProvider
        implements ConnectionStringProvider {

  private static final ResourceParameter[] CONSUMER_GROUP_PARAMETERS = {
          new ResourceParameter("consumer-group", CONSUMER_GROUP, "MEDIUM")
  };

  /**
   * A public no-arg constructor used by {@link java.util.ServiceLoader} to
   * construct an instance of this provider.
   */
  public KeyVaultConnectionStringProvider() {
    super("key-vault-tnsnames", CONSUMER_GROUP_PARAMETERS);
  }

  /**
   * Retrieves a database connection string from the tnsnames.ora file stored
   * in Azure Key Vault.
   * <p>
   * This method accesses the file in Azure Key Vault, decodes it if stored
   * in base64 format, and parses the tnsnames.ora content. It returns the
   * connection string for a specified consumer group (e.g., HIGH, MEDIUM,
   * LOW, TRANSACTION_PROCESSING, TRANSACTION_PROCESSING_URGENT) to allow
   * flexible connection configuration.
   * </p>
   *
   * @param parameterValues The parameters required to access the tnsnames.ora
   * file in Azure Key Vault, including the vault URL, the secret name, and
   * the consumer group.
   * @return The connection string associated with the specified consumer group
   * in the tnsnames.ora file.
   * @throws IllegalStateException If there is an error reading the tnsnames.ora file
   * or accessing the Azure Key Vault.
   * @throws IllegalArgumentException If the specified consumer group is invalid or
   * does not exist in the tnsnames.ora file.
   */
  @Override
  public String getConnectionString(Map<Parameter, CharSequence> parameterValues) {

    // Retrieve the secret containing tnsnames.ora content from Azure Key Vault
    String secretValue = getSecret(parameterValues);

    // Decode the base64-encoded tnsnames.ora content
    byte[] fileBytes = Base64.getDecoder().decode(secretValue);

    TNSNames tnsNames;
    try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
      tnsNames = TNSNames.read(inputStream);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read tnsnames.ora content", e);
    }

    String consumerGroupString = parseParameterValues(parameterValues)
            .getRequired(CONSUMER_GROUP)
            .toUpperCase(Locale.ENGLISH);

    TNSNames.ConsumerGroup consumerGroup;
    try {
      consumerGroup = TNSNames.ConsumerGroup.valueOf(consumerGroupString);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid consumer group specified: "
              + consumerGroupString, e);
    }

    String connectionString = tnsNames.getConnectionString(consumerGroup);
    if (connectionString == null) {
      throw new IllegalArgumentException("Consumer group specified is valid but does not exist in tnsnames.ora");
    }

    return connectionString;
  }
}