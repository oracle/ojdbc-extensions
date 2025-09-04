package oracle.ucp.provider.observability.logging;

import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.events.core.UCPEventListener;

import java.util.function.BiConsumer;

/**
 * A utility class for formatting and printing UCP events to standard output.
 * Provides human-readable representations of both regular events and metrics
 * updates.
 *
 * <p><b>Features:</b></p>
 * <ul>
 * <li>Two distinct output formats for regular events vs. metrics updates</li>
 * <li>Consistent, aligned columnar output for readability</li>
 * <li>Thread-safe printing operations</li>
 * <li>Pre-configured consumer for direct use with {@link UCPEventListener}</li>
 * </ul>
 */
public class UCPEventPrinter {
    /** Separator line used in event output */
    private static final String SEPARATOR =
            "=========================================";

    /** Format string for event detail lines */
    private static final String LINE_FORMAT = "%-30s %s%n";

    /**
     * A ready-to-use consumer that prints events to System.out.
     * <p><b>Behavior:</b>
     * <ul>
     * <li>Uses compact format for {@link
     * <li>Uses detailed format for all other event types</li>
     * <li>Handles null context gracefully (prints warning)</li>
     * </ul>
     */
    public static final BiConsumer<UCPEventListener.EventType,
            UCPEventContext> PRINT_EVENT = (eventType, context) -> {
        if (context == null) {
            System.out.println("[WARNING] Received null event context");
            return;
        }

        printStandardEvent(eventType, context);

    };

    /**
     * Prints standard events in detailed, formatted output.
     * <p><b>Output Format:</b>
     * <pre>
     * =========================================
     *  UCP Event: POOL_CREATED
     * =========================================
     * Pool Name:                     orders-pool
     * Timestamp:                     May 14, 2023 14:30:45.123 UTC
     * Max Pool Size:                 20
     * ...
     * =========================================
     * </pre>
     *
     * @param eventType The type of event being printed
     * @param context The event context containing pool data
     */
    private static void printStandardEvent(
            UCPEventListener.EventType eventType,
            UCPEventContext context) {
        StringBuilder sb = new StringBuilder("\n")
                .append(SEPARATOR).append("\n")
                .append(String.format(" UCP Event: %s%n", eventType))
                .append(SEPARATOR).append("\n")
                .append(formatLine("Pool Name:", context.poolName()))
                .append(formatLine("Timestamp:", context.formattedTimestamp()))
                .append(formatLine("Max Pool Size:", context.maxPoolSize()))
                .append(formatLine("Min Pool Size:", context.minPoolSize()))
                .append(formatLine("Borrowed Connections:",
                        context.borrowedConnectionsCount()))
                .append(formatLine("Available Connections:",
                        context.availableConnectionsCount()))
                .append(formatLine("Total Active Connections:",
                        context.totalConnections()))
                .append(formatLine("Average Connection WaitTime:",
                        context.getAverageConnectionWaitTime() + " ms"))
                .append(formatLine("Connections Created:",
                        context.createdConnections()))
                .append(formatLine("Connections Closed:",
                        context.closedConnections()))
                .append(SEPARATOR).append("\n\n");
        System.out.print(sb.toString());
    }

    /**
     * Formats a label-value pair according to {@link #LINE_FORMAT}.
     * @param label The description label (left-aligned)
     * @param value The corresponding value
     * @return Formatted string with consistent alignment
     */
    private static String formatLine(String label, Object value) {
        return String.format(LINE_FORMAT, label, value);
    }
}