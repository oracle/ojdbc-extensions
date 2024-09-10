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
import oracle.jdbc.spi.PasswordProvider;
import oracle.jdbc.spi.UsernameProvider;
import oracle.security.pki.OracleSecretStore;
import oracle.security.pki.OracleWallet;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import static oracle.jdbc.provider.oci.vault.SecretFactory.OCID;

/**
 * <p>
 * A provider for retrieving both the username and password from a Secure
 * External Password Store (SEPS) wallet stored in Oracle Cloud
 * Infrastructure (OCI) Vault. The wallet can be in either SSO or PKCS12 format.
 * If a password is provided, the wallet is treated as PKCS12, otherwise, it
 * is assumed to be in SSO format.
 * </p>
 * <p>
 * This class implements the {@link UsernameProvider} and
 * {@link PasswordProvider} SPIs defined by Oracle JDBC.
 * It is designed to be located and instantiated by
 * {@link java.util.ServiceLoader}.
 * </p>
 *
 * <p>
 * The SEPS wallet is stored in the OCI Vault as a base64-encoded string and
 * is decoded when retrieved by this provider.
 * </p>
 */
public class VaultSEPSProvider
        extends OciResourceProvider
        implements UsernameProvider, PasswordProvider {

  private static final oracle.jdbc.provider.parameter.Parameter<String> PASSWORD =
          oracle.jdbc.provider.parameter.Parameter.create();

  /**
   * The default username secret created by the Oracle `mkstore` command.
   * <p>
   * This constant represents the default name for the username secret that is
   * generated when using Oracle's `mkstore` command to manage secure
   * credentials in Oracle Wallet. By default, Oracle uses the name
   * `oracle.security.client.default_username` for storing the username in
   * the wallet.
   * </p>
   * <p>
   * The `mkstore` command is used to create and manage secure credentials,
   * such as usernames, passwords, and connection strings, in an Oracle Wallet.
   * This provider retrieves the username from the wallet using this default
   * secret name.
   * </p>
   * <p>
   * For more details on the `mkstore` command and how it generates secrets,
   * refer to the Oracle documentation:
   * <a href="https://docs.oracle.com/en/database/oracle/oracle-database/23/dbseg/using-the-orapki-utility-to-manage-pki-elements.html#GUID-25509071-ABC0-4A0E-A3DB-4D4F61024F25">
   * Oracle mkstore Documentation</a>.
   * </p>
   */
  private static final String SECRET_STORE_DEFAULT_USERNAME =
          "oracle.security.client.default_username";

  /**
   * The default password secret created by the Oracle `mkstore` command.
   * <p>
   * This constant represents the default name for the password secret that is
   * generated when using Oracle's `mkstore` command to manage secure
   * credentials in Oracle Wallet. By default, Oracle uses the name
   * `oracle.security.client.default_password` for storing the password in
   * the wallet.
   * </p>
   * <p>
   * Similar to the username, this provider retrieves the password from the
   * wallet using this default secret name. The `mkstore` command is a
   * standard tool for managing credentials securely in Oracle Wallet.
   * </p>
   * <p>
   * For further details on using `mkstore` to manage credentials in an Oracle
   * Wallet, refer to the official Oracle documentation:
   * <a href="https://docs.oracle.com/en/database/oracle/oracle-database/23/dbseg/using-the-orapki-utility-to-manage-pki-elements.html#GUID-25509071-ABC0-4A0E-A3DB-4D4F61024F25">
   * Oracle mkstore Documentation</a>.
   * </p>
   */
  private static final String SECRET_STORE_DEFAULT_PASSWORD =
          "oracle.security.client.default_password";

  private static final ResourceParameter[] PARAMETERS = {
          new ResourceParameter("ocid", OCID),
          new ResourceParameter("walletPassword", PASSWORD)
  };

  /**
   * A public no-arg constructor used by {@link java.util.ServiceLoader} to
   * construct an instance of this provider.
   */
  public VaultSEPSProvider() {
    super("vault-seps", PARAMETERS);
  }

  @Override
  public String getUsername(Map<Parameter, CharSequence> parameterValues) {
    return extractCredentials(getWallet(parameterValues)).username;
  }

  @Override
  public char[] getPassword(Map<Parameter, CharSequence> parameterValues) {
    return extractCredentials(getWallet(parameterValues)).password;
  }

  /**
   * Retrieves the OracleWallet by decoding the base64-encoded wallet stored
   * in OCI Vault and opening it as either SSO or PKCS12, based on whether a
   * password is provided.
   */
  private OracleWallet getWallet(Map<Parameter, CharSequence> parameterValues) {
    ParameterSet parameterSet = parseParameterValues(parameterValues);
    Secret secret = SecretFactory.getInstance()
            .request(parameterSet)
            .getContent();

    char[] walletPassword = parameterSet.getOptional(PASSWORD) != null
            ? parameterSet.getOptional(PASSWORD).toCharArray()
            : null;
    try {
      return openWallet(secret,walletPassword);
    } catch (IOException e) {
      throw new RuntimeException("Failed to open wallet from secret", e);
    }
  }

  /**
   * Opens the SEPS wallet, using the provided secret and password. The
   * wallet is decoded from its base64 form.
   *
   * @param secret The secret retrieved from OCI Vault containing the wallet
   * data.
   * @param walletPassword The password for the wallet, or {@code null} for
   * SSO wallets.
   * @return An OracleWallet object initialized with the wallet data.
   * @throws IOException If an I/O error occurs while opening the wallet.
   */
  private OracleWallet openWallet(Secret secret, char[] walletPassword)
          throws IOException {
    byte[] walletBytes = Base64.getDecoder().decode(secret.getBase64Secret());
    OracleWallet wallet = new OracleWallet();
    wallet.setWalletArray(walletBytes, walletPassword);
    return wallet;
  }

  /**
   * Extracts the username and password from the OracleSecretStore in the
   * OracleWallet.
   * <p>
   * This method retrieves credentials from the wallet using the default
   * aliases generated by Oracle's `mkstore` command:
   * <ul>
   *   <li>{@code oracle.security.client.default_username} for the username</li>
   *   <li>{@code oracle.security.client.default_password} for the password</li>
   * </ul>
   * These default aliases are used to securely fetch credentials stored
   * in Oracle Wallet.
   * </p>
   * <p>
   */
  private Credentials extractCredentials(OracleWallet wallet) {
    try {
      OracleSecretStore secretStore = wallet.getSecretStore();
      String username = null;
      char[] password = null;

      if (secretStore.containsAlias(SECRET_STORE_DEFAULT_USERNAME)) {
        username = new String(secretStore.getSecret(SECRET_STORE_DEFAULT_USERNAME));
      }
      if (secretStore.containsAlias(SECRET_STORE_DEFAULT_PASSWORD)) {
        password = secretStore.getSecret(SECRET_STORE_DEFAULT_PASSWORD);
      }

      return new Credentials(username, password);
    } catch (Exception e) {
      throw new RuntimeException("Failed to extract credentials from wallet", e);
    }
  }

  /**
   * Inner class to hold both the username and password.
   */
  private static final class Credentials {
    final String username;
    final char[] password;

    Credentials(String username, char[] password) {
      this.username = username;
      this.password = password;
    }
  }

}