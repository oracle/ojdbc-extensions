package oracle.ucp.provider.observability.jfr.core;

import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.events.core.UCPEventListener;
import oracle.ucp.events.core.UCPEventListenerProvider;

import java.util.Map;

/**
 * A {@link UCPEventListenerProvider} implementation that records UCP events as
 * Java Flight Recorder (JFR) events for advanced monitoring and diagnostics.
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 * <li>Integrates with Java Flight Recorder for low-overhead event recording</li>
 * <li>Captures complete event context as JFR event attributes</li>
 * <li>Thread-safe singleton listener instance</li>
 * <li>Registered name: "jfr-ucp-listener"</li>
 * </ul>
 *
 * <p><b>Prerequisites:</b></p>
 * <ul>
 * <li>JFR must be enabled on the JVM (-XX:StartFlightRecording)</li>
 * <li>Requires JDK 7u4+ (with JFR support)</li>
 * </ul>
 *
 * @see UCPEventFactory
 * @see jdk.jfr.Event
 */
public final class JFRUCPEventListenerProvider implements
        UCPEventListenerProvider {

  private final UCPEventListener listener;

  /**
   * The singleton event listener instance that records events via JFR.
   * <p><b>Characteristics:</b>
   * <ul>
   * <li>Records events with full context as JFR events</li>
   * <li>Adds all pool metrics as event attributes</li>
   * <li>Extremely low overhead (JFR-optimized)</li>
   * <li>Thread-safe for concurrent event recording</li>
   * </ul>
   */
  public static final UCPEventListener TRACE_EVENT_LISTENER =
    new UCPEventListener() {
    @Override
    public void onUCPEvent(EventType eventType, UCPEventContext context) {
      UCPEventFactory.recordEvent(eventType, context);
    }
  };

  /**
   * Creates a new provider instance that will supply
   * the {@link #TRACE_EVENT_LISTENER}.
   */
  public JFRUCPEventListenerProvider() {
    this.listener = TRACE_EVENT_LISTENER;
  }

  /**
   * Returns the provider's unique name "jfr-ucp-listener".
   *
   * @return The constant provider name
   */
  @Override
  public String getName() {
    return "jfr-ucp-listener";
  }

  /**
   * Returns the JFR recording listener instance, ignoring any configuration.
   *
   * @param config Configuration map (ignored by this implementation)
   * @return The {@link #TRACE_EVENT_LISTENER} instance
   */
  @Override
  public UCPEventListener getListener(Map<String, String> config) {
    return listener;
  }
}