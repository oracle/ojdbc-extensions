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

package oracle.jdbc.provider.oci.authentication;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.RegionProvider;
import com.oracle.bmc.auth.SessionTokenAuthenticationDetailsProvider;
import oracle.jdbc.provider.util.JsonWebTokenParser;
import oracle.jdbc.provider.util.PemData;

import java.io.InputStream;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Map;

/**
 * Provides authentication details of a session token that is stored in memory.
 * The implementation is derived from the OCI SDK's
 * {@link com.oracle.bmc.auth.SessionTokenAuthenticationDetailsProvider}. This
 * implementation will not require security sensitive information to be written
 * to the file system.
 */
public class InteractiveAuthenticationDetails
  implements AuthenticationDetailsProvider, RegionProvider {

  /**
   * Region of the endpoint that resources are requested from a client uses this
   * authentication details provider. If an SDK client is not configured with a
   * region, it will use the region of an {@code AuthenticationDetailsProvider}
   * that implements the {@code RegionProvider} interface.
   */
  private final Region region;

  /**
   * The key ID used to sign requests. For interactive authentication, the ID
   * is composed as:
   * <pre>
   * ST${session-token}
   * </pre>
   * This composition can be found in the implementation of
   * {@link SessionTokenAuthenticationDetailsProvider#getKeyId()}
   */
  private final String keyId;

  /**
   * Fingerprint of the proof of possession key for the session token.
   */
  private final String fingerprint;

  /**
   * OCID of the tenant which was authenticated with.
   */
  private final String tenantId;

  /**
   * OCID of the user that authenticated.
   */
  private final String userId;

  /**
   * PEM encoding of the private proof of possession key.
   */
  private final PemData privateKey;

  /**
   * Constructs of provider of interactive authentication details. This
   * constructor extracts details from the claims of a {@code sessionToken},
   * which is assumed to a JSON Web Token (JWT) encoding.
   *
   * @param region Region of the endpoint that resources are requested from
   *   using the session token (The SDK resolves an end point using this value).
   *   Not null.
   * @param sessionToken Session token received from authentication with the
   * login endpoint. Not null.
   * @param proofOfPossession Proof of possession keys for the
   * {@code sessionToken}. Not null.
   */
  InteractiveAuthenticationDetails(
    Region region, String sessionToken, KeyPair proofOfPossession) {

    this.region = region;

    this.privateKey = PemData.encodePrivateKey(proofOfPossession.getPrivate());
    this.fingerprint = encodeFingerprint(proofOfPossession.getPublic());

    Map<String, String> tokenClaims =
      JsonWebTokenParser.parseClaims(sessionToken);
    this.tenantId = tokenClaims.get("tenant");
    this.userId = tokenClaims.get("sub");
    this.keyId = "ST$" + sessionToken;
  }

  @Override
  public Region getRegion() {
    return region;
  }

  @Override
  public String getKeyId() {
    return keyId;
  }

  @Override
  public String getFingerprint() {
    return fingerprint;
  }

  @Override
  public String getTenantId() {
    return tenantId;
  }

  @Override
  public String getUserId() {
    return userId;
  }

  @Override
  public InputStream getPrivateKey() {
    return privateKey.createInputStream();
  }

  @SuppressWarnings("deprecation")
  @Override
  public String getPassPhrase() {
    return null;
  }

  @Override
  public char[] getPassphraseCharacters() {
    return null;
  }

  /**
   * Encodes a public key as a
   * <a href="https://docs.oracle.com/en-us/iaas/Content/API/Concepts/apisigningkey.htm#four">
   * fingerprint recognized by OCI
   * </a>.
   * @param publicKey Public key to encode. Not null.
   * @return Fingerprint of the public key. Not null.
   */
  private static String encodeFingerprint(PublicKey publicKey) {

    final MessageDigest md5;
    try {
      md5 = MessageDigest.getInstance("MD5");
    }
    catch (NoSuchAlgorithmException noMd5) {
      throw new IllegalStateException("Failed to create an MD5 digest", noMd5);
    }

    md5.update(publicKey.getEncoded());
    byte[] hash = md5.digest();

    StringBuilder builder =
      new StringBuilder((hash.length * 2) + (hash.length - 1));

    builder.append(Integer.toHexString(Byte.toUnsignedInt(hash[0])));
    for (int i = 1; i < hash.length; i++) {
      builder.append(':');
      builder.append(Integer.toHexString(Byte.toUnsignedInt(hash[i])));
    }

    return builder.toString();
  }
}
