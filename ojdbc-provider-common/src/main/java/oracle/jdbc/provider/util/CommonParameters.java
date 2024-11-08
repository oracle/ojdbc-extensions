/*
 ** Copyright (c) 2024 Oracle and/or its affiliates.
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

package oracle.jdbc.provider.util;

import oracle.jdbc.provider.parameter.Parameter;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;
import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.SENSITIVE;

/**
 * A utility class that defines common parameters used across various
 * resource providers for Oracle JDBC.
 * This class contains predefined parameters such as the type of wallet file,
 * password, and connection string index, which are used to configure resource
 * providers that interact with secure storage and retrieve credentials or keys.
 *
 */
public final class CommonParameters {

  private CommonParameters() {}

  /**
   * A parameter for specifying the password used to decrypt the file if it is
   * password-protected. The password is necessary for PKCS12 and encrypted
   * PEM files.
   * If the file is not encrypted (e.g., SSO format or non password-protected
   * PEM), this parameter can be {@code null}. It is
   * only required when dealing with password-protected files.
   */
  public static final Parameter<String> PASSWORD =
          Parameter.create(SENSITIVE);

  /**
   * A parameter for specifying the type of the file being used.
   * This parameter defines the format of the file and dictates how it
   * should be processed.
   * The acceptable values are:
   * <ul>
   *     <li>{@code SSO} - For Single Sign-On format.</li>
   *     <li>{@code PKCS12} - For PKCS12 keystore format, which may be
   *     password-protected.</li>
   *     <li>{@code PEM} - For PEM-encoded format, which may include
   *     encrypted or unencrypted private keys and certificates.</li>
   * </ul>
   *
   * The type parameter is required to correctly parse and handle the file
   * data.
   */
  public static final Parameter<String> TYPE =
    Parameter.create(REQUIRED);

  /**
   * A parameter for specifying the connection string index in cases where
   * multiple connection strings are stored in a wallet.
   * This parameter allows selecting a specific set of credentials based on the
   * indexed connection string in the Secure External Password Store (SEPS)
   * wallet. If multiple connection strings are present, this parameter helps to
   * disambiguate which connection string and its associated credentials should
   * be used.
   * For example, if the SEPS wallet contains multiple connection strings, such as:
   * <ul>
   *     <li>{@code oracle.security.client.connect_string1}</li>
   *     <li>{@code oracle.security.client.username1}</li>
   *     <li>{@code oracle.security.client.password1}</li>
   *     <li>{@code oracle.security.client.connect_string2}</li>
   *     <li>{@code oracle.security.client.username2}</li>
   *     <li>{@code oracle.security.client.password2}</li>
   * </ul>
   * The {@code connectionStringIndex} can be set to {@code 1} or {@code 2} to
   * select the appropriate credential set.
   * If this parameter is {@code null}, the provider will attempt to use default
   * credentials or the first connection string if only one is present in the wallet.
   * If multiple connection strings exist and this parameter is not set, an error
   * will be thrown to avoid ambiguity.
   */
  public static final Parameter<String> CONNECTION_STRING_INDEX =
    Parameter.create();

  /**
   * A parameter for specifying the TNS alias used to retrieve the connection
   * string from the tnsnames.ora file.
   *
   * <p>This parameter allows selecting a specific connection string based on
   * the alias defined in the tnsnames.ora file. The alias
   * corresponds to the network service name that maps to a database service
   * and its connection parameters.</p>
   *
   * <p><b>Usage Example:</b></p>
   * <p>Suppose your tnsnames.ora file contains the following entries:</p>
   * <pre>
   * MYDB1 =
   *   (DESCRIPTION =
   *     (ADDRESS = (PROTOCOL = TCP)(HOST = dbhost1.example.com)(PORT = 1521))
   *     (CONNECT_DATA =
   *       (SERVICE_NAME = mydb1_service)
   *     )
   *   )
   *
   * MYDB2 =
   *   (DESCRIPTION =
   *     (ADDRESS = (PROTOCOL = TCP)(HOST = dbhost2.example.com)(PORT = 1521))
   *     (CONNECT_DATA =
   *       (SERVICE_NAME = mydb2_service)
   *     )
   *   )
   * </pre>
   * <p>You can set the tnsAlias parameter to <code>"MYDB1"</code> or
   * <code>"MYDB2"</code> to select the corresponding connection string.</p>
   *
   * <p><b>Note:</b> The alias should exactly match one of the aliases defined
   * in your tnsnames.ora file.</p>
   */
  public static final Parameter<String> TNS_ALIAS = Parameter.create();


}
