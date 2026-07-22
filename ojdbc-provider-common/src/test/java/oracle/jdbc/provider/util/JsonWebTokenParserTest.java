/*
 ** Copyright (c) 2026 Oracle and/or its affiliates.
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

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that {@link JsonWebTokenParser} correctly decodes the payload of
 * a JWT, including payloads that require base64url decoding (that is, a
 * payload containing a '-' or '_' character).
 */
public class JsonWebTokenParserTest {

  /**
   * A JWT whose payload segment is known to base64url-encode with both a
   * '-' and a '_' character. The payload decodes to:
   * <pre>{"exp":9999999999,"sub":"a","data":"?>?>?>?>"}</pre>
   * The header and signature segments are arbitrary, as
   * {@link JsonWebTokenParser} does not parse or verify them.
   */
  private static final String JWT_WITH_URL_SAFE_CHARACTERS =
    "eyJhbGciOiJIUzI1NiJ9" +
    "." +
    "eyJleHAiOjk5OTk5OTk5OTksInN1YiI6ImEiLCJkYXRhIjoiPz4_Pj8-Pz4ifQ" +
    "." +
    "signature-is-not-verified";

  /**
   * Verifies that {@link JsonWebTokenParser#parseClaims(CharSequence)} can
   * decode a payload whose base64url encoding contains a '-' and a '_'
   * character. Prior to a fix, this method used
   * {@link java.util.Base64#getDecoder()} (the standard alphabet) instead of
   * {@link java.util.Base64#getUrlDecoder()}, and would throw an
   * {@code IllegalArgumentException} ("Illegal base64 character") for any
   * JWT payload containing these characters, which real-world JWTs
   * (including OCI session tokens) routinely do.
   */
  @Test
  public void testParseClaimsWithUrlSafeCharacters() {
    Map<String, String> claims =
      JsonWebTokenParser.parseClaims(JWT_WITH_URL_SAFE_CHARACTERS);

    assertEquals("a", claims.get("sub"));
    assertEquals("?>?>?>?>", claims.get("data"));
    assertEquals("9999999999", claims.get("exp"));
  }

  /**
   * Verifies that {@link JsonWebTokenParser#parseExp(CharSequence)} can
   * parse the "exp" claim of a JWT whose payload requires base64url
   * decoding.
   */
  @Test
  public void testParseExpWithUrlSafeCharacters() {
    OffsetDateTime exp = JsonWebTokenParser.parseExp(JWT_WITH_URL_SAFE_CHARACTERS);
    assertEquals(9999999999L, exp.toEpochSecond());
  }

  /**
   * Sanity check: a JWT whose payload happens not to require any base64url
   * specific characters should continue to decode correctly, as it did
   * before the fix.
   */
  @Test
  public void testParseClaimsWithoutUrlSafeCharacters() {
    String jwt =
      "eyJhbGciOiJIUzI1NiJ9" +
      "." +
      "eyJzdWIiOiJ0ZXN0LXVzZXIiLCJleHAiOjEyMzR9" +
      "." +
      "signature-is-not-verified";

    Map<String, String> claims = JsonWebTokenParser.parseClaims(jwt);
    assertEquals("test-user", claims.get("sub"));
    assertEquals("1234", claims.get("exp"));
  }

  /**
   * Verifies that a malformed JWT (missing the "header.payload.signature"
   * structure) is rejected.
   */
  @Test
  public void testParseClaimsRejectsInvalidSyntax() {
    assertThrows(
      IllegalArgumentException.class,
      () -> JsonWebTokenParser.parseClaims("not-a-jwt"));
  }
}