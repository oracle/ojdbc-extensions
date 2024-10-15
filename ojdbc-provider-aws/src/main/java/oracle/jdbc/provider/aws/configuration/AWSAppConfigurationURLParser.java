/*
 ** Copyright (c) 2023 Oracle and/or its affiliates.
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

import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.parameter.ParameterSetParser;
import oracle.jdbc.provider.parameter.UriParameters;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;

/**
 * A URL parser used by {@link AWSAppConfigProvider}.
 */
public class AWSAppConfigurationURLParser {
  /**
   * According to the documentation of AWS SDK, the IDs are consist of
   * alphanumeric characters.
   */
  private static final String NAME = "(\\w*)";

  private static final Pattern CONFIG_URL = Pattern
      .compile("a\\/" + NAME + "\\/c\\/" + NAME + "\\/e\\/" + NAME
              + "(\\?(.*))?", // parameters to the provider
          Pattern.CASE_INSENSITIVE);

  /**
   * Parameter representing the "KEY=..." name-value pair which may appear in
   * the query section of a URL. This is a reserved value for Feature Flag.
   */
  public static final Parameter<String> KEY = Parameter.create();

  /**
   * Parameter representing the "REGION=..." name-value pair which may appear in
   * the query section of a URL.
   */
  public static final Parameter<String> REGION = Parameter.create(REQUIRED);

  /**
   * Parser that recognizes the named parameters which appear in the query
   * section of a URL. Parameter related to authentication are configured by
   * {@link AWSConfigurationParameters#configureBuilder(ParameterSetParser.Builder)}
   */
  private static final ParameterSetParser PARAMETER_SET_PARSER =
    AWSConfigurationParameters.configureBuilder(
      ParameterSetParser.builder()
          .addParameter("KEY", KEY)
          .addParameter("REGION", REGION))
      .build();

  /** Parameters parsed from the query section of a URL */
  private final ParameterSet parameters;

  /**
   * <p>
   * Construct a URL parser used by {@link AWSAppConfigProvider}. The
   * {@code url} is a fragment section of the URL processed by the JDBC driver,
   * which has a format of:
   * </p><pre>{@code
   *   {appconfig-name}[?key=prefix&label=value&option1=value1&option2=value2...]
   * }</pre>
   *
   * @param url url to be parsed
   */
  public AWSAppConfigurationURLParser(String url) {
    Matcher urlMatcher = CONFIG_URL.matcher(url);

    if (urlMatcher.matches()) {
      applicationIdentifier = urlMatcher.group(1);
      configurationProfileIdentifier = urlMatcher.group(2);
      environmentIdentifier = urlMatcher.group(3);

      if (urlMatcher.groupCount() >= 4)
        parameters =
            PARAMETER_SET_PARSER.parseNamedValues(UriParameters.parse(url));
      else
        parameters = ParameterSet.empty();
    } else {
      throw new IllegalArgumentException("URL doesn't match the format \"a/" +
          "<application-id>/c/<configuration-profile-id>/e/<environment-id>" +
          "[?options]\": " + url);
    }
  }

  /**
   * Returns the application Identifier of the AppConfig.
   * @return the application Identifier of the AppConfig
   */
  public String applicationIdentifier() {
    return applicationIdentifier;
  }

  /**
   * Returns the configuration Profile Identifier of the AppConfig.
   * @return the configuration Profile Identifier of the AppConfig
   */
  public String configurationProfileIdentifier() {
    return configurationProfileIdentifier;
  }

  /**
   * Returns the environment Identifier of the AppConfig.
   * @return the environment Identifier of the AppConfig
   */
  public String environmentIdentifier() {
    return environmentIdentifier;
  }

  /**
   * Returns the set of parameters parsed from a URL. The set may contain
   * any parameter defined by {@link AWSConfigurationParameters}, along with
   * the {@link #KEY} parameter defined by this class.
   *
   * @return Set of parsed parameters. Not null. May be empty.
   */
  public ParameterSet getParameters() {
    return parameters;
  }

  private final String applicationIdentifier;
  private final String configurationProfileIdentifier;
  private final String environmentIdentifier;
}
