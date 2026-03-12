package oracle.ucp.provider.observability;

import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.events.core.UCPEventListener;
import oracle.ucp.events.core.UCPEventListener.EventType;
import oracle.ucp.provider.observability.jfr.core.JFRUCPEventListenerProvider;
import oracle.ucp.provider.observability.jfr.core.UCPEventFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("JFR UCP Event Listener Provider Tests")
class JFRUCPEventListenerProviderTest {

  private UCPEventContext mockContext;

  @BeforeEach
  void setUpMockContext() {
    mockContext = mock(UCPEventContext.class);
    when(mockContext.poolName()).thenReturn("TestPool");
    when(mockContext.timestamp()).thenReturn(123456789L);
    when(mockContext.maxPoolSize()).thenReturn(50);
    when(mockContext.minPoolSize()).thenReturn(2);
    when(mockContext.borrowedConnectionsCount()).thenReturn(3);
    when(mockContext.availableConnectionsCount()).thenReturn(5);
    when(mockContext.totalConnections()).thenReturn(8);
    when(mockContext.closedConnections()).thenReturn(1);
    when(mockContext.createdConnections()).thenReturn(9);
    when(mockContext.getAverageConnectionWaitTime()).thenReturn(10L);
  }

  @Nested
  @DisplayName("JFRUCPEventListenerProvider")
  class ProviderTests {

    private JFRUCPEventListenerProvider provider;

    @BeforeEach
    void setUp() {
      provider = new JFRUCPEventListenerProvider();
    }

    @Test
    @DisplayName("getName() returns the expected provider identifier")
    void testGetName() {
      assertEquals("jfr-ucp-listener", provider.getName());
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
    @DisplayName("isDesiredEvent() returns true for all event types by default")
    void testIsDesiredEventReturnsTrueForAll() {
      UCPEventListener listener = provider.getListener(Collections.emptyMap());
      for (EventType type : EventType.values()) {
        assertTrue(listener.isDesiredEvent(type),
            "Expected isDesiredEvent to return true for: " + type);
      }
    }
  }


  @Nested
  @DisplayName("UCPEventFactory — null safety")
  class EventFactoryNullSafetyTests {

    @Test
    @DisplayName("recordEvent() throws NullPointerException when EventType is null")
    void testRecordEventNullType() {
      assertThrows(NullPointerException.class, () ->
          UCPEventFactory.recordEvent(null, mockContext));
    }

    @Test
    @DisplayName("recordEvent() throws NullPointerException when context is null")
    void testRecordEventNullContext() {
      assertThrows(NullPointerException.class, () ->
          UCPEventFactory.recordEvent(EventType.CONNECTION_BORROWED, null));
    }
  }

  @Nested
  @DisplayName("UCPEventFactory — event recording")
  class EventFactoryRecordingTests {

    @ParameterizedTest(name = "recordEvent() does not throw for EventType.{0}")
    @EnumSource(EventType.class)
    @DisplayName("recordEvent() does not throw for any known EventType")
    void testRecordEventDoesNotThrowForAllTypes(EventType type) {
      assertDoesNotThrow(() -> UCPEventFactory.recordEvent(type, mockContext));
    }

    @Test
    @DisplayName("recordEvent() does not throw when context returns zero/default values")
    void testRecordEventWithZeroValues() {
      UCPEventContext zeroContext = mock(UCPEventContext.class);
      when(zeroContext.poolName()).thenReturn("");
      when(zeroContext.timestamp()).thenReturn(0L);
      when(zeroContext.maxPoolSize()).thenReturn(0);
      when(zeroContext.minPoolSize()).thenReturn(0);
      when(zeroContext.borrowedConnectionsCount()).thenReturn(0);
      when(zeroContext.availableConnectionsCount()).thenReturn(0);
      when(zeroContext.totalConnections()).thenReturn(0);
      when(zeroContext.closedConnections()).thenReturn(0);
      when(zeroContext.createdConnections()).thenReturn(0);
      when(zeroContext.getAverageConnectionWaitTime()).thenReturn(0L);

      assertDoesNotThrow(() ->
          UCPEventFactory.recordEvent(EventType.CONNECTION_BORROWED, zeroContext));
    }
  }

  @Nested
  @DisplayName("UCPEventListener — onUCPEvent robustness")
  class ListenerOnEventTests {

    @ParameterizedTest(name = "onUCPEvent() does not throw for EventType.{0}")
    @EnumSource(EventType.class)
    @DisplayName("onUCPEvent() does not throw for any known EventType")
    void testOnUCPEventDoesNotThrowForAllTypes(EventType type) {
      JFRUCPEventListenerProvider provider = new JFRUCPEventListenerProvider();
      UCPEventListener listener = provider.getListener(Collections.emptyMap());
      assertDoesNotThrow(() -> listener.onUCPEvent(type, mockContext));
    }

    @Test
    @DisplayName("onUCPEvent() reads all context fields during event creation")
    void testOnUCPEventReadsAllContextFields() {
      JFRUCPEventListenerProvider provider = new JFRUCPEventListenerProvider();
      UCPEventListener listener = provider.getListener(Collections.emptyMap());

      listener.onUCPEvent(EventType.CONNECTION_BORROWED, mockContext);

      verify(mockContext).poolName();
      verify(mockContext).timestamp();
      verify(mockContext).maxPoolSize();
      verify(mockContext).minPoolSize();
      verify(mockContext).borrowedConnectionsCount();
      verify(mockContext).availableConnectionsCount();
      verify(mockContext).totalConnections();
      verify(mockContext).closedConnections();
      verify(mockContext).createdConnections();
      verify(mockContext).getAverageConnectionWaitTime();
    }
  }
}