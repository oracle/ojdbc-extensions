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

package oracle.jdbc.provider.oci.authentication;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.ConfigFileReader.ConfigFile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

/**
 * Utility methods for processing
 * <a href="https://docs.oracle.com/en-us/iaas/Content/API/Concepts/sdkconfig.htm">
 * OCI Configuration Files
 * </a>
 */
final class ConfigFileUtil {

  /**
   * Config file parameter that identifies the file system path of a
   * <a
   * href="https://docs.oracle.com/en-us/iaas/Content/API/Concepts/sdk_authentication_methods.htm#sdk_authentication_methods_session_token">
   * session token
   * </a>
   */
  public static final String SECURITY_TOKEN_FILE = "security_token_file";

  /**
   * Config file parameter that identifies an
   * <a href="https://docs.oracle.com/en-us/iaas/Content/General/Concepts/regions.htm#top">
   * OCI region code
   * </a>
   */
  public static final String REGION = "region";

  /**
   * Config file parameter that identifies the OCID of a tenancy.
   */
  public static final String TENANCY = "tenancy";

  /**
   * Name of the default profile in an OCI Configuration File
   */
  public static final String DEFAULT_PROFILE = "DEFAULT";

  /**
   * Path of the default OCI Configuration File
   */
  public static final Path DEFAULT_PATH = Paths.get(
      System.getProperty("user.home"), ".oci", "config")
    .toAbsolutePath();

  private ConfigFileUtil() {}

  /**
   * <p>
   * Returns the {@link ConfigFile} for a {@code profile} at the given config
   * file {@code path}, or returns {@code null} if the file or profile do not
   * exist.
   * </p><p>
   * The {@code ConfigFile} returned by this method includes all parameters of
   * the given {@code profile}
   * <em>
   * along with parameters of the {@link #DEFAULT_PROFILE}.
   * </em>
   * </p>
   */
  public static ConfigFile getConfigFile(Path path, String profile) {
    if (!Files.exists(path))
      return null;

    try {
      return ConfigFileReader.parse(path.toString(), profile);
    }
    catch (IllegalArgumentException illegalArgumentException) {
      // ConfigFileReader.parse(...) throws IllegalArgumentException if the
      // profile does not exist (although the JavaDoc does not specify this
      // behavior).
      return null;
    }
    catch (IOException ioException) {
      throw new IllegalStateException(
        "Failed to read configuration file", ioException);
    }
  }

  /**
   * Returns the value of a {@code parameter} for a {@code profile}, or the
   * {@link #DEFAULT_PROFILE}, in an OCI Configuration File at the given
   * {@code path}. This method returns {@code null} if the file does not exist,
   * the profile does not exist, or the parameter is not defined.
   */
  public static String getParameter(
    Path path, String profile, String parameter) {
    ConfigFile configFile = getConfigFile(path, profile);
    if (configFile == null)
      return null;

    return configFile.get(parameter);
  }

  /**
   * Creates or updates a {@code profile} in an OCI configuration file at the
   * given {@code path}. When this method returns, the profile <em>only</em> the
   * given set of {@code parameters}; <em>Any existing parameters for the
   * profile will be gone</em>. This method creates the configurtion file if it
   * does not already exist.
   *
   * @param profile Profile to update. Not null.
   * @param parameters Parameters of the profile. Not null.
   */
  public static void update(
    Path path, String profile, Map<String, String> parameters) {

    remove(path, profile);

    try (BufferedWriter writer =
           Files.newBufferedWriter(path, CREATE, APPEND)) {

      writer.write("\n[" + profile + "]\n");

      for (Map.Entry<String, String> parameter : parameters.entrySet())
        writeConfigParameter(writer, parameter.getKey(), parameter.getValue());
    }
    catch (IOException ioException) {
      throw new IllegalStateException(
        "Failed to create configuration file", ioException);
    }
  }

  /**
   * Removes a {@code profile} from an OCI configuration file at the given
   * {@code path}. This method does nothing if the file or the profile do not
   * exist.
   * @param path Path of a configuration file. Not null.
   * @param profile Profile of a configuration file. Not null.
   */
  public static void remove(Path path, String profile) {
    if (!Files.exists(path))
      return;

    final CharBuffer newFileBuffer;

    try (BufferedReader reader = Files.newBufferedReader(path)) {
      newFileBuffer = CharBuffer.allocate(Math.toIntExact(Files.size(path)));

      String profileLine = "[" + profile + "]";

      String line;
      for (line = reader.readLine(); line != null; line = reader.readLine()) {
        // Find the line where the profile begins
        if (line.trim().equals(profileLine)) {

          // Read past the profile name and any lines that follow until a new
          // profile begins.
          for (line = reader.readLine(); line != null; line = reader.readLine()) {
            if (line.startsWith("[")) // a new profile begins
              break;
          }

          // Check if the last line was read
          if (line == null)
            break;

        }

        // Keep any lines that are not part of the removed profile
        newFileBuffer.put(line + '\n');
      }
    }
    catch (IOException ioException) {
      throw new IllegalStateException(
        "Failed to read configuration file", ioException);
    }

    // Write the new file, which is the same as the old file, except no longer
    // includes the removed profile
    try {
      Files.write(
        path,
        newFileBuffer.flip().toString().getBytes(UTF_8));
    }
    catch (IOException ioException) {
      throw new IllegalStateException(
        "Failed to update configuration file", ioException);
    }
  }

  /**
   * Writes a parameter for an OCI configuration file.
   * @param writer Writer for a configuration file. Not null.
   * @param name Name of the parameter. Not null.
   * @param value Value of the parameter. Not null.
   * @throws IOException If file I/O fails.
   */
  private static void writeConfigParameter(
    BufferedWriter writer, String name, String value)
    throws IOException {
    writer.write(name);
    writer.write('=');
    writer.write(value);
    writer.write('\n');
  }
}
