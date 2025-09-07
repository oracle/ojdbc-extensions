package oracle.ucp.provider.observability.logging;

import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.events.core.UCPEventListener;

import java.util.function.BiConsumer;

/**
 * Utility for formatting and printing UCP events to standard output.
 * Provides human-readable event representations with consistent formatting.
 */
public class UCPEventPrinter {

    /** Separator line for event output */
    private static final String SEPARATOR = "=========================================";

    /** Format string for aligned event detail lines */
    private static final String LINE_FORMAT = "%-30s %s%n";

    /**
     * Ready-to-use consumer that prints events to System.out.
     * Handles null contexts gracefully and uses detailed formatting for all events.
     */
    public static final BiConsumer<UCPEventListener.EventType, UCPEventContext> PRINT_EVENT =
            (eventType, context) -> {
                if (context == null) {
                    System.out.println("[WARNING] Received null event context");
                    return;
                }
                printStandardEvent(eventType, context);
            };

    /**
     * Prints events in detailed, formatted output with pool metrics.
     *
     * @param eventType type of event being printed
     * @param context event context containing pool data
     */
    private static void printStandardEvent(UCPEventListener.EventType eventType,
                                           UCPEventContext context) {
        StringBuilder sb = new StringBuilder("\n")
                .append(SEPARATOR).append("\n")
                .append(String.format(" UCP Event: %s%n", eventType))
                .append(SEPARATOR).append("\n")
                .append(formatLine("Pool Name:", context.poolName()))
                .append(formatLine("Timestamp:", context.formattedTimestamp()))
                .append(formatLine("Max Pool Size:", context.maxPoolSize()))
                .append(formatLine("Min Pool Size:", context.minPoolSize()))
                .append(formatLine("Borrowed Connections:", context.borrowedConnectionsCount()))
                .append(formatLine("Available Connections:", context.availableConnectionsCount()))
                .append(formatLine("Total Active Connections:", context.totalConnections()))
                .append(formatLine("Average Connection WaitTime:",
                        context.getAverageConnectionWaitTime() + " ms"))
                .append(formatLine("Connections Created:", context.createdConnections()))
                .append(formatLine("Connections Closed:", context.closedConnections()))
                .append(SEPARATOR).append("\n\n");
        System.out.print(sb.toString());
    }

    /**
     * Formats a label-value pair with consistent alignment.
     *
     * @param label description label (left-aligned)
     * @param value corresponding value
     * @return formatted string with alignment
     */
    private static String formatLine(String label, Object value) {
        return String.format(LINE_FORMAT, label, value);
    }
}
