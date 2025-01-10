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

package oracle.jdbc.provider.hashicorp.hcpvaultsecret.secrets;

import oracle.jdbc.provider.cache.CachedResourceFactory;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.hashicorp.hcpvaultsecret.HcpVaultResourceFactory;
import oracle.jdbc.provider.hashicorp.hcpvaultsecret.authentication.HcpVaultCredentials;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;
import static oracle.jdbc.provider.util.ParameterUtil.getRequiredOrFallback;

/**
 * <p>
 * Factory for retrieving secrets from (HCP) Vault Secrets.
 * Responsible for making API calls to the HCP Vault and parsing responses.
 * </p>
 * <p>
 * The secrets API URL structure follows the format:
 * </p>
 * <pre>
 * {@code
 * https://api.cloud.hashicorp.com/secrets/2023-11-28/organizations/{ORG_ID}/projects/{PROJECT_ID}/apps/{APP_NAME}/secrets/{SECRET_NAME}:open
 * }
 * </pre>
 * <p>
 * For more details, refer to the official HCP Vault Secrets API documentation:
 * <a href="https://developer.hashicorp.com/hcp/tutorials/get-started-hcp-vault-secrets/hcp-vault-secrets-retrieve-secret">
 * Retrieve a Secret from HCP Vault Secrets
 * </a>
 * </p>
 */
public final class HcpVaultSecretsManagerFactory extends HcpVaultResourceFactory<String> {

  /**
   * Parameter for the organization ID. Required.
   */
  public static final Parameter<String> ORG_ID = Parameter.create(REQUIRED);

  /**
   * Parameter for the project ID. Required.
   */
  public static final Parameter<String> PROJECT_ID = Parameter.create(REQUIRED);

  /**
   * Parameter for the application name. Required.
   */
  public static final Parameter<String> APP_NAME = Parameter.create(REQUIRED);

  /**
   * Parameter for the secret name. Required.
   */
  public static final Parameter<String> SECRET_NAME = Parameter.create(REQUIRED);

  /**
   * Parameter for the optional key in the secret JSON.
   */
  public static final Parameter<String> KEY = Parameter.create();

  private static final String HCP_SECRETS_API_URL_FORMAT =
          "https://api.cloud.hashicorp.com/secrets/2023-11-28/organizations/%s/projects/%s/apps/%s/secrets/%s:open";


  private static final ResourceFactory<String> INSTANCE =
          CachedResourceFactory.create(new HcpVaultSecretsManagerFactory());

  private HcpVaultSecretsManagerFactory() {}

  public static ResourceFactory<String> getInstance() {
    return INSTANCE;
  }

  @Override
  public Resource<String> request(HcpVaultCredentials credentials, ParameterSet parameterSet) {
    String orgId = getRequiredOrFallback(parameterSet, ORG_ID, "ORG_ID");
    String projectId = getRequiredOrFallback(parameterSet, PROJECT_ID, "PROJECT_ID");
    String appName = getRequiredOrFallback(parameterSet, APP_NAME, "APP_NAME");
    String secretName = getRequiredOrFallback(parameterSet, SECRET_NAME, "SECRET_NAME");

    String hcpUrl = String.format(HCP_SECRETS_API_URL_FORMAT, orgId, projectId, appName, secretName);

    String secretsJson = HcpVaultApiClient.fetchSecrets(hcpUrl, credentials.getHcpApiToken());

    return Resource.createPermanentResource(secretsJson, true);
  }

}
