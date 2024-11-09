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

import static oracle.jdbc.provider.aws.configuration.AwsConfigurationParameters.REGION;

/**
 * A URL parser used by {@link AwsAppConfigurationURLParser}.
 */
public class AwsAppConfigurationURLParser {
  /**
   * According to the documentation of Azure SDK, configuration store names must
   * contain only alpha-numeric ASCII characters or '-', and be between 5 and 50
   * characters. Names must not contain the sequence '---'.
   */
  private static final String NAME = "([-\\w]*)";

  private static final Pattern CONFIG_URL = Pattern
      .compile("a/" + NAME + "/c/" + NAME + "/e/" + NAME
              + "("
              + "\\?(.*)"
              + ")?", // parameters to the provider
          Pattern.CASE_INSENSITIVE);

  /**
   * Parameter representing the "KEY=..." name-value pair which may appear in
   * the query section of a URL.
   */
  public static final Parameter<String> KEY = Parameter.create();

  /**
   * Parser that recognizes the named parameters which appear in the query
   * section of a URL.
   */
  private static final ParameterSetParser PARAMETER_SET_PARSER =
      AwsConfigurationParameters.configureBuilder(
              ParameterSetParser.builder()
                  .addParameter("key", KEY))
          .build();

  /** Parameters parsed from the query section of a URL */
  private final ParameterSet parameters;

  /**
   * <p>
   * Construct a URL parser used by {@link AwsAppConfigProvider}. The
   * {@code url} is a fragment section of the URL processed by the JDBC driver,
   * which has a format of:
   * </p><pre>{@code
   *   {appconfig-name}[?key=prefix&label=value&option1=value1&option2=value2...]
   * }</pre>
   *
   * @param url url to be parsed
   */
  public AwsAppConfigurationURLParser(String url) {
    Matcher urlMatcher = CONFIG_URL.matcher(url);

    if (urlMatcher.matches()) {
      applicationIdentifier = urlMatcher.group(1);
      configurationProfileIdentifier = urlMatcher.group(2);
      environmentIdentifier = urlMatcher.group(3);

      if (urlMatcher.groupCount() > 3) {
        // Options exist in the URL
        parameters =
            PARAMETER_SET_PARSER.parseNamedValues(UriParameters.parse(url));
        prefix = parameters.getOptional(KEY);
      } else {
        parameters = ParameterSet.empty();
      }
    }
    else {
      throw new IllegalArgumentException("URL doesn't match");
    }
  }

  public String applicationIdentifier() {
    return applicationIdentifier;
  }
  public String configurationProfileIdentifier() {
    return configurationProfileIdentifier;
  }
  public String environmentIdentifier() {
    return environmentIdentifier;
  }

  /**
   * Returns the set of parameters parsed from a URL. The set contain
   * {@link #KEY} and parameters defined by
   * {@link AwsConfigurationParameters}.
   *
   * @return Set of parsed parameters. Not null. May be empty.
   */
  public ParameterSet getParameters() {
    return parameters;
  }

  private final String applicationIdentifier;
  private final String configurationProfileIdentifier;
  private final String environmentIdentifier;
  private String prefix;

}