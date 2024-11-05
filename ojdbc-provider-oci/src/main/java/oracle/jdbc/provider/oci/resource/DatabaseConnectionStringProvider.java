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

package oracle.jdbc.provider.oci.resource;

import oracle.jdbc.provider.oci.database.WalletFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.resource.ResourceParameter;
import oracle.jdbc.provider.util.Wallet;
import oracle.jdbc.spi.ConnectionStringProvider;

import java.util.Locale;
import java.util.Map;

import static oracle.jdbc.provider.util.CommonParameters.CONSUMER_GROUP;

/**
 * <p>
 * A provider of connection strings for an Autonomous Database. The connection
 * string is extracted the tnsnames.ora file of a wallet zip.
 * </p><p>
 * This class implements the {@link ConnectionStringProvider} SPI defined by
 * Oracle JDBC. It is designed to be located and instantiated by
 * {@link java.util.ServiceLoader}.
 * </p>
 */
public final class DatabaseConnectionStringProvider
  extends OciResourceProvider
  implements ConnectionStringProvider {

  private static final ResourceParameter[] PARAMETERS =
    new ResourceParameter[] {
      new ResourceParameter("ocid", WalletFactory.OCID),
      new ResourceParameter("consumer-group", CONSUMER_GROUP, "MEDIUM")
    };

  /**
   * Public no-arg constructor called by {@link java.util.ServiceLoader}
   */
  public DatabaseConnectionStringProvider() {
    super("database-connection-string", PARAMETERS);
  }

  @Override
  public String getConnectionString(
    Map<Parameter, CharSequence> parameterValues) {

    ParameterSet parameterSet = parseParameterValues(parameterValues);

    Wallet wallet = WalletFactory.getInstance()
        .request(parameterSet)
        .getContent();

    String consumerGroup =
      parameterSet.getRequired(CONSUMER_GROUP)
        .toUpperCase(Locale.ENGLISH);

    switch (consumerGroup) {
      case "HIGH": return wallet.getHighConnectionString();
      case "MEDIUM": return wallet.getMediumConnectionString();
      case "LOW": return wallet.getLowConnectionString();
      case "TP": return wallet.getTransactionProcessingConnectionString();
      case "TPURGENT":
        return wallet.getTransactionProcessingUrgentConnectionString();
      default:
        throw new IllegalArgumentException(
            "Unrecognized consumer group: " + consumerGroup);
    }
  }
}
