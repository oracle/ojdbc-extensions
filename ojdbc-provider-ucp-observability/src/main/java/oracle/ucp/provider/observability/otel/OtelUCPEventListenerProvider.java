package oracle.ucp.provider.observability.otel;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.*;
import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.events.core.UCPEventListener;
import oracle.ucp.events.core.UCPEventListenerProvider;

import java.util.Map;

/**
 * OpenTelemetry provider for UCP connection pool metrics.
 *
 * <p>Each UCP event carries a snapshot of pool data at the moment it fired.
 * This provider simply records that data directly into OTel instruments
 * </p>
 *
 */
public final class OtelUCPEventListenerProvider
  implements UCPEventListenerProvider {

  private volatile UCPEventListener listener;

  @Override
  public String getName() {
    return "otel-ucp-listener";
  }

  @Override
  public UCPEventListener getListener(Map<String, String> config) {
    if (listener == null) {
      synchronized (this) {
        if (listener == null) {
          listener = new OtelUCPEventListener();
        }
      }
    }
    return listener;
  }

  private static final class OtelUCPEventListener implements UCPEventListener {

    private static final long serialVersionUID = 1L;

    private final Meter meter = GlobalOpenTelemetry.getMeter("oracle.ucp");

    private static final AttributeKey<String> POOL_NAME =
      AttributeKey.stringKey("db.client.connection.pool.name");

    private final LongCounter poolCreatedCounter =
      meter.counterBuilder("db.client.connection.pool.created")
        .setDescription("Number of POOL_CREATED events.")
        .setUnit("{event}").build();

    private final LongCounter poolStartingCounter =
      meter.counterBuilder("db.client.connection.pool.starting")
        .setDescription("Number of POOL_STARTING events.")
        .setUnit("{event}").build();

    private final LongCounter poolStartedCounter =
      meter.counterBuilder("db.client.connection.pool.started")
        .setDescription("Number of POOL_STARTED events.")
        .setUnit("{event}").build();

    private final LongCounter poolStoppedCounter =
      meter.counterBuilder("db.client.connection.pool.stopped")
        .setDescription("Number of POOL_STOPPED events.")
        .setUnit("{event}").build();

    private final LongCounter poolDestroyedCounter =
      meter.counterBuilder("db.client.connection.pool.destroyed")
        .setDescription("Number of POOL_DESTROYED events.")
        .setUnit("{event}").build();

    private final LongCounter poolRefreshedCounter =
      meter.counterBuilder("db.client.connection.pool.refreshed")
        .setDescription("Number of POOL_REFRESHED events.")
        .setUnit("{event}").build();

    private final LongCounter poolRecycledCounter =
      meter.counterBuilder("db.client.connection.pool.recycled")
        .setDescription("Number of POOL_RECYCLED events.")
        .setUnit("{event}").build();

    private final LongCounter poolPurgedCounter =
      meter.counterBuilder("db.client.connection.pool.purged")
        .setDescription("Number of POOL_PURGED events.")
        .setUnit("{event}").build();

    private final LongCounter connectionCreatedCounter =
      meter.counterBuilder("db.client.connection.created")
        .setDescription("Number of CONNECTION_CREATED events.")
        .setUnit("{event}").build();

    private final LongCounter connectionBorrowedCounter =
      meter.counterBuilder("db.client.connection.borrowed")
        .setDescription("Number of CONNECTION_BORROWED events.")
        .setUnit("{event}").build();

    private final LongCounter connectionReturnedCounter =
      meter.counterBuilder("db.client.connection.returned")
        .setDescription("Number of CONNECTION_RETURNED events.")
        .setUnit("{event}").build();

    private final LongCounter connectionClosedCounter =
      meter.counterBuilder("db.client.connection.closed")
        .setDescription("Number of CONNECTION_CLOSED events.")
        .setUnit("{event}").build();


    private final LongHistogram borrowedConnectionsHistogram =
      meter.histogramBuilder("db.client.connection.borrowed_count")
        .setDescription("Snapshot of borrowed connections count at the time of the event.")
        .setUnit("{connection}").ofLongs().build();

    private final LongHistogram availableConnectionsHistogram =
      meter.histogramBuilder("db.client.connection.available_count")
        .setDescription("Snapshot of available connections count at the time of the event.")
        .setUnit("{connection}").ofLongs().build();

    private final LongHistogram totalConnectionsHistogram =
      meter.histogramBuilder("db.client.connection.total_count")
        .setDescription("Snapshot of total connections (borrowed + available) at the time of the event.")
        .setUnit("{connection}").ofLongs().build();

    private final LongHistogram createdConnectionsHistogram =
      meter.histogramBuilder("db.client.connection.created_count")
        .setDescription("Snapshot of cumulative created connections at the time of the event.")
        .setUnit("{connection}").ofLongs().build();

    private final LongHistogram closedConnectionsHistogram =
      meter.histogramBuilder("db.client.connection.closed_count")
        .setDescription("Snapshot of cumulative closed connections at the time of the event.")
        .setUnit("{connection}").ofLongs().build();

    private final LongHistogram avgWaitTimeHistogram =
      meter.histogramBuilder("db.client.connection.wait_time")
        .setDescription("Average connection wait time in milliseconds at the time of the event.")
        .setUnit("ms").ofLongs().build();

    private final LongHistogram maxPoolSizeHistogram =
      meter.histogramBuilder("db.client.connection.max_pool_size")
        .setDescription("Snapshot of configured max pool size at the time of the event.")
        .setUnit("{connection}").ofLongs().build();

    private final LongHistogram minPoolSizeHistogram =
      meter.histogramBuilder("db.client.connection.min_pool_size")
        .setDescription("Snapshot of configured min pool size at the time of the event.")
        .setUnit("{connection}").ofLongs().build();


    @Override
    public void onUCPEvent(EventType eventType, UCPEventContext ctx) {
      if (eventType == null || ctx == null) {
        return;
      }

      Attributes attrs = Attributes.of(POOL_NAME, ctx.poolName());

      borrowedConnectionsHistogram.record(ctx.borrowedConnectionsCount(), attrs);
      availableConnectionsHistogram.record(ctx.availableConnectionsCount(), attrs);
      totalConnectionsHistogram.record(ctx.totalConnections(), attrs);
      createdConnectionsHistogram.record(ctx.createdConnections(), attrs);
      closedConnectionsHistogram.record(ctx.closedConnections(), attrs);
      avgWaitTimeHistogram.record(ctx.getAverageConnectionWaitTime(), attrs);
      maxPoolSizeHistogram.record(ctx.maxPoolSize(), attrs);
      minPoolSizeHistogram.record(ctx.minPoolSize(), attrs);

      switch (eventType) {
        case POOL_CREATED:    poolCreatedCounter.add(1, attrs);    break;
        case POOL_STARTING:   poolStartingCounter.add(1, attrs);   break;
        case POOL_STARTED:    poolStartedCounter.add(1, attrs);    break;
        case POOL_STOPPED:    poolStoppedCounter.add(1, attrs);    break;
        case POOL_DESTROYED:  poolDestroyedCounter.add(1, attrs);  break;
        case POOL_REFRESHED:  poolRefreshedCounter.add(1, attrs);  break;
        case POOL_RECYCLED:   poolRecycledCounter.add(1, attrs);   break;
        case POOL_PURGED:     poolPurgedCounter.add(1, attrs);     break;
        case CONNECTION_CREATED:  connectionCreatedCounter.add(1, attrs);  break;
        case CONNECTION_BORROWED: connectionBorrowedCounter.add(1, attrs); break;
        case CONNECTION_RETURNED: connectionReturnedCounter.add(1, attrs); break;
        case CONNECTION_CLOSED:   connectionClosedCounter.add(1, attrs);   break;
        default: break;
      }
    }
  }
}