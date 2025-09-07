package oracle.ucp.provider.observability.jfr.core;

import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.events.core.UCPEventListener;
import oracle.ucp.events.core.UCPEventListenerProvider;

import java.util.Map;

/**
 * Provider that supplies a UCP event listener for recording JFR events.
 * Integrates UCP events with Java Flight Recorder for low-overhead monitoring.
 */
public final class JFRUCPEventListenerProvider implements UCPEventListenerProvider {

  private final UCPEventListener listener;

  /**
   * Singleton listener that records UCP events as JFR events.
   * Thread-safe and optimized for minimal overhead.
   */
  public static final UCPEventListener TRACE_EVENT_LISTENER = new UCPEventListener() {
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