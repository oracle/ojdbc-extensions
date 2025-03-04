/*
 ** Copyright (c) 2025 Oracle and/or its affiliates.
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

package oracle.jdbc.provider.hashicorp.hcpvaultdedicated.authentication;

import oracle.jdbc.driver.oauth.OpaqueAccessToken;

import java.time.OffsetDateTime;

/**
 * Represents a cached Vault authentication token with its expiration time.
 * <p>
 * The cached token contains the {@link OpaqueAccessToken} and its expiration time.
 * It is used to avoid redundant authentication requests by checking token validity
 * before re-authenticating.
 * </p>
 */
public class CachedToken {
  private static final long TOKEN_TTL_BUFFER = 60_000; // 1-minute buffer in milliseconds

  private final OpaqueAccessToken token;

  /**
   * Constructs a new {@code CachedToken} instance.
   *
   * @param token The {@link OpaqueAccessToken} to cache. Must not be null.
   * @throws IllegalArgumentException if {@code token} is null.
   */
  public CachedToken(OpaqueAccessToken token) {
    if (token == null) {
      throw new IllegalArgumentException("Token must not be null.");
    }
    this.token = token;
  }

  /**
   * Retrieves the cached access token.
   *
   * @return the cached {@link OpaqueAccessToken}.
   */
  public OpaqueAccessToken getToken() {
    return token;
  }

  /**
   * Checks if the cached token is still valid.
   *
   * @return {@code true} if the token is still valid; {@code false} otherwise.
   */
  public boolean isValid() {
    OffsetDateTime expiration = this.token.expiration();
    if (expiration == null) {
      return false;
    }
    long currentTimeMillis = System.currentTimeMillis();
    long expirationTimeMillis = token.expiration().toInstant().toEpochMilli();

    return currentTimeMillis < (expirationTimeMillis - TOKEN_TTL_BUFFER);
  }
}
