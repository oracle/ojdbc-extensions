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

import oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication.DedicatedVaultAuthenticationMethod;
import oracle.jdbc.provider.hashicorp.util.AbstractVaultResourceProvider;
import oracle.jdbc.provider.resource.ResourceParameter;

import java.util.stream.Stream;

import static oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication.DedicatedVaultParameters.*;
import static oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication.DedicatedVaultParameters.PASSWORD;

/**
 * Super class of all {@code OracleResourceProvider} implementations
 * that request a resource from Hcp Vault Dedicated. This super class defines
 * parameters for authentication with Hcp Vault Dedicated.
 */
public class HcpVaultDedicatedResourceProvider extends AbstractVaultResourceProvider {

   static final ResourceParameter[] PARAMETERS = {
     new ResourceParameter("authenticationMethod", AUTHENTICATION_METHOD,
       "auto-detect",
       HcpVaultDedicatedResourceProvider::parseAuthenticationMethod),
     new ResourceParameter("vaultAddr", VAULT_ADDR),
     new ResourceParameter("vaultNamespace", NAMESPACE, DEFAULT_NAMESPACE),
     new ResourceParameter("vaultUsername", USERNAME),
     new ResourceParameter("vaultPassword", PASSWORD),
     new ResourceParameter("vaultToken", VAULT_TOKEN),
     new ResourceParameter("roleId", ROLE_ID),
     new ResourceParameter("secretId", SECRET_ID),
     new ResourceParameter("userPassAuthPath", USERPASS_AUTH_PATH, DEFAULT_USERPASS_PATH),
     new ResourceParameter("appRoleAuthPath", APPROLE_AUTH_PATH, DEFAULT_APPROLE_PATH),
     new ResourceParameter("githubToken", GITHUB_TOKEN),
     new ResourceParameter("githubAuthPath", GITHUB_AUTH_PATH, DEFAULT_GITHUB_PATH),
  };

  /**
   * <p>
   * Constructs a provider identified by the name:
   * </p>
   * <pre>{@code
   *   ojdbc-provider-hcpvault-dedicated-{resourceType}
   * }</pre>
   * <p>
   * This constructor defines all parameters related to authentication with
   * HashiCorp Vault Dedicated.
   * Subclasses must call this constructor with any additional parameters for
   * the specific resource they provide.
   * </p>
   *
   * @param resourceType The resource type identifier that appears in the name of
   * the provider. Not null.
   * @param parameters Additional parameters specific to the subclass provider.
   */
  protected HcpVaultDedicatedResourceProvider(
    String resourceType, ResourceParameter... parameters) {
    super("hcpvault-dedicated", resourceType,
      Stream.concat(
        Stream.of(PARAMETERS),
        Stream.of(parameters))
      .toArray(ResourceParameter[]::new));
  }

  /**
   * Parses the "authentication-method" parameter into a recognized
   * {@link DedicatedVaultAuthenticationMethod}.
   *
   * @param authenticationMethod The authentication method as a string.
   * @return The corresponding {@link DedicatedVaultAuthenticationMethod}.
   * @throws IllegalArgumentException if the authentication method is unrecognized.
   */
  private static DedicatedVaultAuthenticationMethod parseAuthenticationMethod(String authenticationMethod) {
    switch (authenticationMethod) {
      case "vault-token":
        return DedicatedVaultAuthenticationMethod.VAULT_TOKEN;
      case "userpass":
        return DedicatedVaultAuthenticationMethod.USERPASS;
      case "approle":
        return DedicatedVaultAuthenticationMethod.APPROLE;
      case "github":
        return DedicatedVaultAuthenticationMethod.GITHUB;
      case "auto-detect":
        return DedicatedVaultAuthenticationMethod.AUTO_DETECT;
      default:
        throw new IllegalArgumentException("Unrecognized authentication method: " + authenticationMethod);
    }
  }

  /**
   * Maps ResourceParameter names to their corresponding environment variable keys.
   *
   * @param paramName The ResourceParameter name (e.g., "vaultAddr").
   * @return The corresponding environment variable key (e.g., "VAULT_ADDR").
   */
  @Override
   protected String getEnvVariableForParameter(String paramName) {
    switch (paramName) {
      case "vaultAddr": return PARAM_VAULT_ADDR;
      case "vaultNamespace": return PARAM_VAULT_NAMESPACE;
      case "vaultUsername": return PARAM_VAULT_USERNAME;
      case "vaultPassword": return PARAM_VAULT_PASSWORD;
      case "vaultToken": return PARAM_VAULT_TOKEN;
      case "roleId": return PARAM_VAULT_ROLE_ID;
      case "secretId": return PARAM_VAULT_SECRET_ID;
      case "userPassAuthPath": return PARAM_USERPASS_AUTH_PATH;
      case "appRoleAuthPath": return PARAM_APPROLE_AUTH_PATH;
      case "githubAuthPath": return PARAM_GITHUB_AUTH_PATH;
      default: return paramName;
    }
  }

}
