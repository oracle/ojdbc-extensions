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

import oracle.jdbc.provider.resource.ResourceParameter;
import oracle.jdbc.spi.PasswordProvider;
import oracle.jdbc.spi.UsernameProvider;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.util.WalletUtils;

import java.util.Base64;
import java.util.Map;

import static oracle.jdbc.provider.util.CommonParameters.CONNECTION_STRING_INDEX;
import static oracle.jdbc.provider.util.CommonParameters.PASSWORD;

/**
 * <p>
 * A provider for an Oracle SEPS (Secure External Password Store) wallet stored
 * as a base64-encoded secret in AWS Secrets Manager.
 * </p>
 * <p>
 * This class supports both PKCS12 and SSO wallet formats. It implements the
 * {@link UsernameProvider} and {@link PasswordProvider} SPIs defined by Oracle JDBC.
 * The wallet is decoded and parsed to extract credentials.
 * </p>
 * <p>
 * It is designed to be instantiated via {@link java.util.ServiceLoader}.
 * </p>
 */
public class SecretsManagerSEPSProvider
  extends SecretsManagerSecretProvider
  implements UsernameProvider, PasswordProvider {

  private static final ResourceParameter[] PARAMETERS = {
    new ResourceParameter("walletPassword", PASSWORD),
    new ResourceParameter("connectionStringIndex", CONNECTION_STRING_INDEX)
  };

  /**
   * A public no-arg constructor used by {@link java.util.ServiceLoader} to
   * construct an instance of this provider.
   */
  public SecretsManagerSEPSProvider() {
    super("secrets-manager-seps", PARAMETERS);
  }

  @Override
  public String getUsername(Map<Parameter, CharSequence> parameterValues) {
    return getWalletCredentials(parameterValues).username();
  }

  @Override
  public char[] getPassword(Map<Parameter, CharSequence> parameterValues) {
    return getWalletCredentials(parameterValues).password();
  }

  /**
   * Retrieves the OracleWallet by decoding the base64-encoded wallet stored
   * in AWS Secrets Manager and opening it as either SSO or PKCS12, based on
   * whether a password is provided.
   */
  private WalletUtils.Credentials getWalletCredentials(Map<Parameter, CharSequence> parameterValues) {
    ParameterSet parameterSet = parseParameterValues(parameterValues);
    String secret = getSecret(parameterValues);

    byte[] walletBytes = Base64.getDecoder().decode(secret);

    char[] walletPassword = parameterSet.getOptional(PASSWORD) != null
      ? parameterSet.getOptional(PASSWORD).toCharArray() : null;

    String connectionStringIndex = parameterSet.getOptional(CONNECTION_STRING_INDEX);
    return WalletUtils.getCredentials(walletBytes, walletPassword, connectionStringIndex);
  }
}
