/*
 ** Copyright (c) 2024 Oracle and/or its affiliates.
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
package oracle.jdbc.provider.gcp.configuration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import oracle.jdbc.driver.OracleConfigurationJsonProvider;
import oracle.jdbc.provider.gcp.secrets.GcpSecretManagerFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.util.OracleConfigurationCache;

/**
 * A provider for JSON payload which contains configuration from GCP Secret
 * Manager.
 * See {@link #getJson(String)} for the spec of the JSON payload.
 **/
public class GcpSecretManagerConfigurationProvider
    extends OracleConfigurationJsonProvider {

  @Override
  public String getType() {
    return "gcpsecretmanager";
  }

  /**
   * {@inheritDoc}
   * <p>
   * Returns the JSON payload stored in GCP Secret Manager secret.
   * </p>
   * 
   * @param location resource name of the secret version (to obtain the resource
   *                 name, click on "Actions" and "Copy resource name")
   * @return JSON payload
   */
  @Override
  public InputStream getJson(String location) throws SQLException {
    Map<String, String> namedValues = new HashMap<>();
    namedValues.put("secretVersionName", location);
    ParameterSet parameterSet = GcpConfigurationParameters.getParser().parseNamedValues(namedValues);
    return new ByteArrayInputStream(
        GcpSecretManagerFactory.getInstance().request(parameterSet).getContent().getData().toByteArray());
  }

  /**
   * {@inheritDoc}
   * @return cache of this provider which is used to store configuration
   */
  @Override
  public OracleConfigurationCache getCache() {
    return CACHE;
  }
}
