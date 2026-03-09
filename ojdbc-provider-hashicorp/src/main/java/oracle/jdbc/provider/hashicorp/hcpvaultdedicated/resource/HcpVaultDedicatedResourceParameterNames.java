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

package oracle.jdbc.provider.hashicorp.hcpvaultdedicated.resource;

/**
 * Centralized parameter name constants used by HCP Vault Dedicated resource providers.
 */
public class HcpVaultDedicatedResourceParameterNames {

  private HcpVaultDedicatedResourceParameterNames() {}

  public static final String VAULT_ADDR = "vaultAddr";
  public static final String VAULT_NAMESPACE = "vaultNamespace";
  public static final String VAULT_USERNAME = "vaultUsername";
  public static final String VAULT_PASSWORD = "vaultPassword";
  public static final String VAULT_TOKEN = "vaultToken";
  public static final String ROLE_ID = "roleId";
  public static final String SECRET_ID = "secretId";
  public static final String USERPASS_AUTH_PATH = "userPassAuthPath";
  public static final String APPROLE_AUTH_PATH = "appRoleAuthPath";
  public static final String GITHUB_TOKEN = "githubToken";
  public static final String GITHUB_AUTH_PATH = "githubAuthPath";

  public static final String SECRET_PATH = "secretPath";
  public static final String FIELD_NAME = "fieldName";

  public static final String TNS_ALIAS = "tnsAlias";
  public static final String CONNECTION_STRING_INDEX = "connectionStringIndex";
  public static final String WALLET_PASSWORD = "walletPassword";
  public static final String TYPE = "type";
}
