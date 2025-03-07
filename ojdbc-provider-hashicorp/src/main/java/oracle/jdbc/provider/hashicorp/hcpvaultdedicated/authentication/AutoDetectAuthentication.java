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

import java.util.Collections;
import java.util.Map;

/**
 * Automatically selects the best authentication method based on available parameters.
 * <p>
 * The selection priority is:
 * <ol>
 *   <li>VAULT_TOKEN</li>
 *   <li>USERPASS</li>
 *   <li>APPROLE</li>
 *   <li>GITHUB</li>
 * </ol>
 * The method attempts each authentication type in order until successful.
 */
public class AutoDetectAuthentication extends AbstractDedicatedVaultAuthentication{

  /**
   * Singleton instance of {@link AutoDetectAuthentication}.
   */
  public static final AutoDetectAuthentication INSTANCE = new AutoDetectAuthentication();

  private AutoDetectAuthentication() {
    // Private constructor to prevent external instantiation
  }

  @Override
  public CachedToken generateToken(ParameterSet parameterSet) {
    IllegalStateException previousFailure;

    try {
      return VaultTokenAuthentication.INSTANCE.generateToken(parameterSet);
    } catch (RuntimeException noVaultToken) {
      previousFailure = new IllegalStateException(
              "Failed to authenticate using a Vault token", noVaultToken
      );
    }

    try {
      return UserpassAuthentication.INSTANCE.generateToken(parameterSet);
    } catch (RuntimeException noUserpass) {
      previousFailure.addSuppressed(new IllegalStateException(
              "Failed to authenticate using Userpass credentials", noUserpass
      ));
    }

    try {
      return AppRoleAuthentication.INSTANCE.generateToken(parameterSet);
    } catch (RuntimeException noAppRole) {
      previousFailure.addSuppressed(new IllegalStateException(
              "Failed to authenticate using AppRole credentials", noAppRole
      ));
    }

    try {
      return GitHubAuthentication.INSTANCE.generateToken(parameterSet);
    } catch (RuntimeException noGitHub) {
      previousFailure.addSuppressed(new IllegalStateException(
              "Failed to authenticate using GitHub credentials", noGitHub
      ));
    }

    throw previousFailure;
  }

  @Override
  public Map<String, Object> generateCacheKey(ParameterSet parameterSet) {
    AbstractDedicatedVaultAuthentication[] authenticationMethods = {
      VaultTokenAuthentication.INSTANCE,
      UserpassAuthentication.INSTANCE,
      AppRoleAuthentication.INSTANCE,
      GitHubAuthentication.INSTANCE
    };

    for (AbstractDedicatedVaultAuthentication authentication : authenticationMethods) {
      Map<String, Object> cacheKey = authentication.generateCacheKey(parameterSet);
      if (!cacheKey.isEmpty()) {
        return cacheKey;
      }
    }
    return Collections.emptyMap();
  }
}
