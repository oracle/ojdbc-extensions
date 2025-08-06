package oracle.ucp.provider.observability.otel;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.events.core.UCPEventListener;
import oracle.ucp.events.core.UCPEventListenerProvider;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpenTelemetry provider for UCP events that exports observability data
 * including metrics, traces, and logs to OpenTelemetry-compatible backends.
 *
 * <p><b>Registration Name:</b> "otel-ucp-listener"</p>
 *
 * @see UCPEventListenerProvider
 */
public final class OpenTelemetryUCPEventListenerProvider implements UCPEventListenerProvider {

    private final UCPEventListener listener;

    /**
     * OpenTelemetry event listener that exports UCP events as telemetry data.
     */
    public static final class OpenTelemetryUCPEventListener implements UCPEventListener {

        private final OpenTelemetry openTelemetry;
        private final Meter meter;
        private final Tracer tracer;
        private final Logger logger;

        // Metrics instruments
        private final LongUpDownCounter borrowedConnectionsGauge;
        private final LongUpDownCounter availableConnectionsGauge;
        private final LongUpDownCounter totalConnectionsGauge;
        private final LongCounter connectionsCreatedCounter;
        private final LongCounter connectionsClosedCounter;
        private final DoubleHistogram connectionWaitTimeHistogram;
        private final LongCounter poolEventsCounter;

        // Active spans tracking
        private final Map<String, Span> activePoolSpans = new ConcurrentHashMap<>();

        // Common attribute keys
        private static final AttributeKey<String> POOL_NAME = AttributeKey.stringKey("ucp.pool.name");
        private static final AttributeKey<String> EVENT_TYPE = AttributeKey.stringKey("ucp.event.type");
        private static final AttributeKey<Long> MAX_POOL_SIZE = AttributeKey.longKey("ucp.pool.max_size");
        private static final AttributeKey<Long> MIN_POOL_SIZE = AttributeKey.longKey("ucp.pool.min_size");
        private static final AttributeKey<Long> BORROWED_CONNECTIONS = AttributeKey.longKey("ucp.connections.borrowed");
        private static final AttributeKey<Long> AVAILABLE_CONNECTIONS = AttributeKey.longKey("ucp.connections.available");
        private static final AttributeKey<Long> TOTAL_CONNECTIONS = AttributeKey.longKey("ucp.connections.total");

        public OpenTelemetryUCPEventListener() {
            this(GlobalOpenTelemetry.get());
        }

        public OpenTelemetryUCPEventListener(OpenTelemetry openTelemetry) {
            this.openTelemetry = openTelemetry;
            this.meter = openTelemetry.getMeter("oracle.ucp.events");
            this.tracer = openTelemetry.getTracer("oracle.ucp.events");
            this.logger = openTelemetry.getLogsBridge().get("oracle.ucp.events");

            // Initialize metrics instruments
            this.borrowedConnectionsGauge = meter
                    .upDownCounterBuilder("ucp.connections.borrowed")
                    .setDescription("Number of currently borrowed connections")
                    .setUnit("connections")
                    .build();

            this.availableConnectionsGauge = meter
                    .upDownCounterBuilder("ucp.connections.available")
                    .setDescription("Number of currently available connections")
                    .setUnit("connections")
                    .build();

            this.totalConnectionsGauge = meter
                    .upDownCounterBuilder("ucp.connections.total")
                    .setDescription("Total number of active connections")
                    .setUnit("connections")
                    .build();

            this.connectionsCreatedCounter = meter
                    .counterBuilder("ucp.connections.created")
                    .setDescription("Total number of connections created since pool start")
                    .setUnit("connections")
                    .build();

            this.connectionsClosedCounter = meter
                    .counterBuilder("ucp.connections.closed")
                    .setDescription("Total number of connections closed since pool start")
                    .setUnit("connections")
                    .build();

            this.connectionWaitTimeHistogram = meter
                    .histogramBuilder("ucp.connection.wait_time")
                    .setDescription("Time spent waiting for connections")
                    .setUnit("ms")
                    .build();

            this.poolEventsCounter = meter
                    .counterBuilder("ucp.events.total")
                    .setDescription("Total number of UCP events")
                    .setUnit("events")
                    .build();
        }

        @Override
        public void onUCPEvent(EventType eventType, UCPEventContext context) {
            try {
                // Common attributes for all telemetry data
                Attributes commonAttributes = buildCommonAttributes(eventType, context);

                // Record metrics
                recordMetrics(context, commonAttributes);

                // Record traces for relevant events
                recordTraces(eventType, context, commonAttributes);

                // Record structured logs
                recordLogs(eventType, context, commonAttributes);

                // Increment event counter
                poolEventsCounter.add(1, commonAttributes);

            } catch (Exception e) {
                // Log error but don't throw to avoid disrupting pool operations
                logger.logRecordBuilder()
                        .setSeverity(Severity.ERROR)
                        .setBody("Failed to process UCP event: " + e.getMessage())
                        .setAttribute(EVENT_TYPE, eventType.name())
                        .setAttribute(POOL_NAME, context.poolName())
                        .setAttribute(AttributeKey.stringKey("error.message"), e.getMessage())
                        .emit();
            }
        }

        /**
         * Records metrics for pool state and performance.
         */
        private void recordMetrics(UCPEventContext context, Attributes attributes) {
            // Record current connection counts as gauge values
            borrowedConnectionsGauge.add(context.borrowedConnectionsCount(), attributes);
            availableConnectionsGauge.add(context.availableConnectionsCount(), attributes);
            totalConnectionsGauge.add(context.totalConnections(), attributes);

            // Record cumulative counters
            connectionsCreatedCounter.add(context.createdConnections(), attributes);
            connectionsClosedCounter.add(context.closedConnections(), attributes);

            // Record wait time histogram
            if (context.getAverageConnectionWaitTime() > 0) {
                connectionWaitTimeHistogram.record(context.getAverageConnectionWaitTime(), attributes);
            }
        }

        /**
         * Records distributed traces for pool and connection lifecycle events.
         */
        private void recordTraces(EventType eventType, UCPEventContext context, Attributes attributes) {
            String poolName = context.poolName();

            switch (eventType) {
                case POOL_CREATED:
                    Span span = tracer.spanBuilder("pool.created")
                            .setSpanKind(SpanKind.INTERNAL)
                            .setAllAttributes(attributes)
                            .startSpan();
                    activePoolSpans.put(poolName + ":lifecycle", span);
                    break;

                case POOL_STARTING:
                    tracer.spanBuilder("pool.starting")
                            .setSpanKind(SpanKind.INTERNAL)
                            .setAllAttributes(attributes)
                            .startSpan()
                            .end();
                    break;

                case POOL_STARTED:
                    tracer.spanBuilder("pool.started")
                            .setSpanKind(SpanKind.INTERNAL)
                            .setAllAttributes(attributes)
                            .startSpan()
                            .end();
                    break;

                case POOL_STOPPED:
                    tracer.spanBuilder("pool.stopped")
                            .setSpanKind(SpanKind.INTERNAL)
                            .setAllAttributes(attributes)
                            .startSpan()
                            .end();
                    break;

                case POOL_RESTARTING:
                    tracer.spanBuilder("pool.restarting")
                            .setSpanKind(SpanKind.INTERNAL)
                            .setAllAttributes(attributes)
                            .startSpan()
                            .end();
                    break;

                case POOL_RESTARTED:
                    tracer.spanBuilder("pool.restarted")
                            .setSpanKind(SpanKind.INTERNAL)
                            .setAllAttributes(attributes)
                            .startSpan()
                            .end();
                    break;

                case POOL_DESTROYED:
                    tracer.spanBuilder("pool.destroyed")
                            .setSpanKind(SpanKind.INTERNAL)
                            .setAllAttributes(attributes)
                            .startSpan()
                            .end();
                    // End the lifecycle span if it exists
                    Span lifecycleSpan = activePoolSpans.remove(poolName + ":lifecycle");
                    if (lifecycleSpan != null) {
                        lifecycleSpan.setAllAttributes(attributes).end();
                    }
                    break;

                case CONNECTION_CREATED:
                    tracer.spanBuilder("connection.created")
                            .setSpanKind(SpanKind.INTERNAL)
                            .setAllAttributes(attributes)
                            .startSpan()
                            .end();
                    break;

                case CONNECTION_BORROWED:
                    tracer.spanBuilder("connection.borrowed")
                            .setSpanKind(SpanKind.INTERNAL)
                            .setAllAttributes(attributes)
                            .startSpan()
                            .end();
                    break;

                case CONNECTION_RETURNED:
                    tracer.spanBuilder("connection.returned")
                            .setSpanKind(SpanKind.INTERNAL)
                            .setAllAttributes(attributes)
                            .startSpan()
                            .end();
                    break;

                case CONNECTION_CLOSED:
                    tracer.spanBuilder("connection.closed")
                            .setSpanKind(SpanKind.INTERNAL)
                            .setAllAttributes(attributes)
                            .startSpan()
                            .end();
                    break;

                case POOL_REFRESHED:
                    tracer.spanBuilder("pool.refreshed")
                            .setSpanKind(SpanKind.INTERNAL)
                            .setAllAttributes(attributes)
                            .startSpan()
                            .end();
                    break;

                case POOL_RECYCLED:
                    tracer.spanBuilder("pool.recycled")
                            .setSpanKind(SpanKind.INTERNAL)
                            .setAllAttributes(attributes)
                            .startSpan()
                            .end();
                    break;

                case POOL_PURGED:
                    tracer.spanBuilder("pool.purged")
                            .setSpanKind(SpanKind.INTERNAL)
                            .setAllAttributes(attributes)
                            .startSpan()
                            .end();
                    break;
            }
        }

        /**
         * Records structured logs for all events.
         */
        private void recordLogs(EventType eventType, UCPEventContext context, Attributes attributes) {
            Severity severity;

            if (eventType == EventType.POOL_CREATED || eventType == EventType.POOL_STARTED ||
                    eventType == EventType.POOL_DESTROYED || eventType == EventType.POOL_REFRESHED ||
                    eventType == EventType.POOL_RECYCLED || eventType == EventType.POOL_PURGED) {
                severity = Severity.INFO;
            } else if (eventType == EventType.CONNECTION_CREATED || eventType == EventType.CONNECTION_BORROWED ||
                    eventType == EventType.CONNECTION_RETURNED || eventType == EventType.CONNECTION_CLOSED) {
                severity = Severity.DEBUG;
            } else if (eventType == EventType.POOL_STOPPED || eventType == EventType.POOL_RESTARTING ||
                    eventType == EventType.POOL_RESTARTED || eventType == EventType.POOL_STARTING) {
                severity = Severity.WARN;
            } else {
                severity = Severity.DEBUG;
            }

            String message = String.format("UCP Event: %s - Pool: %s, MaxSize: %d, MinSize: %d, Borrowed: %d, Available: %d, Total: %d, Created: %d, Closed: %d, AvgWaitTime: %d ms",
                    eventType.name(),
                    context.poolName(),
                    context.maxPoolSize(),
                    context.minPoolSize(),
                    context.borrowedConnectionsCount(),
                    context.availableConnectionsCount(),
                    context.totalConnections(),
                    context.createdConnections(),
                    context.closedConnections(),
                    context.getAverageConnectionWaitTime());

            logger.logRecordBuilder()
                    .setTimestamp(Instant.ofEpochMilli(context.timestamp()))
                    .setSeverity(severity)
                    .setBody(message)
                    .setAllAttributes(attributes)
                    .emit();
        }

        /**
         * Builds common attributes used across all telemetry data.
         */
        private Attributes buildCommonAttributes(EventType eventType, UCPEventContext context) {
            return Attributes.builder()
                    .put(POOL_NAME, context.poolName())
                    .put(EVENT_TYPE, eventType.name())
                    .put(AttributeKey.longKey("ucp.timestamp"), context.timestamp())
                    .put(MAX_POOL_SIZE, (long) context.maxPoolSize())
                    .put(MIN_POOL_SIZE, (long) context.minPoolSize())
                    .put(BORROWED_CONNECTIONS, (long) context.borrowedConnectionsCount())
                    .put(AVAILABLE_CONNECTIONS, (long) context.availableConnectionsCount())
                    .put(TOTAL_CONNECTIONS, (long) context.totalConnections())
                    .put(AttributeKey.longKey("ucp.connections.created"), (long) context.createdConnections())
                    .put(AttributeKey.longKey("ucp.connections.closed"), (long) context.closedConnections())
                    .put(AttributeKey.longKey("ucp.connection.avg_wait_time_ms"), context.getAverageConnectionWaitTime())
                    .build();
        }

        @Override
        public boolean isDesiredEvent(EventType eventType) {
            // Listen to all events for comprehensive observability
            return true;
        }
    }

    /**
     * Creates a new OpenTelemetry UCP event listener provider.
     */
    public OpenTelemetryUCPEventListenerProvider() {
        this.listener = new OpenTelemetryUCPEventListener();
    }

    @Override
    public String getName() {
        return "otel-ucp-listener";
    }

    @Override
    public UCPEventListener getListener(Map<String, String> config) {
        return listener;
    }
}