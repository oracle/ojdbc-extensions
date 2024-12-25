package oracle.jdbc.provider.hashicorp.dedicated.authentication;

/**
 * A method of authentication using HashiCorp Vault.
 */
public enum HashicorpAuthenticationMethod {
  /**
   * Authentication using a Vault token (e.g., read from environment variable).
   */
  TOKEN
}
