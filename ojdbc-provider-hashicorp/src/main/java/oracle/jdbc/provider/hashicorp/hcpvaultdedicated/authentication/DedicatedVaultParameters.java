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

import oracle.jdbc.provider.hashicorp.hcpvaultdedicated.configuration.DedicatedVaultConfigurationParameters;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.parameter.ParameterSetParser;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.*;

/**
 * Contains parameter definitions for interacting with Dedicated HashiCorp Vault.
 * <p>
 * This class provides a centralized definition of parameters used across different components
 * when working with HashiCorp Vault Dedicated. It defines the parameter names, environment variable keys,
 * and default values for various configuration settings such as the Vault address, token, namespace, and
 * authentication credentials for multiple authentication methods (Vault token, Userpass, AppRole, and GitHub).
 * In addition, it offers utility methods to retrieve these parameter values from a {@link ParameterSet}
 * with appropriate fallbacks to system properties, environment variables, or default values.
 * </p>
 */

public class DedicatedVaultParameters {

  // Common constants for System Properties and environment variable names
  static final String PARAM_VAULT_ADDR = "VAULT_ADDR";
  static final String PARAM_VAULT_TOKEN = "VAULT_TOKEN";
  static final String PARAM_VAULT_NAMESPACE = "VAULT_NAMESPACE";
  static final String PARAM_VAULT_USERNAME = "VAULT_USERNAME";
  static final String PARAM_VAULT_PASSWORD = "VAULT_PASSWORD";
  static final String PARAM_GITHUB_TOKEN = "GITHUB_TOKEN";
  static final String PARAM_USERPASS_AUTH_PATH = "USERPASS_AUTH_PATH";
  static final String PARAM_APPROLE_AUTH_PATH = "APPROLE_AUTH_PATH";
  static final String PARAM_GITHUB_AUTH_PATH = "GITHUB_AUTH_PATH";
  static final String PARAM_VAULT_ROLE_ID = "ROLE_ID";
  static final String PARAM_VAULT_SECRET_ID = "SECRET_ID";

  // Default values
  static final String DEFAULT_NAMESPACE = "admin";
  static final String DEFAULT_USERPASS_PATH = "userpass";
  static final String DEFAULT_APPROLE_PATH = "approle";
  static final String DEFAULT_GITHUB_PATH = "github";

  /**
   * <p>
   * Parameter for configuring the authentication method.
   * Must be provided in the {@link ParameterSet}.
   * </p>
   */
  public static final Parameter<DedicatedVaultAuthenticationMethod> AUTHENTICATION_METHOD =
          Parameter.create(REQUIRED);

  /** The path of the secret in Vault. Required. */
  public static final Parameter<String> SECRET_PATH = Parameter.create(REQUIRED);

  /**
   * The name of the key if the secret is a JSON with multiple fields.
   * This is optional.
   */
  public static final Parameter<String> KEY = Parameter.create();

  /**
   * The Vault address. If not specified, fallback to system property or environment var.
   */
  public static final Parameter<String> VAULT_ADDR =
          Parameter.create(REQUIRED, ALLOW_ENV, ALLOW_SYSTEM_PROPERTY);

  /**
   * The Vault token. If not specified, fallback to system property or environment var.
   */
  public static final Parameter<String> VAULT_TOKEN = Parameter.create(REQUIRED, ALLOW_ENV, ALLOW_SYSTEM_PROPERTY);

  /**
   *  The field name for extracting a specific value from the JSON.
   */
  public static final Parameter<String> FIELD_NAME = Parameter.create();

  /**
   * The username for Userpass authentication. Required for Userpass method.
   */
  public static final Parameter<String> USERNAME = Parameter.create(REQUIRED, ALLOW_ENV, ALLOW_SYSTEM_PROPERTY);

  /**
   *  The password for Userpass authentication. Required for Userpass method.
   */
  public static final Parameter<String> PASSWORD = Parameter.create(REQUIRED, ALLOW_ENV, ALLOW_SYSTEM_PROPERTY);

  /**
   *  The path for Userpass authentication. Optional.
   */
  public static final Parameter<String> USERPASS_AUTH_PATH = Parameter.create(ALLOW_ENV, ALLOW_SYSTEM_PROPERTY);

  /**
   *  The namespace for the Vault API request. Optional.
   */
  public static final Parameter<String> NAMESPACE = Parameter.create(ALLOW_ENV, ALLOW_SYSTEM_PROPERTY);

  /**
   * The Role ID for AppRole authentication. Required for AppRole method.
   * <p>
   * The Role ID identifies the role to use for authentication. It must be
   * configured in Vault as part of the AppRole authentication setup.
   * </p>
   */
  public static final Parameter<String> ROLE_ID = Parameter.create(REQUIRED, ALLOW_ENV, ALLOW_SYSTEM_PROPERTY);

  /**
   * The Secret ID for AppRole authentication. Required for AppRole method.
   * <p>
   * The Secret ID is a credential tied to a specific role and used in
   * conjunction with the Role ID for AppRole authentication.
   * </p>
   */
  public static final Parameter<String> SECRET_ID = Parameter.create(REQUIRED, ALLOW_ENV, ALLOW_SYSTEM_PROPERTY);

  /**
   * The path for AppRole authentication. Optional.
   * <p>
   * This parameter specifies the path where the AppRole authentication
   * method is enabled. The default path is "approle". If the method is
   * enabled at a different path, that value should be provided here.
   * </p>
   */
  public static final Parameter<String> APPROLE_AUTH_PATH = Parameter.create(ALLOW_ENV, ALLOW_SYSTEM_PROPERTY);

  /**
   * The GitHub personal access token. Required for GitHub authentication method.
   * <p>
   * This token is used to authenticate with the HashiCorp Vault via the
   * GitHub authentication method. The token should be a valid GitHub
   * personal access token with the necessary permissions configured in
   * the Vault policy.
   * </p>
   */
  public static final Parameter<String> GITHUB_TOKEN = Parameter.create(REQUIRED, ALLOW_ENV, ALLOW_SYSTEM_PROPERTY);

  /**
   * The path for GitHub authentication. Optional.
   * <p>
   * This parameter specifies the path where the GitHub authentication method
   * is enabled. The default path is "github". If the GitHub authentication
   * method is enabled at a custom path, provide this parameter with the
   * appropriate value.
   * </p>
   */
  public static final Parameter<String> GITHUB_AUTH_PATH = Parameter.create(ALLOW_ENV, ALLOW_SYSTEM_PROPERTY);

  /**
   * Retrieves the Vault server address.
   *
   * @param parameterSet the source parameter set.
   * @return the Vault address.
   */
  public static String getVaultAddress(ParameterSet parameterSet) {
    return parameterSet.getRequired(VAULT_ADDR, PARAM_VAULT_ADDR);
  }

  /**
   * Retrieves the Vault namespace.
   *
   * @param parameterSet the source parameter set.
   * @return the namespace.
   */
  public static String getNamespace(ParameterSet parameterSet) {
    return parameterSet.getOptional(NAMESPACE, PARAM_VAULT_NAMESPACE);
  }

  /**
   * Retrieves the Vault token.
   *
   * @param parameterSet the source parameter set.
   * @return the Vault token.
   */
  public static String getVaultToken(ParameterSet parameterSet) {
    return parameterSet.getRequired(VAULT_TOKEN, PARAM_VAULT_TOKEN);
  }

  /**
   * Retrieves the username for Userpass authentication.
   *
   * @param parameterSet the source parameter set.
   * @return the username.
   */
  public static String getUsername(ParameterSet parameterSet) {
    return parameterSet.getRequired(USERNAME, PARAM_VAULT_USERNAME);
  }

  /**
   * Retrieves the password for Userpass authentication.
   *
   * @param parameterSet the source parameter set.
   * @return the password.
   */
  public static String getPassword(ParameterSet parameterSet) {
    return parameterSet.getRequired(PASSWORD, PARAM_VAULT_PASSWORD);
  }

  /**
   * Retrieves the GitHub personal access token.
   *
   * @param parameterSet the source parameter set.
   * @return the GitHub token.
   */
  public static String getGitHubToken(ParameterSet parameterSet) {
    return parameterSet.getRequired(GITHUB_TOKEN, PARAM_GITHUB_TOKEN);
  }

  /**
   * Retrieves the Role ID for AppRole authentication.
   *
   * @param parameterSet the source parameter set.
   * @return the Role ID.
   */
  public static String getRoleId(ParameterSet parameterSet) {
    return parameterSet.getRequired(ROLE_ID, PARAM_VAULT_ROLE_ID);
  }

  /**
   * Retrieves the Secret ID for AppRole authentication.
   *
   * @param parameterSet the source parameter set.
   * @return the Secret ID.
   */
  public static String getSecretId(ParameterSet parameterSet) {
    return parameterSet.getRequired(SECRET_ID, PARAM_VAULT_SECRET_ID);
  }

  /**
   * Retrieves the authentication path for Userpass.
   *
   * @param parameterSet the source parameter set.
   * @return the Userpass authentication path.
   */
  public static String getUserpassAuthPath(ParameterSet parameterSet) {
    return parameterSet.getOptional(USERPASS_AUTH_PATH, PARAM_USERPASS_AUTH_PATH);
  }

  /**
   * Retrieves the authentication path for AppRole.
   *
   * @param parameterSet the source parameter set.
   * @return the AppRole authentication path.
   */
  public static String getAppRoleAuthPath(ParameterSet parameterSet) {
    return parameterSet.getOptional(APPROLE_AUTH_PATH, PARAM_APPROLE_AUTH_PATH);
  }

  /**
   * Retrieves the authentication path for GitHub.
   *
   * @param parameterSet the source parameter set.
   * @return the GitHub authentication path.
   */
  public static String getGitHubAuthPath(ParameterSet parameterSet) {
    return parameterSet.getOptional(GITHUB_AUTH_PATH, PARAM_GITHUB_AUTH_PATH);
  }

  public static final ParameterSetParser PARAMETER_SET_PARSER =
    DedicatedVaultConfigurationParameters.configureBuilder(
      ParameterSetParser.builder()
        .addParameter("value", SECRET_PATH)
        .addParameter("key", KEY)
        .addParameter(PARAM_VAULT_ADDR, VAULT_ADDR)
        .addParameter(PARAM_VAULT_TOKEN, VAULT_TOKEN)
        .addParameter(PARAM_VAULT_USERNAME, USERNAME)
        .addParameter(PARAM_VAULT_PASSWORD, PASSWORD)
        .addParameter(PARAM_USERPASS_AUTH_PATH, USERPASS_AUTH_PATH, DEFAULT_USERPASS_PATH)
        .addParameter(PARAM_VAULT_NAMESPACE, NAMESPACE, DEFAULT_NAMESPACE)
        .addParameter(PARAM_VAULT_ROLE_ID, ROLE_ID)
        .addParameter(PARAM_VAULT_SECRET_ID, SECRET_ID)
        .addParameter(PARAM_APPROLE_AUTH_PATH, APPROLE_AUTH_PATH, DEFAULT_APPROLE_PATH)
        .addParameter(PARAM_GITHUB_TOKEN, GITHUB_TOKEN)
        .addParameter(PARAM_GITHUB_AUTH_PATH, GITHUB_AUTH_PATH, DEFAULT_GITHUB_PATH)
        .addParameter("FIELD_NAME", FIELD_NAME))
        .addParameter("type", Parameter.create())
      .build();
}
