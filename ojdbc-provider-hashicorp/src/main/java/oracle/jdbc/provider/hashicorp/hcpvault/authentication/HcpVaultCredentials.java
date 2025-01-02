package oracle.jdbc.provider.hashicorp.hcpvault.authentication;

/**
 * Holds the HCP API token obtained from the client_credentials flow.
 */
public final class HcpVaultCredentials {
  private final String hcpApiToken;

  public HcpVaultCredentials(String hcpApiToken) {
    this.hcpApiToken = hcpApiToken;
  }

  public String getHcpApiToken() {
    return hcpApiToken;
  }
}
