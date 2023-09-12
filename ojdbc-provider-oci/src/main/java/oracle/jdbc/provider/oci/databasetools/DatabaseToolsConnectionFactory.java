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
package oracle.jdbc.provider.oci.databasetools;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.databasetools.DatabaseToolsClient;
import com.oracle.bmc.databasetools.model.DatabaseToolsConnection;
import com.oracle.bmc.databasetools.model.DatabaseToolsConnectionOracleDatabase;
import com.oracle.bmc.databasetools.requests.GetDatabaseToolsConnectionRequest;
import com.oracle.bmc.databasetools.responses.GetDatabaseToolsConnectionResponse;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.oci.OciResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;

/**
 * <p>
 * Factory for requesting DatabaseToolsConnnection from OCI. Objects are
 * represented as {@link DatabaseToolsConnection}.
 * </p>
 */
public class DatabaseToolsConnectionFactory extends
    OciResourceFactory<DatabaseToolsConnection> {

  public static final Parameter<String> CONNECTION_OCID =
      Parameter.create(REQUIRED);
  private static final ResourceFactory<DatabaseToolsConnection>
      INSTANCE = new DatabaseToolsConnectionFactory();
  private DatabaseToolsConnectionFactory() {}

  /**
   * @return a singleton of {@code DatabaseToolsConnectionFactory}
   */
  public static ResourceFactory<DatabaseToolsConnection> getInstance() {
    return INSTANCE;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Requests the content of an Connection in {@code DatabaseToolsConnection}
   * type from the Database Tools service. A copy of
   * {@code DatabaseToolsConnection} is returned.
   * </p><p>
   * The {@code parameterSet} is required to include a {@link #CONNECTION_OCID}.
   * </p>
   * @param authenticationDetails Authentication details configured by the
   * {@code parameterSet}. Not null.
   * @param parameterSet Set of parameters that configure the request. Not null.
   * @return DatabaseToolsConnection which represents the content of a
   * Connection.
   * @throws IllegalArgumentException If the parser cannot find matching strings
   * in the URL Path.
   * @throws IllegalStateException If the {@code DatabaseToolsConnection}
   * returned from the Database Tools Connection service cannot be read, or
   * {@code ObjectStorageClient} cannot be acquired.
   */
  @Override
  protected Resource<DatabaseToolsConnection> request(
    AbstractAuthenticationDetailsProvider authenticationDetails,
    ParameterSet parameterSet) {
    String connectionOcid = parameterSet.getRequired(CONNECTION_OCID);

    try (DatabaseToolsClient client =
        DatabaseToolsClient.builder().build(authenticationDetails)) {

      GetDatabaseToolsConnectionResponse getResponse = client
          .getDatabaseToolsConnection(
              GetDatabaseToolsConnectionRequest
                  .builder()
                  .databaseToolsConnectionId(connectionOcid)
                  .build());

      return Resource.createPermanentResource(
          getResponse.getDatabaseToolsConnection(), true);

    } catch (Exception e) {
      throw new IllegalStateException(
        "Error occurs while acquiring Database Tools Connection client", e);
    }
  }
}