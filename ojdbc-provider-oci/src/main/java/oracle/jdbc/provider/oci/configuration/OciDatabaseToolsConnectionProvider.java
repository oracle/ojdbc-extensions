package oracle.jdbc.provider.oci.configuration;

import com.oracle.bmc.databasetools.model.*;
import com.oracle.bmc.model.BmcException;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.provider.oci.databasetools.DatabaseToolsConnectionFactory;
import oracle.jdbc.provider.oci.vault.SecretFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.spi.OracleConfigurationProvider;
import oracle.jdbc.util.OracleConfigurationCache;
import oracle.jdbc.util.OracleConfigurationProviderNetworkError;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class OciDatabaseToolsConnectionProvider
    implements OracleConfigurationProvider {

  private static final String CONFIG_TIME_TO_LIVE =
    "config_time_to_live";
  private static final long MS_TIMEOUT = 60_000L;
  private static final long MS_REFRESH_INTERVAL = 60_000L;

  private final OracleConfigurationCache cache = OracleConfigurationCache
    .create(100);

  @Override
  public Properties getConnectionProperties(String location)
      throws SQLException {

    Properties cachedProp = cache.get(location);
    if (Objects.nonNull(cachedProp)) {
      return cachedProp;
    }

    // Retrieve and add the properties to the cache
    Properties properties = getRemoteProperties(location);

    if (properties.containsKey(CONFIG_TIME_TO_LIVE)) {
      long configTimeToLive = Long.parseLong(
        properties.getProperty(CONFIG_TIME_TO_LIVE));

      // properties stored in the cache should not contain information of TTL
      properties.remove(CONFIG_TIME_TO_LIVE);
      cache.put(
        location,
        properties,
        configTimeToLive,
        () -> this.refreshProperties(location),
        MS_TIMEOUT,
        MS_REFRESH_INTERVAL);
    } else {
      cache.put(location,
        properties,
        () -> this.refreshProperties(location),
        MS_TIMEOUT,
        MS_REFRESH_INTERVAL);
    }

    return properties;
  }

  @Override
  public String getType() {
    return "ocidbtools";
  }

  private Properties getRemoteProperties(String location) {
    // Split connection ocid from options
    Map<String, String> options = null;
    String[] params = location.split("\\?");
    if (params.length > 1) {
      options = OracleConfigurationProvider.mapOptions(params[1]);
    } else {
      options = new HashMap<>();
    }

    ParameterSet commonParameters =
      OciConfigurationParameters.getParser()
        .parseNamedValues(options)
        .copyBuilder()
        .add("connection_ocid",
          DatabaseToolsConnectionFactory.CONNECTION_OCID,
          params[0]).build();

    DatabaseToolsConnectionOracleDatabase connection =
      DatabaseToolsConnectionFactory.getInstance()
        .request(commonParameters)
        .getContent();

    LifecycleState state = connection.getLifecycleState();
    if (!(state.equals(LifecycleState.Active) || state.equals(
      LifecycleState.Updating))) {
      throw new IllegalStateException(
        "Connection requested is in invalid state. Only ACTIVE or UPDATING " +
          "are valid. Current state: " + state);
    }

    Properties properties = new Properties();
    properties.put("URL",
      "jdbc:oracle:thin:@" + connection.getConnectionString());
    properties.put("user", connection.getUserName());

    // Get password from Secret
    ParameterSet passwordParameters = commonParameters.copyBuilder()
      .add("value", SecretFactory.OCID,
        ((DatabaseToolsUserPasswordSecretId) connection.getUserPassword())
          .getSecretId())
      .build();

    properties.put("password", new String(
      SecretFactory.getInstance()
        .request(passwordParameters)
        .getContent()
        .toCharArray()));

    // Get Wallet from Secret
    if (connection.getKeyStores() != null
      && !connection.getKeyStores().isEmpty()) {
      DatabaseToolsKeyStore keyStore = connection.getKeyStores().get(0);
      String secretId =
        ((DatabaseToolsKeyStoreContentSecretId) keyStore.getKeyStoreContent())
          .getSecretId();

      ParameterSet walletParameters = commonParameters.copyBuilder()
        .add("value", SecretFactory.OCID, secretId)
        .build();

      properties.put(
        OracleConnection.CONNECTION_PROPERTY_WALLET_LOCATION,
        "data:;base64," +
          SecretFactory.getInstance()
            .request(walletParameters)
            .getContent()
            .getBase64Secret());
    }

    Map<String, String> advancedProps = connection.getAdvancedProperties();
    if (advancedProps != null)
      properties.putAll(connection.getAdvancedProperties());

    return properties;
  }

  private Properties refreshProperties(String location)
    throws OracleConfigurationProviderNetworkError {
    try {
      return getRemoteProperties(location);
    } catch (BmcException bmcException) {
      throw new OracleConfigurationProviderNetworkError(bmcException);
    }
  }
}