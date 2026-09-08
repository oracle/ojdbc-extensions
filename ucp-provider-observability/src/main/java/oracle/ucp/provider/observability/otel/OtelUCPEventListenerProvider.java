/*
 ** Copyright (c) 2026 Oracle and/or its affiliates.
 **
 ** The Universal Permissive License (UPL), Version 1.0
 **
 ** Subject to the condition set forth below, permission is hereby granted to any
 ** person obtaining a copy of this software, associated documentation and/or data
 ** (collectively the "Software"), free of charge and under any and all copyright
 ** rights in the Software, and any and all patent rights owned or freely
 ** licensable by each licensor hereunder covering either (i) the unmodified
 ** Software as contributed to or provided by such licensor, or (ii) the Larger
 ** Works (as defined below), to deal in both
 **
 ** (a) the Software, and
 ** (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 ** one is included with the Software (each a "Larger Work" to which the Software
 ** is contributed by such licensors),
 **
 ** without restriction, including without limitation the rights to copy, create
 ** derivative works of, display, perform, and distribute the Software and make,
 ** use, sell, offer for sale, import, export, have made, and have sold the
 ** Software and the Larger Work(s), and to sublicense the foregoing rights on
 ** either these or other terms.
 **
 ** This license is subject to the following condition:
 ** The above copyright notice and either this complete permission notice or at
 ** a minimum a reference to the UPL must be included in all copies or
 ** substantial portions of the Software.
 **
 ** THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 ** IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 ** FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 ** AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 ** LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 ** OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 ** SOFTWARE.
 */

package oracle.ucp.provider.observability.otel;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.*;
import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.events.core.UCPEventListener;
import oracle.ucp.events.core.UCPEventListenerProvider;
import oracle.ucp.provider.observability.UCPObservabilityConfiguration;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpenTelemetry provider for UCP connection pool metrics.
 *
 * <p>Metrics follow the
 * <a href="https://opentelemetry.io/docs/specs/semconv/database/database-metrics/">
 * OTel semantic conventions for database client connection pools</a>.
 *
 * <h2>Spec-aligned metrics</h2>
 * <ul>
 *   <li>{@code db.client.connection.count} (observable LongGauge,
 *       state={@code used}|{@code idle}) —
 *       uses an observable gauge because UCP provides absolute snapshots rather than
 *       incremental deltas.</li>
 *   <li>{@code db.client.connection.max} (observable LongGauge) — updated on
 *       pool lifecycle events only, since maxPoolSize is a configuration constant.</li>
 *   <li>{@code db.client.connection.min} (observable LongGauge) — sourced from
 *       {@link UCPEventContext#minPoolSize()}. Updated on pool lifecycle events only.</li>
 * </ul>
 *
 * <h2>UCP-specific metrics</h2>
 * <ul>
 *   <li>{@code oracle.ucp.connection.established} (observable LongGauge) —
 *       cumulative connections opened.</li>
 *   <li>{@code oracle.ucp.connection.closed} (observable LongGauge) —
 *       cumulative connections closed.</li>
 *   <li>{@code oracle.ucp.connection.borrow_wait_time.avg} (observable DoubleGauge, seconds) —
 *       cumulative pool-wide average time spent waiting to borrow a connection,
 *       as reported by UCP.</li>
 *   <li>{@code oracle.ucp.observability.enabled} (observable LongGauge, listener={@code OTEL}) —
 *       runtime status for OpenTelemetry emission. A value of {@code 1} means
 *       OTel emission is enabled; {@code 0} means it is disabled.</li>
 * </ul>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>{@code oracle.ucp.connection.borrow_wait_time.avg}: UCP exposes a cumulative
 *       pool-wide average, not a per-borrow value.</li>
 *   <li>{@code db.client.connection.min}: {@link UCPEventContext#minPoolSize()} is the
 *       configured minimum total pool size.</li>
 *   <li>Serialization: OTel instrument fields are not serializable; attempting to serialize
 *       this listener throws {@link NotSerializableException}.</li>
 *   <li>Pool state memory: a {@code PoolState} entry per pool name is retained until
 *       {@code POOL_DESTROYED} fires.</li>
 *   <li>When OTel emission is disabled at runtime, collection callbacks stop
 *       recording connection pool metrics. Last-value exporters can then mark
 *       those pool series stale or absent; use {@code oracle.ucp.observability.enabled}
 *       to identify that OTel emission is disabled.</li>
 * </ul>
 */
public final class OtelUCPEventListenerProvider
  implements UCPEventListenerProvider {

  private static final Object LISTENER_LOCK = new Object();

  private static volatile UCPEventListener listener;

  @Override
  public String getName() {
    return "otel-ucp-listener";
  }

  @Override
  public UCPEventListener getListener(Map<String, String> config) {
    // config is intentionally unused: OTel SDK configuration is managed
    // externally via the SDK setup, not through UCP's provider config map.
    if (listener == null) {
      synchronized (LISTENER_LOCK) {
        if (listener == null) {
          listener = new OtelUCPEventListener();
        }
      }
    }
    return listener;
  }

  private static final class OtelUCPEventListener
    implements UCPEventListener {

    private static final long serialVersionUID = 1L;

    // Attribute keys — static: stateless, shared across all pools.
    private static final AttributeKey<String> POOL_NAME =
      AttributeKey.stringKey("db.client.connection.pool.name");

    private static final AttributeKey<String> STATE =
      AttributeKey.stringKey("db.client.connection.state");

    private static final AttributeKey<String> LISTENER =
      AttributeKey.stringKey("listener");

    private static final Attributes OTEL_LISTENER =
      Attributes.of(LISTENER, UCPObservabilityConfiguration.OTEL);

    // Meter and instruments — instance fields, intentionally NOT static.
    // GlobalOpenTelemetry.getMeter() is called at listener construction time
    // (inside getListener()), which happens after the application registers
    // its OTel SDK. Static initialization would fire at class-load time —
    // before the SDK is ready — and silently produce permanent no-ops.
    private final Meter meter =
      GlobalOpenTelemetry.getMeter("oracle.ucp");

    // Per-pool state, pre-built Attributes objects reused on every event to
    // avoid per-call allocation under high load.
    private static final class PoolState {
      final Attributes attrs;
      final Attributes attrsUsed;
      final Attributes attrsIdle;
      volatile int borrowedConnections;
      volatile int availableConnections;
      volatile int closedConnections;
      volatile int createdConnections;
      volatile int maxPoolSize;
      volatile int minPoolSize;
      volatile long averageBorrowWaitTimeMs;

      PoolState(String poolName,
        AttributeKey<String> poolNameKey,
        AttributeKey<String> stateKey) {
        this.attrs     = Attributes.of(poolNameKey, poolName);
        this.attrsUsed = Attributes.of(poolNameKey, poolName, stateKey, "used");
        this.attrsIdle = Attributes.of(poolNameKey, poolName, stateKey, "idle");
      }
    }

    private final ConcurrentHashMap<String, PoolState> poolStates =
      new ConcurrentHashMap<>();

    // db.client.connection.count (observable LongGauge, state=used|idle)
    private final ObservableLongGauge connectionCount =
      meter.gaugeBuilder("db.client.connection.count")
        .setDescription("The number of connections that are currently in the state described by the state attribute.")
        .setUnit("{connection}").ofLongs()
        .buildWithCallback(measurement -> {
          if (!isOtelEnabled()) {
            return;
          }
          poolStates.forEach((poolName, state) -> {
            measurement.record(state.borrowedConnections, state.attrsUsed);
            measurement.record(state.availableConnections, state.attrsIdle);
          });
        });

    // db.client.connection.max (observable LongGauge)
    // Updated on pool lifecycle events only — maxPoolSize rarely changes.
    private final ObservableLongGauge connectionMax =
      meter.gaugeBuilder("db.client.connection.max")
        .setDescription("The maximum number of open connections allowed.")
        .setUnit("{connection}").ofLongs()
        .buildWithCallback(measurement -> {
          if (!isOtelEnabled()) {
            return;
          }
          poolStates.forEach((poolName, state) ->
            measurement.record(state.maxPoolSize, state.attrs));
        });

    // db.client.connection.min (observable LongGauge)
    // Sourced from minPoolSize().
    // Updated on pool lifecycle events only.
    private final ObservableLongGauge connectionMin =
      meter.gaugeBuilder("db.client.connection.min")
        .setDescription(
            "The configured minimum number of open connections allowed. " +
            "Sourced from UCP's minPoolSize.")
        .setUnit("{connection}").ofLongs()
        .buildWithCallback(measurement -> {
          if (!isOtelEnabled()) {
            return;
          }
          poolStates.forEach((poolName, state) ->
            measurement.record(state.minPoolSize, state.attrs));
        });

    // oracle.ucp.connection.borrow_wait_time.avg (observable DoubleGauge, seconds)
    // UCP exposes a cumulative pool-wide average, not per-borrow wait time.
    // UCP value is in ms — divided by 1000.0 at collection time.
    private final ObservableDoubleGauge averageBorrowWaitTime =
      meter.gaugeBuilder("oracle.ucp.connection.borrow_wait_time.avg")
        .setDescription(
            "Cumulative pool-wide average time spent waiting to borrow a connection, " +
            "as reported by UCP.")
        .setUnit("s")
        .buildWithCallback(measurement -> {
          if (!isOtelEnabled()) {
            return;
          }
          poolStates.forEach((poolName, state) ->
            measurement.record(state.averageBorrowWaitTimeMs / 1000.0, state.attrs));
        });

    // oracle.ucp.connection.established (observable LongGauge)
    // Observable gauge rather than counter: UCP exposes an absolute lifetime total.
    private final ObservableLongGauge connectionEstablished =
      meter.gaugeBuilder("oracle.ucp.connection.established")
        .setDescription("Cumulative number of physical connections opened since pool start.")
        .setUnit("{connection}").ofLongs()
        .buildWithCallback(measurement -> {
          if (!isOtelEnabled()) {
            return;
          }
          poolStates.forEach((poolName, state) ->
            measurement.record(state.createdConnections, state.attrs));
        });

    // oracle.ucp.connection.closed (observable LongGauge) — same reasoning as above.
    private final ObservableLongGauge connectionClosed =
      meter.gaugeBuilder("oracle.ucp.connection.closed")
        .setDescription("Cumulative number of physical connections closed since pool start.")
        .setUnit("{connection}").ofLongs()
        .buildWithCallback(measurement -> {
          if (!isOtelEnabled()) {
            return;
          }
          poolStates.forEach((poolName, state) ->
            measurement.record(state.closedConnections, state.attrs));
        });

    // oracle.ucp.observability.enabled (observable LongGauge, listener=OTEL)
    // Reports whether OTel emission is enabled. This lets dashboards
    // distinguish live OTel values from absent/stale values after disablement.
    private final ObservableLongGauge observabilityEnabled =
      meter.gaugeBuilder("oracle.ucp.observability.enabled")
        .setDescription("Whether UCP provider emission is enabled for the listener backend.")
        .setUnit("1").ofLongs()
        .buildWithCallback(measurement ->
          measurement.record(isOtelEnabled() ? 1 : 0, OTEL_LISTENER));

    // Serialization guard — OTel instrument fields are not serializable.
    private void writeObject(ObjectOutputStream ignored) throws IOException {
      throw new NotSerializableException(
        OtelUCPEventListener.class.getName() +
          ": OTel instrument fields are not serializable.");
    }

    @Override
    public void onUCPEvent(EventType eventType, UCPEventContext ctx) {
      if (eventType == null || ctx == null) {
        return;
      }

      if (eventType == EventType.POOL_DESTROYED) {
        String poolName = ctx.poolName();
        if (poolName != null) {
          poolStates.remove(poolName);
        }
        return;
      }

      if (!isOtelEnabled()) {
        return;
      }

      String poolName = ctx.poolName();
      // ConcurrentHashMap does not permit null keys.
      if (poolName == null) {
        return;
      }

      PoolState state = poolStates.computeIfAbsent(
        poolName, k -> new PoolState(k, POOL_NAME, STATE));

      updateSnapshot(eventType, ctx, state);
    }

    /**
     * Updates the latest pool state snapshot for collection callbacks.
     * Extracted from {@link #onUCPEvent} to keep routing and recording logic separate.
     */
    private void updateSnapshot(EventType eventType, UCPEventContext ctx, PoolState state) {

      state.borrowedConnections = ctx.borrowedConnectionsCount();
      state.availableConnections = ctx.availableConnectionsCount();
      state.createdConnections = ctx.createdConnections();
      state.closedConnections = ctx.closedConnections();

      // db.client.connection.max / min — pool lifecycle events only.
      // Maintenance events (POOL_REFRESHED, POOL_RECYCLED, POOL_PURGED) are
      // excluded; they contribute only a live connection snapshot.
      switch (eventType) {
        case POOL_CREATED:
        case POOL_STARTING:
        case POOL_STARTED:
        case POOL_STOPPED:
          state.maxPoolSize = ctx.maxPoolSize();
          state.minPoolSize = ctx.minPoolSize();
          break;
        case CONNECTION_BORROWED:
          double avgWaitMs = ctx.getAverageConnectionWaitTime();
          if (avgWaitMs >= 0) {
            state.averageBorrowWaitTimeMs = (long) avgWaitMs;
          }
          break;
        default:
          break;
      }
    }

    private static boolean isOtelEnabled() {
      return UCPObservabilityConfiguration.getInstance()
        .isListenerEnabled(UCPObservabilityConfiguration.OTEL);
    }
  }
}
