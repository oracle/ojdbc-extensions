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

import oracle.jdbc.provider.parameter.ParameterSet;
import java.util.Map;

import static oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication.DedicatedVaultParameters.*;

/**
 * Handles authentication using the AppRole method for HashiCorp Vault.
 */
public class AppRoleAuthentication extends AbstractDedicatedVaultAuthentication {

  /**
   * Singleton instance of {@link AppRoleAuthentication}.
   */
  public static final AppRoleAuthentication INSTANCE = new AppRoleAuthentication();

  private AppRoleAuthentication() {
    // Private constructor to prevent external instantiation
  }

  @Override
  public CachedToken generateToken(ParameterSet parameterSet) {
    String vaultAddr = getVaultAddress(parameterSet);
    String namespace = getNamespace(parameterSet);
    String roleId = getRoleId(parameterSet);
    String secretId = getSecretId(parameterSet);
    String authPath = getAppRoleAuthPath(parameterSet);

    String authEndpoint = buildAuthEndpoint(vaultAddr, APPROLE_LOGIN_TEMPLATE, authPath);
    String payload = createJsonPayload(APPROLE_PAYLOAD_TEMPLATE, roleId, secretId);
    return performAuthentication(authEndpoint, payload, namespace, null, "Failed to authenticate with AppRole");
  }

  @Override
  public Map<String, Object> generateCacheKey(ParameterSet parameterSet) {
    return parameterSet.filterParameters(new String[]{
            PARAM_VAULT_ADDR, PARAM_VAULT_NAMESPACE, PARAM_APPROLE_AUTH_PATH, PARAM_VAULT_ROLE_ID, PARAM_VAULT_SECRET_ID
    });
  }
}
