package oracle.jdbc.provider.hashicorp.dedicated.authentication;

/**
 * A method of authentication using Dedicated HashiCorp Vault.
 */
public enum DedicatedVaultAuthenticationMethod {

  /**
   * Authentication using a Vault token, possibly read from a parameter,
   * system property, or environment variable.
   */
  TOKEN
}
