package oracle.jdbc.provider.configuration;

import oracle.jdbc.provider.configuration.generated.OjdbcConfig;
import oracle.jdbc.spi.OracleConfigurationFileReader;
import oracle.jdbc.spi.OracleConfigurationSecretProvider;
import org.pkl.config.java.Config;
import org.pkl.config.java.ConfigEvaluator;
import org.pkl.config.java.JavaType;
import org.pkl.config.java.NoSuchChildException;
import org.pkl.core.Duration;
import org.pkl.core.ModuleSource;
import org.pkl.core.PNull;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static oracle.jdbc.OracleConnection.CONNECTION_PROPERTY_PASSWORD;
import static oracle.jdbc.OracleConnection.CONNECTION_PROPERTY_WALLET_LOCATION;

public class OracleConfigurationPklFileReader implements OracleConfigurationFileReader {
  @Override
  public Properties readProperties(
    InputStream inputStream, Map<String, String> options) throws SQLException {
    Properties properties = new Properties();
    Config pklConfig;

    // Parse Pkl config as text
    try (ConfigEvaluator evaluator = ConfigEvaluator.preconfigured()) {
      String fileString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      pklConfig = evaluator.evaluate(ModuleSource.text(fileString));
    } catch (IOException e) {
      throw new SQLException(e);
    }

    // If key is present, we parse that pkl class
    String key = options.get("key");
    if (key != null) {
      try {
        pklConfig = pklConfig.get(key);
      } catch (NoSuchChildException e) {
        throw new IllegalArgumentException(
          key + " key appears in URL but is missing in module.");
      }
    }

    // User common connect_descriptor as the URL
    OjdbcConfig ojdbcConfig = pklConfig.as(OjdbcConfig.class);

    if (ojdbcConfig.connect_descriptor != null)
      properties.put("URL",
        "jdbc:oracle:thin:@" + ojdbcConfig.connect_descriptor);

    if (ojdbcConfig.user != null)
      properties.put("user", ojdbcConfig.user);

    if (ojdbcConfig.password != null) {
      // Get password from the provider
      String passwordProviderType = ojdbcConfig.password.type;
      OracleConfigurationSecretProvider passwordSecretProvider =
        OracleConfigurationSecretProvider.find(passwordProviderType);

      byte[] decodedPassword = Base64
        .getDecoder()
        .decode(new String(passwordSecretProvider.getSecret(
          toSecretMap(ojdbcConfig.password))));
      properties.setProperty(CONNECTION_PROPERTY_PASSWORD,
        new String(decodedPassword));
    }

    if (ojdbcConfig.wallet_location != null) {
      // Get wallet from the provider
      String walletProviderType = ojdbcConfig.wallet_location.type;
      OracleConfigurationSecretProvider walletSecretProvider =
        OracleConfigurationSecretProvider.find(walletProviderType);

      char[] walletCharArray =
        walletSecretProvider.getSecret(
          toSecretMap(ojdbcConfig.wallet_location));

      // The value returned from the provider is a base64 encoded char array of
      // a wallet in base64 format. So after the fist decoding, the value is
      // still base64 encoded.
      String wallet = new String(
        Base64.getDecoder()
          .decode(new String(walletCharArray)));
      properties.setProperty(CONNECTION_PROPERTY_WALLET_LOCATION,
        "data:;base64," + wallet);
    }

    // Retrieve config time to live
    if (ojdbcConfig.config_time_to_live != null) {
      properties.setProperty("config_time_to_live",
        String.valueOf(
          durationToInt(ojdbcConfig.config_time_to_live)));
    }

    // Retrieve Jdbc connection properties
    try {
      properties.putAll(toJdbcProperties(ojdbcConfig.jdbc));
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }

    return properties;

    /*
    // User common connect_descriptor as the URL
    properties.put("URL",
      "jdbc:oracle:thin:@" +
        pklConfig.get("connect_descriptor").as(String.class));

    Optional.ofNullable(
      getOptional(pklConfig,"user")).ifPresent(
        u -> properties.put("user", u.as(String.class)));

    // Retrieve the password depending on the provider, if present
    Optional.ofNullable(
      getOptional(pklConfig,"password")).ifPresent(
      passwordConfig -> {
        Map<String, String> passwordOptions = toSecretMap(passwordConfig);
        String passwordProviderType = passwordConfig.get("type").as(String.class);

        OracleConfigurationSecretProvider passwordSecretProvider =
          OracleConfigurationSecretProvider.find(passwordProviderType);

        // Decode password returned from the provider
        properties.setProperty(CONNECTION_PROPERTY_PASSWORD,
          new String(Base64
            .getDecoder()
            .decode(
              new String(passwordSecretProvider.getSecret(passwordOptions)))));
      });

    // Retrieve the wallet content depending on the provider, if present
    Optional.ofNullable(
      getOptional(pklConfig,"wallet_location")).ifPresent(
      walletConfig -> {
        Map<String, String> walletOptions = toSecretMap(walletConfig);
        String walletProviderType = walletConfig.get("type").as(String.class);

        OracleConfigurationSecretProvider walletSecretProvider =
          OracleConfigurationSecretProvider.find(walletProviderType);

        // Get the wallet content from the provider
        String decodedSsoWallet = new String(
          Base64.getDecoder()
            .decode(new String(walletSecretProvider.getSecret(walletOptions))));
        properties.setProperty(CONNECTION_PROPERTY_WALLET_LOCATION,
          "data:;base64," + decodedSsoWallet);
      });


    // Retrieve config_time_to_live


    // Retrieve jdbc properties
    Config jdbcConfig = getOptional(pklConfig,("jdbc"));
    if (jdbcConfig != null) {
      Map<String, Object> jdbcProperties = jdbcConfig.as(
        JavaType.mapOf(String.class, Object.class));
      jdbcProperties.forEach((k, s) -> {
        if (s != null)
          properties.setProperty(k, s.toString());
      });
    }

    return properties;
    */

    /*
    try (Evaluator evaluator = Evaluator.preconfigured()) {

      Properties properties = new Properties();

      String fileString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      PObject module = evaluator.evaluate(ModuleSource.text(fileString));

      // If key is present, we parse that pkl class
      String key = options.get("key");
      if (key != null ) {
        try {
          var v = module.get(key);
          if (v != null)
            module = (PObject)v;
        } catch (NoSuchPropertyException e) {
          throw new IllegalArgumentException(
            key + " key appears in URL but is missing in module.", e);
        }
      }

      module.getProperties().forEach((k,v) -> {
        // Skip if value is a null or is a type of PNull
        if (v == null || v instanceof PNull) return;

        // User common connect_descriptor as the URL
        if (k.equals("connect_descriptor")) {
          k = "URL";
          v = "jdbc:oracle:thin:@" + v;
        }

        // Retrieve the password depending on the provider, if present
        if (k.equals(CONNECTION_PROPERTY_PASSWORD)) {
          OracleJsonObject password = json
            .get(CONNECTION_PROPERTY_PASSWORD)
            .asJsonObject();
          String secretType = password.getString("type");

          // Load (or recover) the password depending on the type
          OracleConfigurationSecretProvider<OracleJsonObject> provider = null;
          if (secretProviders.containsKey(secretType)) {
            provider = secretProviders.get(secretType);
          } else {
            provider = OracleConfigurationSecretProvider.find(secretType);
            secretProviders.put(secretType, provider);
          }

          // Get the decoded password from the provider
          properties.setProperty(CONNECTION_PROPERTY_PASSWORD,
            new String(Base64
              .getDecoder()
              .decode(new String(provider.getSecret(password)))));
        }

        // wallet_location
        if (k.startsWith("wallet_location")) {
          k = OracleConnection.CONNECTION_PROPERTY_WALLET_LOCATION;
          v = "data:;base64," + v;
        }

        properties.put(k, v);
      });

      return properties;
    } catch (IOException e) {
      throw new SQLException(e);
    }
    */
  }

  @Override
  public String getType() {
    return "pkl";
  }

  /**
   * @return value of the element under node, if presents. Otherwise, return null.
   */
  private Config getOptional(Config node, String s) {
    try {
      Config child =  node.get(s);
      if (!(child.getRawValue() instanceof PNull)) // not a leaf node
        return child;
    } catch (NoSuchChildException e) {
      // no-op
    }

    return null;
  }

  private Map<String, String> toSecretMap(Config secretConfig) {
    // Convert Pkl config to map
    Map<String, String> options = new HashMap<>();
    options.put("value", secretConfig.get("value").as(String.class));

    Config authConfig = getOptional(secretConfig, "authentication");
    if (authConfig != null) {
      Map<String, String> authMap = authConfig.as(
        JavaType.mapOf(String.class, String.class));
      // Rename the key from "method" to "authentication" and put into the password options
      options.put("authentication", authMap.remove("method"));
      // Put the rest into authentication options
      options.putAll(authMap);
    }

    return options;
  }

  private Map<String, String> toSecretMap(OjdbcConfig.Secret secretConfig) {
    // Convert Pkl config to map
    Map<String, String> options = new HashMap<>();
    options.put("value", secretConfig.value);

    if (secretConfig.authentication != null) {
      // Rename the key from "method" to "authentication" and put into the password options
      options.put("authentication", secretConfig.authentication.remove("method"));
      // Put the rest into authentication options
      options.putAll(secretConfig.authentication);
    }

    return options;
  }

  private Properties toJdbcProperties(OjdbcConfig.Jdbc ojdbcConfig)
    throws IllegalAccessException {

    Properties properties = new Properties();

    Field[] fields = ojdbcConfig.getClass().getDeclaredFields();
    for (Field field : fields) {
      Object value = field.get(ojdbcConfig);
      if (value == null) continue;

      Class<?> type = field.getType();
      if (type == Duration.class) {
        value = durationToInt((Duration)value);
      }

      // Replace $$ with .
      String name = field.getName().replace("$$", ".");
      properties.setProperty(name, value.toString());
    }

    return properties;
  }

  private int durationToInt(Duration duration) {
    return Double.valueOf(duration.getValue()).intValue();
  }
}
