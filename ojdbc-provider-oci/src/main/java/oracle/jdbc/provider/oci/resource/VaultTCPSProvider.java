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

import oracle.jdbc.provider.oci.vault.Secret;
import oracle.jdbc.provider.oci.vault.SecretFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.resource.ResourceParameter;
import oracle.jdbc.provider.util.TlsUtils;
import oracle.jdbc.spi.TlsConfigurationProvider;

import javax.net.ssl.SSLContext;
import java.util.Base64;
import java.util.Map;

import static oracle.jdbc.provider.oci.vault.SecretFactory.OCID;
import static oracle.jdbc.provider.util.CommonParameters.TYPE;
import static oracle.jdbc.provider.util.CommonParameters.PASSWORD;

/**
 * <p>
 * A provider for TCPS/TLS files used to establish secure TLS communication
 * with an Autonomous Database. The file is retrieved from OCI Vault, where
 * it is stored as a base64-encoded string. This provider supports different
 * file types including SSO, PKCS12, and PEM formats.
 * </p>
 * <p>
 * The type of the file must be explicitly specified using the {@code type}
 * parameter. Based on the type, the file may contain private keys and
 * certificates for establishing secure communication. A password is only
 * required
 * for PKCS12 or encrypted PEM files.
 * </p>
 * <p>
 * This class implements the {@link TlsConfigurationProvider} SPI defined by
 * Oracle JDBC and is designed to be instantiated via
 * {@link java.util.ServiceLoader}.
 * </p>
 */
public class VaultTCPSProvider
        extends OciResourceProvider
        implements TlsConfigurationProvider {

  private static final ResourceParameter[] PARAMETERS = {
          new ResourceParameter("ocid", OCID),
          new ResourceParameter("walletPassword", PASSWORD),
          new ResourceParameter("type", TYPE)
  };

  /**
   * A public no-arg constructor used by {@link java.util.ServiceLoader} to
   * construct an instance of this provider.
   */
  public VaultTCPSProvider() {
    super("vault-tls", PARAMETERS);
  }

  /**
   * Retrieves an SSLContext by loading a file from OCI Vault and configuring it
   * for secure TLS communication with Autonomous Database.
   * <p>
   * The file is stored in OCI Vault as a base64-encoded string. The type of
   * the file
   * (SSO, PKCS12, or PEM) must be explicitly provided, and the method
   * processes the file
   * data accordingly, extracting keys and certificates, and creating an
   * SSLContext.
   * </p>
   *
   * @param parameterValues The parameters required to access the file,
   * including the OCID, password (if applicable), and file type (SSO,
   * PKCS12, PEM).
   * @return An initialized SSLContext for establishing secure communications.
   * @throws IllegalStateException If the SSLContext cannot be created due to
   * errors during processing.
   */
  @Override
  public SSLContext getSSLContext(Map<Parameter, CharSequence> parameterValues) {
    try {
      ParameterSet parameterSet = parseParameterValues(parameterValues);
      Secret secret = SecretFactory
              .getInstance()
              .request(parameterSet)
              .getContent();

      byte[] fileBytes = Base64
              .getDecoder()
              .decode(secret.getBase64Secret());

      char[] password = parameterSet.getOptional(PASSWORD) != null
              ? parameterSet.getOptional(PASSWORD).toCharArray()
              : null;

      String type = parameterSet.getRequired(TYPE);
      return TlsUtils.createSSLContext(fileBytes, password, type);
    } catch (Exception e) {
      throw new IllegalStateException
              ("Failed to create SSLContext from the file", e);
    }
  }

}