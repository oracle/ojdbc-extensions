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

package oracle.jdbc.provider.aws.configuration;

import oracle.jdbc.driver.configuration.OracleConfigurationParsableProvider;
import oracle.jdbc.provider.aws.appconfig.AppConfigFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.parameter.ParameterSetParser;
import oracle.jdbc.util.OracleConfigurationCache;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static oracle.jdbc.provider.aws.appconfig.AppConfigFactory.*;
import static oracle.jdbc.provider.aws.configuration.AwsConfigurationParameters.KEY;
import static oracle.jdbc.provider.aws.configuration.AwsConfigurationParameters.REGION;

/**
 * A provider for JSON payload which contains configuration data from AWS
 * AppConfig
 * See {@link #getInputStream(String)} for the spec of the JSON payload.
 **/
public class AwsAppConfigConfigurationProvider extends OracleConfigurationParsableProvider {

  private static final OracleConfigurationCache CACHE = OracleConfigurationCache.create(100);

  static final ParameterSetParser PARAMETER_SET_PARSER = AwsConfigurationParameters.configureBuilder(
    ParameterSetParser.builder()
      .addParameter("value", APP_CONFIG_APPLICATION)
      .addParameter("appconfig_environment", APP_CONFIG_ENVIRONMENT)
      .addParameter("appconfig_profile", APP_CONFIG_PROFILE)
      .addParameter("key", KEY)
      .addParameter("AWS_REGION", REGION))
    .build();

  /**
   * {@inheritDoc}
   * <p>
   * Returns the JSON payload stored in AWS AppConfig.
   * </p>
   *
   * @param parameterName The application identifier or name for the
   * configuration
   * @return JSON payload as an InputStream
   */
  @Override
  public InputStream getInputStream(String parameterName) {
    final String VALUE = "value";
    Map<String, String> opts = new HashMap<>(options);
    opts.put(VALUE, parameterName);

    ParameterSet parameters = PARAMETER_SET_PARSER.parseNamedValues(opts);

    return AppConfigFactory.getInstance()
      .request(parameters)
      .getContent();
  }

  @Override
  public String getType() {
    return "awsappconfig";
  }

  /**
   * {@inheritDoc}
   * @return cache of this provider which is used to store configuration
   */
  @Override
  public OracleConfigurationCache getCache() {
    return CACHE;
  }

  /**
   * {@inheritDoc}
   * @return the parser type
   */
  @Override
  public String getParserType(String location) {
    return "json";
  }
}