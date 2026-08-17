/*
 ** Copyright (c) 2026 Oracle and/or its affiliates.
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
 ** The above copyright notice and either this complete permission notice or
 ** at a minimum a reference to the UPL must be included in all copies or
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

package oracle.jdbc.provider.spring;

import oracle.jdbc.provider.util.cli.ProviderSetupCli;

import java.util.LinkedHashMap;
import java.util.Scanner;

/**
 * Interactive setup helper for users running this provider jar directly.
 * <p>
 * This module offers no centralized configuration provider: it registers
 * only a single resource provider, an {@code EndUserSecurityContext}
 * provider that integrates with Spring Security. Its {@code dataRoles} and
 * {@code authorityRolePrefix} parameters (and likewise its
 * {@code endUserContextAttributes} and {@code authorityAttributesPrefix}
 * parameters) are not alternatives to choose between -- the provider merges
 * whichever of each pair are configured -- so this wizard, unlike the SEPS
 * wallet wizards in other providers, prompts for all of them independently
 * rather than forcing a single choice.
 * </p>
 */
public final class SpringProviderSetup extends ProviderSetupCli {

  private static final String README_URL =
    "https://github.com/oracle/ojdbc-extensions/blob/main/ojdbc-provider-spring/README.md";

  private SpringProviderSetup(Scanner scanner) {
    super(scanner);
  }

  /**
   * Starts the interactive setup helper. Prints a short info banner unless
   * {@code --setup} is passed, in which case it runs the wizard.
   *
   * @param args Pass {@code --setup} to run the wizard.
   */
  public static void main(String[] args) {
    new SpringProviderSetup(new Scanner(System.in)).start(args);
  }

  @Override
  protected String displayName() {
    return "Oracle JDBC Providers for Spring";
  }

  @Override
  protected String description() {
    return "Providers for integrations between Oracle JDBC and Spring.";
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
   * centralized configuration provider -- only the End User Security
   * Context resource provider.
   */
  @Override
  protected void setupCentralizedConfigUrl() {
    throw new AssertionError();
  }

  @Override
  protected void setupResourceProvider() {
    switch (promptMenu("Choose a resource provider:",
      "End User Security Context",
      "Back")) {
      case 1:
        setupEndUserSecurityContextProvider();
        break;
      case 2:
        break;
      default:
        throw new AssertionError();
    }
  }

  private void setupEndUserSecurityContextProvider() {
    String prefix = "oracle.jdbc.provider.endUserSecurityContext";
    LinkedHashMap<String, String> properties = new LinkedHashMap<>();
    properties.put(prefix, "ojdbc-provider-spring-end-user-security-context");
    properties.put(prefix + ".registrationId",
      readRequired("Registration ID (the OAuth 2.0 client registration ID "
        + "configured under spring.security.oauth2.client.registration.<id>) "
        + "[required]: "));
    addIfPresent(properties, prefix + ".dataRoles",
      readOptional("Data roles, comma-separated [optional, e.g. "
        + "common_user_role,read_only_role]: "));
    addIfPresent(properties, prefix + ".endUserContextAttributes",
      readOptional("End user context attributes, as a JSON object "
        + "[optional, e.g. {\"schema.context\":{\"key\":\"value\"}}]: "));
    addIfPresent(properties, prefix + ".authorityRolePrefix",
      readOptional("Authority role prefix, used to derive data roles from "
        + "Spring Security authorities [optional, e.g. "
        + "ORACLE_DATA_ROLE_]: "));
    addIfPresent(properties, prefix + ".authorityAttributesPrefix",
      readOptional("Authority attributes prefix, used to derive end user "
        + "context attributes from Spring Security authorities "
        + "[optional, e.g. ORACLE_CONTEXT_ATTRIBUTES_]: "));

    addResourceProperties(properties,
      "End User Security Context Provider from Spring",
      "#end-user-security-context-provider");
  }
}