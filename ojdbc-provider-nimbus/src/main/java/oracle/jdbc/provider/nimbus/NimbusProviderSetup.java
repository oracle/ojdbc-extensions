/*
 *  Copyright (c) 2026 Oracle and/or its affiliates.
 *
 *  The Universal Permissive License (UPL), Version 1.0
 *
 *  Subject to the condition set forth below, permission is hereby granted to any
 *  person obtaining a copy of this software, associated documentation and/or data
 *  (collectively the "Software"), free of charge and under any and all copyright
 *  rights in the Software, and any and all patent rights owned or freely
 *  licensable by each licensor hereunder covering either (i) the unmodified
 *  Software as contributed to or provided by such licensor, or (ii) the Larger
 *  Works (as defined below), to deal in both
 *
 *  (a) the Software, and
 *  (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 *  one is included with the Software (each a "Larger Work" to which the Software
 *  is contributed by such licensors),
 *
 *  without restriction, including without limitation the rights to copy, create
 *  derivative works of, display, perform, and distribute the Software and make,
 *  use, sell, offer for sale, import, export, have made, and have sold the
 *  Software and the Larger Work(s), and to sublicense the foregoing rights on
 *  either these or other terms.
 *
 *  This license is subject to the following condition:
 *  The above copyright notice and either this complete permission notice or at
 *  a minimum a reference to the UPL must be included in all copies or
 *  substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package oracle.jdbc.provider.nimbus;

import oracle.jdbc.provider.util.cli.ProviderSetupCli;

import java.util.LinkedHashMap;
import java.util.Scanner;

/**
 * Interactive setup helper for users running this provider jar directly.
 * This module offers no centralized configuration provider: it registers
 * only a single resource provider, an OAuth 2.0 access token provider
 * using the Nimbus SDK.
 */
public final class NimbusProviderSetup extends ProviderSetupCli {

  private static final String README_URL =
    "https://github.com/oracle/ojdbc-extensions/blob/main/ojdbc-provider-nimbus/README.md";

  private NimbusProviderSetup(Scanner scanner) {
    super(scanner);
  }

  /**
   * Starts the interactive setup helper. Prints a short info banner unless
   * {@code --setup} is passed, in which case it runs the wizard.
   *
   * @param args Pass {@code --setup} to run the wizard.
   */
  public static void main(String[] args) {
    new NimbusProviderSetup(new Scanner(System.in)).start(args);
  }

  @Override
  protected String displayName() {
    return "Oracle JDBC Providers for Nimbus";
  }

  @Override
  protected String description() {
    return "Providers for integration between Oracle JDBC and the Nimbus "
      + "OAuth 2.0 SDK with OpenID Connect extensions.";
  }

  @Override
  protected String readmeUrl() {
    return README_URL;
  }

  @Override
  protected boolean hasCentralizedConfig() {
    return false;
  }

  /**
   * Unreachable: {@link #hasCentralizedConfig()} returns false, so the main
   * menu never offers the option that would call this. This module has no
   * centralized configuration provider, only the Access Token resource
   * provider.
   */
  @Override
  protected void setupCentralizedConfigUrl() {
    throw new AssertionError();
  }

  @Override
  protected void setupResourceProvider() {
    switch (promptMenu("Choose a resource provider:",
      "Access Token",
      "Back")) {
      case 1:
        setupAccessTokenProvider();
        break;
      case 2:
        break;
      default:
        throw new AssertionError();
    }
  }

  private void setupAccessTokenProvider() {
    String prefix = "oracle.jdbc.provider.accessToken";
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, "ojdbc-provider-nimbus-token");
    properties.put(prefix + ".tokenEndpoint",
      readRequired("Token endpoint "
        + "(e.g. https://example.com/oauth2/v1/token) [required]: "));

    switch (promptMenu("Choose a grant type:",
      "Client credentials",
      "Password",
      "Authorization code")) {
      case 1:
        properties.put(prefix + ".grantType", "client_credentials");
        properties.put(prefix + ".clientId", readRequired("Client ID [required]: "));
        properties.put(prefix + ".clientSecret",
          readRequired("Client secret [required]: "));
        break;
      case 2:
        properties.put(prefix + ".grantType", "password");
        properties.put(prefix + ".username",
          readRequired("Resource owner username [required]: "));
        properties.put(prefix + ".password",
          readRequired("Resource owner password [required]: "));
        addIfPresent(properties, prefix + ".clientId",
          readOptional("Client ID [optional, required by some "
            + "authorization servers]: "));
        addIfPresent(properties, prefix + ".clientSecret",
          readOptional("Client secret [optional, required by some "
            + "authorization servers]: "));
        break;
      case 3:
        properties.put(prefix + ".grantType", "authorization_code");
        properties.put(prefix + ".clientId", readRequired("Client ID [required]: "));
        addIfPresent(properties, prefix + ".clientSecret",
          readOptional("Client secret [optional, only if the client is "
            + "confidential]: "));
        properties.put(prefix + ".authorizationEndpoint",
          readRequired("Authorization endpoint "
            + "(e.g. https://example.com/oauth2/v1/authorize) [required]: "));
        properties.put(prefix + ".redirectUri",
          readRequired("Redirect URI (e.g. http://localhost:1977) [required]: "));
        break;
      default:
        throw new AssertionError();
    }

    addIfPresent(properties, prefix + ".scope",
      readOptional("Scope [optional in general, but usually required by "
        + "the authorization server, e.g. "
        + "https://tenant-name.cloud-name.example.com/scope.name; "
        + "multiple scopes are separated by spaces]: "));

    addResourceProperties(properties,
      "Access Token Provider from the Nimbus OAuth 2.0 SDK",
      "#access-token-provider");
  }
}