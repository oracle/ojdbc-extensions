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

package oracle.jdbc.provider.pkl.configuration;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.spi.OracleConfigurationProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PklParserTest {
  private static final String RESOURCE_NAME = "JdbcConfig.pkl";
  private static final String TEST_CONNECT_DESCRIPTOR = "dbhost:1521/orclpdb1";
  private static final String TEST_USER = "scott";
  private static final String TEST_PASSWORD = "tiger";

  private static File file;

  static {
    OracleConfigurationProvider.allowedProviders.add("file");
  }

  private static final OracleConfigurationProvider PROVIDER =
      OracleConfigurationProvider.find("file");

  @BeforeAll
  public static void setup() throws Exception {
    // Create a file
    file = File.createTempFile("myJdbcConfig", ".pkl");
    file.createNewFile();

    URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource(RESOURCE_NAME);
    if (resourceUrl == null) {
      throw new RuntimeException("Resource not found: " + RESOURCE_NAME);
    }

    FileWriter writer = new FileWriter(file);
    String content = "amends \"file://" + resourceUrl.getPath() + "\"\n\n"
        + "connect_descriptor = \"" + TEST_CONNECT_DESCRIPTOR + "\"\n"
        + "user = \"" + TEST_USER + "\"\n\n"
        + "// Password in OCI Vault\n"
        + "password {\n"
        + "  type = \"base64\"\n"
        + "  value = \"" + Base64.getEncoder().encodeToString(TEST_PASSWORD.getBytes(StandardCharsets.UTF_8)) + "\"\n"
        + "}\n\n"
        + "jdbc {\n"
        + "  autoCommit = false\n"
        + "  `oracle.jdbc.loginTimeout` = 3.s\n"
        + "}\n";
    writer.write(content);
    writer.close();

    System.out.println(content);
  }

  @Test
  void testPkl() throws Exception {
    final String location =  file.getAbsolutePath();

    Properties properties = PROVIDER
        .getConnectionProperties(location + "?parser=pkl");


    assertTrue(properties.containsKey("URL"), "Should contain property URL");
    assertEquals(TEST_USER, properties.getProperty(OracleConnection.CONNECTION_PROPERTY_USER_NAME));
    assertEquals(TEST_PASSWORD, properties.getProperty(OracleConnection.CONNECTION_PROPERTY_PASSWORD));
    assertEquals("false", properties.getProperty(OracleConnection.CONNECTION_PROPERTY_AUTOCOMMIT));
    assertEquals("3", properties.getProperty(OracleConnection.CONNECTION_PROPERTY_LOGIN_TIMEOUT));
  }
}
