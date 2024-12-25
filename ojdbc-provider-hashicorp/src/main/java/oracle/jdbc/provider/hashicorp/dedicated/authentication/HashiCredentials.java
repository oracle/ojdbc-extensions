package oracle.jdbc.provider.hashicorp.authentication;

/**
 * Simple credentials object for HashiCorp Vault that holds a token.
 */
public final class HashiCredentials {
  private final String vaultToken;

  public HashiCredentials(String vaultToken) {
    this.vaultToken = vaultToken;
  }

  public String getVaultToken() {
    return vaultToken;
  }
}
