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

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static oracle.jdbc.provider.factory.TestResourceFactory.ID;
import static org.junit.jupiter.api.Assertions.*;

/** Verifies {@link CachedResourceFactory} */
public class CachedResourceFactoryTest {

  /** Verfies behavior with a single thread */
  @Test
  public void testSingleThread() {

    TestResourceFactory<String> factory = new TestResourceFactory<>();
    ResourceFactory<String> cachedFactory =
      CachedResourceFactory.create(factory);

    // Cache a resource. It is removed after the first call to testRequest, and
    // expected to be returned from the cache on the second call.
    TestResource resource0 = new TestResource("0");
    factory.addResource(resource0.getContent(), resource0);
    assertSame(resource0, testRequest(resource0.getContent(), factory, cachedFactory));
    assertSame(resource0, testRequest(resource0.getContent(), factory, cachedFactory));

    // Cache another resource with a different ID. It is also removed after the
    // first call to testRequest, and expected to be returned from the cache on
    // the second call.
    TestResource resource1 = new TestResource("1");
    factory.addResource(resource1.getContent(), resource1);
    assertSame(resource1, testRequest(resource1.getContent(), factory, cachedFactory));
    assertSame(resource1, testRequest(resource1.getContent(), factory, cachedFactory));

    // Expect a cached resource to be replaced if it's invalid
    resource0.setValid(false);
    TestResource newResource0 = new TestResource("new 0");
    factory.addResource(newResource0.getContent(), newResource0);
    assertSame(newResource0, testRequest(newResource0.getContent(), factory, cachedFactory));

    // Cache the maximum number of resources, and expect the least recently used
    // resource to be evicted. There are currently two resources in the cache,
    // and resource1 is the least recently used.
    int cacheSize = Integer.getInteger("oracle.jdbc.provider.CACHE_SIZE", 16);
    for (int i = 2; i < cacheSize + 1; i++) {
      TestResource testResource = new TestResource(String.valueOf(i));
      factory.addResource(testResource.getContent(), testResource);
      assertSame(testResource, testRequest(testResource.getContent(), factory, cachedFactory));
      assertSame(testResource, testRequest(testResource.getContent(), factory, cachedFactory));
    }

    // Expect newResource0 to not be evicted
    assertSame(newResource0, testRequest(newResource0.getContent(), factory, cachedFactory));

    // Expect resource1 to have been evicted
    TestResource newResource1 = new TestResource("new 1");
    factory.addResource(newResource1.getContent(), newResource1);
    assertSame(newResource1, testRequest(newResource1.getContent(), factory, cachedFactory));
  }

  /** Verifies behavior with multiple threads */
  @Test
  public void testMultipleThreads() {

    TestResourceFactory<String> factory =
      new TestResourceFactory<>();

    ResourceFactory<String> cachedFactory =
      CachedResourceFactory.create(factory);

    // Add a resource
    TestResource resource0 = new TestResource("0");
    ParameterSet parameterSet0 = ParameterSet.builder()
      .add("id", ID, resource0.getContent())
      .build();
    factory.addResource(resource0.getContent(), resource0);

    // Request the same resource on multiple threads.
    runMultipleThreads(16, 10,
      () -> assertSame(resource0, cachedFactory.request(parameterSet0)));

    // Expect a single call to request the resource
    assertEquals(1, factory.getRequestCount());

    // Add another resource
    TestResource resource1 = new TestResource("1");
    ParameterSet parameterSet1 = ParameterSet.builder()
      .add("id", ID, resource1.getContent())
      .build();
    factory.addResource(resource1.getContent(), resource1);

    // Request the other resource on multiple threads
    runMultipleThreads(16, 10,
      () -> assertSame(resource1, cachedFactory.request(parameterSet1)));

    // Expect a single call to request the other resource
    assertEquals(2, factory.getRequestCount());

    // Invalidate the first resource, and replace it with a new one
    resource0.setValid(false);
    TestResource newResource0 = new TestResource("new 0");
    factory.addResource(resource0.getContent(), newResource0);

    // Request the new resource on multiple threads
    runMultipleThreads(16, 10, () ->
      assertSame(newResource0, cachedFactory.request(parameterSet0)));

    // Expect a single call to request the new resource
    assertEquals(3, factory.getRequestCount());

    // Cache the maximum number of resources, and expect the least recently used
    // resource to be evicted. There are currently two resources in the cache,
    // and resource1 is the least recently used.
    int cacheSize = Integer.getInteger("oracle.jdbc.provider.CACHE_SIZE", 16);

    List<TestResource> newResources =
      IntStream.range(2, cacheSize + 1)
        .mapToObj(String::valueOf)
        .map(TestResource::new)
        .peek(newResource ->
          factory.addResource(newResource.getContent(), newResource))
        .collect(Collectors.toList());

    // Create cache entries concurrently
    ConcurrentLinkedQueue<TestResource> resourceQueue =
      new ConcurrentLinkedQueue<>(newResources);

    runMultipleThreads(resourceQueue.size(), 1, () -> {
        TestResource resource = resourceQueue.remove();
        assertSame(
          resource,
          testRequest(resource.getContent(), factory, cachedFactory));
      });

    // Expect a single call to request each newly cached resource
    assertEquals(3 + newResources.size(), factory.getRequestCount());

    // Expect newResource0 to not be evicted
    factory.removeResource(newResource0.getContent());
    assertSame(newResource0, cachedFactory.request(parameterSet0));

    // Expect all newly cached resources to remain cached
    for (TestResource newResource : newResources)
      assertSame(
        newResource,
        testRequest(newResource.getContent(), factory, cachedFactory));

    // Expect resource1 to have been evicted
    TestResource newResource1 = new TestResource("new 1");
    factory.addResource(resource1.getContent(), newResource1);
    assertSame(
      newResource1,
      testRequest(resource1.getContent(), factory, cachedFactory));
  }

  /**
   * Invokes {@link ResourceFactory#request(ParameterSet)} with a
   * {@link ParameterSet} having {@link TestResourceFactory#ID} set to the given
   * {@code id}. The resource is then removed by calling
   * {@link TestResourceFactory#removeResource(String)} with the given
   * {@code id}. It is expected that the cache will continue to return the
   * removed resource until it is no longer valid, or it is evicted as the least
   * recently used resource.
   */
  private <T> Resource<T> testRequest(
    String id, TestResourceFactory<T> testResourceFactory,
    ResourceFactory<T> cachedResourceFactory) {

    ParameterSet parameterSet =
      ParameterSet.builder()
        .add("id", ID, id)
        .build();

    Resource<T> resource = cachedResourceFactory.request(parameterSet);
    testResourceFactory.removeResource(id);

    return resource;
  }

  private static void runMultipleThreads(
    int threadCount, int iterations, Runnable task) {
    Thread[] threads = new Thread[threadCount];

    for (int i = 0; i < threads.length; i++) {
      threads[i] = new Thread(() -> {
        for (int j = 0; j < iterations; j++)
          task.run();
      });
    }

    for (Thread thread : threads)
      thread.start();

    try {
      for (Thread thread : threads)
        thread.join(30_000);
    }
    catch (InterruptedException interruptedException) {
      for (Thread thread : threads)
        thread.interrupt();

      fail(interruptedException);
    }
  }

}
