package oracle.jdbc.provider.azure.configuration;

import oracle.jdbc.spi.OracleConfigurationSecretProvider;

import java.util.Map;

public class AzureVaultSecretProvider implements OracleConfigurationSecretProvider {

  @Override
  public char[] getSecret(Map<String, String> map) {
    return new char[0];
  }

  @Override
  public String getSecretType() {
    return null;
  }
}