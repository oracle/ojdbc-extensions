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

package oracle.jdbc.provider.oci;

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import oracle.jdbc.provider.oci.authentication.AuthenticationDetailsFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;

/**
 * <p>
 * Common super class for {@link ResourceFactory} implementations that request
 * resources from OCI. Subclasses implement
 * {@link #request(AbstractAuthenticationDetailsProvider, ParameterSet)} to
 * request a particular type of resource. Subclasses must declare the
 * {@link Parameter} objects they recognize in a {@link ParameterSet}.
 * </p><p>
 * This super class is responsible for obtaining authentication details.
 * Authentication is delegated to {@link AuthenticationDetailsFactory},
 * and may be configured by the {@link Parameter} objects declared in that
 * class. A {@link AbstractAuthenticationDetailsProvider} created by
 * {@link AuthenticationDetailsFactory} is passed to the
 * {@link #request(AbstractAuthenticationDetailsProvider, ParameterSet)} method
 * of a subclass.
 * </p>
 */
public abstract class OciResourceFactory<T> implements ResourceFactory<T> {

  /** Factory to create authentication details recognized by the OCI SDK */
  private final AuthenticationDetailsFactory authenticationDetailsFactory
    = AuthenticationDetailsFactory.getInstance();

  @Override
  public final Resource<T> request(ParameterSet parameterSet) {
    AbstractAuthenticationDetailsProvider authenticationDetails =
        authenticationDetailsFactory.request(parameterSet).getContent();

    try {
      return request(authenticationDetails, parameterSet);
    }
    catch (Exception exception) {
      throw new IllegalStateException(
        "Request failed with parameters: " + parameterSet,
        exception);
    }
  }

  /**
   * <p>
   * Abstract method that subclasses implement to request a particular type of
   * resource from OCI. Subclasses must return a {@link Resource} that
   * implements {@link Resource#isValid()} to indicate when a cached resource is
   * no longer valid.
   * </p><p>
   * Subclasses should declare the {@link Parameter} objects they recognize in
   * the {@code parameterSet}.
   * </p>
   *
   * @param authenticationDetails Authentication details configured by the
   *   {@code parameterSet}. Not null.
   * @param parameterSet Set of parameters that configure the request. Not null.
   * @return The resource requested based on configured of the
   *   {@code parameterSet}, and authenticated by the given
   *   {@code authenticationDetails}. Not null.
   * @throws IllegalStateException If the request fails to return a resource.
   * @throws IllegalArgumentException If the {@code parameterSet} does not
   * include a required parameter, or does not represent a valid configuration.
   */
  protected abstract Resource<T> request(
      AbstractAuthenticationDetailsProvider authenticationDetails,
      ParameterSet parameterSet);

}
