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
import java.util.function.Consumer;

/**
 * OpenTelemetry provider for UCP connection pool metrics.
 * <p>
 * This provider converts Oracle UCP events into OpenTelemetry metrics
 * following database client semantic conventions.
 * </p>
 */
public final class OtelUCPEventListenerProvider
  implements UCPEventListenerProvider {

  private static final UCPEventListener LISTENER =
    new OtelUCPEventListener();

  @Override
  public String getName() {
    return "otel-ucp-listener";
  }

  @Override
  public UCPEventListener getListener(Map<String, String> config) {
    return LISTENER;
  }

  /**
   * Internal listener that converts UCP events to OpenTelemetry
   * metrics. Thread-safe and handles all 14 UCP event types.
   */
  private static final class OtelUCPEventListener
    implements UCPEventListener {
    private static final long serialVersionUID = 1L;

    private final Meter meter =
      GlobalOpenTelemetry.getMeter("oracle.ucp");

    private static final AttributeKey<String> POOL_NAME =
      AttributeKey.stringKey("pool.name");
    private static final AttributeKey<String> STATE =
      AttributeKey.stringKey("state");

    private final Map<String, UCPEventContext> contextCache =
      new ConcurrentHashMap<String, UCPEventContext>();

    private final ObservableLongGauge usedConnectionsGauge;
    private final ObservableLongGauge idleConnectionsGauge;
    private final ObservableLongGauge totalConnectionsGauge;
    private final ObservableLongGauge maxConnectionsGauge;
    private final ObservableLongGauge minConnectionsGauge;
    private final ObservableLongGauge totalCreatedGauge;
    private final ObservableLongGauge totalClosedGauge;

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

    private final LongHistogram waitTimeHistogram;

    OtelUCPEventListener() {
      this.usedConnectionsGauge =
        meter.gaugeBuilder("db.client.connections.used")
          .setDescription(
            "The number of connections that are currently in use")
          .setUnit("{connection}")
          .ofLongs()
          .buildWithCallback(
            new Consumer<ObservableLongMeasurement>() {
              @Override
              public void accept(ObservableLongMeasurement measurement) {
                for (UCPEventContext ctx : contextCache.values()) {
                  measurement.record(ctx.borrowedConnectionsCount(),
                    Attributes.of(POOL_NAME, ctx.poolName(), STATE,
                      "used"));
                }
              }
            });

      this.idleConnectionsGauge =
        meter.gaugeBuilder("db.client.connections.idle")
          .setDescription(
            "The number of available connections for use")
          .setUnit("{connection}")
          .ofLongs()
          .buildWithCallback(
            new Consumer<ObservableLongMeasurement>() {
              @Override
              public void accept(ObservableLongMeasurement measurement) {
                for (UCPEventContext ctx : contextCache.values()) {
                  measurement.record(ctx.availableConnectionsCount(),
                    Attributes.of(POOL_NAME, ctx.poolName(), STATE,
                      "idle"));
                }
              }
            });

      this.totalConnectionsGauge =
        meter.gaugeBuilder("db.client.connections.count")
          .setDescription(
            "The total number of connections (idle + used)")
          .setUnit("{connection}")
          .ofLongs()
          .buildWithCallback(
            new Consumer<ObservableLongMeasurement>() {
              @Override
              public void accept(ObservableLongMeasurement measurement) {
                for (UCPEventContext ctx : contextCache.values()) {
                  measurement.record(ctx.totalConnections(),
                    Attributes.of(POOL_NAME, ctx.poolName()));
                }
              }
            });

      this.maxConnectionsGauge =
        meter.gaugeBuilder("db.client.connections.max")
          .setDescription("The maximum size of the pool")
          .setUnit("{connection}")
          .ofLongs()
          .buildWithCallback(
            new Consumer<ObservableLongMeasurement>() {
              @Override
              public void accept(ObservableLongMeasurement measurement) {
                for (UCPEventContext ctx : contextCache.values()) {
                  measurement.record(ctx.maxPoolSize(),
                    Attributes.of(POOL_NAME, ctx.poolName()));
                }
              }
            });

      this.minConnectionsGauge =
        meter.gaugeBuilder("db.client.connections.min")
          .setDescription("The minimum size of the pool")
          .setUnit("{connection}")
          .ofLongs()
          .buildWithCallback(
            new Consumer<ObservableLongMeasurement>() {
              @Override
              public void accept(ObservableLongMeasurement measurement) {
                for (UCPEventContext ctx : contextCache.values()) {
                  measurement.record(ctx.minPoolSize(),
                    Attributes.of(POOL_NAME, ctx.poolName()));
                }
              }
            });

      this.totalCreatedGauge =
        meter.gaugeBuilder("db.client.connections.created")
          .setDescription("The total number of connections created")
          .setUnit("{connection}")
          .ofLongs()
          .buildWithCallback(
            new Consumer<ObservableLongMeasurement>() {
              @Override
              public void accept(ObservableLongMeasurement measurement) {
                for (UCPEventContext ctx : contextCache.values()) {
                  measurement.record(ctx.createdConnections(),
                    Attributes.of(POOL_NAME, ctx.poolName()));
                }
              }
            });

      this.totalClosedGauge =
        meter.gaugeBuilder("db.client.connections.closed")
          .setDescription("The total number of connections closed")
          .setUnit("{connection}")
          .ofLongs()
          .buildWithCallback(
            new Consumer<ObservableLongMeasurement>() {
              @Override
              public void accept(ObservableLongMeasurement measurement) {
                for (UCPEventContext ctx : contextCache.values()) {
                  measurement.record(ctx.closedConnections(),
                    Attributes.of(POOL_NAME, ctx.poolName()));
                }
              }
            });

      this.poolCreatedCounter =
        meter.counterBuilder("db.client.connection.pool.created")
          .setDescription("Number of connection pool creation events")
          .setUnit("{event}")
          .build();

      this.poolStartingCounter =
        meter.counterBuilder("db.client.connection.pool.starting")
          .setDescription("Number of pool starting events")
          .setUnit("{event}")
          .build();

      this.poolStartedCounter =
        meter.counterBuilder("db.client.connection.pool.started")
          .setDescription("Number of pool started events")
          .setUnit("{event}")
          .build();

      this.poolStoppedCounter =
        meter.counterBuilder("db.client.connection.pool.stopped")
          .setDescription("Number of pool stopped events")
          .setUnit("{event}")
          .build();

      this.poolRestartingCounter =
        meter.counterBuilder("db.client.connection.pool.restarting")
          .setDescription("Number of pool restarting events")
          .setUnit("{event}")
          .build();

      this.poolRestartedCounter =
        meter.counterBuilder("db.client.connection.pool.restarted")
          .setDescription("Number of pool restarted events")
          .setUnit("{event}")
          .build();

      this.poolDestroyedCounter =
        meter.counterBuilder("db.client.connection.pool.destroyed")
          .setDescription(
            "Number of connection pool destruction events")
          .setUnit("{event}")
          .build();

      this.connectionCreatedCounter =
        meter.counterBuilder("db.client.connection.created")
          .setDescription("Number of connection creation events")
          .setUnit("{event}")
          .build();

      this.connectionBorrowedCounter =
        meter.counterBuilder("db.client.connection.borrowed")
          .setDescription("Number of connection borrowed events")
          .setUnit("{event}")
          .build();

      this.connectionReturnedCounter =
        meter.counterBuilder("db.client.connection.returned")
          .setDescription("Number of connection returned events")
          .setUnit("{event}")
          .build();

      this.connectionClosedCounter =
        meter.counterBuilder("db.client.connection.closed")
          .setDescription("Number of connection closed events")
          .setUnit("{event}")
          .build();

      this.poolRefreshedCounter =
        meter.counterBuilder("db.client.connection.pool.refreshed")
          .setDescription("Number of pool refresh operations")
          .setUnit("{operation}")
          .build();

      this.poolRecycledCounter =
        meter.counterBuilder("db.client.connection.pool.recycled")
          .setDescription("Number of pool recycle operations")
          .setUnit("{operation}")
          .build();

      this.poolPurgedCounter =
        meter.counterBuilder("db.client.connection.pool.purged")
          .setDescription("Number of pool purge operations")
          .setUnit("{operation}")
          .build();

      this.waitTimeHistogram =
        meter.histogramBuilder("db.client.connections.wait_time")
          .setDescription(
            "The time it took to obtain an open connection from the pool")
          .setUnit("ms")
          .ofLongs()
          .build();
    }

    @Override
    public void onUCPEvent(EventType eventType, UCPEventContext context) {
      if (context == null || eventType == null) {
        return;
      }

      contextCache.put(context.poolName(), context);

      Attributes attrs = Attributes.of(POOL_NAME, context.poolName());

      switch (eventType) {
        case POOL_CREATED:
          poolCreatedCounter.add(1, attrs);
          break;
        case POOL_STARTING:
          poolStartingCounter.add(1, attrs);
          break;
        case POOL_STARTED:
          poolStartedCounter.add(1, attrs);
          break;
        case POOL_STOPPED:
          poolStoppedCounter.add(1, attrs);
          break;
        case POOL_RESTARTING:
          poolRestartingCounter.add(1, attrs);
          break;
        case POOL_RESTARTED:
          poolRestartedCounter.add(1, attrs);
          break;
        case POOL_DESTROYED:
          poolDestroyedCounter.add(1, attrs);
          break;
        case CONNECTION_CREATED:
          connectionCreatedCounter.add(1, attrs);
          break;
        case CONNECTION_BORROWED:
          connectionBorrowedCounter.add(1, attrs);
          long waitTime = context.getAverageConnectionWaitTime();
          if (waitTime > 0) {
            waitTimeHistogram.record(waitTime, attrs);
          }
          break;
        case CONNECTION_RETURNED:
          connectionReturnedCounter.add(1, attrs);
          break;
        case CONNECTION_CLOSED:
          connectionClosedCounter.add(1, attrs);
          break;
        case POOL_REFRESHED:
          poolRefreshedCounter.add(1, attrs);
          break;
        case POOL_RECYCLED:
          poolRecycledCounter.add(1, attrs);
          break;
        case POOL_PURGED:
          poolPurgedCounter.add(1, attrs);
          break;
      }
    }
  }
}