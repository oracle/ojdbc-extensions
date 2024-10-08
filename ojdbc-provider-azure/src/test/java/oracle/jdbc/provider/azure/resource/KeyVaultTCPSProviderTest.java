package oracle.jdbc.provider.azure.resource;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.azure.AzureTestProperty;
import oracle.jdbc.spi.OracleResourceProvider;
import oracle.jdbc.spi.TlsConfigurationProvider;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.createParameterValues;
import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.findProvider;
import static org.junit.jupiter.api.Assertions.*;

public class KeyVaultTCPSProviderTest {
  private static final TlsConfigurationProvider PROVIDER =
          findProvider(
            TlsConfigurationProvider.class, "ojdbc-provider-azure-key-vault-tls");

  /**
   * Verifies that {@link KeyVaultTCPSProvider#getParameters()}  includes parameters
   * to configure a vault URL and secret name.
   */
  @Test
  public void testGetParameters() {
    Collection<? extends OracleResourceProvider.Parameter> parameters = PROVIDER.getParameters();
    assertNotNull(parameters);

    OracleResourceProvider.Parameter vaultUrlParameter =
            parameters.stream()
                    .filter(parameter -> "vaultUrl".equals(parameter.name()))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
    assertFalse(vaultUrlParameter.isSensitive());
    assertTrue(vaultUrlParameter.isRequired());
    assertNull(vaultUrlParameter.defaultValue());

    OracleResourceProvider.Parameter secretNameParameter =
            parameters.stream()
                    .filter(parameter -> "secretName".equals(parameter.name()))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
    assertFalse(secretNameParameter.isSensitive());
    assertTrue(secretNameParameter.isRequired());
    assertNull(secretNameParameter.defaultValue());

    OracleResourceProvider.Parameter typeParameter =
            parameters.stream()
                    .filter(parameter -> "type".equals(parameter.name()))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
    assertFalse(typeParameter.isSensitive());
    assertTrue(typeParameter.isRequired());
    assertNull(typeParameter.defaultValue());

    OracleResourceProvider.Parameter passwordParameter =
            parameters.stream()
                    .filter(parameter -> "password".equals(parameter.name()))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
    assertTrue(passwordParameter.isSensitive());
    assertFalse(passwordParameter.isRequired());
    assertNull(passwordParameter.defaultValue());

  }

  @Test
  public void testGetSSLContext() {

    Map<String, String> testParameters = new HashMap<>();
    testParameters.put(
            "vaultUrl",
            TestProperties.getOrAbort(AzureTestProperty.AZURE_KEY_VAULT_URL));
    testParameters.put(
            "secretName",
            TestProperties.getOrAbort(AzureTestProperty.AZURE_KEY_VAULT_SECRET_NAME));

    testParameters.put(
            "type",
            TestProperties.getOrAbort(AzureTestProperty.AZURE_TLS_FILE_TYPE));

    Optional.ofNullable(TestProperties.getOptional(
                    AzureTestProperty.AZURE_TLS_FILE_PASSWORD))
            .ifPresent(password -> testParameters.put("password",
                    password));

    AzureResourceProviderTestUtil.configureAuthentication(testParameters);

    Map<OracleResourceProvider.Parameter, CharSequence> parameterValues =
            createParameterValues(PROVIDER, testParameters);
    assertNotNull(PROVIDER.getSSLContext(parameterValues));

  }
}
