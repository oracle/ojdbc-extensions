package oracle.ucp.provider.observability.logging;

import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.events.core.UCPEventListener;
import oracle.ucp.events.core.UCPEventListenerProvider;

import java.util.Map;

/**
 * A {@link UCPEventListenerProvider} implementation that provides event listeners
 * which log UCP events to standard output in human-readable format.
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 * <li>Prints detailed event information using {@link UCPEventPrinter}</li>
 * <li>Thread-safe singleton listener instance</li>
 * <li>Ignores configuration (always uses standard formatting)</li>
 * <li>Registered name: "logging-ucp-listener"</li>
 * </ul>
 *
 * <p><b>Output Format:</b></p>
 * Events are printed in two formats:
 * <ol>
 * <li><b>Standard Events:</b> Multi-line formatted output with all context
 * details</li>
 * <li><b>Metrics Updates:</b> Compact single-line format for frequent metrics
 * </li>
 * </ol>
 *
 * @see UCPEventPrinter
 * @see UCPEventListenerProvider
 */
public final class LoggingUCPEventListenerProvider implements UCPEventListenerProvider {

    private final UCPEventListener listener;

    /**
     * The singleton event listener instance that logs all events using
     * {@link UCPEventPrinter}.
     * <p><b>Characteristics:</b>
     * <ul>
     * <li>Prints both event type and full context details</li>
     * <li>Automatically selects appropriate output format</li>
     * <li>Thread-safe for concurrent event handling</li>
     * </ul>
     */
    public static final UCPEventListener TRACE_EVENT_LISTENER = new UCPEventListener() {
        @Override
        public void onUCPEvent(EventType eventType, UCPEventContext context) {
            UCPEventPrinter.PRINT_EVENT.accept(eventType, context);
        }
    };

    /**
     * Creates a new provider instance that will supply the
     * {@link #TRACE_EVENT_LISTENER}.
     */
    public LoggingUCPEventListenerProvider() {
        this.listener = TRACE_EVENT_LISTENER;
    }

    /**
     * Returns the provider's unique name "logging-ucp-listener".
     * @return The constant provider name
     */
    @Override
    public String getName() {
        return "logging-ucp-listener";
    }

    /**
     * Returns the logging listener instance, ignoring any configuration.
     *
     * @param config Configuration map (ignored by this implementation)
     * @return The {@link #TRACE_EVENT_LISTENER} instance
     */
    @Override
    public UCPEventListener getListener(Map<String, String> config) {
        return listener;
    }
}