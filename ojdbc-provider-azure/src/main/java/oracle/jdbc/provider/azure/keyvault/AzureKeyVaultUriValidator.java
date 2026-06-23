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

package oracle.jdbc.provider.azure.keyvault;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Validates and normalizes Azure Key Vault URLs and secret URIs.
 */
public final class AzureKeyVaultUriValidator {

  private static final String[] APPROVED_HOST_SUFFIXES = {
      ".vault.azure.net",
      ".vault.azure.cn",
      ".vault.usgovcloudapi.net",
      ".vault.microsoftazure.de",
      ".vault.sovcloud-api.fr",
      ".managedhsm.azure.net",
      ".managedhsm.azure.cn",
      ".managedhsm.usgovcloudapi.net",
      ".managedhsm.microsoftazure.de",
      ".managedhsm.sovcloud-api.fr"
  };

  private AzureKeyVaultUriValidator() { }

  /**
   * Validates and parses a Key Vault secret URI.
   * @param value Secret URI. Not null.
   * @return Parsed secret reference.
   */
  public static SecretReference parseSecretUri(String value) {
    URI uri = parse(value);
    validateAuthority(uri, value);
    validateNoQueryOrFragment(uri, value);

    String path = uri.getPath();
    if (path == null || !path.startsWith("/secrets/")) {
      throw new IllegalArgumentException(
        "The Vault Secret URI should contain \"/secrets\" followed by the name of the Secret: "
          + value);
    }

    String secretPath = path.substring("/secrets/".length());
    String[] segments = secretPath.split("/");
    if (segments.length == 0 || segments[0].trim().isEmpty()) {
      throw new IllegalArgumentException("Missing secret name in Vault URI: " + value);
    }

    if (segments.length > 2) {
      throw new IllegalArgumentException("Invalid Vault Secret URI path: " + value);
    }

    String secretVersion = null;
    if (segments.length == 2) {
      if (segments[1].trim().isEmpty()) {
        throw new IllegalArgumentException("Invalid Vault Secret URI path: " + value);
      }
      secretVersion = segments[1];
    }

    return new SecretReference(
      "https://" + normalizeHost(uri.getHost()),
      segments[0],
      secretVersion);
  }

  private static URI parse(String value) {
    try {
      return new URI(value);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid Azure Key Vault URI: " + value, e);
    }
  }

  private static void validateAuthority(URI uri, String originalValue) {
    if (!"https".equalsIgnoreCase(uri.getScheme())) {
      throw new IllegalArgumentException(
        "Azure Key Vault URIs must use HTTPS: " + originalValue);
    }

    if (uri.getUserInfo() != null) {
      throw new IllegalArgumentException(
        "Azure Key Vault URIs must not contain user info: " + originalValue);
    }

    if (uri.getPort() != -1) {
      throw new IllegalArgumentException(
        "Azure Key Vault URIs must not include an explicit port: " + originalValue);
    }

    normalizeHost(uri.getHost());
  }

  private static void validateNoQueryOrFragment(URI uri, String originalValue) {
    if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
      throw new IllegalArgumentException(
        "Azure Key Vault URIs must not include a query or fragment: " + originalValue);
    }
  }

  private static String normalizeHost(String host) {
    if (host == null || host.trim().isEmpty()) {
      throw new IllegalArgumentException("Missing Azure Key Vault host");
    }

    String normalizedHost = host.toLowerCase(Locale.ROOT);
    for (String approvedHostSuffix : APPROVED_HOST_SUFFIXES) {
      if (normalizedHost.endsWith(approvedHostSuffix)) {
        return normalizedHost;
      }
    }

    throw new IllegalArgumentException(
      "Azure Key Vault host is not allowed: " + host);
  }

  /**
   * Parsed Azure Key Vault secret reference.
   */
  public static final class SecretReference {
    private final String vaultUrl;
    private final String secretName;
    private final String secretVersion;

    private SecretReference(
      String vaultUrl, String secretName, String secretVersion) {
      this.vaultUrl = vaultUrl;
      this.secretName = secretName;
      this.secretVersion = secretVersion;
    }

    public String getVaultUrl() {
      return vaultUrl;
    }

    public String getSecretName() {
      return secretName;
    }

    public String getSecretVersion() {
      return secretVersion;
    }
  }
}
