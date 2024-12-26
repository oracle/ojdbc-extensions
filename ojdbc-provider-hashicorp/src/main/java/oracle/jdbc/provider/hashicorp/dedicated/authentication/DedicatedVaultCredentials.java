package oracle.jdbc.provider.hashicorp.dedicated.authentication;

/**
 * Holds a Vault token for Dedicated Vault usage.
 */
public final class DedicatedVaultCredentials {
  private final String vaultToken;

  public DedicatedVaultCredentials(String vaultToken) {
    this.vaultToken = vaultToken;
  }

  public String getVaultToken() {
    return vaultToken;
  }
}
