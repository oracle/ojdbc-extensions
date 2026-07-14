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
 ** one is included with the Software (each a "Larger Work" to which the
 ** Software is contributed by such licensors),
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
import oracle.ucp.events.core.UCPEventListenerProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Composite UCP Observability Event Listener Provider Tests")
class ObservabilityUCPEventListenerProviderTest {

  @Test
  @DisplayName("getName() returns the composite provider identifier")
  void testGetName() {
    ObservabilityUCPEventListenerProvider provider =
      new ObservabilityUCPEventListenerProvider();

    assertEquals("ucp-observability-listener", provider.getName());
  }

  @Test
  @DisplayName("getListener() returns the same composite listener instance")
  void testGetListenerReturnsSameInstance() {
    ObservabilityUCPEventListenerProvider provider =
      new ObservabilityUCPEventListenerProvider();

    UCPEventListener listener1 = provider.getListener(Collections.emptyMap());
    UCPEventListener listener2 = provider.getListener(null);

    assertNotNull(listener1);
    assertSame(listener1, listener2);
  }

  @Test
  @DisplayName("onUCPEvent() delegates the event to JFR and OTel listeners")
  void testOnUCPEventDelegatesToBothListeners() {
    UCPEventListener jfr = mock(UCPEventListener.class);
    UCPEventListener otel = mock(UCPEventListener.class);
    UCPEventContext ctx = mock(UCPEventContext.class);

    ObservabilityUCPEventListenerProvider provider =
      new ObservabilityUCPEventListenerProvider(
        providerReturning(jfr), providerReturning(otel));

    provider.getListener(null).onUCPEvent(EventType.POOL_REFRESHED, ctx);

    verify(jfr).onUCPEvent(EventType.POOL_REFRESHED, ctx);
    verify(otel).onUCPEvent(EventType.POOL_REFRESHED, ctx);
  }

  @Test
  @DisplayName("Composite listener throws NotSerializableException on serialization attempt")
  void testListenerIsNotSerializable() {
    ObservabilityUCPEventListenerProvider provider =
      new ObservabilityUCPEventListenerProvider();

    UCPEventListener listener = provider.getListener(Collections.emptyMap());

    assertThrows(NotSerializableException.class, () ->
      new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(listener));
  }

  private static UCPEventListenerProvider providerReturning(
      UCPEventListener listener) {
    UCPEventListenerProvider provider = mock(UCPEventListenerProvider.class);
    when(provider.getListener(any())).thenReturn(listener);
    return provider;
  }
}
