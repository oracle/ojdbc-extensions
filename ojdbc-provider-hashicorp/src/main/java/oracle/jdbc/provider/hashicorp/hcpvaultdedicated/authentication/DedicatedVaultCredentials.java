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

package oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication;

/**
 * <p>
 * Holds the credentials required for authenticating with Dedicated
 * HashiCorp Vault.
 * </p><p>
 * This class encapsulates credentials used for making secure
 * requests to the Vault API, supporting both Vault tokens and
 * Userpass authentication.
 * </p>
 */
public final class DedicatedVaultCredentials {

  private final String vaultToken;
  private final String username;
  private final String password;

  /**
   * Constructs a new {@code DedicatedVaultCredentials} object with
   * the provided Vault token.
   *
   * @param vaultToken the token used to authenticate API requests to
   * the Vault. Must not be null or empty.
   */
  public DedicatedVaultCredentials(String vaultToken) {
    this.vaultToken = vaultToken;
    this.username = null;
    this.password = null;
  }

  /**
   * Constructs a new {@code DedicatedVaultCredentials} object with
   * the provided username and password.
   *
   * @param vaultToken the token used for authentication.
   * @param username the username for Userpass authentication.
   * @param password the password for Userpass authentication.
   */
  public DedicatedVaultCredentials(String vaultToken, String username, String password) {
    this.vaultToken = vaultToken;
    this.username = username;
    this.password = password;
  }

  /**
   * Returns the Vault token used for authentication.
   *
   * @return the Vault token as a {@link String}.
   */
  public String getVaultToken() {
    return vaultToken;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

}
