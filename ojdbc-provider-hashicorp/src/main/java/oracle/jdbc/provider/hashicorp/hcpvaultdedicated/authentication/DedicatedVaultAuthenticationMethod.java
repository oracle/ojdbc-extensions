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

package oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication;

/**
 * Enumeration of authentication methods supported by Dedicated HashiCorp Vault.
 */
public enum DedicatedVaultAuthenticationMethod {

  /**
   * Authentication using a Vault token.
   * <p>
   * The Vault token is a secure string used to authenticate API requests
   * to the HashiCorp Vault. It can be provided through configuration parameters,
   * environment variables, or system properties.
   * </p>
   */
  VAULT_TOKEN,

  /**
   * Authentication using the Userpass method.
   * <p>
   * The Userpass method allows authentication using a username and password.
   * It is suitable for scenarios where user credentials are managed directly
   * by Vault. For more information, see the HashiCorp Vault documentation:
   * <a href="https://developer.hashicorp.com/vault/api-docs/auth/userpass">
   * Userpass Authentication API</a>.
   * </p>
   */
  USERPASS,

  /**
   * Authentication using the AppRole method.
   * <p>
   * The AppRole method allows authentication using a Role ID and Secret ID.
   * This method is designed for machine-to-machine authentication or
   * service-based applications. For more information, see the HashiCorp Vault
   * documentation:
   * <a href="https://developer.hashicorp.com/vault/api-docs/auth/approle">
   * AppRole Authentication API</a>.
   * </p>
   */
  APPROLE,

  /**
   * Authentication using the GitHub method.
   * <p>
   * The GitHub method allows authentication using a GitHub personal access token.
   * This is particularly useful for applications or developers using GitHub
   * as an identity provider for Vault. For more information, see:
   * <a href="https://developer.hashicorp.com/vault/docs/auth/github">
   * GitHub Authentication API</a>.
   * </p>
   */
  GITHUB,

  /**
   * Automatically selects the best authentication method based on available parameters.
   *
   * <p>Priority order:</p>
   * <ol>
   *   <li>Uses the Vault token if available.</li>
   *   <li>Falls back to Userpass authentication.</li>
   *   <li>Then attempts AppRole authentication.</li>
   *   <li>Finally, tries GitHub authentication.</li>
   * </ol>
   */
  AUTO_DETECT
}
