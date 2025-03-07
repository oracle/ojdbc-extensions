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

package oracle.jdbc.provider.hashicorp.hcpvaultsecret.authentication;

import oracle.jdbc.provider.hashicorp.hcpvaultsecret.configuration.HcpVaultConfigurationParameters;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.parameter.ParameterSetParser;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.*;
import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.ALLOW_SYSTEM_PROPERTY;

/**
 * Contains parameter definitions for interacting with HCP Vault Secrets.
 * <p>
 * This class centralizes configuration parameters used for authenticating
 * with and retrieving secrets from HCP Vault Secrets.
 * </p>
 */
public class HcpVaultSecretParameters {

  /**
   * Constants representing the configuration parameter names for HCP Vault Secrets.
   * <p>
   * These constants serve as both parameter names within the {@link ParameterSet}
   * and as keys for environment variables or system properties.
   * </p>
   */
  static final String PARAM_HCP_ORG_ID = "HCP_ORG_ID";
  static final String PARAM_HCP_PROJECT_ID = "HCP_PROJECT_ID";
  static final String PARAM_HCP_APP_NAME = "HCP_APP_NAME";
  static final String PARAM_HCP_CLIENT_ID = "HCP_CLIENT_ID";
  static final String PARAM_HCP_CLIENT_SECRET = "HCP_CLIENT_SECRET";
  private static final String PARAM_HCP_SECRET_NAME = "SECRET_NAME";
  static final String PARAM_HCP_CREDENTIALS_FILE = "HCP_CREDENTIALS_FILE";
  private static final String DEFAULT_CREDENTIALS_FILE_PATH =
          System.getProperty("user.home") + "/.config/hcp/creds-cache.json";

  /**
   * Parameter indicating the authentication method to use for HCP Vault Secrets.
   */
  public static final Parameter<HcpVaultAuthenticationMethod> AUTHENTICATION_METHOD = Parameter.create(REQUIRED);

  /**
   * Parameter for the OAuth2 client ID. Required.
   */
  public static final Parameter<String> HCP_CLIENT_ID = Parameter.create(REQUIRED, ALLOW_ENV, ALLOW_SYSTEM_PROPERTY);

  /**
   * Parameter for the OAuth2 client secret. Required.
   */
  public static final Parameter<String> HCP_CLIENT_SECRET = Parameter.create(REQUIRED, ALLOW_ENV, ALLOW_SYSTEM_PROPERTY);

  /**
   * Parameter for the credentials file path.
   * By default, the credentials file is expected at:
   * <code>System.getProperty("user.home") + "/.config/hcp/creds-cache.json"</code>.
   */
  public static final Parameter<String> HCP_CREDENTIALS_FILE = Parameter.create(REQUIRED, ALLOW_ENV, ALLOW_SYSTEM_PROPERTY);

  /**
   * Parameter for the organization ID. Required.
   */
  public static final Parameter<String> HCP_ORG_ID = Parameter.create(REQUIRED, ALLOW_ENV, ALLOW_SYSTEM_PROPERTY);

  /**
   * Parameter for the project ID. Required.
   */
  public static final Parameter<String> HCP_PROJECT_ID = Parameter.create(REQUIRED, ALLOW_ENV, ALLOW_SYSTEM_PROPERTY);

  /**
   * Parameter for the application name. Required.
   */
  public static final Parameter<String> HCP_APP_NAME = Parameter.create(REQUIRED, ALLOW_ENV, ALLOW_SYSTEM_PROPERTY);

  /**
   * Parameter for the secret name. Required.
   */
  public static final Parameter<String> SECRET_NAME = Parameter.create(REQUIRED, ALLOW_ENV, ALLOW_SYSTEM_PROPERTY);

  /**
   * Parameter for the optional key in the secret JSON.
   */
  public static final Parameter<String> KEY = Parameter.create();

  /**
   * Retrieves the HCP client ID.
   *
   * @param parameterSet the parameter set.
   * @return the HCP client ID.
   */
  public static String getHcpClientId(ParameterSet parameterSet) {
    return parameterSet.getRequired(HCP_CLIENT_ID, PARAM_HCP_CLIENT_ID);
  }

  /**
   * Retrieves the HCP client secret.
   *
   * @param parameterSet the parameter set.
   * @return the HCP client secret.
   */
  public static String getHcpClientSecret(ParameterSet parameterSet) {
    return parameterSet.getRequired(HCP_CLIENT_SECRET, PARAM_HCP_CLIENT_SECRET);
  }

  /**
   * Retrieves the path to the credentials file.
   *
   * @param parameterSet the parameter set.
   * @return the credentials file path.
   */
  public static String getHcpCredentialsFile(ParameterSet parameterSet) {
    return parameterSet.getOptional(HCP_CREDENTIALS_FILE,
            PARAM_HCP_CREDENTIALS_FILE, DEFAULT_CREDENTIALS_FILE_PATH);
  }

  /**
   * Retrieves the HCP organization ID.
   *
   * @param parameterSet the parameter set.
   * @return the HCP organization ID.
   */
  public static String getHcpOrgId(ParameterSet parameterSet) {
    return parameterSet.getRequired(HCP_ORG_ID, PARAM_HCP_ORG_ID);
  }

  /**
   * Retrieves the HCP project ID.
   *
   * @param parameterSet the parameter set.
   * @return the HCP project ID.
   */
  public static String getHcpProjectId(ParameterSet parameterSet) {
    return parameterSet.getRequired(HCP_PROJECT_ID, PARAM_HCP_PROJECT_ID);
  }

  /**
   * Retrieves the HCP application name.
   *
   * @param parameterSet the parameter set.
   * @return the HCP application name.
   */
  public static String getHcpAppName(ParameterSet parameterSet) {
    return parameterSet.getRequired(HCP_APP_NAME, PARAM_HCP_APP_NAME);
  }

  /**
   * Retrieves the Secret name.
   *
   * @param parameterSet the parameter set.
   * @return the HCP application name.
   */
  public static String getSecretName(ParameterSet parameterSet) {
    return parameterSet.getRequired(SECRET_NAME);
  }

  public static final ParameterSetParser PARAMETER_SET_PARSER =
    HcpVaultConfigurationParameters.configureBuilder(
      ParameterSetParser.builder()
        .addParameter("value", SECRET_NAME)
        .addParameter(PARAM_HCP_ORG_ID, HCP_ORG_ID)
        .addParameter(PARAM_HCP_PROJECT_ID, HCP_PROJECT_ID)
        .addParameter(PARAM_HCP_APP_NAME, HCP_APP_NAME)
        .addParameter(PARAM_HCP_CLIENT_ID, HCP_CLIENT_ID)
        .addParameter(PARAM_HCP_CLIENT_SECRET, HCP_CLIENT_SECRET)
        .addParameter(PARAM_HCP_CREDENTIALS_FILE, HCP_CREDENTIALS_FILE, DEFAULT_CREDENTIALS_FILE_PATH)
        .addParameter("KEY", KEY))
        .addParameter("type", Parameter.create())
      .build();

}
