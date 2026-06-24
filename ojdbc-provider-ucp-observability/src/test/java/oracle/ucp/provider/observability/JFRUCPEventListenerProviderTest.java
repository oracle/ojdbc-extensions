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

    @Test
    @DisplayName("recordEvent() does not throw when poolName is null")
    void testRecordEventWithNullPoolName() {
      UCPEventContext nullPoolNameContext = mock(UCPEventContext.class);
      when(nullPoolNameContext.poolName()).thenReturn(null);
      when(nullPoolNameContext.timestamp()).thenReturn(0L);
      when(nullPoolNameContext.maxPoolSize()).thenReturn(0);
      when(nullPoolNameContext.minPoolSize()).thenReturn(0);
      when(nullPoolNameContext.borrowedConnectionsCount()).thenReturn(0);
      when(nullPoolNameContext.availableConnectionsCount()).thenReturn(0);
      when(nullPoolNameContext.totalConnections()).thenReturn(0);
      when(nullPoolNameContext.closedConnections()).thenReturn(0);
      when(nullPoolNameContext.createdConnections()).thenReturn(0);
      when(nullPoolNameContext.getAverageConnectionWaitTime()).thenReturn(0L);

      assertDoesNotThrow(() ->
        UCPEventFactory.recordEvent(EventType.CONNECTION_BORROWED, nullPoolNameContext));
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

    @ParameterizedTest(name = "onUCPEvent() reads all context fields for EventType.{0}")
    @EnumSource(value = EventType.class, names = {
      "POOL_CREATED", "CONNECTION_BORROWED", "POOL_REFRESHED"
    })
    @DisplayName("onUCPEvent() reads all context fields for each event category")
    void testOnUCPEventReadsAllContextFields(EventType type) {
      JFRUCPEventListenerProvider provider = new JFRUCPEventListenerProvider();
      UCPEventListener listener = provider.getListener(Collections.emptyMap());

      listener.onUCPEvent(type, mockContext);

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