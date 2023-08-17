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

package oracle.jdbc.provider.azure.resource;

import oracle.jdbc.provider.azure.authentication.AzureAuthenticationMethod;
import oracle.jdbc.provider.azure.authentication.TokenCredentialFactory;
import oracle.jdbc.provider.resource.AbstractResourceProvider;
import oracle.jdbc.provider.resource.ResourceParameter;

import java.util.stream.Stream;

import static oracle.jdbc.provider.azure.authentication.AzureAuthenticationMethod.PASSWORD;
import static oracle.jdbc.provider.azure.authentication.AzureAuthenticationMethod.*;
import static oracle.jdbc.provider.azure.authentication.TokenCredentialFactory.*;

/**
 * Super class of all {@code OracleResourceProvider} implementations
 * that request a resource from Azure. This super class defines parameters for
 * authentication with Azure.
 */
public abstract class AzureResourceProvider extends AbstractResourceProvider {

  private static final ResourceParameter[] PARAMETERS = {
    new ResourceParameter("authenticationMethod", AUTHENTICATION_METHOD,
      "auto-detect",
      AzureResourceProvider::parseAuthenticationMethod),
    new ResourceParameter("tenantId", TENANT_ID),
    new ResourceParameter("clientId", CLIENT_ID),
    new ResourceParameter("clientCertificatePath", CLIENT_CERTIFICATE_PATH),
    new ResourceParameter("clientCertificatePassword", CLIENT_CERTIFICATE_PASSWORD),
    new ResourceParameter("redirectUri", REDIRECT_URL),
    new ResourceParameter("clientSecret", CLIENT_SECRET),
    new ResourceParameter("username", USERNAME),
    new ResourceParameter("password", TokenCredentialFactory.PASSWORD)
  };

  /**
   * <p>
   * Constructs a provider identified by the name:
   * </p><pre>{@code
   *   ojdbc-provider-azure-{resourceType}
   * }</pre><p>
   * This constructor defines all parameters related to authentication.
   * Subclasses must call this constructor with any additional parameters for
   * the specific resource they provide.
   * </p>
   *
   * @param resourceType The resource type identifier that appears the name of
   * the provider. Not null.
   * @param parameters parameters that are specific to the subclass provider.
   * Not null.
   */
  protected AzureResourceProvider(
    String resourceType, ResourceParameter... parameters) {
    super("azure", resourceType,
      Stream.concat(
          Stream.of(PARAMETERS),
          Stream.of(parameters))
        .toArray(ResourceParameter[]::new));
  }

  /**
   * Parses the "authentication-method" URI parameter as an
   * {@link AzureAuthenticationMethod}
   * recognized by the {@link TokenCredentialFactory}.
   */
  private static AzureAuthenticationMethod parseAuthenticationMethod(
    String authenticationMethod) {

    switch (authenticationMethod) {
      case "service-principal": return SERVICE_PRINCIPLE;
      case "managed-identity": return MANAGED_IDENTITY;
      case "password": return PASSWORD;
      case "device-code": return DEVICE_CODE;
      case "interactive": return INTERACTIVE;
      case "auto-detect": return AUTO_DETECT;
      default:
        throw new IllegalArgumentException(
          "Unrecognized value: " + authenticationMethod);
    }
  }

}

