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
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
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
 * Base class for interactive setup helpers run directly from a provider jar
 * (ie: {@code java -jar ojdbc-provider-<cloud>-<version>.jar}). Subclasses
 * supply the cloud-specific menu content (which centralized configuration
 * schemes exist, which resource providers exist, and how authentication is
 * configured for that cloud); this class owns the menu loop, prompting,
 * and export/print/clear plumbing shared by every cloud.
 */
public abstract class ProviderSetupCli {

  private final Scanner scanner;
  private final List<GeneratedUrl> generatedUrls = new ArrayList<>();
  private final LinkedHashMap<String, String> generatedProperties =
    new LinkedHashMap<>();
  private final LinkedHashMap<String, String> generatedPropertyComments =
    new LinkedHashMap<>();

  protected ProviderSetupCli(Scanner scanner) {
    this.scanner = scanner;
  }

  /** Display name printed in the banner, eg: "Oracle JDBC Providers for OCI". */
  protected abstract String displayName();

  /** Base README URL that doc anchors are appended to. */
  protected abstract String readmeUrl();

  /** Presents the menu of centralized configuration URL schemes. */
  protected abstract void setupCentralizedConfigUrl();

  /** Presents the menu of resource providers. */
  protected abstract void setupResourceProvider();

  /**
   * Runs the interactive setup helper. Subclasses expose this from their own
   * {@code main(String[])} method.
   */
  protected final void run() {
    System.out.println();
    System.out.println(displayName() + " " + version());
    System.out.println("Found in: " + location());
    System.out.println(
      "This helper prints setup values. It does not connect to the cloud "
        + "provider or validate credentials.");

    while (true) {
      int choice = hasGeneratedConfiguration()
        ? promptMenu("Choose an option:",
            "Add centralized configuration URL",
            "Add resource provider",
            "Export generated configuration",
            "Clear generated configuration",
            "Exit")
        : promptMenu("Choose an option:",
            "Add centralized configuration URL",
            "Add resource provider",
            "Exit");

      switch (choice) {
        case 1:
          setupCentralizedConfigUrl();
          break;
        case 2:
          setupResourceProvider();
          break;
        case 3:
          if (hasGeneratedConfiguration()) {
            exportGeneratedConfiguration();
          }
          else {
            return;
          }
          break;
        case 4:
          clearGeneratedConfiguration();
          break;
        case 5:
          return;
        default:
          throw new AssertionError();
      }
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

  protected final String readOptional(String prompt) {
    System.out.print(prompt);
    String input = scanner.nextLine().trim();
    return input.isEmpty() ? null : input;
  }

  protected static void addIfPresent(
    Map<String, String> values, String key, String value) {
    if (value != null) {
      values.put(key, value);
    }
  }

  protected static void addWithPrefix(
    Map<String, String> properties, String prefix,
    Map<String, String> suffixValues) {

    for (Map.Entry<String, String> suffixValue : suffixValues.entrySet()) {
      properties.put(prefix + "." + suffixValue.getKey(),
        suffixValue.getValue());
    }
  }

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
        lines.add(property.getKey() + "=" + property.getValue());
      }
    }

    return lines;
  }

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

  private boolean hasGeneratedConfiguration() {
    return !generatedUrls.isEmpty() || !generatedProperties.isEmpty();
  }

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

  private static String queryString(Map<String, String> parameters) {
    if (parameters.isEmpty()) {
      return "";
    }

    StringBuilder query = new StringBuilder("?");
    for (Map.Entry<String, String> parameter : parameters.entrySet()) {
      if (query.length() > 1) {
        query.append('&');
      }
      query.append(urlEncode(parameter.getKey()))
        .append('=')
        .append(urlEncode(parameter.getValue()));
    }
    return query.toString();
  }

  private static String urlEncode(String value) {
    try {
      return URLEncoder.encode(value, "UTF-8");
    }
    catch (UnsupportedEncodingException impossible) {
      throw new AssertionError(impossible);
    }
  }

  private String version() {
    String version = getClass().getPackage().getImplementationVersion();
    return version != null ? version : "unknown";
  }

  private URL location() {
    return getClass().getResource(getClass().getSimpleName() + ".class");
  }

  private static final class GeneratedUrl {
    private final String comment;
    private final String url;

    private GeneratedUrl(String comment, String url) {
      this.comment = comment;
      this.url = url;
    }
  }
}