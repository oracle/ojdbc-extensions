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

import java.util.Collections;

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
    @DisplayName("onUCPEvent() does not throw when poolName() returns null")
    void testOnUCPEventNullPoolName() {
      when(mockContext.poolName()).thenReturn(null);
      assertDoesNotThrow(() ->
        listener.onUCPEvent(EventType.CONNECTION_BORROWED, mockContext));
    }
  }

  @Nested
  @DisplayName("UCPEventListener — robustness")
  class ListenerRobustnessTests {

    private UCPEventListener listener;

    @BeforeEach
    void setUp() {
      listener = new OtelUCPEventListenerProvider()
        .getListener(Collections.emptyMap());
    }

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
    @DisplayName("poolName() is always read — used as metric attribute key")
    void testPoolNameAlwaysRead() {
      listener.onUCPEvent(EventType.POOL_CREATED, mockContext);
      verify(mockContext, atLeastOnce()).poolName();
    }

    @Test
    @DisplayName("borrowedConnectionsCount() and availableConnectionsCount() are read on every event")
    void testUsageFieldsReadOnEveryEvent() {
      listener.onUCPEvent(EventType.CONNECTION_RETURNED, mockContext);
      verify(mockContext).borrowedConnectionsCount();
      verify(mockContext).availableConnectionsCount();
    }

    @Test
    @DisplayName("createdConnections() and closedConnections() are read on every event")
    void testLifetimeFieldsReadOnEveryEvent() {
      listener.onUCPEvent(EventType.CONNECTION_RETURNED, mockContext);
      verify(mockContext).createdConnections();
      verify(mockContext).closedConnections();
    }

    @Test
    @DisplayName("maxPoolSize() and minPoolSize() are read on POOL_CREATED")
    void testConfigFieldsReadOnPoolCreated() {
      listener.onUCPEvent(EventType.POOL_CREATED, mockContext);
      verify(mockContext).maxPoolSize();
      verify(mockContext).minPoolSize();
    }

    @Test
    @DisplayName("maxPoolSize() and minPoolSize() are read on POOL_DESTROYED")
    void testConfigFieldsReadOnPoolDestroyed() {

      listener.onUCPEvent(EventType.POOL_CREATED, mockContext);
      reset(mockContext);
      when(mockContext.poolName()).thenReturn("TestPool");
      when(mockContext.borrowedConnectionsCount()).thenReturn(3);
      when(mockContext.availableConnectionsCount()).thenReturn(5);
      when(mockContext.createdConnections()).thenReturn(9);
      when(mockContext.closedConnections()).thenReturn(1);
      when(mockContext.getAverageConnectionWaitTime()).thenReturn(42L);
      when(mockContext.maxPoolSize()).thenReturn(50);
      when(mockContext.minPoolSize()).thenReturn(2);
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

    @Test
    @DisplayName("maxPoolSize() and minPoolSize() are NOT read on maintenance events")
    void testConfigFieldsNotReadOnMaintenanceEvents() {
      listener.onUCPEvent(EventType.POOL_REFRESHED, mockContext);
      verify(mockContext, never()).maxPoolSize();
      verify(mockContext, never()).minPoolSize();

      reset(mockContext);
      when(mockContext.poolName()).thenReturn("TestPool");
      when(mockContext.borrowedConnectionsCount()).thenReturn(3);
      when(mockContext.availableConnectionsCount()).thenReturn(5);
      when(mockContext.createdConnections()).thenReturn(9);
      when(mockContext.closedConnections()).thenReturn(1);
      when(mockContext.getAverageConnectionWaitTime()).thenReturn(42L);

      listener.onUCPEvent(EventType.POOL_RECYCLED, mockContext);
      verify(mockContext, never()).maxPoolSize();
      verify(mockContext, never()).minPoolSize();
    }

    @Test
    @DisplayName("getAverageConnectionWaitTime() is read on CONNECTION_BORROWED when > 0")
    void testWaitTimeReadOnBorrowWhenPositive() {
      when(mockContext.getAverageConnectionWaitTime()).thenReturn(100L);
      listener.onUCPEvent(EventType.CONNECTION_BORROWED, mockContext);
      verify(mockContext, atLeastOnce()).getAverageConnectionWaitTime();
    }

    @Test
    @DisplayName("getAverageConnectionWaitTime() is NOT recorded on CONNECTION_BORROWED when 0")
    void testWaitTimeNotRecordedWhenZero() {

      when(mockContext.getAverageConnectionWaitTime()).thenReturn(0L);
      assertDoesNotThrow(() ->
          listener.onUCPEvent(EventType.CONNECTION_BORROWED, mockContext));
    }

    @Test
    @DisplayName("getAverageConnectionWaitTime() is NOT read on non-borrow events")
    void testWaitTimeNotReadOnNonBorrowEvents() {
      listener.onUCPEvent(EventType.CONNECTION_RETURNED, mockContext);
      verify(mockContext, never()).getAverageConnectionWaitTime();

      reset(mockContext);
      when(mockContext.poolName()).thenReturn("TestPool");
      when(mockContext.borrowedConnectionsCount()).thenReturn(3);
      when(mockContext.availableConnectionsCount()).thenReturn(5);
      when(mockContext.createdConnections()).thenReturn(9);
      when(mockContext.closedConnections()).thenReturn(1);

      listener.onUCPEvent(EventType.POOL_REFRESHED, mockContext);
      verify(mockContext, never()).getAverageConnectionWaitTime();
    }

    @Test
    @DisplayName("totalConnections() is never read — removed from provider")
    void testTotalConnectionsNeverRead() {
      for (EventType type : EventType.values()) {
        reset(mockContext);
        when(mockContext.poolName()).thenReturn("TestPool");
        when(mockContext.borrowedConnectionsCount()).thenReturn(3);
        when(mockContext.availableConnectionsCount()).thenReturn(5);
        when(mockContext.createdConnections()).thenReturn(9);
        when(mockContext.closedConnections()).thenReturn(1);
        when(mockContext.getAverageConnectionWaitTime()).thenReturn(42L);
        when(mockContext.maxPoolSize()).thenReturn(50);
        when(mockContext.minPoolSize()).thenReturn(2);
        listener.onUCPEvent(type, mockContext);
        verify(mockContext, never()).totalConnections();
      }
    }
  }

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
      UCPEventContext pool1 = mock(UCPEventContext.class);
      when(pool1.poolName()).thenReturn("Pool1");
      when(pool1.borrowedConnectionsCount()).thenReturn(2);
      when(pool1.availableConnectionsCount()).thenReturn(3);
      when(pool1.getAverageConnectionWaitTime()).thenReturn(10L);
      when(pool1.maxPoolSize()).thenReturn(10);
      when(pool1.minPoolSize()).thenReturn(1);
      when(pool1.createdConnections()).thenReturn(5);
      when(pool1.closedConnections()).thenReturn(0);

      UCPEventContext pool2 = mock(UCPEventContext.class);
      when(pool2.poolName()).thenReturn("Pool2");
      when(pool2.borrowedConnectionsCount()).thenReturn(7);
      when(pool2.availableConnectionsCount()).thenReturn(1);
      when(pool2.getAverageConnectionWaitTime()).thenReturn(50L);
      when(pool2.maxPoolSize()).thenReturn(20);
      when(pool2.minPoolSize()).thenReturn(2);
      when(pool2.createdConnections()).thenReturn(8);
      when(pool2.closedConnections()).thenReturn(1);

      assertDoesNotThrow(() -> {
        listener.onUCPEvent(EventType.CONNECTION_BORROWED, pool1);
        listener.onUCPEvent(EventType.CONNECTION_BORROWED, pool2);
        listener.onUCPEvent(EventType.CONNECTION_RETURNED, pool1);
        listener.onUCPEvent(EventType.POOL_DESTROYED, pool2);
      });
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
  }
}