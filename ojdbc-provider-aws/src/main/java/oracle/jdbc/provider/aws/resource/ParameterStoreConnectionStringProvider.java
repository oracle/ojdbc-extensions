package oracle.jdbc.provider.aws.resource;

import oracle.jdbc.spi.ConnectionStringProvider;
import oracle.jdbc.provider.resource.ResourceParameter;
import oracle.jdbc.provider.util.TNSNames;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static oracle.jdbc.provider.util.CommonParameters.TNS_ALIAS;
import static oracle.jdbc.provider.util.FileUtils.decodeIfBase64;

/**
 * Provider that retrieves tnsnames.ora from AWS Parameter Store and extracts a connection string.
 */
public class ParameterStoreConnectionStringProvider
        extends ParameterStoreSecretProvider
        implements ConnectionStringProvider {

  private static final ResourceParameter[] PARAMETERS = {
          new ResourceParameter(AwsParameterStoreResourceParameterNames.TNS_ALIAS, TNS_ALIAS)
  };

  public ParameterStoreConnectionStringProvider() {
    super("parameterstore-tnsnames", PARAMETERS);
  }

  @Override
  public String getConnectionString(Map<Parameter, CharSequence> parameterValues) {
    String alias = parseParameterValues(parameterValues).getRequired(TNS_ALIAS);
    byte[] fileBytes = decodeIfBase64(getSecret(parameterValues).getBytes());

    try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
      TNSNames tnsNames = TNSNames.read(inputStream);
      String connectionString = tnsNames.getConnectionStringByAlias(alias);
      if (connectionString == null) {
        throw new IllegalArgumentException("Alias specified does not exist in tnsnames.ora: " + alias);
      }
      return connectionString;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read tnsnames.ora content", e);
    }
  }
}
