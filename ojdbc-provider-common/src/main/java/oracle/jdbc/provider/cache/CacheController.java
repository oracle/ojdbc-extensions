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

package oracle.jdbc.provider.cache;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class exposes methods to manage cached resources that are retained by
 * providers. The programmatic API of this class is intended for use by
 * application code, and will remain stable across releases.
 */
public final class CacheController {

  /**
   * Set of {@code CachedResourceFactory} instances that have been registered
   * with this controller. The instances are retained as weak references that
   * may be garbage collected. The {@link #LOCK} must be acquired before
   * accessing this field.
   */
  private static final Set<WeakReference<CachedResourceFactory<?>>> FACTORIES =
    new HashSet<>();

  /** Guards access to {@link #FACTORIES} */
  private static final ReentrantLock LOCK = new ReentrantLock();

  private CacheController() { }

  /**
   * Clears all cached resources that were requested before this method was
   * called. Subsequent requests for the same resources will require the provider
   * to re-authenticate with the service that manages the resource.
   */
  public static void clearAllCaches() {
    LOCK.lock();
    try {
      Set<WeakReference<CachedResourceFactory<?>>> clearedReferences =
        new HashSet<>(1);

      for (WeakReference<CachedResourceFactory<?>> reference : FACTORIES) {

        CachedResourceFactory<?> cache = reference.get();

        if (cache == null) {
          clearedReferences.add(reference);
          continue;
        }

        cache.clearCache();
      }

      FACTORIES.removeAll(clearedReferences);
    }
    finally {
      LOCK.unlock();
    }
  }

  /**
   * Registers a factory that caches resources with the controller. The
   * {@link CachedResourceFactory#clearCache()} method of the {@code factory} is
   * called when application code calls {@link #clearAllCaches()}.
   * @param factory Factory that is registered with the controller. Not null.
   */
  static void register(CachedResourceFactory<?> factory) {
    LOCK.lock();
    try {
      FACTORIES.add(new WeakReference<>(factory));
    }
    finally {
      LOCK.unlock();
    }
  }

}
