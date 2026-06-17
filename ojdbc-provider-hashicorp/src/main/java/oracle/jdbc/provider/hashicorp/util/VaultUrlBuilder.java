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

package oracle.jdbc.provider.hashicorp.util;

import java.net.URI;
import java.util.Locale;

/**
 * Shared helpers for safe Vault URL construction and path validation.
 */
public final class VaultUrlBuilder {

  private VaultUrlBuilder() { }

  /**
   * Parses and validates a Vault base URI string.
   *
   * @param vaultAddr Vault base address. Not null/empty.
   * @return Parsed URI.
   */
  public static URI parseBaseUri(String vaultAddr) {
    if (vaultAddr == null || vaultAddr.isEmpty()) {
      throw new IllegalArgumentException("vault Addr must not be null or empty");
    }
    try {
      return URI.create(vaultAddr);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid vault address: " + vaultAddr, e);
    }
  }

  /**
   * Builds a canonical URL string from base URI components and a path.
   *
   * @param baseUri Base URI. Not null.
   * @param endpointPath Endpoint path. Not null.
   * @param failureMessage Error message when URI assembly fails.
   * @return Canonical URL string.
   */
  public static String buildCanonicalUrl(
          URI baseUri, String endpointPath, String failureMessage) {
    try {
      return new URI(
              baseUri.getScheme(),
              baseUri.getUserInfo(),
              baseUri.getHost(),
              baseUri.getPort(),
              endpointPath,
              null,
              null)
              .toString();
    } catch (Exception e) {
      throw new IllegalArgumentException(failureMessage, e);
    }
  }

  /**
   * Joins base path and suffix path without introducing duplicate separators.
   *
   * @param basePath Base path from URI.
   * @param suffixPath Endpoint path suffix.
   * @return Joined path.
   */
  public static String joinPaths(String basePath, String suffixPath) {
    String normalizedSuffix = suffixPath.startsWith("/") ? suffixPath : "/" + suffixPath;
    if (basePath == null || basePath.isEmpty() || "/".equals(basePath)) {
      return normalizedSuffix;
    }

    String normalizedBase = basePath.startsWith("/") ? basePath : "/" + basePath;
    if (normalizedBase.endsWith("/")) {
      normalizedBase = normalizedBase.substring(0, normalizedBase.length() - 1);
    }
    return normalizedBase + normalizedSuffix;
  }

  /**
   * Rejects path-shaping tokens that can alter routing or parsing.
   *
   * @param value Input text.
   * @param name Field name for error messages.
   */
  public static void rejectDangerousPathTokens(String value, String name) {
    String lower = value.toLowerCase(Locale.ROOT);
    if (value.indexOf('?') >= 0
            || value.indexOf('#') >= 0
            || value.indexOf('\\') >= 0
            || lower.contains("%2f")
            || lower.contains("%5c")) {
      throw new IllegalArgumentException("Invalid " + name + ": " + value);
    }
  }

  /**
   * Rejects path traversal dot-segments.
   *
   * @param path Path value that may contain '/'.
   * @param name Field name for error messages.
   */
  public static void rejectDotSegments(String path, String name) {
    String[] segments = path.split("/", -1);
    for (String segment : segments) {
      if (".".equals(segment) || "..".equals(segment)) {
        throw new IllegalArgumentException("Invalid " + name + " segment: " + segment);
      }
    }
  }
}
