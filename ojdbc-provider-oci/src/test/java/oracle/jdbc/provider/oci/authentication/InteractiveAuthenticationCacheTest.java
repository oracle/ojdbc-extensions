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

package oracle.jdbc.provider.oci.authentication;

import com.oracle.bmc.Region;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
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
 * Verifies {@link InteractiveAuthenticationCache} using synthetic
 * {@link Supplier} logins, rather than real, browser-based interactive
 * authentication.
 */
public class InteractiveAuthenticationCacheTest {

  /** A parameter used only to build distinct test identities */
  private static final Parameter<String> TEST_ID = Parameter.create();

  /**
   * Verifies that a second call for the same identity reuses the result of
   * the first call, rather than invoking {@code login} again.
   */
  @Test
  public void testCachesSuccessfulLogin() {
    ParameterSet identity = identity("testCachesSuccessfulLogin");
    AtomicInteger loginCount = new AtomicInteger(0);

    Supplier<InteractiveAuthenticationDetails> login = () -> {
      loginCount.incrementAndGet();
      return fakeDetails(OffsetDateTime.now().plusHours(1));
    };

    InteractiveAuthenticationDetails first =
      InteractiveAuthenticationCache.get(identity, login);
    InteractiveAuthenticationDetails second =
      InteractiveAuthenticationCache.get(identity, login);

    assertSame(first, second);
    assertEquals(1, loginCount.get());
  }

  /**
   * Verifies that two different identities are cached independently: each
   * invokes its own {@code login}, and neither result is reused for the
   * other.
   */
  @Test
  public void testDifferentIdentitiesAreCachedIndependently() {
    ParameterSet identityA = identity("testDifferentIdentitiesAreCachedIndependently-A");
    ParameterSet identityB = identity("testDifferentIdentitiesAreCachedIndependently-B");

    InteractiveAuthenticationDetails detailsA =
      InteractiveAuthenticationCache.get(
        identityA, () -> fakeDetails(OffsetDateTime.now().plusHours(1)));
    InteractiveAuthenticationDetails detailsB =
      InteractiveAuthenticationCache.get(
        identityB, () -> fakeDetails(OffsetDateTime.now().plusHours(1)));

    assertNotSame(detailsA, detailsB);

    // Each identity still independently returns its own cached login.
    assertSame(
      detailsA,
      InteractiveAuthenticationCache.get(identityA, InteractiveAuthenticationCacheTest::fail));
    assertSame(
      detailsB,
      InteractiveAuthenticationCache.get(identityB, InteractiveAuthenticationCacheTest::fail));
  }

  /**
   * <p>
   * Verifies that a failed login is not cached: a following call for the
   * same identity attempts a fresh login, rather than replaying the same
   * exception indefinitely. A session timeout is an ordinary event that
   * must be retryable with a fresh prompt, not a permanent failure.
   * </p>
   */
  @Test
  public void testFailedLoginIsNotCached() {
    ParameterSet identity = identity("testFailedLoginIsNotCached");
    AtomicInteger loginCount = new AtomicInteger(0);

    Supplier<InteractiveAuthenticationDetails> login = () -> {
      if (loginCount.incrementAndGet() == 1)
        throw new IllegalStateException("Simulated login timeout");

      return fakeDetails(OffsetDateTime.now().plusHours(1));
    };

    assertThrows(
      IllegalStateException.class,
      () -> InteractiveAuthenticationCache.get(identity, login));
    assertEquals(1, loginCount.get());

    InteractiveAuthenticationDetails details =
      InteractiveAuthenticationCache.get(identity, login);

    assertEquals(2, loginCount.get());

    // The now-successful login is cached normally.
    assertSame(details, InteractiveAuthenticationCache.get(identity, login));
    assertEquals(2, loginCount.get());
  }

  /**
   * Verifies that a cached login whose session token has expired is not
   * reused: a following call for the same identity attempts a fresh login.
   */
  @Test
  public void testExpiredLoginIsNotReused() {
    ParameterSet identity = identity("testExpiredLoginIsNotReused");
    AtomicInteger loginCount = new AtomicInteger(0);

    InteractiveAuthenticationDetails expired =
      InteractiveAuthenticationCache.get(identity, () -> {
        loginCount.incrementAndGet();
        // Expires immediately (in the past), so it is stale as soon as it
        // is cached.
        return fakeDetails(OffsetDateTime.now().minusSeconds(1));
      });

    InteractiveAuthenticationDetails fresh =
      InteractiveAuthenticationCache.get(identity, () -> {
        loginCount.incrementAndGet();
        return fakeDetails(OffsetDateTime.now().plusHours(1));
      });

    assertNotSame(expired, fresh);
    assertEquals(2, loginCount.get());

    // The now-valid login is cached normally.
    assertSame(
      fresh,
      InteractiveAuthenticationCache.get(identity, InteractiveAuthenticationCacheTest::fail));
  }

  /**
   * Verifies that many threads requesting the same, not yet cached identity
   * concurrently share a single login, rather than each triggering their
   * own.
   */
  @Test
  public void testConcurrentRequestsShareOneLogin() throws Exception {
    ParameterSet identity = identity("testConcurrentRequestsShareOneLogin");
    AtomicInteger loginCount = new AtomicInteger(0);

    // Delays the login just long enough for multiple threads to reliably
    // observe no cached value yet, and attempt to log in concurrently.
    CountDownLatch allThreadsReady = new CountDownLatch(16);
    Supplier<InteractiveAuthenticationDetails> login = () -> {
      loginCount.incrementAndGet();
      awaitLatch(allThreadsReady);
      return fakeDetails(OffsetDateTime.now().plusHours(1));
    };

    ExecutorService executor = Executors.newFixedThreadPool(16);
    try {
      List<Future<InteractiveAuthenticationDetails>> futures =
        IntStream.range(0, 16)
          .mapToObj(i -> executor.submit(() -> {
            allThreadsReady.countDown();
            return InteractiveAuthenticationCache.get(identity, login);
          }))
          .collect(Collectors.toList());

      List<InteractiveAuthenticationDetails> results = new ArrayList<>();
      for (Future<InteractiveAuthenticationDetails> future : futures)
        results.add(future.get(30, TimeUnit.SECONDS));

      InteractiveAuthenticationDetails first = results.get(0);
      for (InteractiveAuthenticationDetails result : results)
        assertSame(first, result);

      assertEquals(1, loginCount.get());
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

  /**
   * A login {@code Supplier} that fails the test if it is ever invoked
   *
   */
  private static InteractiveAuthenticationDetails fail() {
    throw new AssertionError(
      "login should not be invoked: a cached value was expected");
  }

  /**
   *  Returns a {@code ParameterSet} identifying a distinct test identity
   * */
  private static ParameterSet identity(String id) {
    return ParameterSet.builder()
      .add("id", TEST_ID, id)
      .build();
  }

  /**
   * Returns a synthetic {@link InteractiveAuthenticationDetails}, with a
   * real, generated key pair, and a session token whose "exp" claim encodes
   * the given {@code expirationTime}.
   */
  private static InteractiveAuthenticationDetails fakeDetails(
      OffsetDateTime expirationTime) {

    KeyPair keyPair;
    try {
      keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
    }
    catch (NoSuchAlgorithmException noSuchAlgorithmException) {
      throw new IllegalStateException(noSuchAlgorithmException);
    }

    String payload = String.format(
      "{\"tenant\":\"test-tenant\",\"sub\":\"test-user\",\"exp\":%d}",
      expirationTime.toEpochSecond());

    String encodedPayload = Base64.getUrlEncoder().withoutPadding()
      .encodeToString(payload.getBytes(StandardCharsets.UTF_8));

    String sessionToken = "header." + encodedPayload + ".signature";

    return new InteractiveAuthenticationDetails(
      Region.EU_AMSTERDAM_1, sessionToken, keyPair);
  }
}