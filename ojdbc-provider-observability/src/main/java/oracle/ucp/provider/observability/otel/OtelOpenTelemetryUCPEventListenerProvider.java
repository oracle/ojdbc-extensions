package oracle.ucp.provider.observability.otel;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.*;
import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.events.core.UCPEventListener;
import oracle.ucp.events.core.UCPEventListenerProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link UCPEventListenerProvider} implementation that records UCP events as
 * OpenTelemetry metrics for integration with Prometheus, Grafana, and other
 * observability platforms.
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 * <li>Converts UCP events into OpenTelemetry metrics</li>
 * <li>Supports all 13 UCP event types with consistent metric collection</li>
 * <li>Thread-safe singleton listener instance</li>
 * <li>Registered name: "opentelemetry-ucp-listener"</li>
 * </ul>
 *
 * <p><b>Metrics Generated:</b></p>
 * <ul>
 * <li><b>Gauges:</b> Current pool state (borrowed, available, total connections)</li>
 * <li><b>Counters:</b> Cumulative events (connections created/closed, pool operations)</li>
 * <li><b>Histograms:</b> Performance metrics (connection wait times)</li>
 * </ul>
 *
 * <p><b>Prerequisites:</b></p>
 * <ul>
 * <li>OpenTelemetry SDK must be configured and initialized</li>
 * <li>Metrics exporter should be configured (e.prometheus.yml., Prometheus)</li>
 * </ul>
 *
 * @see UCPEventListenerProvider
 * @see Meter
 */
public final class OtelOpenTelemetryUCPEventListenerProvider implements UCPEventListenerProvider {

    private final UCPEventListener listener;

    /**
     * The singleton event listener instance that records events as OpenTelemetry metrics.
     * <p><b>Characteristics:</b>
     * <ul>
     * <li>Records both current state and event occurrences</li>
     * <li>Adds pool name and event type as metric labels</li>
     * <li>Extremely efficient metric collection</li>
     * <li>Thread-safe for concurrent event recording</li>
     * </ul>
     */
    public static final UCPEventListener TRACE_EVENT_LISTENER = new OtelOpenTelemetryUCPEventListener();

    /**
     * Creates a new provider instance that will supply the {@link #TRACE_EVENT_LISTENER}.
     */
    public OtelOpenTelemetryUCPEventListenerProvider() {
        this.listener = TRACE_EVENT_LISTENER;
    }

    /**
     * Returns the provider's unique name "opentelemetry-ucp-listener".
     * @return The constant provider name
     */
    @Override
    public String getName() {
        return "opentelemetry-ucp-listener";
    }

    /**
     * Returns the OpenTelemetry listener instance, ignoring any configuration.
     * @param config Configuration map (ignored by this implementation)
     * @return The {@link #TRACE_EVENT_LISTENER} instance
     */
    @Override
    public UCPEventListener getListener(Map<String, String> config) {
        return listener;
    }

    /**
     * Internal OpenTelemetry event listener implementation that converts UCP events
     * into metrics using the OpenTelemetry API.
     */
    private static class OtelOpenTelemetryUCPEventListener implements UCPEventListener {
        private static final long serialVersionUID = 1L;

        // OpenTelemetry Meter for creating instruments
        private final Meter meter = GlobalOpenTelemetry.getMeter("oracle.ucp.events");

        // Attribute keys for metric labels
        private static final AttributeKey<String> POOL_NAME_KEY = AttributeKey.stringKey("pool_name");
        private static final AttributeKey<String> EVENT_TYPE_KEY = AttributeKey.stringKey("event_type");

        // === GAUGE METRICS (Current State) ===
        private final ObservableDoubleGauge maxPoolSizeGauge;
        private final ObservableDoubleGauge minPoolSizeGauge;
        private final ObservableDoubleGauge borrowedConnectionsGauge;
        private final ObservableDoubleGauge availableConnectionsGauge;
        private final ObservableDoubleGauge totalConnectionsGauge;
        private final ObservableDoubleGauge closedConnectionsGauge;
        private final ObservableDoubleGauge createdConnectionsGauge;
        private final ObservableDoubleGauge averageWaitTimeGauge;

        // === COUNTER METRICS (Event Occurrences) ===
        // Pool Lifecycle Events
        private final LongCounter poolCreatedCounter;
        private final LongCounter poolStartingCounter;
        private final LongCounter poolStartedCounter;
        private final LongCounter poolStoppedCounter;
        private final LongCounter poolRestartingCounter;
        private final LongCounter poolRestartedCounter;
        private final LongCounter poolDestroyedCounter;

        // Connection Lifecycle Events
        private final LongCounter connectionCreatedCounter;
        private final LongCounter connectionBorrowedCounter;
        private final LongCounter connectionReturnedCounter;
        private final LongCounter connectionClosedCounter;

        // Maintenance Operations
        private final LongCounter poolRefreshedCounter;
        private final LongCounter poolRecycledCounter;
        private final LongCounter poolPurgedCounter;

        // === HISTOGRAM METRICS (Performance) ===
        private final DoubleHistogram connectionWaitTimeHistogram;

        // Cache for storing latest context per pool for gauge callbacks
        private final Map<String, UCPEventContext> latestContextByPool = new ConcurrentHashMap<>();

        public OtelOpenTelemetryUCPEventListener() {
            // Initialize Gauge metrics (these observe current state)
            this.maxPoolSizeGauge = meter.gaugeBuilder("ucp_max_pool_size")
                    .setDescription("Configured maximum size of the connection pool")
                    .setUnit("connections")
                    .buildWithCallback(this::recordMaxPoolSize);

            this.minPoolSizeGauge = meter.gaugeBuilder("ucp_min_pool_size")
                    .setDescription("Configured minimum size of the connection pool")
                    .setUnit("connections")
                    .buildWithCallback(this::recordMinPoolSize);

            this.borrowedConnectionsGauge = meter.gaugeBuilder("ucp_borrowed_connections")
                    .setDescription("Current number of borrowed connections")
                    .setUnit("connections")
                    .buildWithCallback(this::recordBorrowedConnections);

            this.availableConnectionsGauge = meter.gaugeBuilder("ucp_available_connections")
                    .setDescription("Current number of available idle connections")
                    .setUnit("connections")
                    .buildWithCallback(this::recordAvailableConnections);

            this.totalConnectionsGauge = meter.gaugeBuilder("ucp_total_connections")
                    .setDescription("Total number of active connections (borrowed + available)")
                    .setUnit("connections")
                    .buildWithCallback(this::recordTotalConnections);

            this.closedConnectionsGauge = meter.gaugeBuilder("ucp_closed_connections_total")
                    .setDescription("Lifetime count of closed connections")
                    .setUnit("connections")
                    .buildWithCallback(this::recordClosedConnections);

            this.createdConnectionsGauge = meter.gaugeBuilder("ucp_created_connections_total")
                    .setDescription("Lifetime count of created connections")
                    .setUnit("connections")
                    .buildWithCallback(this::recordCreatedConnections);

            this.averageWaitTimeGauge = meter.gaugeBuilder("ucp_average_wait_time_ms")
                    .setDescription("Average time a user thread waits to obtain a connection")
                    .setUnit("ms")
                    .buildWithCallback(this::recordAverageWaitTime);

            // Initialize Counter metrics (these count event occurrences)
            // Pool Lifecycle Counters
            this.poolCreatedCounter = meter.counterBuilder("ucp_pool_created_total")
                    .setDescription("Total number of pool creation events")
                    .setUnit("events")
                    .build();

            this.poolStartingCounter = meter.counterBuilder("ucp_pool_starting_total")
                    .setDescription("Total number of pool starting events")
                    .setUnit("events")
                    .build();

            this.poolStartedCounter = meter.counterBuilder("ucp_pool_started_total")
                    .setDescription("Total number of pool started events")
                    .setUnit("events")
                    .build();

            this.poolStoppedCounter = meter.counterBuilder("ucp_pool_stopped_total")
                    .setDescription("Total number of pool stopped events")
                    .setUnit("events")
                    .build();

            this.poolRestartingCounter = meter.counterBuilder("ucp_pool_restarting_total")
                    .setDescription("Total number of pool restarting events")
                    .setUnit("events")
                    .build();

            this.poolRestartedCounter = meter.counterBuilder("ucp_pool_restarted_total")
                    .setDescription("Total number of pool restarted events")
                    .setUnit("events")
                    .build();

            this.poolDestroyedCounter = meter.counterBuilder("ucp_pool_destroyed_total")
                    .setDescription("Total number of pool destroyed events")
                    .setUnit("events")
                    .build();

            // Connection Lifecycle Counters
            this.connectionCreatedCounter = meter.counterBuilder("ucp_connection_created_total")
                    .setDescription("Total number of connection creation events")
                    .setUnit("events")
                    .build();

            this.connectionBorrowedCounter = meter.counterBuilder("ucp_connection_borrowed_total")
                    .setDescription("Total number of connection borrowed events")
                    .setUnit("events")
                    .build();

            this.connectionReturnedCounter = meter.counterBuilder("ucp_connection_returned_total")
                    .setDescription("Total number of connection returned events")
                    .setUnit("events")
                    .build();

            this.connectionClosedCounter = meter.counterBuilder("ucp_connection_closed_total")
                    .setDescription("Total number of connection closed events")
                    .setUnit("events")
                    .build();

            // Maintenance Operation Counters
            this.poolRefreshedCounter = meter.counterBuilder("ucp_pool_refreshed_total")
                    .setDescription("Total number of pool refresh events")
                    .setUnit("events")
                    .build();

            this.poolRecycledCounter = meter.counterBuilder("ucp_pool_recycled_total")
                    .setDescription("Total number of pool recycle events")
                    .setUnit("events")
                    .build();

            this.poolPurgedCounter = meter.counterBuilder("ucp_pool_purged_total")
                    .setDescription("Total number of pool purge events")
                    .setUnit("events")
                    .build();

            // Initialize Histogram metrics (these track performance distributions)
            this.connectionWaitTimeHistogram = meter.histogramBuilder("ucp_connection_wait_time_ms")
                    .setDescription("Distribution of connection wait times")
                    .setUnit("ms")
                    .build();
        }

        @Override
        public void onUCPEvent(EventType eventType, UCPEventContext context) {
            if (context == null || eventType == null) {
                return; // Skip null contexts or event types
            }

            // Update latest context cache for gauge callbacks
            latestContextByPool.put(context.poolName(), context);

            // Create common attributes
            Attributes baseAttributes = Attributes.of(
                    POOL_NAME_KEY, context.poolName(),
                    EVENT_TYPE_KEY, eventType.name()
            );

            Attributes poolAttributes = Attributes.of(POOL_NAME_KEY, context.poolName());

            // Record event occurrence counter based on event type
            switch (eventType) {
                // Pool Lifecycle Events
                case POOL_CREATED:
                    poolCreatedCounter.add(1, poolAttributes);
                    break;
                case POOL_STARTING:
                    poolStartingCounter.add(1, poolAttributes);
                    break;
                case POOL_STARTED:
                    poolStartedCounter.add(1, poolAttributes);
                    break;
                case POOL_STOPPED:
                    poolStoppedCounter.add(1, poolAttributes);
                    break;
                case POOL_RESTARTING:
                    poolRestartingCounter.add(1, poolAttributes);
                    break;
                case POOL_RESTARTED:
                    poolRestartedCounter.add(1, poolAttributes);
                    break;
                case POOL_DESTROYED:
                    poolDestroyedCounter.add(1, poolAttributes);
                    break;

                // Connection Lifecycle Events
                case CONNECTION_CREATED:
                    connectionCreatedCounter.add(1, poolAttributes);
                    break;
                case CONNECTION_BORROWED:
                    connectionBorrowedCounter.add(1, poolAttributes);
                    // Record current wait time when connection is borrowed
                    if (context.getAverageConnectionWaitTime() > 0) {
                        connectionWaitTimeHistogram.record(context.getAverageConnectionWaitTime(), poolAttributes);
                    }
                    break;
                case CONNECTION_RETURNED:
                    connectionReturnedCounter.add(1, poolAttributes);
                    break;
                case CONNECTION_CLOSED:
                    connectionClosedCounter.add(1, poolAttributes);
                    break;

                // Maintenance Operations
                case POOL_REFRESHED:
                    poolRefreshedCounter.add(1, poolAttributes);
                    break;
                case POOL_RECYCLED:
                    poolRecycledCounter.add(1, poolAttributes);
                    break;
                case POOL_PURGED:
                    poolPurgedCounter.add(1, poolAttributes);
                    break;
                default:
                    // Handle unexpected event types gracefully
                    break;
            }
        }

        // === GAUGE CALLBACK METHODS ===
        // These methods are called by OpenTelemetry when metrics are scraped

        private void recordMaxPoolSize(ObservableDoubleMeasurement measurement) {
            for (UCPEventContext context : latestContextByPool.values()) {
                measurement.record(context.maxPoolSize(),
                        Attributes.of(POOL_NAME_KEY, context.poolName()));
            }
        }

        private void recordMinPoolSize(ObservableDoubleMeasurement measurement) {
            for (UCPEventContext context : latestContextByPool.values()) {
                measurement.record(context.minPoolSize(),
                        Attributes.of(POOL_NAME_KEY, context.poolName()));
            }
        }

        private void recordBorrowedConnections(ObservableDoubleMeasurement measurement) {
            for (UCPEventContext context : latestContextByPool.values()) {
                measurement.record(context.borrowedConnectionsCount(),
                        Attributes.of(POOL_NAME_KEY, context.poolName()));
            }
        }

        private void recordAvailableConnections(ObservableDoubleMeasurement measurement) {
            for (UCPEventContext context : latestContextByPool.values()) {
                measurement.record(context.availableConnectionsCount(),
                        Attributes.of(POOL_NAME_KEY, context.poolName()));
            }
        }

        private void recordTotalConnections(ObservableDoubleMeasurement measurement) {
            for (UCPEventContext context : latestContextByPool.values()) {
                measurement.record(context.totalConnections(),
                        Attributes.of(POOL_NAME_KEY, context.poolName()));
            }
        }

        private void recordClosedConnections(ObservableDoubleMeasurement measurement) {
            for (UCPEventContext context : latestContextByPool.values()) {
                measurement.record(context.closedConnections(),
                        Attributes.of(POOL_NAME_KEY, context.poolName()));
            }
        }

        private void recordCreatedConnections(ObservableDoubleMeasurement measurement) {
            for (UCPEventContext context : latestContextByPool.values()) {
                measurement.record(context.createdConnections(),
                        Attributes.of(POOL_NAME_KEY, context.poolName()));
            }
        }

        private void recordAverageWaitTime(ObservableDoubleMeasurement measurement) {
            for (UCPEventContext context : latestContextByPool.values()) {
                measurement.record(context.getAverageConnectionWaitTime(),
                        Attributes.of(POOL_NAME_KEY, context.poolName()));
            }
        }
    }
}