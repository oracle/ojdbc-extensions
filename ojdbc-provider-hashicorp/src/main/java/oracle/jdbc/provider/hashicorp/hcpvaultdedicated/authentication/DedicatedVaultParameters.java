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
import oracle.jdbc.provider.hashicorp.util.Parameterutil;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.parameter.ParameterSetParser;

import java.util.HashMap;
import java.util.Map;

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

  // Common constants for System Properties and environment variable and
  // parameter names
  public static final String PARAM_VAULT_ADDR = "VAULT_ADDR";
  public static final String PARAM_VAULT_TOKEN = "VAULT_TOKEN";
  public static final String PARAM_VAULT_NAMESPACE = "VAULT_NAMESPACE";
  public static final String PARAM_VAULT_USERNAME = "VAULT_USERNAME";
  public static final String PARAM_VAULT_PASSWORD = "VAULT_PASSWORD";
  public static final String PARAM_GITHUB_TOKEN = "GITHUB_TOKEN";
  public static final String PARAM_USERPASS_AUTH_PATH = "USERPASS_AUTH_PATH";
  public static final String PARAM_APPROLE_AUTH_PATH = "APPROLE_AUTH_PATH";
  public static final String PARAM_GITHUB_AUTH_PATH = "GITHUB_AUTH_PATH";
  public static final String PARAM_VAULT_ROLE_ID = "ROLE_ID";
  public static final String PARAM_VAULT_SECRET_ID = "SECRET_ID";

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
  public static final Parameter<String> VAULT_ADDR = Parameter.create(REQUIRED);

  /**
   * The Vault token. If not specified, fallback to system property or environment var.
   */
  public static final Parameter<String> VAULT_TOKEN = Parameter.create(REQUIRED);

  /**
   *  The field name for extracting a specific value from the JSON.
   */
  public static final Parameter<String> FIELD_NAME = Parameter.create();

  /**
   * The username for Userpass authentication. Required for Userpass method.
   */
  public static final Parameter<String> USERNAME = Parameter.create(REQUIRED);

  /**
   *  The password for Userpass authentication. Required for Userpass method.
   */
  public static final Parameter<String> PASSWORD = Parameter.create(REQUIRED);

  /**
   *  The path for Userpass authentication. Optional.
   */
  public static final Parameter<String> USERPASS_AUTH_PATH = Parameter.create();

  /**
   *  The namespace for the Vault API request. Optional.
   */
  public static final Parameter<String> NAMESPACE = Parameter.create();

  /**
   * The Role ID for AppRole authentication. Required for AppRole method.
   * <p>
   * The Role ID identifies the role to use for authentication. It must be
   * configured in Vault as part of the AppRole authentication setup.
   * </p>
   */
  public static final Parameter<String> ROLE_ID = Parameter.create(REQUIRED);

  /**
   * The Secret ID for AppRole authentication. Required for AppRole method.
   * <p>
   * The Secret ID is a credential tied to a specific role and used in
   * conjunction with the Role ID for AppRole authentication.
   * </p>
   */
  public static final Parameter<String> SECRET_ID = Parameter.create(REQUIRED);

  /**
   * The path for AppRole authentication. Optional.
   * <p>
   * This parameter specifies the path where the AppRole authentication
   * method is enabled. The default path is "approle". If the method is
   * enabled at a different path, that value should be provided here.
   * </p>
   */
  public static final Parameter<String> APPROLE_AUTH_PATH = Parameter.create();

  /**
   * The GitHub personal access token. Required for GitHub authentication method.
   * <p>
   * This token is used to authenticate with the HashiCorp Vault via the
   * GitHub authentication method. The token should be a valid GitHub
   * personal access token with the necessary permissions configured in
   * the Vault policy.
   * </p>
   */
  public static final Parameter<String> GITHUB_TOKEN = Parameter.create(REQUIRED);

  /**
   * The path for GitHub authentication. Optional.
   * <p>
   * This parameter specifies the path where the GitHub authentication method
   * is enabled. The default path is "github". If the GitHub authentication
   * method is enabled at a custom path, provide this parameter with the
   * appropriate value.
   * </p>
   */
  public static final Parameter<String> GITHUB_AUTH_PATH = Parameter.create();

  /**
   * Builds a resolved ParameterSet from the given options map.
   * <p>
   * This method makes a defensive copy of the provided map, ensures that a default
   * authentication method is set, fills in missing keys (using fallback values from
   * system properties or environment variables) based on the authentication method,
   * and then parses the updated map into a ParameterSet.
   * </p>
   *
   * @param inputOpts The input options map.
   * @return The resolved ParameterSet.
   */
  public static ParameterSet buildResolvedParameterSet(Map<String, String> inputOpts) {
    Map<String, String> opts = new HashMap<>(inputOpts);

    opts.putIfAbsent("authentication", "auto_detect");
    String authStr = opts.get("authentication");
    DedicatedVaultAuthenticationMethod authMethod =
            DedicatedVaultAuthenticationMethod.valueOf(authStr.toUpperCase());

    opts.computeIfAbsent(PARAM_VAULT_ADDR, Parameterutil::getFallback);
    opts.computeIfAbsent(PARAM_VAULT_NAMESPACE, Parameterutil::getFallback);

    switch (authMethod) {
      case VAULT_TOKEN:
        opts.computeIfAbsent(PARAM_VAULT_TOKEN, Parameterutil::getFallback);
        break;
      case GITHUB:
        opts.computeIfAbsent(PARAM_GITHUB_TOKEN, Parameterutil::getFallback);
        opts.computeIfAbsent(PARAM_GITHUB_AUTH_PATH, Parameterutil::getFallback);
        break;
      case APPROLE:
        opts.computeIfAbsent(PARAM_VAULT_ROLE_ID, Parameterutil::getFallback);
        opts.computeIfAbsent(PARAM_VAULT_SECRET_ID, Parameterutil::getFallback);
        opts.computeIfAbsent(PARAM_APPROLE_AUTH_PATH, Parameterutil::getFallback);
        break;
      case USERPASS:
        opts.computeIfAbsent(PARAM_VAULT_USERNAME, Parameterutil::getFallback);
        opts.computeIfAbsent(PARAM_VAULT_PASSWORD, Parameterutil::getFallback);
        opts.computeIfAbsent(PARAM_USERPASS_AUTH_PATH, Parameterutil::getFallback);
        break;
      case AUTO_DETECT:
        opts.computeIfAbsent(PARAM_VAULT_TOKEN, Parameterutil::getFallback);
        opts.computeIfAbsent(PARAM_VAULT_USERNAME, Parameterutil::getFallback);
        opts.computeIfAbsent(PARAM_VAULT_PASSWORD, Parameterutil::getFallback);
        opts.computeIfAbsent(PARAM_USERPASS_AUTH_PATH, Parameterutil::getFallback);
        opts.computeIfAbsent(PARAM_VAULT_ROLE_ID, Parameterutil::getFallback);
        opts.computeIfAbsent(PARAM_VAULT_SECRET_ID, Parameterutil::getFallback);
        opts.computeIfAbsent(PARAM_APPROLE_AUTH_PATH, Parameterutil::getFallback);
        opts.computeIfAbsent(PARAM_GITHUB_TOKEN, Parameterutil::getFallback);
        opts.computeIfAbsent(PARAM_GITHUB_AUTH_PATH, Parameterutil::getFallback);
        break;
      default:
        break;
    }

    return PARAMETER_SET_PARSER.parseNamedValues(opts);
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
