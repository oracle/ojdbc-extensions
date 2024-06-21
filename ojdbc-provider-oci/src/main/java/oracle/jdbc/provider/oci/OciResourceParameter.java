package oracle.jdbc.provider.oci;

import oracle.jdbc.provider.parameter.Parameter;

public class OciResourceParameter {
  private OciResourceParameter() {}

  /** OCID of a OCI resource */
  public static final Parameter<String> OCID = Parameter.create();
}
