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

package oracle.jdbc.provider.azure.authentication;

import com.azure.core.credential.TokenCredential;
import oracle.jdbc.provider.parameter.ParameterSet;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * <p>
 * A cache of {@link TokenCredential} objects, keyed on the identity of an
 * {@link AzureAuthenticationMethod#INTERACTIVE} or
 * {@link AzureAuthenticationMethod#DEVICE_CODE} login (see
 * {@link TokenCredentialFactory#INTERACTIVE_IDENTITY_PARAMETERS}).
 * </p><p>
 * Both of these methods require a human to complete a login with a web
 * browser. Building a fresh, never-used credential for every resource
 * request that needs one would mean prompting that human again and again for
 * what is, from their perspective, a single sign-in.
 * </p><p>
 * An Azure {@code TokenCredential} does not perform a login when it is
 * constructed: authentication happens lazily, the first time something
 * calls {@link TokenCredential#getToken} on it. From that point on, the
 * credential manages caching and refreshing its own access token
 * internally, in memory, for as long as it continues to be used; this is
 * documented, default behavior of the Azure Identity library, requiring no
 * opt-in configuration, for both {@link AzureAuthenticationMethod#INTERACTIVE}
 * and {@link AzureAuthenticationMethod#DEVICE_CODE} credentials. See
 * <a href="https://github.com/Azure/azure-sdk-for-java/blob/azure-identity_1.18.0/sdk/identity/azure-identity/TOKEN_CACHING.md">
 * Token caching in the Azure Identity client library
 * </a>: <i>"In-memory token caching is the default option provided by the
 * Azure Identity library... With in-memory token caching, the library first
 * determines if a valid access token for the requested resource is already
 * stored in memory. If a valid token is found, it's returned to the app
 * without the need to make another request to Microsoft Entra ID."</i>
 * </p><p>
 * This means the cache in this class only needs to ensure that requests
 * sharing the same identity are handed the exact same {@code TokenCredential}
 * instance, rather than each getting its own, never-yet-authenticated one
 * that would independently prompt for a fresh login on first use. There is
 * no need for this cache to track an expiration itself, or to distinguish a
 * successful login from a failed one: the credential is not actually used
 * until later, outside of this cache's control.
 * </p>
 */
final class InteractiveTokenCredentialCache {

  /**
   * Cached credentials, keyed on identity.
   * */
  private static final ConcurrentMap<ParameterSet, TokenCredential> ENTRIES =
    new ConcurrentHashMap<>();

  private InteractiveTokenCredentialCache() {}

  /**
   * <p>
   * Returns a cached {@code TokenCredential} for the given {@code identity},
   * or invokes {@code credentialSupplier} to build one if none is cached.
   * </p><p>
   * This is a thread safe method: if multiple threads call this method
   * concurrently with an equal {@code identity}, {@code credentialSupplier}
   * is guaranteed to be invoked by at most one of them, and all of them
   * receive the same resulting {@code TokenCredential}.
   * </p>
   *
   * @param identity Identifies the credential to return. Not null.
   * @param credentialSupplier Builds a new credential. Only invoked when no
   * credential is already cached for {@code identity}. Not null.
   * @return A {@code TokenCredential} for {@code identity}, either cached or
   * freshly built by {@code credentialSupplier}. Not null.
   */
  static TokenCredential get(
      ParameterSet identity, Supplier<TokenCredential> credentialSupplier) {
    return ENTRIES.computeIfAbsent(identity, identityKey -> credentialSupplier.get());
  }
}