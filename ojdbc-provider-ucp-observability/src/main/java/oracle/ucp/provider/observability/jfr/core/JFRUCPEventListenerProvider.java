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

package oracle.ucp.provider.observability.jfr.core;

import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.events.core.UCPEventListener;
import oracle.ucp.events.core.UCPEventListenerProvider;

import java.util.Map;

/**
 * Provider that supplies a UCP event listener for recording JFR events.
 * Integrates UCP events with Java Flight Recorder for low-overhead monitoring.
 */
public final class JFRUCPEventListenerProvider
  implements UCPEventListenerProvider {

  private final UCPEventListener listener;


  /**
   * Singleton listener that records UCP events as JFR events.
   * Thread-safe and optimized for minimal overhead.
   */
  public static final UCPEventListener TRACE_EVENT_LISTENER =
    new UCPEventListener() {
      private static final long serialVersionUID = 1L;

      @Override
      public void onUCPEvent(EventType eventType, UCPEventContext context) {
        UCPEventFactory.recordEvent(eventType, context);
      }
    };

  /**
   * Creates a new provider instance.
   */
  public JFRUCPEventListenerProvider() {
    this.listener = TRACE_EVENT_LISTENER;
  }

  /**
   * Returns the provider's unique identifier.
   *
   * @return "jfr-ucp-listener"
   */
  @Override
  public String getName() {
    return "jfr-ucp-listener";
  }

  /**
   * Returns the JFR recording listener instance.
   *
   * @param config configuration map (ignored)
   * @return the JFR event listener
   */
  @Override
  public UCPEventListener getListener(Map<String, String> config) {
    return listener;
  }
}