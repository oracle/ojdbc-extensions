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

import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.ParameterSet;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * A factory that caches resources it has previously requested. This class
 * delegates to another {@code ResourceFactory} that requests a resource from an
 * external service, and then caches the resource for subsequent requests.
 * </p><p>
 * Cached resources are keyed to the {@link ParameterSet} passed to
 * {@link #request(ParameterSet)}. A cached resource is returned when the
 * {@code ParameterSet} passed this method is equal to a {@code ParameterSet}
 * passed to a previous call.
 * </p><p>
 * The class relies on {@link Resource#isValid()} to determine when a
 * cached resource needs to be evicted. Subclasses are responsible for returning
 * {@code Resource} objects that implement {@code isValid} correctly for the
 * particular type of resource they request.
 * </p><p>
 * The cache constrains the maximum the number of resources it can store at any
 * given time. By default, a maximum of 16 resources will be cached. A
 * non-default maximum may be configured by the "oracle.jdbc.provider.CACHE_SIZE"
 * system property. The least recently used resource is evicted when the cache
 * reaches its maximum size, and a new resource is requested.
 * </p>
 */
public final class CachedResourceFactory<T> implements ResourceFactory<T> {

  /**
   * The maximum number of resources retained in the cache. May be configured
   * using a system property.
   */
  private static final int CACHE_SIZE =
    Integer.getInteger("oracle.jdbc.provider.CACHE_SIZE", 16);

  /**
   * Factory that creates cached resources.
   */
  private final ResourceFactory<T> resourceFactory;

  /** Guards access to {@link #values} */
  private final ReentrantLock lock;

  /**
   * Retains cached values. The least recently used value is evicted when the
   * number of values exceeds the {@link #CACHE_SIZE}.
   */
  private final LruCache<ParameterSet, Future<Resource<T>>> values;

  /**
   * Constructs a factory that caches resources requested from the provided
   * {@code resourceFactory}.
   * @param resourceFactory Factory that cached resources are requested from.
   * Not null.
   */
  private CachedResourceFactory(ResourceFactory<T> resourceFactory) {
    this.lock = new ReentrantLock();
    this.values = new LruCache<>(CACHE_SIZE);
    this.resourceFactory = resourceFactory;
  }

  /**
   * Creates a factory that caches resources from the provided
   * {@code resourceFactory}. Cached {@code Resource} objects are evicted when
   * {@code isValid()} returns {@code false}, or when application code calls
   * {@link CacheController#clearAllCaches()}.
   *
   * @param <T> The class of objects that represent a resource.
   * @param resourceFactory Factory that cached resources are requested from.
   * Not null.
   * @return A factory that caches resources from the {@code resourceFactory}.
   * Not null.
   *
   * @implNote This static method should be the only place where instances of
   * {@code CachedResourceFactory} are created. It needs to ensure that all
   * instances are registered with the {@code CacheController}.
   */
  public static <T> ResourceFactory<T> create(
    ResourceFactory<T> resourceFactory) {

    CachedResourceFactory<T> cachedResourceFactory =
      new CachedResourceFactory<>(resourceFactory);

    CacheController.register(cachedResourceFactory);

    return cachedResourceFactory;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Returns a cached resource from a previous request, if available. Otherwise,
   * the resource is requested from the delegate resource factory, and then
   * retained in the cache for subsequent requests.
   * </p><p>
   * This is a thread safe method. If multiple threads may call this method with
   * equal {@code parameterSet} objects, and no resource is cached for that
   * parameter set, then only one thread will request a resource from the
   * delegate resource factory. All other threads will wait for the single
   * request to complete, and then return the same {@code Resource} object (or
   * error) that results from that request.
   * </p>
   */
  @Override
  public Resource<T> request(ParameterSet parameterSet) {
    Objects.requireNonNull(parameterSet, "parameterSet is null");

    // Attempt to get a cached value
    Future<Resource<T>> existingResourceFuture;
    lock.lock();
    try {
      existingResourceFuture = values.get(parameterSet);
    }
    finally {
      lock.unlock();
    }

    // Return the value if it is present and still valid
    if (existingResourceFuture != null) {
      Resource<T> resource = await(existingResourceFuture);

      if (resource.isValid())
        return resource;
    }

    // Create a task to request a new value
    FutureTask<Resource<T>> newResourceTask =
      new FutureTask<>(() -> resourceFactory.request(parameterSet));

    Future<Resource<T>> newResourceFuture;
    lock.lock();
    try {
      // Update the map, unless another thread has already done so
      newResourceFuture = values.compute(
        parameterSet,
        (currentKey, currentResourceFuture) ->
          currentResourceFuture == existingResourceFuture
            ? newResourceTask
            : currentResourceFuture);
    }
    finally {
      lock.unlock();
    }

    if (newResourceFuture.equals(newResourceTask)) {
      // The map has been updated with the task created by this thread. Run the
      // task so that other threads can receive the value.
      newResourceTask.run();
    }

    // TODO: It is possible that the returned value is still not valid. If that
    //  becomes an issue, then a retry strategy should be implemented.
    return await(newResourceFuture);
  }

  /**
   * Clears this cache, evicting all previously requested resources.
   */
  public void clearCache() {
    lock.lock();
    try {
      values.clear();
    }
    finally {
      lock.unlock();
    }
  }

  /**
   * Returns or throws the result of a {@code future}, blocking the current
   * thread until the future is complete.
   */
  private Resource<T> await(Future<Resource<T>> future) {
    try {
      return future.get();
    }
    catch (ExecutionException executionException) {
      Throwable cause = executionException.getCause();

      if (cause instanceof Error)
        throw (Error) cause;
      else if (cause instanceof IllegalStateException)
        throw (IllegalStateException) cause;
      else
        throw new IllegalStateException(cause);
    }
    catch (InterruptedException interruptedException) {
      throw new IllegalStateException(interruptedException);
    }
  }

  /**
   * A map that evicts the least recently used (LUR) entry when the number of
   * stored values exceeds a maximum size.
   */
  private static final class LruCache<K,V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 1L;

    /** Maximum number of stored values */
    private final int maximumSize;

    private LruCache(int maximumSize) {
      // The maximum size is known, and is expected to be small, maybe no more
      // than 32 cache entries. Configure the HashMap with an initial capacity
      // large enough to store the maximum size of value. The load factor is
      // configured such that the threshold will never be reached.
      // It is noted that HashMap.put(...) executes code similar to this:
      //   if (size++ > threshold)
      //     resize();
      //   removeEldestEntry();
      // This has a resize occur *before* removeEldestEntry is called and has
      // a chance to reduce the size. For this reason, the load factor of 1.1 is
      // given, which should mean the "threshold" = maximumSize * 1.1, which
      // will be larger than the maximumSize.
      super(maximumSize, 1.1f, true);
      this.maximumSize = maximumSize;
    }

    @Override
    public boolean removeEldestEntry(Map.Entry<K, V> entry) {
      return size() > maximumSize;
    }
  }
}
