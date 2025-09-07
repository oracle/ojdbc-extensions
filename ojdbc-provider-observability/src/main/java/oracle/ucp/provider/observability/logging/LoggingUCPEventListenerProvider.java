package oracle.ucp.provider.observability.logging;

import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.events.core.UCPEventListener;
import oracle.ucp.events.core.UCPEventListenerProvider;

import java.util.Map;

/**
 * Provider that supplies a UCP event listener for logging events to standard output.
 * Uses UCPEventPrinter for human-readable formatted output.
 */
public final class LoggingUCPEventListenerProvider implements UCPEventListenerProvider {

    private final UCPEventListener listener;

    /**
     * Singleton listener that logs all UCP events using UCPEventPrinter.
     * Thread-safe and prints detailed formatted output for all event types.
     */
    public static final UCPEventListener TRACE_EVENT_LISTENER = new UCPEventListener() {
        @Override
        public void onUCPEvent(EventType eventType, UCPEventContext context) {
            UCPEventPrinter.PRINT_EVENT.accept(eventType, context);
        }
    };

    /**
     * Creates a new provider instance.
     */
    public LoggingUCPEventListenerProvider() {
        this.listener = TRACE_EVENT_LISTENER;
    }

    /**
     * Returns the provider's unique identifier.
     *
     * @return "logging-ucp-listener"
     */
    @Override
    public String getName() {
        return "logging-ucp-listener";
    }

    /**
     * Returns the logging listener instance.
     *
     * @param config configuration map (ignored)
     * @return the logging event listener
     */
    @Override
    public UCPEventListener getListener(Map<String, String> config) {
        return listener;
    }
}