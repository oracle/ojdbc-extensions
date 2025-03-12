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
import oracle.jdbc.provider.hashicorp.util.Parameterutil;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.parameter.ParameterSetParser;

import java.util.HashMap;
import java.util.Map;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.*;

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
  public static final String PARAM_HCP_ORG_ID = "HCP_ORG_ID";
  public static final String PARAM_HCP_PROJECT_ID = "HCP_PROJECT_ID";
  public static final String PARAM_HCP_APP_NAME = "HCP_APP_NAME";
  public static final String PARAM_HCP_CLIENT_ID = "HCP_CLIENT_ID";
  public static final String PARAM_HCP_CLIENT_SECRET = "HCP_CLIENT_SECRET";
  public static final String PARAM_HCP_CREDENTIALS_FILE =
          "HCP_CREDENTIALS_FILE";
  private static final String DEFAULT_CREDENTIALS_FILE_PATH =
          System.getProperty("user.home") + "/.config/hcp/creds-cache.json";

  /**
   * Parameter indicating the authentication method to use for HCP Vault Secrets.
   */
  public static final Parameter<HcpVaultAuthenticationMethod> AUTHENTICATION_METHOD = Parameter.create(REQUIRED);

  /**
   * Parameter for the OAuth2 client ID. Required.
   */
  public static final Parameter<String> HCP_CLIENT_ID = Parameter.create(REQUIRED);

  /**
   * Parameter for the OAuth2 client secret. Required.
   */
  public static final Parameter<String> HCP_CLIENT_SECRET = Parameter.create(REQUIRED);

  /**
   * Parameter for the credentials file path.
   * By default, the credentials file is expected at:
   * <code>System.getProperty("user.home") + "/.config/hcp/creds-cache.json"</code>.
   */
  public static final Parameter<String> HCP_CREDENTIALS_FILE = Parameter.create(REQUIRED);

  /**
   * Parameter for the organization ID. Required.
   */
  public static final Parameter<String> HCP_ORG_ID = Parameter.create(REQUIRED);

  /**
   * Parameter for the project ID. Required.
   */
  public static final Parameter<String> HCP_PROJECT_ID = Parameter.create(REQUIRED);

  /**
   * Parameter for the application name. Required.
   */
  public static final Parameter<String> HCP_APP_NAME = Parameter.create(REQUIRED);

  /**
   * Parameter for the secret name. Required.
   */
  public static final Parameter<String> SECRET_NAME = Parameter.create(REQUIRED);

  /**
   * Parameter for the optional key in the secret JSON.
   */
  public static final Parameter<String> KEY = Parameter.create();

  /**
   * Builds a ParameterSet from the given options map.
   * <p>
   * This method makes a defensive copy of the provided map, ensures that a default
   * authentication method is set, and then fills in missing keys using fallback values
   * (from system properties or environment variables) based on the authentication method.
   * Finally, it parses the updated map into a ParameterSet.
   * </p>
   *
   * @param inputOpts The input options map.
   * @return The ParameterSet.
   */
  public static ParameterSet buildResolvedParameterSet(Map<String, String> inputOpts) {
    Map<String, String> opts = new HashMap<>(inputOpts);

    opts.putIfAbsent("authentication", "auto_detect");
    String authStr = opts.get("authentication");
    HcpVaultAuthenticationMethod authMethod =
            HcpVaultAuthenticationMethod.valueOf(authStr.toUpperCase());

    opts.computeIfAbsent(PARAM_HCP_ORG_ID, Parameterutil::getFallback);
    opts.computeIfAbsent(PARAM_HCP_PROJECT_ID, Parameterutil::getFallback);
    opts.computeIfAbsent(PARAM_HCP_APP_NAME, Parameterutil::getFallback);

    switch (authMethod) {
      case CLIENT_CREDENTIALS:
        opts.computeIfAbsent(PARAM_HCP_CLIENT_ID, Parameterutil::getFallback);
        opts.computeIfAbsent(PARAM_HCP_CLIENT_SECRET, Parameterutil::getFallback);
        break;
      case CLI_CREDENTIALS_FILE:
        opts.computeIfAbsent(PARAM_HCP_CREDENTIALS_FILE, Parameterutil::getFallback);
        break;
      case AUTO_DETECT:
        opts.computeIfAbsent(PARAM_HCP_CLIENT_ID, Parameterutil::getFallback);
        opts.computeIfAbsent(PARAM_HCP_CLIENT_SECRET, Parameterutil::getFallback);
        opts.computeIfAbsent(PARAM_HCP_CREDENTIALS_FILE, Parameterutil::getFallback);
        break;
      default:
        break;
    }
    return  PARAMETER_SET_PARSER.parseNamedValues(opts);
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
