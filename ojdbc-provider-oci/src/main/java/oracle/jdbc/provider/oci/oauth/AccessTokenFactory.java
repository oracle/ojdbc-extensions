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

package oracle.jdbc.provider.oci.oauth;

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.identitydataplane.DataplaneClient;
import com.oracle.bmc.identitydataplane.model.GenerateScopedAccessTokenDetails;
import com.oracle.bmc.identitydataplane.model.SecurityToken;
import com.oracle.bmc.identitydataplane.requests.GenerateScopedAccessTokenRequest;
import oracle.jdbc.AccessToken;
import oracle.jdbc.provider.cache.CachedResourceFactory;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.oci.OciResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;

import javax.security.auth.DestroyFailedException;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.function.Supplier;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;

/**
 * Factory for requesting access tokens from the Dataplane service. Tokens are
 * represented as {@link AccessToken} objects. Oracle JDBC can use these objects
 * to authenticate with a database of the ADB service.
 */
public final class AccessTokenFactory extends OciResourceFactory<Supplier<?extends AccessToken>> {

  /**
   * Scope that configures a compartment and/or database for which access is
   * requested. Values have a URN of this form:
   * <pre>
   * urn:oracle:db::id::{compartment-ocid}[::database-ocid]
   * </pre>
   * A URN may identify all database of all compartments with a "*":
   * <pre>
   * urn:oracle:db::id::*
   * </pre>
   */
  public static final Parameter<String> SCOPE = Parameter.create(REQUIRED);

  private static final ResourceFactory<Supplier<?extends AccessToken>> INSTANCE = CachedResourceFactory.create(new AccessTokenFactory());

  private AccessTokenFactory() { }

  /**
   * Returns a singleton of {@code AccessTokenFactory}.
   * @return a singleton of {@code AccessTokenFactory}
   */
  public static ResourceFactory<Supplier<? extends AccessToken>> getInstance() {
    return INSTANCE;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Create Json web token Cache ,The cache will use DataPlane to request access token service .
   * The {@code parameterSet} is required to include a {@link #SCOPE} parameter.
   * </p>
   */
  @Override
  public Resource<Supplier<? extends AccessToken>> request(
      AbstractAuthenticationDetailsProvider authenticationDetails,
      ParameterSet parameterSet) {
    Supplier<? extends AccessToken> accessTokenSupplier = AccessToken.createJsonWebTokenCache(()->createAccessToken(parameterSet,authenticationDetails));
    // we save the supplier as a resource
    return Resource.createPermanentResource(accessTokenSupplier,  true);
  }

  /**
   * Creates a JDBC access token using the provided authentication details and parameter set,
   * This method serves as the supplier function used by the driver's cache to update the token.
   * @param parameterSet the parameter set
   * @param authenticationDetails authentication information used to authenticate in OCI
   * @return the created JDBC access token
   */
  public  AccessToken createAccessToken(ParameterSet parameterSet, AbstractAuthenticationDetailsProvider authenticationDetails) {

    KeyPair keyPair = generateKeyPair();
    String scope = parameterSet.getRequired(SCOPE);
    String token = requestOciAccessToken(authenticationDetails, scope, keyPair);
    PrivateKey privateKey = keyPair.getPrivate();

    final AccessToken accessToken;
    char[] tokenChars = token.toCharArray();
    try {
      accessToken = AccessToken.createJsonWebToken(tokenChars, privateKey);
    }
    finally {
      Arrays.fill(tokenChars, (char)0);
      tryDestroy(privateKey);
    }
    return accessToken;
  }
  /**
   * Requests an Oci access token using the provided authentication details and scope and keyPair.
   * @param authenticationDetails the authentication details to authenticate in oci
   * @param scope the scope
   * @param keyPair the key pair
   * @return the requested Oci access token
   */
  public  String requestOciAccessToken(AbstractAuthenticationDetailsProvider authenticationDetails, String scope, KeyPair keyPair) {
    SecurityToken securityToken = requestSecurityToken(
            authenticationDetails, scope, keyPair.getPublic());
    return securityToken.getToken();
  }

  /**
   * @return A public-private key pair for proof of possession when token
   * requested by this provider is presented for validation.
   */
  private static KeyPair generateKeyPair() {
    try {
      return KeyPairGenerator.getInstance("RSA").generateKeyPair();
    }
    catch (NoSuchAlgorithmException noSuchAlgorithmException) {
      throw new IllegalStateException(
          "Failed to generated a proof of possession key pair. " +
              "See cause for details.",
          noSuchAlgorithmException);
    }
  }

  private static SecurityToken requestSecurityToken(
      AbstractAuthenticationDetailsProvider authenticationDetails,
      String scope, PublicKey publicKey) {
    try (DataplaneClient client =
             DataplaneClient.builder().build(authenticationDetails)) {

      String base64PublicKey =
          Base64.getEncoder()
              .encodeToString(publicKey.getEncoded());

      GenerateScopedAccessTokenDetails details =
          GenerateScopedAccessTokenDetails.builder()
            .publicKey(base64PublicKey)
            .scope(scope)
            .build();

      GenerateScopedAccessTokenRequest request =
          GenerateScopedAccessTokenRequest.builder()
              .generateScopedAccessTokenDetails(details)
              .build();

      return client.generateScopedAccessToken(request)
          .getSecurityToken();
    }
  }

  /**
   * Tries to clear a private key from memory, if possible.
   */
  private static void tryDestroy(PrivateKey privateKey) {
    try {
      privateKey.destroy();
    }
    catch (DestroyFailedException destroyFailedException) {
      // Not recovering or throwing this up the stack because the default
      // implementation will throw DestroyFailedException. So catching this
      // exception may indicate an unsupported operation more so than it does an
      // actual problem.
    }
  }
}
