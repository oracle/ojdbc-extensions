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
import oracle.jdbc.provider.factory.TestResource;
import oracle.jdbc.provider.factory.TestResourceFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the behavior of {@link CacheController}
 */
public class CacheControllerTest {

  /**
   * On a single thread, invoke {@link CacheController#clearAllCaches()}, and
   * verify that a single {@link CachedResourceFactory} no longer retains
   * cached resources.
   */
  @Test
  public void testSingleThreadSingleFactory() throws Exception {

    // Verify behavior when no instances have been created (assumes GC has
    // kicked out instances from previous tests...)
    for (int i = 0; i < 25; i++) {
      System.gc();
      CacheController.clearAllCaches();
    }

    // Create a cache, and attempt to clear it when it is already empty.
    TestResourceFactory<String> factory = new TestResourceFactory<>();
    ResourceFactory<String> cachedFactory =
      CachedResourceFactory.create(factory);
    CacheController.clearAllCaches();

    // Cache a resource, clear the cache, and check that a new resource is
    // requested
    TestResource aResource = new TestResource("a");
    ParameterSet aParameterSet = factory.addResource("a", aResource);
    assertSame(aResource, cachedFactory.request(aParameterSet));
    TestResource bResource = new TestResource("b");
    factory.addResource("a", bResource);
    assertSame(aResource, cachedFactory.request(aParameterSet));
    CacheController.clearAllCaches();
    assertSame(bResource, cachedFactory.request(aParameterSet));

    // Clear references to the cache and factory, then attempt to clear the
    // cache. The CacheController should detect that the objects have been
    // garbage collected.
    WeakReference<ResourceFactory<String>> cachedFactoryReference =
      new WeakReference<>(cachedFactory);
    cachedFactory = null;
    factory = null;
    while (cachedFactoryReference.get() != null) {
      Thread.sleep(100);
      System.gc();
    }
    CacheController.clearAllCaches();
  }

  /**
   * On a single thread, invoke {@link CacheController#clearAllCaches()}, and
   * verify that a multiple {@link CachedResourceFactory}s no longer retain
   * cached resources.
   */
  @Test
  public void testSingleThreadMultipleFactory() throws Exception {

    // Create more caches, caches more resources with them, clear them, and
    // verify that new resources are requested.
    TestResourceFactory<String> factory0 = new TestResourceFactory<>();
    ResourceFactory<String> cachedFactory0 =
      CachedResourceFactory.create(factory0);
    TestResource aResource0 = new TestResource("0");
    ParameterSet aParameterSet0 = factory0.addResource("0", aResource0);
    assertSame(aResource0, cachedFactory0.request(aParameterSet0));
    TestResource bResource0 = new TestResource("0");
    factory0.addResource("0", bResource0);


    TestResourceFactory<String> factory1 = new TestResourceFactory<>();
    ResourceFactory<String> cachedFactory1 =
      CachedResourceFactory.create(factory1);
    TestResource aResource1 = new TestResource("1");
    ParameterSet aParameterSet1 = factory1.addResource("1", aResource1);
    assertSame(aResource1, cachedFactory1.request(aParameterSet1));
    TestResource bResource1 = new TestResource("1");
    factory1.addResource("1", bResource1);

    CacheController.clearAllCaches();
    assertSame(bResource0, cachedFactory0.request(aParameterSet0));
    assertSame(bResource1, cachedFactory1.request(aParameterSet1));

  }

  /**
   * Verifies behavior when multiple threads request resources while another
   * thread clears the cache.
   */
  @Test
  public void testMultipleThreads() throws Exception {
    int threadCount = 32;
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
    try {
      BlockingQueue<Future<Void>> queue = new LinkedBlockingQueue<>();
      ExecutorCompletionService<Void> completionService =
        new ExecutorCompletionService<>(executorService, queue);

      CountDownLatch latch = new CountDownLatch(threadCount);

      for (int i = 0; i < threadCount; i++) {
        completionService.submit(() -> {

          TestResourceFactory<String> factory = new TestResourceFactory<>();
          TestResource aResource = new TestResource("a");
          ParameterSet aParameterSet = factory.addResource("a", aResource);

          ResourceFactory<String> cachedFactory =
            CachedResourceFactory.create(factory);
          assertSame(aResource, cachedFactory.request(aParameterSet));

          // Create a new resource, and expect the old resource to be cached.
          TestResource bResource = new TestResource("b");
          factory.addResource("a", bResource);
          assertSame(aResource, cachedFactory.request(aParameterSet));

          // Trigger the latch which allows the main thread to proceed with
          // clearing the cache.
          latch.countDown();

          // Expect the new resource to eventually be returned.
          while (true) {
            Resource<String> newResource = cachedFactory.request(aParameterSet);

            if (newResource == aResource)
              continue;

            assertSame(bResource, newResource);
            break;
          }
          return null;
        });
      }

      // Wait for all threads to enter the while loop seen above, and then clear
      // the cache.
      assertTrue(latch.await(15, TimeUnit.SECONDS));
      CacheController.clearAllCaches();


      for (int i = 0; i < threadCount; i++) {
        completionService.take()
          .get(15, TimeUnit.SECONDS);
      }
    }
    finally {
      executorService.shutdown();
    }
  }

}
