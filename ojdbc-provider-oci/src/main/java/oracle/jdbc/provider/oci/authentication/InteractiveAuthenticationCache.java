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

import oracle.jdbc.provider.parameter.ParameterSet;

import java.time.OffsetDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Supplier;

/**
 * <p>
 * A cache of {@link InteractiveAuthenticationDetails}, keyed on the identity
 * of an {@link AuthenticationMethod#INTERACTIVE} login (see
 * {@link AuthenticationDetailsFactory#INTERACTIVE_IDENTITY_PARAMETERS}).
 * </p><p>
 * Interactive authentication requires a human to complete a login with a web
 * browser, which may take anywhere from a few seconds to several minutes.
 * Requesting a fresh login every time a resource needs to authenticate would
 * mean prompting that human to log in again and again for what is, from
 * their perspective, a single sign-in. This class exists so that multiple
 * requests which share the same identity can reuse one login, for as long as
 * the session token it produced remains valid.
 * </p><p>
 * Two properties of this cache follow from what it stores:
 * </p><ul>
 *   <li>
 *     A failed login is not cached: a login failure (such as a browser
 *     prompt that timed out) is expected to be retried with a fresh prompt,
 *     not replayed indefinitely.
 *   </li>
 *   <li>
 *     A session token has a well defined expiration (its "exp" claim), so
 *     entries are actively removed once they stop being useful.
 *   </li>
 * </ul>
 */
final class InteractiveAuthenticationCache {

  /**
   * <p>
   * Cached logins, keyed on identity. A value may represent a login that is
   * still in progress ({@link FutureTask#isDone()} is {@code false}), or one
   * that has completed, either successfully or with a failure.
   * </p><p>
   * Entries are never replaced in place: a stale or failed entry is instead
   * removed (see {@link #removeStaleEntries()}), or superseded by a new
   * {@code FutureTask} installed by {@link #get(ParameterSet, Supplier)}.
   * </p>
   */
  private static final ConcurrentMap<ParameterSet, FutureTask<InteractiveAuthenticationDetails>>
    ENTRIES = new ConcurrentHashMap<>();

  private InteractiveAuthenticationCache() {}

  /**
   * <p>
   * Returns cached authentication details for the given {@code identity}, or
   * invokes {@code login} to perform a new login if none is cached.
   * </p><p>
   * This is a thread safe method. If multiple threads call this method
   * concurrently with an equal {@code identity}, and no login is yet cached
   * for it, only one thread will invoke {@code login}. The other threads
   * block until that single login completes, and then share its result, or
   * its failure.
   * </p>
   *
   * @param identity Identifies the login to return. Not null.
   * @param login Performs a fresh interactive login. Only invoked when no
   * valid login is already cached for {@code identity}. Not null.
   * @return Authentication details for {@code identity}, either cached or
   * freshly obtained from {@code login}. Not null.
   * @throws IllegalStateException If a fresh login is required, and
   * {@code login} fails with a checked exception.
   */
  static InteractiveAuthenticationDetails get(
      ParameterSet identity, Supplier<InteractiveAuthenticationDetails> login) {

    removeStaleEntries();

    // removeStaleEntries() guarantees that any entry still present, and
    // already done, is valid: use it as-is, without touching the map again.
    // An entry that is present but not yet done belongs to a login already
    // in progress on another thread; it is also used as-is, so that this
    // thread waits for that single login rather than starting its own.
    FutureTask<InteractiveAuthenticationDetails> existingTask = ENTRIES.get(identity);

    FutureTask<InteractiveAuthenticationDetails> task = existingTask != null
      ? existingTask
      : ENTRIES.computeIfAbsent(identity, key -> new FutureTask<>(login::get));

    // A no-op if another thread has already called run() on this task.
    task.run();

    return await(task);
  }

  /**
   * Removes any entry that has completed, and is no longer useful: one that
   * failed, or one whose session token has expired. An entry that is still
   * in progress is never removed by this method.
   */
  private static void removeStaleEntries() {
    ENTRIES.values().removeIf(InteractiveAuthenticationCache::isStale);
  }

  /**
   * Returns {@code true} if {@code task} has completed, and its result is no
   * longer usable: either it failed, or the session token it produced has
   * expired.
   */
  private static boolean isStale(FutureTask<InteractiveAuthenticationDetails> task) {
    if (!task.isDone())
      return false;

    InteractiveAuthenticationDetails details = getIfSuccessful(task);

    return details == null
      || !OffsetDateTime.now().isBefore(details.getExpirationTime());
  }

  /**
   * Returns the result of a completed {@code task}, or {@code null} if it
   * completed with an exception. Unlike {@link #await(FutureTask)}, this
   * method does not propagate that exception to the caller.
   */
  private static InteractiveAuthenticationDetails getIfSuccessful(
      FutureTask<InteractiveAuthenticationDetails> task) {
    try {
      return task.get();
    }
    catch (ExecutionException executionException) {
      return null;
    }
    catch (InterruptedException interruptedException) {
      throw new IllegalStateException(interruptedException);
    }
  }

  /**
   * Returns the result of {@code task}, blocking the calling thread until it
   * completes. Unlike {@link #getIfSuccessful(FutureTask)}, this method
   * rethrows the cause of a failed {@code task} to the caller.
   */
  private static InteractiveAuthenticationDetails await(
      FutureTask<InteractiveAuthenticationDetails> task) {
    try {
      return task.get();
    }
    catch (ExecutionException executionException) {
      Throwable cause = executionException.getCause();

      if (cause instanceof Error)
        throw (Error) cause;
      else if (cause instanceof RuntimeException)
        throw (RuntimeException) cause;
      else
        throw new IllegalStateException(cause);
    }
    catch (InterruptedException interruptedException) {
      throw new IllegalStateException(interruptedException);
    }
  }
}