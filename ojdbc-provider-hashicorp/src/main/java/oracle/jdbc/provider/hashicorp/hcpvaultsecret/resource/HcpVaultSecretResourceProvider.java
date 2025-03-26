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

package oracle.jdbc.provider.hashicorp.hcpvaultsecret.resource;

import oracle.jdbc.provider.hashicorp.hcpvaultsecret.authentication.HcpVaultAuthenticationMethod;
import oracle.jdbc.provider.hashicorp.util.AbstractVaultResourceProvider;
import oracle.jdbc.provider.resource.ResourceParameter;

import java.util.stream.Stream;

import static oracle.jdbc.provider.hashicorp.hcpvaultsecret.authentication.HcpVaultSecretParameters.*;

/**
 * Super class of all {@code OracleResourceProvider} implementations
 * that request a resource from HCP Vault Secrets. This class defines
 * parameters for authentication with HCP Vault Secrets.
 */
public class HcpVaultSecretResourceProvider extends AbstractVaultResourceProvider {

  static final ResourceParameter[] PARAMETERS = {
    new ResourceParameter("authenticationMethod", AUTHENTICATION_METHOD,
      "auto-detect",
      HcpVaultSecretResourceProvider::parseAuthenticationMethod),
    new ResourceParameter("orgId", HCP_ORG_ID),
    new ResourceParameter("projectId", HCP_PROJECT_ID),
    new ResourceParameter("appName", HCP_APP_NAME),
    new ResourceParameter("clientId", HCP_CLIENT_ID),
    new ResourceParameter("clientSecret", HCP_CLIENT_SECRET),
    new ResourceParameter("credentialsFile", HCP_CREDENTIALS_FILE, DEFAULT_CREDENTIALS_FILE_PATH),
  };

  protected HcpVaultSecretResourceProvider(String resourceType, ResourceParameter... additionalParameters) {
    super("hcpvault-secrets", resourceType,
      Stream.concat(
        Stream.of(PARAMETERS),
        Stream.of(additionalParameters))
      .toArray(ResourceParameter[]::new));
  }

  private static HcpVaultAuthenticationMethod parseAuthenticationMethod(String method) {
    switch (method) {
      case "client-credentials":
        return HcpVaultAuthenticationMethod.CLIENT_CREDENTIALS;
      case "cli-credentials-file":
        return HcpVaultAuthenticationMethod.CLI_CREDENTIALS_FILE;
      case "auto-detect":
        return HcpVaultAuthenticationMethod.AUTO_DETECT;
      default:
        throw new IllegalArgumentException("Unrecognized authentication method: " + method);
    }
  }

  @Override
  protected String getEnvVariableForParameter(String paramName) {
    switch (paramName) {
      case "orgId": return PARAM_HCP_ORG_ID;
      case "projectId": return PARAM_HCP_PROJECT_ID;
      case "appName": return PARAM_HCP_APP_NAME;
      case "clientId": return PARAM_HCP_CLIENT_ID;
      case "clientSecret": return PARAM_HCP_CLIENT_SECRET;
      case "credentialsFile": return PARAM_HCP_CREDENTIALS_FILE;
      default: return paramName;
    }
  }
}
