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
    when(mockContext.totalConnections()).thenReturn(8);
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
      UCPEventListener listener = provider.getListener(Collections.emptyMap());
      assertNotNull(listener);
    }

    @Test
    @DisplayName("getListener() returns a non-null listener when config is null")
    void testGetListenerWithNullConfig() {
      UCPEventListener listener = provider.getListener(null);
      assertNotNull(listener);
    }

    @Test
    @DisplayName("getListener() returns the same instance on repeated calls (singleton)")
    void testGetListenerReturnsSameInstance() {
      UCPEventListener listener1 = provider.getListener(Collections.emptyMap());
      UCPEventListener listener2 = provider.getListener(Collections.emptyMap());
      assertSame(listener1, listener2);
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
  }

  @Nested
  @DisplayName("UCPEventListener — onUCPEvent robustness")
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
      when(zeroContext.poolName()).thenReturn("");
      when(zeroContext.borrowedConnectionsCount()).thenReturn(0);
      when(zeroContext.availableConnectionsCount()).thenReturn(0);
      when(zeroContext.totalConnections()).thenReturn(0);
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
  @DisplayName("UCPEventListener — histogram fields read on every event")
  class ListenerHistogramFieldTests {

    private UCPEventListener listener;

    @BeforeEach
    void setUp() {
      listener = new OtelUCPEventListenerProvider()
          .getListener(Collections.emptyMap());
    }

    @Test
    @DisplayName("poolName() is always read — used as metric attribute on every event")
    void testPoolNameAlwaysRead() {
      listener.onUCPEvent(EventType.POOL_CREATED, mockContext);
      verify(mockContext, atLeastOnce()).poolName();
    }

    @Test
    @DisplayName("All 8 histogram fields are read on CONNECTION_BORROWED")
    void testAllHistogramFieldsReadOnBorrow() {
      listener.onUCPEvent(EventType.CONNECTION_BORROWED, mockContext);
      verify(mockContext).borrowedConnectionsCount();
      verify(mockContext).availableConnectionsCount();
      verify(mockContext).totalConnections();
      verify(mockContext).createdConnections();
      verify(mockContext).closedConnections();
      verify(mockContext).getAverageConnectionWaitTime();
      verify(mockContext).maxPoolSize();
      verify(mockContext).minPoolSize();
    }

    @Test
    @DisplayName("All 8 histogram fields are read on POOL_CREATED")
    void testAllHistogramFieldsReadOnPoolCreated() {
      listener.onUCPEvent(EventType.POOL_CREATED, mockContext);
      verify(mockContext).borrowedConnectionsCount();
      verify(mockContext).availableConnectionsCount();
      verify(mockContext).totalConnections();
      verify(mockContext).createdConnections();
      verify(mockContext).closedConnections();
      verify(mockContext).getAverageConnectionWaitTime();
      verify(mockContext).maxPoolSize();
      verify(mockContext).minPoolSize();
    }

    @Test
    @DisplayName("All 8 histogram fields are read on POOL_DESTROYED")
    void testAllHistogramFieldsReadOnPoolDestroyed() {
      listener.onUCPEvent(EventType.POOL_DESTROYED, mockContext);
      verify(mockContext).borrowedConnectionsCount();
      verify(mockContext).availableConnectionsCount();
      verify(mockContext).totalConnections();
      verify(mockContext).createdConnections();
      verify(mockContext).closedConnections();
      verify(mockContext).getAverageConnectionWaitTime();
      verify(mockContext).maxPoolSize();
      verify(mockContext).minPoolSize();
    }

    @ParameterizedTest(name = "All histogram fields are read for EventType.{0}")
    @EnumSource(EventType.class)
    @DisplayName("All 8 histogram fields are read for every EventType")
    void testAllHistogramFieldsReadForAllEventTypes(EventType type) {
      listener.onUCPEvent(type, mockContext);
      verify(mockContext).borrowedConnectionsCount();
      verify(mockContext).availableConnectionsCount();
      verify(mockContext).totalConnections();
      verify(mockContext).createdConnections();
      verify(mockContext).closedConnections();
      verify(mockContext).getAverageConnectionWaitTime();
      verify(mockContext).maxPoolSize();
      verify(mockContext).minPoolSize();
    }
  }


  @Nested
  @DisplayName("UCPEventListener — multiple pool independence")
  class ListenerMultiPoolTests {

    private UCPEventListener listener;

    @BeforeEach
    void setUp() {
      listener = new OtelUCPEventListenerProvider()
          .getListener(Collections.emptyMap());
    }

    @Test
    @DisplayName("Multiple pools can emit events independently without interference")
    void testMultiplePoolsIndependent() {
      UCPEventContext pool1Context = mock(UCPEventContext.class);
      when(pool1Context.poolName()).thenReturn("Pool1");
      when(pool1Context.borrowedConnectionsCount()).thenReturn(2);
      when(pool1Context.availableConnectionsCount()).thenReturn(3);
      when(pool1Context.totalConnections()).thenReturn(5);
      when(pool1Context.getAverageConnectionWaitTime()).thenReturn(10L);
      when(pool1Context.maxPoolSize()).thenReturn(10);
      when(pool1Context.minPoolSize()).thenReturn(1);
      when(pool1Context.createdConnections()).thenReturn(5);
      when(pool1Context.closedConnections()).thenReturn(0);

      UCPEventContext pool2Context = mock(UCPEventContext.class);
      when(pool2Context.poolName()).thenReturn("Pool2");
      when(pool2Context.borrowedConnectionsCount()).thenReturn(7);
      when(pool2Context.availableConnectionsCount()).thenReturn(1);
      when(pool2Context.totalConnections()).thenReturn(8);
      when(pool2Context.getAverageConnectionWaitTime()).thenReturn(50L);
      when(pool2Context.maxPoolSize()).thenReturn(20);
      when(pool2Context.minPoolSize()).thenReturn(2);
      when(pool2Context.createdConnections()).thenReturn(8);
      when(pool2Context.closedConnections()).thenReturn(1);

      assertDoesNotThrow(() -> {
        listener.onUCPEvent(EventType.CONNECTION_BORROWED, pool1Context);
        listener.onUCPEvent(EventType.CONNECTION_BORROWED, pool2Context);
        listener.onUCPEvent(EventType.CONNECTION_RETURNED, pool1Context);
        listener.onUCPEvent(EventType.POOL_DESTROYED, pool2Context);
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