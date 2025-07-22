package oracle.jdbc.provider.aws.resource;

/**
 * Centralized parameter name constants used by AWS Parameter Store resource providers.
 */
public final class AwsParameterStoreResourceParameterNames {

  private AwsParameterStoreResourceParameterNames() {}

  /** The AWS region where the parameter is stored (e.g., eu-north-1). */
  public static final String AWS_REGION = "awsRegion";

  /** The name of the parameter stored in AWS Parameter Store. */
  public static final String PARAMETER_NAME = "parameterName";

  /** Optional field name to extract from a JSON parameter value. */
  public static final String FIELD_NAME = "fieldName";

  /** The alias used to retrieve a connection string from tnsnames.ora. */
  public static final String TNS_ALIAS = "tnsAlias";

  /** Optional password used to decrypt the wallet (for PKCS12 or encrypted PEM). */
  public static final String WALLET_PASSWORD = "walletPassword";

  /** The wallet format: SSO, PKCS12, or PEM. */
  public static final String TYPE = "type";

  /** Index of the credential set in the wallet */
  public static final String CONNECTION_STRING_INDEX = "connectionStringIndex";
}
