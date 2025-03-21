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

package oracle.jdbc.provider.oci.resource;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.oci.OciTestProperty;
import oracle.jdbc.spi.ConnectionStringProvider;
import oracle.jdbc.spi.OracleResourceProvider.Parameter;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.createParameterValues;
import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.findProvider;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Verifies {@link DatabaseConnectionStringProvider} */
public class DatabaseConnectionStringProviderTest {

  private static final ConnectionStringProvider PROVIDER =
    findProvider(
      ConnectionStringProvider.class,
      "ojdbc-provider-oci-database-connection-string");

  @Test
  public void test() {
    Map<String, CharSequence> testParameters = new HashMap<>();
    testParameters.put("authenticationMethod", "config-file");
    testParameters.put("configFile", TestProperties.getOrAbort(
      OciTestProperty.OCI_CONFIG_FILE));
    testParameters.put("profile", TestProperties.getOrAbort(
      OciTestProperty.OCI_CONFIG_PROFILE));
    testParameters.put("ocid", TestProperties.getOrAbort(
      OciTestProperty.OCI_DATABASE_OCID));

    Map<Parameter, CharSequence> parameterValues =
      createParameterValues(PROVIDER, testParameters);
    String descriptor = PROVIDER.getConnectionString(parameterValues);
    assertNotNull(descriptor);
  }

  @Test
  public void missingOCIDTest() {
    Map<String, CharSequence> testParameters = new HashMap<>();
    testParameters.put("authenticationMethod", "config-file");

    Map<Parameter, CharSequence> parameterValues =
      createParameterValues(PROVIDER, testParameters);
    IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> {
      PROVIDER.getConnectionString(parameterValues);
    });
    assertEquals("No value defined for parameter \"ocid\"", 
    illegalStateException.getCause().getMessage(), "Wrong error message");
  }

  @Test
  public void testWithConsumerGroup() {
    Map<String, CharSequence> testParameters = new HashMap<>();
    testParameters.put("authenticationMethod", "config-file");
    testParameters.put("configFile", TestProperties.getOrAbort(
      OciTestProperty.OCI_CONFIG_FILE));
    testParameters.put("profile", TestProperties.getOrAbort(
      OciTestProperty.OCI_CONFIG_PROFILE));
    testParameters.put("ocid", TestProperties.getOrAbort(
      OciTestProperty.OCI_DATABASE_OCID));
    testParameters.put("consumerGroup", "HIGH");

    Map<Parameter, CharSequence> parameterValues =
      createParameterValues(PROVIDER, testParameters);
    String descriptor = PROVIDER.getConnectionString(parameterValues);
    assertNotNull(descriptor);
  }
}
