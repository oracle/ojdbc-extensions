package oracle.jdbc.provider.pkl.configuration.parser;

import oracle.jdbc.provider.pkl.configuration.parser.generated.OjdbcConfig;
import oracle.jdbc.spi.OracleConfigurationParser;
import oracle.jdbc.spi.OracleConfigurationSecretProvider;
import org.pkl.config.java.Config;
import org.pkl.config.java.ConfigEvaluator;
import org.pkl.config.java.NoSuchChildException;
import org.pkl.core.Duration;
import org.pkl.core.ModuleSource;
import org.pkl.core.PNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static oracle.jdbc.OracleConnection.CONNECTION_PROPERTY_PASSWORD;
import static oracle.jdbc.OracleConnection.CONNECTION_PROPERTY_WALLET_LOCATION;

public class YamlParser implements OracleConfigurationParser {
  @Override
  public Properties parse(
    InputStream inputStream, Map<String, String> options) throws SQLException {

    Properties properties = new Properties();
    Config pklConfig;

    try (ConfigEvaluator evaluator = ConfigEvaluator.preconfigured()) {
      // Create a temp file that is deleted after JVM terminates
      File tempFile = File.createTempFile("temp", ".pkl");
      tempFile.deleteOnExit();

      byte[] buffer = inputStream.readAllBytes();
      try (OutputStream outputStream = new FileOutputStream(tempFile)) {
        outputStream.write(buffer);
      }

      final String pklScript = "import \"package://pkg.pkl-lang.org/pkl-pantry/pkl.experimental.deepToTyped@1.0.0#/deepToTyped.pkl\" as t " +
        "import \"pkl:yaml\" " +
        "import \"https://raw.githubusercontent.com/oracle/ojdbc-extensions/support-pkl/ojdbc-provider-pkl/src/main/resources/ojdbcConfig.pkl\" " +
        "file = read(\"file://" + tempFile.getAbsolutePath() + "\") " +
        "parsed = new yaml.Parser {}.parse(file) " +
        "config = t.apply(ojdbcConfig.getClass(), parsed)";

      pklConfig = evaluator.evaluate(ModuleSource.text(pklScript));

      pklConfig = pklConfig.get("config");

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
  }

  @Override
  public String getType() {
    return "yaml";
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

    if (ojdbcConfig != null) {
      Field[] fields = ojdbcConfig.getClass().getDeclaredFields();
      for (Field field : fields) {
        Object value = field.get(ojdbcConfig);
        if (value == null) continue;

        Class<?> type = field.getType();
        if (type == Duration.class) {
          value = durationToInt((Duration) value);
        }

        // Replace $$ with .
        String name = field.getName().replace("$$", ".");
        properties.setProperty(name, value.toString());
      }
    }

    return properties;
  }

  private int durationToInt(Duration duration) {
    return Double.valueOf(duration.getValue()).intValue();
  }
}
