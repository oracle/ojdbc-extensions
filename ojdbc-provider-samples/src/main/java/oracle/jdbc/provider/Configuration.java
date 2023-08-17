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
package oracle.jdbc.provider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static java.lang.String.format;

/**
 * Provides configuration from JVM system properties, environment variables,
 * or a configuration file.
 */
public final class Configuration {

  /**
   * Path of a configuration file. It is configuration.properties in the current
   * directory by default. It can be set to a non-default path with:
   * -DconfigurationFilePath=...
   */
  private static final Path FILE_PATH = Paths.get(System.getProperty(
      "configurationFilePath",
      "configuration.properties"))
    .toAbsolutePath();

  private static final Properties CONFIGURATION_FILE = loadFile();

  private Configuration() { };

  /**
   * Provides configuration from JVM system properties, environment variables,
   * or a configuration file.
   *
   * @param name Name of a configurable value. Not null.
   * @return The value configured for the given {@code name}. Not null.
   * @throws IllegalStateException If no value is configured for the given
   * {@code name}.
   */
  public static String getRequired(String name) {
    String config = get(name);

    if (config != null) {
      return config;
    } else {
      throw new IllegalStateException(format(
        "\"%s\" is not configured as a JVM system property," +
          " environment variable, or in \"%s\"",
        name, FILE_PATH));
    }
  }

  /**
   * Provides configuration from JVM system properties, environment variables,
   * or a configuration file.
   *
   * @param name Name of a configurable value. Not null.
   * @return The value configured for the given {@code name}, or {@code null}
   * if no value is configured.
   */
  public static String getOptional(String name) {
    return get(name);
  }

  private static String get(String name) {
    String value = System.getProperty(name);
    if (value != null)
      return value;

    value = System.getenv(name);
    if (value != null)
      return value;

    value = CONFIGURATION_FILE.getProperty(name);
    if (value != null)
      return value;

    return null;
  }

  private static Properties loadFile() {
    Properties properties = new Properties();

    if (!Files.exists(FILE_PATH))
      return properties;

    try (InputStream inputStream = Files.newInputStream(FILE_PATH)) {
      properties.load(inputStream);
    }
    catch (IOException ioException) {
      throw new IllegalStateException(ioException);
    }

    return properties;
  }


}
