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
import oracle.jdbc.spi.TlsConfigurationProvider;

import javax.net.ssl.SSLContext;
import java.util.Map;

/**
 * <p>
 * A provider of keys and certificates for mTLS communication with an
 * Autonomous Database. The certificates are extracted from a wallet zip
 * that is requested from OCI.
 * </p><p>
 * This class implements the {@link TlsConfigurationProvider} SPI defined by
 * Oracle JDBC. It is designed to be located and instantiated by
 * {@link java.util.ServiceLoader}.
 * </p>
 */
public final class DatabaseTlsProvider
  extends OciResourceProvider
  implements TlsConfigurationProvider {

  private static final ResourceParameter[] PARAMETERS = {
    new ResourceParameter("ocid", WalletFactory.OCID)
  };

  /**
   * Public no-arg constructor called by {@link java.util.ServiceLoader}
   */
  public DatabaseTlsProvider() {
    super("database-tls", PARAMETERS);
  }

  /**
   * {@inheritDoc}
   * <p>
   * Returns trust and key material used to establish a TLS connection with
   * an Autonomous Database. The {@code configUri} is required to have an
   * "{@code ocid}" parameter that identifies the wallet.
   * </p>
   */
  @Override
  public SSLContext getSSLContext(
    Map<Parameter, CharSequence> parameterValues) {
    return getWallet(parameterValues)
            .getSSLContext();
  }

}
