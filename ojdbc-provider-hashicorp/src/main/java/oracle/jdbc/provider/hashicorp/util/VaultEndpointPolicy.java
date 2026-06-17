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
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Security policy for Vault endpoint validation and credential safety.
 */
public final class VaultEndpointPolicy {

  private static final String HTTPS_SCHEME = "https";
  private static final String HTTP_SCHEME = "http";
  private static final String TRUE = "true";

  private static final String KEY_ALLOW_INSECURE_VAULT_ADDR =
          "ALLOW_INSECURE_VAULT_ADDR";

  private static final String KEY_TRUSTED_HOSTS =
          "TRUSTED_VAULT_HOSTS";

  private VaultEndpointPolicy() { }

  /**
   * Validates a Vault URL before any outbound request.
   *
   * @param urlStr URL to validate. Not null.
   * @throws IllegalArgumentException if URL is invalid or insecure.
   */
  public static void validateVaultUrl(String urlStr) {
    URI uri = parse(urlStr);
    String scheme = uri.getScheme();
    if (!HTTPS_SCHEME.equalsIgnoreCase(scheme)) {
      if (!(HTTP_SCHEME.equalsIgnoreCase(scheme) && isInsecureVaultAddrAllowed())) {
        throw new IllegalArgumentException(
                "Only HTTPS Vault endpoints are allowed: " + urlStr);
      }
    }
    if (scheme == null || scheme.isEmpty()) {
      throw new IllegalArgumentException(
              "Vault endpoint must include a URL scheme: " + urlStr);
    }
    if (uri.getHost() == null || uri.getHost().isEmpty()) {
      throw new IllegalArgumentException(
              "Vault endpoint must include a valid host: " + urlStr);
    }
  }

  /**
   * Ensures sensitive credentials are only used with trusted Vault endpoints.
   *
   * @param vaultAddr Vault endpoint URL. Not null.
   * @param credentialName Credential identifier used for diagnostic messages.
   */
  public static void ensureCredentialAllowed(
          String vaultAddr, String credentialName) {

    validateVaultUrl(vaultAddr);

    if (!isTrustedVaultAddr(vaultAddr)) {
      throw new IllegalStateException(
              "Refusing to use credential '" + credentialName
                      + "' with untrusted vaultAddr: " + vaultAddr);
    }
  }

  /**
   * Indicates whether HTTP Vault addresses are explicitly allowed.
   *
   * @return {@code true} if insecure HTTP endpoints are allowed; otherwise {@code false}.
   */
  private static boolean isInsecureVaultAddrAllowed() {
    String value = readAny(KEY_ALLOW_INSECURE_VAULT_ADDR);
    return TRUE.equalsIgnoreCase(value);
  }

  /**
   * Evaluates whether a Vault address is trusted for credential usage.
   *
   * @param vaultAddr Vault endpoint URL. Not null.
   * @return {@code true} if the address matches {@code TRUSTED_VAULT_HOSTS};
   * otherwise {@code false}.
   */
  private static boolean isTrustedVaultAddr(String vaultAddr) {
    URI vaultUri = parse(vaultAddr);
    String vaultHost = normalizeHost(vaultUri.getHost());
    int vaultPort = effectivePort(vaultUri);

    Set<String> trustedHosts = loadTrustedHosts();
    return trustedHosts.contains(hostPort(vaultHost, vaultPort))
            || trustedHosts.contains(vaultHost);
  }

  /**
   * Loads trusted host entries from configuration.
   *
   * @return A set of normalized host or host:port entries. Not null.
   */
  private static Set<String> loadTrustedHosts() {
    String trustedHosts = readAny(KEY_TRUSTED_HOSTS);
    if (trustedHosts == null || trustedHosts.trim().isEmpty()) {
      return Collections.emptySet();
    }

    return Arrays.stream(trustedHosts.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .map(VaultEndpointPolicy::normalizeHostPortToken)
            .collect(Collectors.toSet());
  }

  /**
   * Normalizes a host token or host:port token for case-insensitive comparison.
   *
   * @param token Input token.
   * @return Normalized token.
   */
  private static String normalizeHostPortToken(String token) {
    if (token.contains("://")) {
      URI uri = parse(token);
      String host = normalizeHost(uri.getHost());
      if (host.isEmpty()) {
        throw new IllegalArgumentException(
                "Invalid TRUSTED_VAULT_HOSTS entry: " + token);
      }
      int port = uri.getPort();
      return port == -1 ? host : hostPort(host, port);
    }

    int index = token.indexOf(':');
    if (index < 0) {
      return token.toLowerCase(Locale.ROOT);
    }
    String host = token.substring(0, index).toLowerCase(Locale.ROOT);
    String port = token.substring(index + 1);
    return host + ":" + port;
  }

  /**
   * Formats a host and port as a single host:port token.
   *
   * @param host Host name.
   * @param port Port number.
   * @return A host:port token.
   */
  private static String hostPort(String host, int port) {
    return host + ":" + port;
  }

  /**
   * Normalizes a host value for case-insensitive comparison.
   *
   * @param host Host value, nullable.
   * @return Lowercase host, or empty string if null.
   */
  private static String normalizeHost(String host) {
    if (host == null) {
      return "";
    }
    return host.toLowerCase(Locale.ROOT);
  }

  /**
   * Returns the effective port for a URI, applying default ports when absent.
   *
   * @param uri Parsed URI. Not null.
   * @return Explicit port, or a scheme default ({@code 443} for HTTPS,
   * {@code 80} for HTTP).
   */
  private static int effectivePort(URI uri) {
    if (uri.getPort() != -1) {
      return uri.getPort();
    }

    if (HTTP_SCHEME.equalsIgnoreCase(uri.getScheme())) {
      return 80;
    }

    return 443;
  }

  /**
   * Parses a URI string.
   *
   * @param value URL text. Not null.
   * @return Parsed URI.
   * @throws IllegalArgumentException if parsing fails.
   */
  private static URI parse(String value) {
    try {
      return URI.create(value);
    } catch (Exception ex) {
      throw new IllegalArgumentException("Invalid vault URL: " + value, ex);
    }
  }

  /**
   * Reads the first configured value from system properties or environment variables.
   *
   * @param keys Keys to check in order.
   * @return The first non-null value, or {@code null} if none is configured.
   */
  private static String readAny(String... keys) {
    for (String key : keys) {
      String value = System.getProperty(key);
      if (value != null) {
        return value;
      }
      value = System.getenv(key);
      if (value != null) {
        return value;
      }
    }
    return null;
  }
}
