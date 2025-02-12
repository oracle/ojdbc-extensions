/*
 ** Copyright (c) 2023 Oracle and/or its affiliates.
 **
 ** The Universal Permissive License (UPL), Version 1.0
 **
 ** Subject to the condition set forth below, permission is hereby granted to any
 ** person obtaining a copy of this software, associated documentation and/or data
 ** (collectively the "Software"), free of charge and under any and all copyright
 ** rights in the Software, and any and all patent rights owned or freely
 ** licensable by each licensor hereunder covering either (i) the unmodified
 ** Software as contributed to or provided by such licensor, or (ii) the Larger
 ** Works (as defined below), to deal in both
 **
 ** (a) the Software, and
 ** (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 ** one is included with the Software (each a "Larger Work" to which the Software
 ** is contributed by such licensors),
 **
 ** without restriction, including without limitation the rights to copy, create
 ** derivative works of, display, perform, and distribute the Software and make,
 ** use, sell, offer for sale, import, export, have made, and have sold the
 ** Software and the Larger Work(s), and to sublicense the foregoing rights on
 ** either these or other terms.
 **
 ** This license is subject to the following condition:
 ** The above copyright notice and either this complete permission notice or at
 ** a minimum a reference to the UPL must be included in all copies or
 ** substantial portions of the Software.
 **
 ** THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 ** IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 ** FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 ** AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 ** LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 ** OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 ** SOFTWARE.
 */

package oracle.jdbc.provider.oci.configuration;

import oracle.jdbc.provider.oci.authentication.AuthenticationDetailsFactory;
import oracle.jdbc.provider.oci.authentication.AuthenticationMethod;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSetParser;

import static oracle.jdbc.provider.oci.authentication.AuthenticationDetailsFactory.*;
import static oracle.jdbc.provider.oci.authentication.AuthenticationMethod.*;
import static oracle.jdbc.provider.oci.objectstorage.ObjectFactory.OBJECT_URL;
import static oracle.jdbc.provider.oci.vault.SecretFactory.OCID;
import static oracle.jdbc.provider.oci.databasetools.DatabaseToolsConnectionFactory.CONNECTION_OCID;

/**
 * <p>
 * The set of named parameters recognized by
 * {@link oracle.jdbc.spi.OracleConfigurationProvider}
 * {@link oracle.jdbc.spi.OracleConfigurationJsonSecretProvider}
 * implementations found in this module.
 * </p><p>
 * Each named parameter is mapped to a {@link Parameter} recognized by the
 * {@link AuthenticationDetailsFactory}.
 * </p>
 */
public final class OciConfigurationParameters {

  private OciConfigurationParameters(){}

  /**
   * Parameter representing the "key=..." name-value pair which may appear in
   * the query section of a URL.
   */
  public static final Parameter<String> KEY = Parameter.create();

  /**
   * <p>
   *   A parser that recognizes parameters of URIs received by
   *   {@link OciObjectStorageProvider}, and JSON objects received by
   *   {@link OciVaultSecretProvider}.
   * </p>
   */
  private static final ParameterSetParser PARAMETER_SET_PARSER =
    ParameterSetParser.builder()
      .addParameter("key", KEY, "")
      .addParameter("value", OCID, "")
      .addParameter("type", Parameter.create())
      .addParameter("object_url", OBJECT_URL, "")
      .addParameter("AUTHENTICATION", AUTHENTICATION_METHOD,
        API_KEY,
        OciConfigurationParameters::parseAuthentication)
      .addParameter("OCI_PROFILE", CONFIG_PROFILE, "DEFAULT")
      .addParameter("OCI_TENANCY", TENANT_ID)
      .addParameter("OCI_USER", USER_ID)
      .addParameter("OCI_FINGERPRINT", FINGERPRINT)
      .addParameter("OCI_KEY_FILE", PRIVATE_KEY)
      .addParameter("OCI_PASS_PHRASE", PASS_PHRASE)
      .build();

  /**
   * @return A parser that recognizes parameters of URIs received by
   * {@link OciObjectStorageProvider}, and JSON objects received by
   * {@link OciVaultSecretProvider}.
   */
  public static ParameterSetParser getParser() {
    return PARAMETER_SET_PARSER;
  }

  /**
   * Parses the AUTHENTICATION URI parameter, mapping it to an
   * {@link AuthenticationMethod} recognized by
   * {@link AuthenticationDetailsFactory}.
   */
  private static AuthenticationMethod parseAuthentication(
      String authentication) {
    switch (authentication) {
      case "OCI_DEFAULT": return API_KEY;
      case "OCI_INSTANCE_PRINCIPAL": return INSTANCE_PRINCIPAL;
      case "OCI_RESOURCE_PRINCIPAL": return RESOURCE_PRINCIPAL;
      case "OCI_INTERACTIVE": return INTERACTIVE;
      default: throw new IllegalArgumentException(
          "Unrecognized value: " + authentication);
    }
  }

}
