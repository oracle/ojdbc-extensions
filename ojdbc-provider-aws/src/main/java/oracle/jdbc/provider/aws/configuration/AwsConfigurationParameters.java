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

import oracle.jdbc.provider.aws.authentication.AwsAuthenticationMethod;
import oracle.jdbc.provider.aws.authentication.AwsCredentialsFactory;
import oracle.jdbc.provider.parameter.ParameterSetParser;

public final class AwsConfigurationParameters {

  /**
   * Private constructor that should never be called: This class is a singleton.
   */
  private AwsConfigurationParameters() {}

  /**
   * Configures a {@code builder} to build a parser that recognizes the common
   * set of parameters accepted by {@link AwsS3ConfigurationProvider} and
   * {@link AwsSecretsManagerConfigurationProvider}.
   * @param builder Builder to configure. Not null.
   * @return The {@code builder}, with configuration added. Not null.
   */
  static ParameterSetParser.Builder configureBuilder(
      ParameterSetParser.Builder builder) {

    return builder.addParameter(
        "AUTHENTICATION",
        AwsCredentialsFactory.AUTHENTICATION_METHOD,
        AwsAuthenticationMethod.DEFAULT,
        AwsConfigurationParameters::parseAuthentication);
  }

  /**
   * Parses the value of the AUTHENTICATION parameter as an
   * {@link AwsAuthenticationMethod}.
   * @param authentication Parameter value to parse. Not null.
   * @return Authentication method parsed from the {@code value}. Not null.
   * @throws IllegalArgumentException If the {@code value} is not recognized.
   */
  private static AwsAuthenticationMethod parseAuthentication(
      String authentication) {
    switch (authentication) {
      case "AWS_DEFAULT":
        return AwsAuthenticationMethod.DEFAULT;
      default:
        throw new IllegalArgumentException(
            "Unrecognized value: " + authentication);
    }
  }
}