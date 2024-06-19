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

package oracle.jdbc.provider.oci.resource;

import oracle.jdbc.AccessToken;
import oracle.jdbc.provider.oci.oauth.AccessTokenFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.resource.ResourceParameter;
import oracle.jdbc.spi.AccessTokenProvider;

import java.util.Map;

import static oracle.jdbc.provider.oci.oauth.AccessTokenFactory.SCOPE;

/**
 * <p>
 * A provider of access tokens from the OCI Dataplane service. These access
 * tokens authorize logins to an Autonomous Database (ADB).
 * </p><p>
 * This supplier may be configured with a scope of access to request. The scope
 * is expressed as a URN that may contain one or more
 * <a href="https://docs.oracle.com/en-us/iaas/Content/General/Concepts/identifiers.htm">
 * OCIDs
 * </a> to identify the database and/or compartment that will be accessed. This
 * URN may have any of the following forms:
 * </p>
 * <dl>
 *   <dt>{@code urn:oracle:db::id::*}</dt>
 *   <dd>
 *     The scope of all databases in the tenancy of the user that has
 *     requested an access token.
 *   </dd>
 *   <dt>{@code urn:oracle:db::id::{compartment-ocid}}</dt>
 *   <dd>
 *     The scope of all databases in the compartment identified by an OCID.
 *   </dd>
 *   <dt>{@code urn:oracle:db::id::{compartment-ocid}::{database-OCID}}</dt>
 *   <dd>
 *     The scope of a single database identified by an OCID, within a
 *     compartment that is also identified by an OCID.
 *   </dd>
 * </dl>
 * <p>
 * This class implements the {@link AccessTokenProvider} SPI defined by
 * Oracle JDBC. It is designed to be located and instantiated by
 * {@link java.util.ServiceLoader}.
 * </p>
 */
public final class DataplaneTokenProvider
  extends OciResourceProvider
  implements AccessTokenProvider {

  private static final ResourceParameter[] PARAMETERS = {
    new ResourceParameter("scope", SCOPE, "urn:oracle:db::id::*")
  };

  /**
   * A public no-arg constructor used by {@link java.util.ServiceLoader} to
   * construct an instance of this provider.
   */
  public DataplaneTokenProvider() {
    super("token", PARAMETERS);
  }

  @Override
  public AccessToken getAccessToken(
    Map<Parameter, CharSequence> parameterValues) {

    ParameterSet parameterSet = parseParameterValues(parameterValues);

    // we get the supplier than we get the token from supplier with get method
    return AccessTokenFactory.getInstance()
        .request(parameterSet)
        .getContent().get();
  }
}
