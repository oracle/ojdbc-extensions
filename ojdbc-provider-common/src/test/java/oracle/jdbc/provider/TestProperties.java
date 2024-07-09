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

package  oracle.jdbc.provider;

import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * Test properties for {@link oracle.jdbc.spi.OracleResourceProvider}
 * implementations that integrate with Azure. Properties are read from a
 * {@link java.util.Properties#load(Reader) properties file}. A file
 * named "test.properties" in the current directory is read by default. A
 * non-default path may be specified by a JVM system property:
 * <pre>
 *   -Doracle.jdbc.provider.TestProperties=/path/to/file.properties
 * </pre>
 */
public final class TestProperties {

  private static final Logger LOGGER =
    Logger.getLogger(TestProperties.class.getName());

  private TestProperties(){}

  /** Path of the file to read properties from */
  private static final Path FILE_PATH = Paths.get(System.getProperty(
    "oracle.jdbc.provider.TestProperties",
    "example-test.properties"))
    .toAbsolutePath();

  /**
   * Properties read from the {@link #FILE_PATH}, or {@code null} the file could not be read
   */
  private static final Properties PROPERTIES = readPropertiesFile();

  /**
   * Reads properties from the {@link #FILE_PATH}, or returns {@code null} if
   * the file can not be read.
   */
  private static Properties readPropertiesFile() {
    Properties properties = new Properties();
    try (InputStream inputStream = Files.newInputStream(FILE_PATH)) {
      properties.load(inputStream);
    }
    catch (IOException ioException) {
      if (LOGGER.isLoggable(Level.WARNING)) {
        LOGGER.log(
          Level.WARNING,
          format("Failed to read test properties file: %s", FILE_PATH),
          ioException);
      }
    }
    return properties;
  }

  /**
   * Returns the properties read from the {@link #FILE_PATH}, if any.
   */
  public static Properties getProperties() {
    Properties properties = new Properties();
    properties.putAll(PROPERTIES);
    return properties;
  }

  /**
   * Returns the value of a named property, or {@code null} if no value is
   * configured.
   * @param enumValue An enum having the {@link Enum#name()} of a property. Not
   * null.
   */
  public static String getOptional(Enum<?> enumValue) {
    return getOptional(enumValue.name());
  }

  /**
   * Returns the value of a named property, or {@code null} if no value is
   * configured.
   * @param name Case-sensitive name of a property. Not null.
   */
  public static String getOptional(String name) {
    return PROPERTIES.getProperty(name);
  }

  /**
   * Returns the value of a named property, or aborts the calling test if no
   * value is configured.
   * @param enumValue An enum having the {@link Enum#name()} of a property.
   */
  public static String getOrAbort(Enum<?> enumValue) {
    return getOrAbort(enumValue.name());
  }

  /**
   * Returns the value of a named property, or aborts the calling test if no
   * value is configured.
   * @param name Case-sensitive name of a property. Not null.
   */
  public static String getOrAbort(String name) {
    String value = getProperties().getProperty(name);
    Assumptions.assumeTrue(
      value != null,
      format("No value configured for \"%s\" in \"%s\"", name, FILE_PATH));
    return value;
  }

  /**
   * Checks if a property is equal to a given value, and aborts the calling
   * test if it is not. The equality check is case-insensitive.
   * @param enumValue An enum having the {@link Enum#name()} of a property. Not
   * null.
   * @param value The value of a property. May be null to indicate that a value
   * is not set, causing a test to abort if the value is set.
   */
  public static void abortIfNotEqual(Enum<?> enumValue, String value) {
    abortIfNotEqual(enumValue.name(), value);
  }

  /**
   * Checks if a property is equal to a given value, and aborts the calling
   * test if it is not. The equality check is case-insensitive.
   * @param name The name of a property. Not null.
   * @param value The value of a property. May be null to indicate that a value
   * is not set, causing a test to abort if the value is set.
   */
  public static void abortIfNotEqual(String name, String value) {
    String actualValue = getOrAbort(name);
    Assumptions.assumeTrue(
      actualValue.equalsIgnoreCase(value),
      format(
        "Value of \"%s\", \"%s\", is not equal to \"%s\"" +
          " in test properties file: %s",
        name, actualValue, value, FILE_PATH));
  }
}
