package oracle.jdbc.provider.azure.resource;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.azure.AzureTestProperty;
import oracle.jdbc.spi.OracleResourceProvider;
import oracle.jdbc.spi.PasswordProvider;
import oracle.jdbc.spi.UsernameProvider;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.createParameterValues;
import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.findProvider;
import static org.junit.jupiter.api.Assertions.*;

public class KeyVaultSEPSProviderTest {
  private static final UsernameProvider USERNAME_PROVIDER =
          findProvider(
            UsernameProvider.class, "ojdbc-provider-azure-key-vault-seps");

  private static final PasswordProvider PASSWORD_PROVIDER =
          findProvider(
            PasswordProvider.class, "ojdbc-provider-azure-key-vault-seps");

  /**
   * Verifies that {@link KeyVaultTCPSProvider#getParameters()}  includes parameters
   * to configure a vault URL and secret name.
   */
  @Test
  public void testProviderParameters() {
    Collection<? extends OracleResourceProvider.Parameter> usernameParameters =
            USERNAME_PROVIDER.getParameters();
    Collection<? extends OracleResourceProvider.Parameter> passwordParameters =
            PASSWORD_PROVIDER.getParameters();

    // Verify both providers have the same parameters
    assertEquals(usernameParameters, passwordParameters,
            "Username and Password providers should have identical parameters");

    assertNotNull(usernameParameters);

    assertNotNull(passwordParameters);

    OracleResourceProvider.Parameter vaultUrlParameter =
            usernameParameters.stream()
                    .filter(parameter -> "vaultUrl".equals(parameter.name()))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
    assertFalse(vaultUrlParameter.isSensitive());
    assertTrue(vaultUrlParameter.isRequired());
    assertNull(vaultUrlParameter.defaultValue());

    OracleResourceProvider.Parameter secretNameParameter =
            usernameParameters.stream()
                    .filter(parameter -> "secretName".equals(parameter.name()))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
    assertFalse(secretNameParameter.isSensitive());
    assertTrue(secretNameParameter.isRequired());
    assertNull(secretNameParameter.defaultValue());

    OracleResourceProvider.Parameter walletPasswordParameter =
            usernameParameters.stream()
                    .filter(parameter -> "walletPassword".equals(parameter.name()))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
    assertFalse(walletPasswordParameter.isSensitive());
    assertFalse(walletPasswordParameter.isRequired());
    assertNull(walletPasswordParameter.defaultValue());

    OracleResourceProvider.Parameter connStringParameter =
            usernameParameters.stream()
                    .filter(parameter -> "connectionStringIndex".equals(parameter.name()))
                    .findFirst()
                    .orElseThrow(AssertionError::new);
    assertFalse(connStringParameter.isSensitive());
    assertFalse(connStringParameter.isRequired());
    assertNull(connStringParameter.defaultValue());

  }

  @Test
  public void testGetUsername() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put(
            "vaultUrl",
            TestProperties.getOrAbort(AzureTestProperty.AZURE_KEY_VAULT_URL));
    testParameters.put(
            "secretName",
            TestProperties.getOrAbort(AzureTestProperty.AZURE_SEPS_WALLET_SECRET_NAME));

    Optional.ofNullable(TestProperties.getOptional(
                    AzureTestProperty.AZURE_SEPS_WALLET_PASSWORD))
            .ifPresent(password -> testParameters.put("walletPassword",
                    password));

    Optional.ofNullable(TestProperties.getOptional(
                    AzureTestProperty.AZURE_SEPS_CONNECTION_STRING_INDEX))
            .ifPresent(password -> testParameters.put("connectionStringIndex",
                    password));

    AzureResourceProviderTestUtil.configureAuthentication(testParameters);

    Map<OracleResourceProvider.Parameter, CharSequence> parameterValues =
            createParameterValues(USERNAME_PROVIDER, testParameters);

    assertNotNull(USERNAME_PROVIDER.getUsername(parameterValues));

  }

  @Test
  public void testGetPassword() {
    Map<String, String> testParameters = new HashMap<>();
    testParameters.put(
            "vaultUrl",
            TestProperties.getOrAbort(AzureTestProperty.AZURE_KEY_VAULT_URL));
    testParameters.put(
            "secretName",
            TestProperties.getOrAbort(AzureTestProperty.AZURE_SEPS_WALLET_SECRET_NAME));

    Optional.ofNullable(TestProperties.getOptional(
                    AzureTestProperty.AZURE_SEPS_WALLET_PASSWORD))
            .ifPresent(password -> testParameters.put("walletPassword",
                    password));

    Optional.ofNullable(TestProperties.getOptional(
                    AzureTestProperty.AZURE_SEPS_CONNECTION_STRING_INDEX))
            .ifPresent(password -> testParameters.put("connectionStringIndex",
                    password));

    AzureResourceProviderTestUtil.configureAuthentication(testParameters);

    Map<OracleResourceProvider.Parameter, CharSequence> parameterValues =
            createParameterValues(USERNAME_PROVIDER, testParameters);

    assertNotNull(PASSWORD_PROVIDER.getPassword(parameterValues));

  }
}
