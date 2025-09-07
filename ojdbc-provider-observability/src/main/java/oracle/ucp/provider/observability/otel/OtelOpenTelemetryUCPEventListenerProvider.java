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
 * Provider that supplies a UCP event listener for recording OpenTelemetry metrics.
 * Converts UCP events into metrics for integration with observability platforms.
 */
public final class OtelOpenTelemetryUCPEventListenerProvider implements UCPEventListenerProvider {

    private final UCPEventListener listener;

    /**
     * Singleton listener that records UCP events as OpenTelemetry metrics.
     * Thread-safe and generates gauges, counters, and histograms.
     */
    public static final UCPEventListener TRACE_EVENT_LISTENER = new OtelOpenTelemetryUCPEventListener();

    /**
     * Creates a new provider instance.
     */
    public OtelOpenTelemetryUCPEventListenerProvider() {
        this.listener = TRACE_EVENT_LISTENER;
    }

    /**
     * Returns the provider's unique identifier.
     *
     * @return "opentelemetry-ucp-listener"
     */
    @Override
    public String getName() {
        return "opentelemetry-ucp-listener";
    }

    /**
     * Returns the OpenTelemetry listener instance.
     *
     * @param config configuration map (ignored)
     * @return the OpenTelemetry event listener
     */
    @Override
    public UCPEventListener getListener(Map<String, String> config) {
        return listener;
    }

    /**
     * Internal listener that converts UCP events into OpenTelemetry metrics.
     * Records pool state as gauges, event occurrences as counters, and performance as histograms.
     */
    private static class OtelOpenTelemetryUCPEventListener implements UCPEventListener {
        private static final long serialVersionUID = 1L;

        private final Meter meter = GlobalOpenTelemetry.getMeter("oracle.ucp.events");

        // Attribute keys for metric labels
        private static final AttributeKey<String> POOL_NAME_KEY = AttributeKey.stringKey("pool_name");
        private static final AttributeKey<String> EVENT_TYPE_KEY = AttributeKey.stringKey("event_type");

        // Gauge metrics for current state
        private final ObservableDoubleGauge maxPoolSizeGauge;
        private final ObservableDoubleGauge minPoolSizeGauge;
        private final ObservableDoubleGauge borrowedConnectionsGauge;
        private final ObservableDoubleGauge availableConnectionsGauge;
        private final ObservableDoubleGauge totalConnectionsGauge;
        private final ObservableDoubleGauge closedConnectionsGauge;
        private final ObservableDoubleGauge createdConnectionsGauge;
        private final ObservableDoubleGauge averageWaitTimeGauge;

        // Counter metrics for event occurrences
        private final LongCounter poolCreatedCounter;
        private final LongCounter poolStartingCounter;
        private final LongCounter poolStartedCounter;
        private final LongCounter poolStoppedCounter;
        private final LongCounter poolRestartingCounter;
        private final LongCounter poolRestartedCounter;
        private final LongCounter poolDestroyedCounter;
        private final LongCounter connectionCreatedCounter;
        private final LongCounter connectionBorrowedCounter;
        private final LongCounter connectionReturnedCounter;
        private final LongCounter connectionClosedCounter;
        private final LongCounter poolRefreshedCounter;
        private final LongCounter poolRecycledCounter;
        private final LongCounter poolPurgedCounter;

        // Histogram metrics for performance
        private final DoubleHistogram connectionWaitTimeHistogram;

        // Cache for storing latest context per pool for gauge callbacks
        private final Map<String, UCPEventContext> latestContextByPool = new ConcurrentHashMap<>();

        /**
         * Initializes all OpenTelemetry metric instruments.
         */
        public OtelOpenTelemetryUCPEventListener() {
            // Initialize gauge metrics
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
                    .setDescription("Total number of active connections")
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
                    .setDescription("Average connection wait time")
                    .setUnit("ms")
                    .buildWithCallback(this::recordAverageWaitTime);

            // Initialize counter metrics
            this.poolCreatedCounter = meter.counterBuilder("ucp_pool_created_total")
                    .setDescription("Total pool creation events")
                    .setUnit("events")
                    .build();

            this.poolStartingCounter = meter.counterBuilder("ucp_pool_starting_total")
                    .setDescription("Total pool starting events")
                    .setUnit("events")
                    .build();

            this.poolStartedCounter = meter.counterBuilder("ucp_pool_started_total")
                    .setDescription("Total pool started events")
                    .setUnit("events")
                    .build();

            this.poolStoppedCounter = meter.counterBuilder("ucp_pool_stopped_total")
                    .setDescription("Total pool stopped events")
                    .setUnit("events")
                    .build();

            this.poolRestartingCounter = meter.counterBuilder("ucp_pool_restarting_total")
                    .setDescription("Total pool restarting events")
                    .setUnit("events")
                    .build();

            this.poolRestartedCounter = meter.counterBuilder("ucp_pool_restarted_total")
                    .setDescription("Total pool restarted events")
                    .setUnit("events")
                    .build();

            this.poolDestroyedCounter = meter.counterBuilder("ucp_pool_destroyed_total")
                    .setDescription("Total pool destroyed events")
                    .setUnit("events")
                    .build();

            this.connectionCreatedCounter = meter.counterBuilder("ucp_connection_created_total")
                    .setDescription("Total connection creation events")
                    .setUnit("events")
                    .build();

            this.connectionBorrowedCounter = meter.counterBuilder("ucp_connection_borrowed_total")
                    .setDescription("Total connection borrowed events")
                    .setUnit("events")
                    .build();

            this.connectionReturnedCounter = meter.counterBuilder("ucp_connection_returned_total")
                    .setDescription("Total connection returned events")
                    .setUnit("events")
                    .build();

            this.connectionClosedCounter = meter.counterBuilder("ucp_connection_closed_total")
                    .setDescription("Total connection closed events")
                    .setUnit("events")
                    .build();

            this.poolRefreshedCounter = meter.counterBuilder("ucp_pool_refreshed_total")
                    .setDescription("Total pool refresh events")
                    .setUnit("events")
                    .build();

            this.poolRecycledCounter = meter.counterBuilder("ucp_pool_recycled_total")
                    .setDescription("Total pool recycle events")
                    .setUnit("events")
                    .build();

            this.poolPurgedCounter = meter.counterBuilder("ucp_pool_purged_total")
                    .setDescription("Total pool purge events")
                    .setUnit("events")
                    .build();

            // Initialize histogram metrics
            this.connectionWaitTimeHistogram = meter.histogramBuilder("ucp_connection_wait_time_ms")
                    .setDescription("Distribution of connection wait times")
                    .setUnit("ms")
                    .build();
        }

        @Override
        public void onUCPEvent(EventType eventType, UCPEventContext context) {
            if (context == null || eventType == null) {
                return;
            }

            // Update latest context cache for gauge callbacks
            latestContextByPool.put(context.poolName(), context);

            Attributes poolAttributes = Attributes.of(POOL_NAME_KEY, context.poolName());

            // Record event occurrence counter based on event type
            switch (eventType) {
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
                case CONNECTION_CREATED:
                    connectionCreatedCounter.add(1, poolAttributes);
                    break;
                case CONNECTION_BORROWED:
                    connectionBorrowedCounter.add(1, poolAttributes);
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
                    break;
            }
        }

        // Gauge callback methods
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