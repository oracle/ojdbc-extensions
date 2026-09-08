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
import oracle.ucp.events.core.UCPEventListenerProvider;
import oracle.ucp.provider.observability.jfr.core.JFRUCPEventListenerProvider;
import oracle.ucp.provider.observability.otel.OtelUCPEventListenerProvider;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Objects;

/**
 * Composite UCP provider that publishes events to both JFR and OpenTelemetry.
 *
 * <p>Use {@code ucp-observability-listener} when both backends should receive
 * the same UCP event stream. Use {@code jfr-ucp-listener} or
 * {@code otel-ucp-listener} when only one backend is needed.
 */
public final class ObservabilityUCPEventListenerProvider
  implements UCPEventListenerProvider {

  private static final String PROVIDER_NAME = "ucp-observability-listener";

  private final Object listenerLock = new Object();

  private final UCPEventListenerProvider jfrProvider;
  private final UCPEventListenerProvider otelProvider;
  private volatile UCPEventListener listener;

  /**
   * Creates a provider that delegates to the built-in JFR and OpenTelemetry
   * UCP listeners.
   */
  public ObservabilityUCPEventListenerProvider() {
    this(
      new JFRUCPEventListenerProvider(),
      new OtelUCPEventListenerProvider());
  }

  // Package-private for tests that inject delegate providers.
  ObservabilityUCPEventListenerProvider(
      UCPEventListenerProvider jfrProvider,
      UCPEventListenerProvider otelProvider) {
    this.jfrProvider = Objects.requireNonNull(
      jfrProvider, "jfrProvider cannot be null");
    this.otelProvider = Objects.requireNonNull(
      otelProvider, "otelProvider cannot be null");
  }

  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  @Override
  public UCPEventListener getListener(Map<String, String> config) {
    if (listener == null) {
      synchronized (listenerLock) {
        if (listener == null) {
          listener = new CompositeUCPEventListener(
            jfrProvider.getListener(config),
            otelProvider.getListener(config));
        }
      }
    }
    return listener;
  }

  private static final class CompositeUCPEventListener
    implements UCPEventListener {

    private static final long serialVersionUID = 1L;

    private final UCPEventListener jfrListener;
    private final UCPEventListener otelListener;

    private void writeObject(ObjectOutputStream ignored) throws IOException {
      throw new NotSerializableException(
        "Composite UCP event listener cannot be serialized.");
    }

    private CompositeUCPEventListener(
        UCPEventListener jfrListener,
        UCPEventListener otelListener) {
      this.jfrListener = Objects.requireNonNull(
        jfrListener, "jfrListener cannot be null");
      this.otelListener = Objects.requireNonNull(
        otelListener, "otelListener cannot be null");
    }

    @Override
    public boolean isDesiredEvent(EventType eventType) {
      return jfrListener.isDesiredEvent(eventType)
        || otelListener.isDesiredEvent(eventType);
    }

    @Override
    public void onUCPEvent(EventType eventType, UCPEventContext ctx) {
      jfrListener.onUCPEvent(eventType, ctx);
      otelListener.onUCPEvent(eventType, ctx);
    }
  }
}
