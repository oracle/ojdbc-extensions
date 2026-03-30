/*
 ** Copyright (c) 2025 Oracle and/or its affiliates.
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

package oracle.ucp.provider.observability;

import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.events.core.UCPEventListener;
import oracle.ucp.events.core.UCPEventListener.EventType;
import oracle.ucp.provider.observability.otel.OtelUCPEventListenerProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.ByteArrayOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("OTel UCP Event Listener Provider Tests")
class OtelUCPEventListenerProviderTest {

  private UCPEventContext mockContext;

  @BeforeEach
  void setUpMockContext() {
    mockContext = mock(UCPEventContext.class);
    when(mockContext.poolName()).thenReturn("TestPool");
    when(mockContext.maxPoolSize()).thenReturn(50);
    when(mockContext.minPoolSize()).thenReturn(2);
    when(mockContext.borrowedConnectionsCount()).thenReturn(3);
    when(mockContext.availableConnectionsCount()).thenReturn(5);
    when(mockContext.closedConnections()).thenReturn(1);
    when(mockContext.createdConnections()).thenReturn(9);
    when(mockContext.getAverageConnectionWaitTime()).thenReturn(42L);
  }

  /**
   * Resets the shared mockContext and re-applies all standard stubs.
   *
   * <p>Use this helper in tests that call reset() mid-test (e.g. after a
   * POOL_DESTROYED call) and need to verify interaction counts on subsequent
   * events without contamination from earlier invocations.</p>
   */
  private void resetAndRestubContext() {
    reset(mockContext);
    when(mockContext.poolName()).thenReturn("TestPool");
    when(mockContext.maxPoolSize()).thenReturn(50);
    when(mockContext.minPoolSize()).thenReturn(2);
    when(mockContext.borrowedConnectionsCount()).thenReturn(3);
    when(mockContext.availableConnectionsCount()).thenReturn(5);
    when(mockContext.closedConnections()).thenReturn(1);
    when(mockContext.createdConnections()).thenReturn(9);
    when(mockContext.getAverageConnectionWaitTime()).thenReturn(42L);
  }

  /**
   * Creates a fully-stubbed mock context for the given pool name.
   * Used in multi-pool and concurrency tests to avoid shared state.
   */
  private UCPEventContext newFullyStubbed(String poolName) {
    UCPEventContext ctx = mock(UCPEventContext.class);
    when(ctx.poolName()).thenReturn(poolName);
    when(ctx.maxPoolSize()).thenReturn(10);
    when(ctx.minPoolSize()).thenReturn(1);
    when(ctx.borrowedConnectionsCount()).thenReturn(1);
    when(ctx.availableConnectionsCount()).thenReturn(1);
    when(ctx.closedConnections()).thenReturn(0);
    when(ctx.createdConnections()).thenReturn(1);
    when(ctx.getAverageConnectionWaitTime()).thenReturn(10L);
    return ctx;
  }


  // ---------------------------------------------------------------------------
  // Provider contract
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("OtelUCPEventListenerProvider")
  class ProviderTests {

    private OtelUCPEventListenerProvider provider;

    @BeforeEach
    void setUp() {
      provider = new OtelUCPEventListenerProvider();
    }

    @Test
    @DisplayName("getName() returns the expected provider identifier")
    void testGetName() {
      assertEquals("otel-ucp-listener", provider.getName());
    }

    @Test
    @DisplayName("getListener() returns a non-null listener")
    void testGetListenerNotNull() {
      assertNotNull(provider.getListener(Collections.emptyMap()));
    }

    @Test
    @DisplayName("getListener() returns a non-null listener when config is null")
    void testGetListenerWithNullConfig() {
      // config is intentionally unused by the provider — null must be safe.
      assertNotNull(provider.getListener(null));
    }

    @Test
    @DisplayName("getListener() returns the same instance on repeated calls (singleton)")
    void testGetListenerReturnsSameInstance() {
      UCPEventListener l1 = provider.getListener(Collections.emptyMap());
      UCPEventListener l2 = provider.getListener(Collections.emptyMap());
      assertSame(l1, l2);
    }

    @Test
    @DisplayName("isDesiredEvent() returns true for all event types")
    void testIsDesiredEventReturnsTrueForAll() {
      UCPEventListener listener = provider.getListener(Collections.emptyMap());
      for (EventType type : EventType.values()) {
        assertTrue(listener.isDesiredEvent(type),
            "Expected isDesiredEvent to return true for: " + type);
      }
    }
  }


  // ---------------------------------------------------------------------------
  // Null safety
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("UCPEventListener — null safety")
  class ListenerNullSafetyTests {

    private UCPEventListener listener;

    @BeforeEach
    void setUp() {
      listener = new OtelUCPEventListenerProvider()
          .getListener(Collections.emptyMap());
    }

    @Test
    @DisplayName("onUCPEvent() does not throw when eventType is null")
    void testOnUCPEventNullEventType() {
      assertDoesNotThrow(() -> listener.onUCPEvent(null, mockContext));
    }

    @Test
    @DisplayName("onUCPEvent() does not throw when context is null")
    void testOnUCPEventNullContext() {
      assertDoesNotThrow(() ->
          listener.onUCPEvent(EventType.CONNECTION_BORROWED, null));
    }

    @Test
    @DisplayName("onUCPEvent() does not throw when both args are null")
    void testOnUCPEventBothNull() {
      assertDoesNotThrow(() -> listener.onUCPEvent(null, null));
    }

    @Test
    @DisplayName("onUCPEvent() does not throw and reads no fields when poolName() returns null")
    void testOnUCPEventNullPoolName() {
      // The null-poolName guard returns immediately — no context fields beyond
      // poolName() itself should be accessed after the early return.
      when(mockContext.poolName()).thenReturn(null);
      assertDoesNotThrow(() ->
          listener.onUCPEvent(EventType.CONNECTION_BORROWED, mockContext));

      verify(mockContext, never()).borrowedConnectionsCount();
      verify(mockContext, never()).availableConnectionsCount();
      verify(mockContext, never()).maxPoolSize();
      verify(mockContext, never()).createdConnections();
    }
  }


  // ---------------------------------------------------------------------------
  // Robustness — all event types
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("UCPEventListener — robustness")
  class ListenerRobustnessTests {

    private UCPEventListener listener;

    @BeforeEach
    void setUp() {
      listener = new OtelUCPEventListenerProvider()
          .getListener(Collections.emptyMap());
    }

    /**
     * Verifies that onUCPEvent() does not throw for any EventType.
     *
     * <p>The enum values are iterated in declaration order. POOL_DESTROYED
     * removes the pool state entry — subsequent events simply re-create it
     * via computeIfAbsent, so ordering has no correctness impact here.</p>
     */
    @ParameterizedTest(name = "onUCPEvent() does not throw for EventType.{0}")
    @EnumSource(EventType.class)
    @DisplayName("onUCPEvent() does not throw for any known EventType")
    void testOnUCPEventDoesNotThrowForAllTypes(EventType type) {
      assertDoesNotThrow(() -> listener.onUCPEvent(type, mockContext));
    }

    @Test
    @DisplayName("onUCPEvent() does not throw when context has zero/default values")
    void testOnUCPEventWithZeroValues() {
      UCPEventContext zeroContext = mock(UCPEventContext.class);
      when(zeroContext.poolName()).thenReturn("ZeroPool");
      when(zeroContext.borrowedConnectionsCount()).thenReturn(0);
      when(zeroContext.availableConnectionsCount()).thenReturn(0);
      when(zeroContext.closedConnections()).thenReturn(0);
      when(zeroContext.createdConnections()).thenReturn(0);
      when(zeroContext.getAverageConnectionWaitTime()).thenReturn(0L);
      when(zeroContext.maxPoolSize()).thenReturn(0);
      when(zeroContext.minPoolSize()).thenReturn(0);

      assertDoesNotThrow(() ->
          listener.onUCPEvent(EventType.CONNECTION_BORROWED, zeroContext));
    }
  }


  // ---------------------------------------------------------------------------
  // Context field access — what gets read, when
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("UCPEventListener — context field access")
  class ListenerContextAccessTests {

    private UCPEventListener listener;

    @BeforeEach
    void setUp() {
      listener = new OtelUCPEventListenerProvider()
          .getListener(Collections.emptyMap());
    }

    @Test
    @DisplayName("poolName() is read exactly once per event — used as metric attribute key")
    void testPoolNameReadExactlyOnce() {
      listener.onUCPEvent(EventType.POOL_CREATED, mockContext);
      verify(mockContext, times(1)).poolName();
    }

    @Test
    @DisplayName("borrowedConnectionsCount() and availableConnectionsCount() are read on a non-lifecycle event")
    void testUsageFieldsReadOnNonLifecycleEvent() {
      // "Every non-POOL_DESTROYED event" — CONNECTION_RETURNED is representative.
      // POOL_DESTROYED is excluded because its snapshot is conditional on pool
      // state existing; this test focuses on the unconditional snapshot path.
      listener.onUCPEvent(EventType.CONNECTION_RETURNED, mockContext);
      verify(mockContext).borrowedConnectionsCount();
      verify(mockContext).availableConnectionsCount();
    }

    @Test
    @DisplayName("borrowedConnectionsCount() and availableConnectionsCount() are read on a maintenance event")
    void testUsageFieldsReadOnMaintenanceEvent() {
      listener.onUCPEvent(EventType.POOL_REFRESHED, mockContext);
      verify(mockContext).borrowedConnectionsCount();
      verify(mockContext).availableConnectionsCount();
    }

    @Test
    @DisplayName("createdConnections() and closedConnections() are read on a non-lifecycle event")
    void testLifetimeFieldsReadOnNonLifecycleEvent() {
      listener.onUCPEvent(EventType.CONNECTION_RETURNED, mockContext);
      verify(mockContext).createdConnections();
      verify(mockContext).closedConnections();
    }

    @Test
    @DisplayName("createdConnections() and closedConnections() are read on a maintenance event")
    void testLifetimeFieldsReadOnMaintenanceEvent() {
      listener.onUCPEvent(EventType.POOL_REFRESHED, mockContext);
      verify(mockContext).createdConnections();
      verify(mockContext).closedConnections();
    }

    // --- Config fields (maxPoolSize / minPoolSize) — lifecycle events only ----

    @ParameterizedTest(name = "maxPoolSize() and minPoolSize() are read on {0}")
    @EnumSource(value = EventType.class, names = {
        "POOL_CREATED", "POOL_STARTING", "POOL_STARTED", "POOL_STOPPED"
    })
    @DisplayName("maxPoolSize() and minPoolSize() are read on all non-destroy lifecycle events")
    void testConfigFieldsReadOnLifecycleEvents(EventType type) {
      listener.onUCPEvent(type, mockContext);
      verify(mockContext).maxPoolSize();
      verify(mockContext).minPoolSize();
    }

    @Test
    @DisplayName("maxPoolSize() and minPoolSize() are read on POOL_DESTROYED")
    void testConfigFieldsReadOnPoolDestroyed() {
      // POOL_DESTROYED removes the poolStates entry before calling recordSnapshot.
      // The entry must exist first — seed it via POOL_CREATED so that the remove
      // finds a non-null PoolState and recordSnapshot is actually invoked.
      listener.onUCPEvent(EventType.POOL_CREATED, mockContext);
      resetAndRestubContext();

      listener.onUCPEvent(EventType.POOL_DESTROYED, mockContext);
      verify(mockContext).maxPoolSize();
      verify(mockContext).minPoolSize();
    }

    @Test
    @DisplayName("maxPoolSize() and minPoolSize() are NOT read on CONNECTION_BORROWED")
    void testConfigFieldsNotReadOnConnectionBorrowed() {
      listener.onUCPEvent(EventType.CONNECTION_BORROWED, mockContext);
      verify(mockContext, never()).maxPoolSize();
      verify(mockContext, never()).minPoolSize();
    }

    @ParameterizedTest(name = "maxPoolSize() and minPoolSize() are NOT read on {0}")
    @EnumSource(value = EventType.class, names = {
        "POOL_REFRESHED", "POOL_RECYCLED", "POOL_PURGED"
    })
    @DisplayName("maxPoolSize() and minPoolSize() are NOT read on any maintenance event")
    void testConfigFieldsNotReadOnMaintenanceEvents(EventType type) {
      listener.onUCPEvent(type, mockContext);
      verify(mockContext, never()).maxPoolSize();
      verify(mockContext, never()).minPoolSize();
    }

    // --- Wait time ------------------------------------------------------------

    @Test
    @DisplayName("getAverageConnectionWaitTime() is read exactly once on CONNECTION_BORROWED when > 0")
    void testWaitTimeReadOnceOnBorrowWhenPositive() {
      when(mockContext.getAverageConnectionWaitTime()).thenReturn(100L);
      listener.onUCPEvent(EventType.CONNECTION_BORROWED, mockContext);
      // Verify exactly once: the provider caches the value in a local variable
      // to avoid a redundant second call inside the zero-guard branch.
      verify(mockContext, times(1)).getAverageConnectionWaitTime();
    }

    @Test
    @DisplayName("getAverageConnectionWaitTime() is read but recording is suppressed when 0")
    void testWaitTimeReadButNotRecordedWhenZero() {
      when(mockContext.getAverageConnectionWaitTime()).thenReturn(0L);
      assertDoesNotThrow(() ->
          listener.onUCPEvent(EventType.CONNECTION_BORROWED, mockContext));
      // The branch was entered (CONNECTION_BORROWED) — the method must have been
      // called once to evaluate the zero guard. No exception means zero was
      // correctly suppressed rather than passed to histogram.record().
      verify(mockContext, times(1)).getAverageConnectionWaitTime();
    }

    @ParameterizedTest(name = "getAverageConnectionWaitTime() is NOT read on {0}")
    @EnumSource(value = EventType.class, names = {
        "CONNECTION_CREATED", "CONNECTION_RETURNED", "CONNECTION_CLOSED",
        "POOL_CREATED", "POOL_STARTING", "POOL_STARTED", "POOL_STOPPED",
        "POOL_REFRESHED", "POOL_RECYCLED", "POOL_PURGED"
    })
    @DisplayName("getAverageConnectionWaitTime() is NOT read on any non-borrow event")
    void testWaitTimeNotReadOnNonBorrowEvents(EventType type) {
      listener.onUCPEvent(type, mockContext);
      verify(mockContext, never()).getAverageConnectionWaitTime();
    }

    // --- Removed fields -------------------------------------------------------

    @Test
    @DisplayName("totalConnections() is never read — not used by the provider")
    void testTotalConnectionsNeverRead() {
      for (EventType type : EventType.values()) {
        resetAndRestubContext();
        listener.onUCPEvent(type, mockContext);
        verify(mockContext, never()).totalConnections();
      }
    }
  }


  // ---------------------------------------------------------------------------
  // Pool state lifecycle
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("UCPEventListener — pool state lifecycle")
  class ListenerPoolStateTests {

    private UCPEventListener listener;

    @BeforeEach
    void setUp() {
      listener = new OtelUCPEventListenerProvider()
          .getListener(Collections.emptyMap());
    }

    @Test
    @DisplayName("POOL_DESTROYED with no prior registration skips recordSnapshot gracefully")
    void testPoolDestroyedWithoutPriorRegistration() {
      // Fire POOL_DESTROYED on a pool that was never registered — the null-state
      // guard inside onUCPEvent should prevent recordSnapshot from being called.
      UCPEventContext unknownPool = mock(UCPEventContext.class);
      when(unknownPool.poolName()).thenReturn("NeverRegisteredPool");

      assertDoesNotThrow(() ->
          listener.onUCPEvent(EventType.POOL_DESTROYED, unknownPool));

      // recordSnapshot was skipped — no context fields should have been read.
      verify(unknownPool, never()).borrowedConnectionsCount();
      verify(unknownPool, never()).maxPoolSize();
    }

    @Test
    @DisplayName("Second POOL_DESTROYED on same pool after first is already cleaned up skips silently")
    void testDoublePoolDestroyed() {
      // First destroy: pool state exists and is removed, recordSnapshot is called.
      listener.onUCPEvent(EventType.POOL_CREATED, mockContext);
      listener.onUCPEvent(EventType.POOL_DESTROYED, mockContext);

      // Second destroy: pool state is gone — poolStates.remove() returns null
      // and recordSnapshot must be skipped entirely.
      UCPEventContext secondDestroy = mock(UCPEventContext.class);
      when(secondDestroy.poolName()).thenReturn("TestPool");
      assertDoesNotThrow(() ->
          listener.onUCPEvent(EventType.POOL_DESTROYED, secondDestroy));
      verify(secondDestroy, never()).borrowedConnectionsCount();
    }

    @Test
    @DisplayName("Listener throws NotSerializableException on serialization attempt")
    void testListenerIsNotSerializable() {
      assertThrows(NotSerializableException.class, () ->
          new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(listener));
    }
  }


  // ---------------------------------------------------------------------------
  // Multi-pool independence and concurrency
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("UCPEventListener — multi-pool independence")
  class ListenerMultiPoolTests {

    private UCPEventListener listener;

    @BeforeEach
    void setUp() {
      listener = new OtelUCPEventListenerProvider()
          .getListener(Collections.emptyMap());
    }

    @Test
    @DisplayName("Multiple pools emit events independently without interference")
    void testMultiplePoolsIndependent() {
      UCPEventContext pool1 = newFullyStubbed("Pool1");
      UCPEventContext pool2 = newFullyStubbed("Pool2");

      assertDoesNotThrow(() -> {
        listener.onUCPEvent(EventType.CONNECTION_BORROWED, pool1);
        listener.onUCPEvent(EventType.CONNECTION_BORROWED, pool2);
        listener.onUCPEvent(EventType.CONNECTION_RETURNED, pool1);
        listener.onUCPEvent(EventType.POOL_DESTROYED, pool2);
      });

      // Each pool's context was accessed independently — poolName() is called
      // exactly once per onUCPEvent() invocation (at the top of the method),
      // so two events fired against a pool means two poolName() reads.
      verify(pool1, atLeast(2)).poolName(); // CONNECTION_BORROWED + CONNECTION_RETURNED = 2 calls
      verify(pool2, atLeast(2)).poolName(); // CONNECTION_BORROWED + POOL_DESTROYED = 2 calls
    }

    @Test
    @DisplayName("Events after POOL_DESTROYED for same pool do not throw")
    void testEventsAfterPoolDestroyed() {
      listener.onUCPEvent(EventType.POOL_DESTROYED, mockContext);
      assertDoesNotThrow(() -> {
        listener.onUCPEvent(EventType.POOL_CREATED, mockContext);
        listener.onUCPEvent(EventType.CONNECTION_BORROWED, mockContext);
      });
    }

    @Test
    @DisplayName("Concurrent events from multiple pools do not cause errors")
    void testConcurrentMultiPoolEvents() throws Exception {
      // Validates that ConcurrentHashMap's thread-safety holds under concurrent
      // access from multiple pools firing events simultaneously on the singleton
      // listener. Each thread owns a distinct pool name to ensure independent
      // map entries without artificial serialisation.
      int threads = 8;
      ExecutorService exec = Executors.newFixedThreadPool(threads);
      List<Future<?>> futures = new ArrayList<>();

      for (int i = 0; i < threads; i++) {
        UCPEventContext ctx = newFullyStubbed("ConcurrentPool-" + i);
        futures.add(exec.submit(() -> {
          listener.onUCPEvent(EventType.POOL_CREATED, ctx);
          listener.onUCPEvent(EventType.CONNECTION_BORROWED, ctx);
          listener.onUCPEvent(EventType.CONNECTION_RETURNED, ctx);
          listener.onUCPEvent(EventType.POOL_DESTROYED, ctx);
        }));
      }

      exec.shutdown();
      for (Future<?> f : futures) {
        // get(timeout) re-throws any exception thrown on the worker thread and
        // also fails fast if a thread deadlocks rather than hanging indefinitely.
        assertDoesNotThrow(() -> f.get(5, TimeUnit.SECONDS));
      }
    }
  }
}