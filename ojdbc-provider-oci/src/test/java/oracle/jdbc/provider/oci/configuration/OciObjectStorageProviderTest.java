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

package oracle.jdbc.provider.oci.configuration;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.oci.OciTestProperty;
import oracle.jdbc.spi.OracleConfigurationProvider;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies the {@link OciObjectStorageProvider} as implementing behavior
 * specified by its JavaDoc.
 */

public class OciObjectStorageProviderTest {

  static {
    OracleConfigurationProvider.allowedProviders.add("ociobject");
  }

  private static final OracleConfigurationProvider PROVIDER =
    OracleConfigurationProvider.find("ociobject");

  /**
   * Verifies the AUTHENTICATION=OCI_DEFAULT parameter setting.
   * This test will fail if (~/.oci/config) or (~/.oraclebmc/config) or the
   * environmental variable OCI_CONFIG_FILE is not set.
   */
  @Test
  public void testDefaultAuthentication() throws SQLException {
    String baseUrl = TestProperties.getOrAbort(
      OciTestProperty.OCI_OBJECT_STORAGE_URL);
    String[] options = new String[] {"AUTHENTICATION=OCI_DEFAULT"};
    String url = composeUrl(baseUrl, options);
    verifyProperties(url);
  }

  /**
   * Verifies the AUTHENTICATION=OCI_DEFAULT parameter setting with key option.
   */
  @Test
  public void testDefaultAuthenticationWithKeyOption() throws SQLException {
    String baseUrl =
      TestProperties.getOrAbort(OciTestProperty.OCI_OBJECT_STORAGE_URL_MULTIPLE_KEYS);
    String[] options = new String[] {
      "AUTHENTICATION=OCI_DEFAULT",
      "key=" + TestProperties.getOrAbort(OciTestProperty.OCI_OBJECT_STORAGE_URL_KEY)};
    String url = composeUrl(baseUrl, options);
    verifyProperties(url);
  }

  /** Verifies a properties object returned with a given URL. */
  private static void verifyProperties(String url) throws SQLException {
    Properties properties = PROVIDER.getConnectionProperties(url);

    assertNotNull(properties);
  }

  private static String composeUrl(String baseUrl, String... options) {
    return String.format("%s?%s", baseUrl, String.join("&", options));
  }
}
