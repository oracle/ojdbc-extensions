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

package oracle.jdbc.provider.util.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Base class for interactive setup wizards run directly from a provider jar
 * (ie: {@code java -jar ojdbc-provider-<name>-<version>.jar --setup}).
 * Subclasses supply the provider-specific menu content (which centralized
 * configuration schemes exist, which resource providers exist, and how
 * authentication is configured); this class owns the menu loop, prompting,
 * and export/print/clear plumbing shared by every provider module that has
 * a wizard. See {@link ProviderJarInfo}, this class's own base, for the
 * plain-info entry point shared by every provider module, wizard or not.
 */
public abstract class ProviderSetupCli extends ProviderJarInfo {

  private final Scanner scanner;
  private final List<GeneratedUrl> generatedUrls = new ArrayList<>();
  private final LinkedHashMap<String, String> generatedProperties =
    new LinkedHashMap<>();
  private final LinkedHashMap<String, String> generatedPropertyComments =
    new LinkedHashMap<>();

  protected ProviderSetupCli(Scanner scanner) {
    this.scanner = scanner;
  }

  /**
   * Presents the menu of centralized configuration URL schemes.
   */
  protected abstract void setupCentralizedConfigUrl();

  /**
   * Presents the menu of resource providers.
   */
  protected abstract void setupResourceProvider();

  /**
   * Whether this provider offers any centralized configuration URL scheme
   * at all. Defaults to true. Override to return false for a provider like
   * Nimbus that only offers resource providers, so the main menu never
   * offers an "Add centralized configuration URL" option that would lead
   * nowhere for that provider.
   *
   * @return True if {@link #setupCentralizedConfigUrl()} does something.
   */
  protected boolean hasCentralizedConfig() {
    return true;
  }

  /**
   * Prints the module name, version, README link, and a pointer to the
   * {@value #SETUP_FLAG} flag that unlocks {@link #run()}.
   */
  @Override
  protected void printInfo() {
    super.printInfo();
    System.out.println();
    System.out.println(
      "Run with " + SETUP_FLAG + " to configure a provider "
        + "interactively (eg: java -jar <this-jar> " + SETUP_FLAG + ").");
  }

  /**
   * Runs the interactive setup wizard. Called by {@link #start(String[])}
   * when {@value #SETUP_FLAG} is present; unlike the default
   * {@link ProviderJarInfo#onSetupRequested()}, this does not also call
   * {@link #printInfo()}, since the wizard prints its own banner below.
   */
  @Override
  protected final void onSetupRequested() {
    run();
  }

  private void run() {
    System.out.println();
    System.out.println(displayName() + " " + version());
    System.out.println(
      "This helper prints setup values. It does not connect to the "
        + "provider or validate credentials.");

    while (true) {
      List<String> labels = new ArrayList<>();
      List<Runnable> actions = new ArrayList<>();

      if (hasCentralizedConfig()) {
        labels.add("Add centralized configuration URL");
        actions.add(this::setupCentralizedConfigUrl);
      }
      labels.add("Add resource provider");
      actions.add(this::setupResourceProvider);
      if (hasGeneratedConfiguration()) {
        labels.add("Export generated configuration");
        actions.add(this::exportGeneratedConfiguration);
        labels.add("Clear generated configuration");
        actions.add(this::clearGeneratedConfiguration);
      }
      labels.add("Exit");
      actions.add(null);

      int choice = promptMenu("Choose an option:", labels.toArray(new String[0]));
      Runnable action = actions.get(choice - 1);
      if (action == null) {
        return;
      }
      action.run();
    }
  }

  /**
   * Records a centralized configuration URL built from {@code baseUrl} and
   * the given authentication {@code parameters} (appended as a query
   * string), then prints a confirmation and a link to the docs.
   */
  protected final void addConfigUrl(
    String baseUrl, Map<String, String> parameters, String comment,
    String docsAnchor) {

    String url = baseUrl + queryString(parameters);
    generatedUrls.add(new GeneratedUrl(comment, url));

    System.out.println();
    System.out.println("Centralized configuration URL added.");
    printDocs(docsAnchor);
  }

  /**
   * Records resource provider {@code properties}, replacing any previously
   * generated properties for the same provider prefix, then prints a
   * confirmation and a link to the docs.
   */
  protected final void addResourceProperties(
    Map<String, String> properties, String comment, String anchor) {

    replaceGeneratedProperties(properties, comment);

    System.out.println();
    System.out.println("Resource provider configuration added.");
    printDocs(anchor);
  }

  protected final void printDocs(String anchor) {
    System.out.println();
    System.out.println("More information:");
    System.out.println(readmeUrl() + anchor);
  }

  /**
   * Prints {@code title} followed by a numbered list of {@code options}
   * (1-based), then reads a choice constrained to that range. The valid
   * range is always derived from {@code options.length}, so the printed
   * list and the accepted choices can never fall out of sync.
   */
  protected final int promptMenu(String title, String... options) {
    System.out.println();
    System.out.println(title);
    for (int i = 0; i < options.length; i++) {
      System.out.println((i + 1) + ". " + options[i]);
    }
    return readChoice(1, options.length);
  }

  protected final int readChoice(int min, int max) {
    while (true) {
      System.out.print("> ");
      String input = scanner.nextLine().trim();
      try {
        int choice = Integer.parseInt(input);
        if (choice >= min && choice <= max) {
          return choice;
        }
      }
      catch (NumberFormatException ignored) {
      }
      System.out.println("Enter a number from " + min + " to " + max + ".");
    }
  }

  /**
   * Prompts for a value that cannot be blank. Keeps re-prompting until the
   * user enters something other than whitespace.
   *
   * @param prompt Text printed before reading input.
   * @return The trimmed, non-empty input. Never null or empty.
   */
  protected final String readRequired(String prompt) {
    while (true) {
      System.out.print(prompt);
      String input = scanner.nextLine().trim();
      if (!input.isEmpty()) {
        return input;
      }
      System.out.println("A value is required.");
    }
  }

  /**
   * Prompts for a value that may be left blank.
   *
   * @param prompt Text printed before reading input.
   * @return The trimmed input, or null if the user entered nothing.
   */
  protected final String readOptional(String prompt) {
    System.out.print(prompt);
    String input = scanner.nextLine().trim();
    return input.isEmpty() ? null : input;
  }

  /**
   * Adds {@code key=value} to {@code values}, but only if {@code value}
   * isn't null. This is how skipped/optional prompts (see
   * {@link #readOptional}) stay out of the generated output instead of
   * showing up as blank properties.
   */
  protected static void addIfPresent(
    Map<String, String> values, String key, String value) {
    if (value != null) {
      values.put(key, value);
    }
  }

  /**
   * Copies every entry of {@code suffixValues} into {@code properties},
   * with {@code prefix + "."} attached to each key. Used to attach a set of
   * authentication parameters (eg: {@code authenticationMethod},
   * {@code awsRegion}) under whichever connection property is currently
   * being configured (eg: {@code oracle.jdbc.provider.password}).
   */
  protected static void addWithPrefix(
    Map<String, String> properties, String prefix,
    Map<String, String> suffixValues) {

    for (Map.Entry<String, String> suffixValue : suffixValues.entrySet()) {
      properties.put(prefix + "." + suffixValue.getKey(),
        suffixValue.getValue());
    }
  }

  /**
   * Merges a freshly-built batch of properties into
   * {@link #generatedProperties}. If the batch reconfigures a connection
   * property that already has a provider stored under it (eg: the user
   * already set up a Password provider and is now setting up another one),
   * the old entries for that property are removed first via
   * {@link #removeGeneratedProperties}, so old and new provider settings
   * never end up mixed together under the same key.
   */
  private void replaceGeneratedProperties(
    Map<String, String> properties, String comment) {
    LinkedHashMap<String, Boolean> providerPrefixes = new LinkedHashMap<>();

    for (Map.Entry<String, String> property : properties.entrySet()) {
      if (property.getValue().startsWith("ojdbc-provider-")) {
        providerPrefixes.put(property.getKey(), Boolean.TRUE);
      }
    }

    for (String providerPrefix : providerPrefixes.keySet()) {
      removeGeneratedProperties(providerPrefix);
      generatedPropertyComments.put(providerPrefix, comment);
    }

    generatedProperties.putAll(properties);
  }

  /**
   * Deletes every stored property whose key is {@code providerPrefix} or
   * starts with {@code providerPrefix + "."} (eg: removing
   * {@code oracle.jdbc.provider.password} also removes
   * {@code oracle.jdbc.provider.password.secretName}, etc). Prints a notice
   * if anything was actually removed.
   */
  private void removeGeneratedProperties(String providerPrefix) {
    boolean replaced = false;
    List<String> existingKeys = new ArrayList<>(generatedProperties.keySet());

    for (String existingKey : existingKeys) {
      if (existingKey.equals(providerPrefix)
        || existingKey.startsWith(providerPrefix + ".")) {
        generatedProperties.remove(existingKey);
        generatedPropertyComments.remove(existingKey);
        replaced = true;
      }
    }

    if (replaced) {
      System.out.println();
      System.out.println("Replaced existing configuration for "
        + providerPrefix + ".");
    }
  }

  /**
   * Handles the "Export generated configuration" menu option: shows the
   * print/append-to-file/back submenu, or tells the user there's nothing to
   * export yet.
   */
  private void exportGeneratedConfiguration() {
    if (!hasGeneratedConfiguration()) {
      System.out.println();
      System.out.println("No generated configuration yet.");
      return;
    }

    System.out.println();
    System.out.println("Choose an export option:");
    System.out.println("1. Print in terminal");
    System.out.println("2. Append to file");
    System.out.println("3. Back");

    switch (readChoice(1, 3)) {
      case 1:
        printGeneratedConfiguration();
        break;
      case 2:
        appendGeneratedConfigurationToFile();
        break;
      case 3:
        break;
      default:
        throw new AssertionError();
    }
  }

  /**
   * Prints everything generated so far straight to the terminal: the
   * centralized configuration URLs (if any), then the resource-provider
   * properties (if any).
   */
  private void printGeneratedConfiguration() {
    System.out.println();
    System.out.println("Generated configuration:");

    if (!generatedUrls.isEmpty()) {
      System.out.println();
      System.out.println("Centralized configuration JDBC URLs:");
      for (GeneratedUrl generatedUrl : generatedUrls) {
        System.out.println("# " + generatedUrl.comment);
        System.out.println(generatedUrl.url);
      }
    }

    if (!generatedProperties.isEmpty()) {
      System.out.println();
      System.out.println("ojdbc.properties:");
      printProperties(generatedProperties, generatedPropertyComments);
    }
  }

  /**
   * Prompts for a file path and appends everything generated so far to it.
   * Always appends, and creates the file if it doesn't exist yet, so running
   * the helper multiple times keeps adding to the same file.
   */
  private void appendGeneratedConfigurationToFile() {
    String filePath = readRequired("File path: ");
    Path path = Paths.get(filePath);

    try {
      boolean hasContent = Files.exists(path) && Files.size(path) > 0L;
      List<String> lines = fileExportLines(hasContent);

      Files.write(path, lines, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE, StandardOpenOption.APPEND);

      System.out.println();
      System.out.println("Generated configuration appended to " + path + ".");
    }
    catch (IOException exception) {
      System.out.println();
      System.out.println("Could not write generated configuration to "
        + path + ": " + exception.getMessage());
    }
  }

  /**
   * Builds the exact text lines to write when exporting to a file. URLs are
   * written as comments only (a raw JDBC URL isn't a valid properties-file
   * line), while properties are written as real {@code key=value} lines.
   *
   * @param appendSeparator Whether to start with a blank line, ie: whether
   * the target file already has content in it.
   */
  private List<String> fileExportLines(boolean appendSeparator) {
    List<String> lines = new ArrayList<>();

    if (appendSeparator) {
      lines.add("");
    }

    lines.add("# Added by Oracle JDBC provider setup helper");

    if (!generatedUrls.isEmpty()) {
      lines.add("# Centralized configuration JDBC URLs:");
      for (GeneratedUrl generatedUrl : generatedUrls) {
        lines.add("# " + generatedUrl.comment);
        lines.add("# " + generatedUrl.url);
      }
    }

    if (!generatedProperties.isEmpty()) {
      if (!generatedUrls.isEmpty()) {
        lines.add("");
      }

      for (Map.Entry<String, String> property
        : generatedProperties.entrySet()) {
        String comment = generatedPropertyComments.get(property.getKey());
        if (comment != null) {
          lines.add("# " + comment);
        }
        lines.add(escapeForPropertiesFile(property.getKey())
          + "=" + escapeForPropertiesFile(property.getValue()));
      }
    }

    return lines;
  }

  /**
   * Escapes {@code value} per the {@code java.util.Properties} file format,
   * so a real properties file this ends up in decodes back to exactly what
   * was typed. Backslash is the escape character in that format (eg: the
   * two characters "\" and "n" together mean a newline, not a literal
   * backslash followed by the letter n), so a value containing a literal
   * backslash, a Windows file path, a password, would otherwise come
   * back silently wrong once loaded: {@code java.util.Properties.load()}
   * (used by Oracle JDBC's own connection properties file) drops any
   * backslash that isn't part of a recognized escape sequence. Only used
   * for the file-export path; terminal output is left as typed, since
   * nothing re-parses it as a properties file.
   */
  private static String escapeForPropertiesFile(String value) {
    StringBuilder escaped = new StringBuilder(value.length());

    for (int i = 0; i < value.length(); i++) {
      char character = value.charAt(i);
      switch (character) {
        case '\\':
          escaped.append("\\\\");
          break;
        case '\n':
          escaped.append("\\n");
          break;
        case '\t':
          escaped.append("\\t");
          break;
        case '\r':
          escaped.append("\\r");
          break;
        case '\f':
          escaped.append("\\f");
          break;
        default:
          escaped.append(character);
      }
    }

    return escaped.toString();
  }

  /**
   * Handles the "Clear generated configuration" menu option: empties
   * everything generated so far, so the user can start over without
   * restarting the jar.
   */
  private void clearGeneratedConfiguration() {
    if (!hasGeneratedConfiguration()) {
      System.out.println();
      System.out.println("No generated configuration to clear.");
      return;
    }

    generatedUrls.clear();
    generatedProperties.clear();
    generatedPropertyComments.clear();

    System.out.println();
    System.out.println("Generated configuration cleared.");
  }

  /**
   * @return True if anything has been generated so far. Controls whether
   * the main menu shows Export/Clear, and guards those two actions from
   * running with nothing to act on.
   */
  private boolean hasGeneratedConfiguration() {
    return !generatedUrls.isEmpty() || !generatedProperties.isEmpty();
  }

  /** Prints each property as {@code # comment} (if any) then {@code key=value}. */
  private static void printProperties(
    Map<String, String> properties, Map<String, String> comments) {

    for (Map.Entry<String, String> property : properties.entrySet()) {
      String comment = comments.get(property.getKey());
      if (comment != null) {
        System.out.println("# " + comment);
      }
      System.out.println(property.getKey() + "=" + property.getValue());
    }
  }

  /**
   * Turns a parameter map into a URL query string, eg:
   * {@code ?AUTHENTICATION=AWS_DEFAULT&AWS_REGION=us-west-2}. Returns an
   * empty string (not a bare {@code "?"}) if {@code parameters} is empty.
   */
  private static String queryString(Map<String, String> parameters) {
    if (parameters.isEmpty()) {
      return "";
    }

    StringBuilder query = new StringBuilder("?");
    for (Map.Entry<String, String> parameter : parameters.entrySet()) {
      if (query.length() > 1) {
        query.append('&');
      }
      query.append(encodeQueryComponent(parameter.getKey()))
        .append('=')
        .append(encodeQueryComponent(parameter.getValue()));
    }
    return query.toString();
  }

  /**
   * Percent-encodes only the characters that would otherwise corrupt the
   * query string this value is embedded in: a space (encoded as {@code
   * %20}, never as {@code +}, the decoder used at connection time,
   * {@code oracle.jdbc.provider.parameter.UriParameters}, deliberately
   * treats a literal {@code +} as a literal {@code +}, not a space), a
   * literal {@code %} (the escape character itself), and {@code &}/{@code
   * =}/{@code #} (which that decoder relies on to find the boundaries
   * between parameters). Every other character including {@code /},
   * which does not need escaping inside a URI query is left exactly as
   * typed, so the printed URL stays as readable as possible instead of
   * being over-encoded like a generic {@code java.net.URLEncoder} would do.
   */
  private static String encodeQueryComponent(String value) {
    StringBuilder encoded = new StringBuilder(value.length());

    for (byte valueByte : value.getBytes(StandardCharsets.UTF_8)) {
      int unsignedByte = valueByte & 0xFF;
      if (unsignedByte == ' ' || unsignedByte == '%' || unsignedByte == '&'
        || unsignedByte == '=' || unsignedByte == '#'
        || unsignedByte < 0x20 || unsignedByte > 0x7E) {
        encoded.append('%')
          .append(Character.forDigit((unsignedByte >>> 4) & 0xF, 16))
          .append(Character.forDigit(unsignedByte & 0xF, 16));
      }
      else {
        encoded.append((char) unsignedByte);
      }
    }

    return encoded.toString();
  }

  /**
   * One centralized configuration URL plus the comment describing it. A
   * plain map won't do here: unlike properties, a URL has no natural
   * "key" of its own to attach a comment to.
   */
  private static final class GeneratedUrl {
    private final String comment;
    private final String url;

    private GeneratedUrl(String comment, String url) {
      this.comment = comment;
      this.url = url;
    }
  }
}