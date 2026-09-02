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
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies {@link InteractiveTokenCredentialCache} using synthetic
 * {@link Supplier} credential builders, rather than a real, browser-based or
 * device-code login.
 */
public class InteractiveTokenCredentialCacheTest {

  /**
   * A parameter used only to build distinct test identities
   */
  private static final Parameter<String> TEST_ID = Parameter.create();

  /**
   * Verifies that a second call for the same identity reuses the credential
   * built by the first call, rather than building a new one.
   */
  @Test
  public void testCachesCredentialForSameIdentity() {
    ParameterSet identity = identity("testCachesCredentialForSameIdentity");
    AtomicInteger buildCount = new AtomicInteger(0);

    Supplier<TokenCredential> credentialSupplier = () -> {
      buildCount.incrementAndGet();
      return fakeCredential();
    };

    TokenCredential first =
      InteractiveTokenCredentialCache.get(identity, credentialSupplier);
    TokenCredential second =
      InteractiveTokenCredentialCache.get(identity, credentialSupplier);

    assertSame(first, second);
    assertEquals(1, buildCount.get());
  }

  /**
   * Verifies that two different identities are cached independently: each
   * builds its own credential, and neither result is reused for the other.
   */
  @Test
  public void testDifferentIdentitiesAreCachedIndependently() {
    ParameterSet identityA = identity("testDifferentIdentitiesAreCachedIndependently-A");
    ParameterSet identityB = identity("testDifferentIdentitiesAreCachedIndependently-B");

    TokenCredential credentialA =
      InteractiveTokenCredentialCache.get(
        identityA, InteractiveTokenCredentialCacheTest::fakeCredential);
    TokenCredential credentialB =
      InteractiveTokenCredentialCache.get(
        identityB, InteractiveTokenCredentialCacheTest::fakeCredential);

    assertNotSame(credentialA, credentialB);

    // Each identity still independently returns its own cached credential.
    assertSame(
      credentialA,
      InteractiveTokenCredentialCache.get(identityA, InteractiveTokenCredentialCacheTest::fail));
    assertSame(
      credentialB,
      InteractiveTokenCredentialCache.get(identityB, InteractiveTokenCredentialCacheTest::fail));
  }

  /**
   * <p>
   * Verifies that a failed attempt to build a credential is not cached: a
   * following call for the same identity attempts to build a fresh one,
   * rather than replaying the same exception indefinitely.
   * </p><p>
   * Building an Azure {@code TokenCredential} does not itself perform
   * authentication (it happens lazily, later, when something calls
   * {@link TokenCredential#getToken}), so a failure here represents a
   * configuration error (such as a missing client ID) rather than a failed
   * login. Even so, such a failure should not be cached indefinitely.
   * </p>
   */
  @Test
  public void testBuildFailureIsNotCached() {
    ParameterSet identity = identity("testBuildFailureIsNotCached");
    AtomicInteger buildCount = new AtomicInteger(0);

    Supplier<TokenCredential> credentialSupplier = () -> {
      if (buildCount.incrementAndGet() == 1)
        throw new IllegalStateException("Simulated configuration error");

      return fakeCredential();
    };

    assertThrows(
      IllegalStateException.class,
      () -> InteractiveTokenCredentialCache.get(identity, credentialSupplier));
    assertEquals(1, buildCount.get());

    TokenCredential credential =
      InteractiveTokenCredentialCache.get(identity, credentialSupplier);

    assertEquals(2, buildCount.get());

    // The now-successful credential is cached normally.
    assertSame(
      credential, InteractiveTokenCredentialCache.get(identity, credentialSupplier));
    assertEquals(2, buildCount.get());
  }

  /**
   * Verifies that many threads requesting the same, not yet cached identity
   * concurrently share a single credential, rather than each building their
   * own.
   */
  @Test
  public void testConcurrentRequestsShareOneCredential() throws Exception {
    ParameterSet identity = identity("testConcurrentRequestsShareOneCredential");
    AtomicInteger buildCount = new AtomicInteger(0);

    // Delays the build just long enough for multiple threads to reliably
    // observe no cached value yet, and attempt to build concurrently.
    CountDownLatch allThreadsReady = new CountDownLatch(16);
    Supplier<TokenCredential> credentialSupplier = () -> {
      buildCount.incrementAndGet();
      awaitLatch(allThreadsReady);
      return fakeCredential();
    };

    ExecutorService executor = Executors.newFixedThreadPool(16);
    try {
      List<Future<TokenCredential>> futures =
        IntStream.range(0, 16)
          .mapToObj(i -> executor.submit(() -> {
            allThreadsReady.countDown();
            return InteractiveTokenCredentialCache.get(identity, credentialSupplier);
          }))
          .collect(Collectors.toList());

      List<TokenCredential> results = new ArrayList<>();
      for (Future<TokenCredential> future : futures)
        results.add(future.get(30, TimeUnit.SECONDS));

      TokenCredential first = results.get(0);
      for (TokenCredential result : results)
        assertSame(first, result);

      assertEquals(1, buildCount.get());
    }
    finally {
      executor.shutdownNow();
    }
  }

  private static void awaitLatch(CountDownLatch latch) {
    boolean allThreadsBecameReady;
    try {
      allThreadsBecameReady = latch.await(30, TimeUnit.SECONDS);
    }
    catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(interruptedException);
    }

    if (! allThreadsBecameReady)
      throw new IllegalStateException(
        "Timed out waiting for all threads to become ready");
  }

  /** A credential {@code Supplier} that fails the test if it is ever invoked */
  private static TokenCredential fail() {
    throw new AssertionError(
      "credential should not be built: a cached value was expected");
  }

  /**
   * Returns a {@code ParameterSet} identifying a distinct test identity
   */
  private static ParameterSet identity(String id) {
    return ParameterSet.builder()
      .add("id", TEST_ID, id)
      .build();
  }

  /**
   * Guarantees each {@link #fakeCredential()} call is distinguishable; see below.
   */
  private static final AtomicInteger FAKE_CREDENTIAL_COUNT = new AtomicInteger(0);

  /**
   * <p>
   * Returns a synthetic {@link TokenCredential} that never actually attempts
   * to acquire a token: {@link TokenCredential#getToken} always errors,
   * since no test in this class needs it to succeed. The purpose of this
   * method is only to verify that distinct instances are correctly cached
   * and reused, not to exercise real Azure authentication.
   * </p><p>
   * The returned lambda captures a value unique to this call
   * ({@link #FAKE_CREDENTIAL_COUNT}), so that two separate calls to this
   * method always produce two distinguishable ({@code assertNotSame})
   * instances. This is deliberate, and not just for a distinct error
   * message: a <em>non</em>-capturing lambda (or method reference) may be
   * cached and reused by the JVM across separate invocations of the
   * enclosing method, since it would have no state of its own to
   * distinguish one invocation from another, which would silently defeat
   * tests in this class that rely on instance identity. Capturing a value
   * here removes that risk without depending on this method being written
   * one particular way (e.g. as an anonymous class instead of a lambda),
   * since an automatic code-style cleanup could otherwise undo that later.
   * </p>
   */
  private static TokenCredential fakeCredential() {
    int id = FAKE_CREDENTIAL_COUNT.incrementAndGet();
    return requestContext -> Mono.error(new UnsupportedOperationException(
      "getToken() is not supported by this synthetic test credential (#" + id + ")"));
  }
}