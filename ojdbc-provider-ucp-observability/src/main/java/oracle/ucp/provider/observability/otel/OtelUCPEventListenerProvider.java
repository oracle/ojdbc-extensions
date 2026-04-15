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
 *   <li>{@code db.client.connection.usage} (LongGauge, state={@code used}|{@code idle}) —
 *       deviates from the spec name {@code db.client.connection.count} (UpDownCounter) to
 *       avoid the reserved Prometheus {@code _count} suffix, and uses LongGauge because
 *       UCP provides absolute snapshots rather than incremental deltas.</li>
 *   <li>{@code db.client.connection.max} (LongGauge) — recorded on pool lifecycle events
 *       only, since maxPoolSize is a configuration constant.</li>
 *   <li>{@code db.client.connection.idle.min} (LongGauge) — approximated from
 *       {@link UCPEventContext#minPoolSize()} (minimum total pool size, not idle-only).
 *       Recorded on pool lifecycle events only.</li>
 *   <li>{@code db.client.connection.wait_time} (DoubleHistogram, seconds) — approximated
 *       from UCP's cumulative pool-wide average; recorded on {@code CONNECTION_BORROWED}
 *       only when {@literal >} 0.</li>
 * </ul>
 *
 * <h2>UCP-specific metrics</h2>
 * <ul>
 *   <li>{@code db.client.connection.established} (LongGauge) — cumulative connections opened.</li>
 *   <li>{@code db.client.connection.closed} (LongGauge) — cumulative connections closed.</li>
 * </ul>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>{@code db.client.connection.wait_time}: UCP exposes a cumulative pool-wide average,
 *       not a per-borrow value.</li>
 *   <li>{@code db.client.connection.idle.min}: {@link UCPEventContext#minPoolSize()} is the
 *       minimum total pool size, not a dedicated idle floor.</li>
 *   <li>Serialization: OTel instrument fields are not serializable; attempting to serialize
 *       this listener throws {@link NotSerializableException}.</li>
 *   <li>Pool state memory: a {@code PoolState} entry per pool name is retained until
 *       {@code POOL_DESTROYED} fires; abandoned pools linger for the JVM lifetime.</li>
 * </ul>
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
    // config is intentionally unused: OTel SDK configuration is managed
    // externally via the SDK setup, not through UCP's provider config map.
    if (listener == null) {
      synchronized (this) {
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

    // Meter and instruments — instance fields, intentionally NOT static.
    // GlobalOpenTelemetry.getMeter() is called at listener construction time
    // (inside getListener()), which happens after the application registers
    // its OTel SDK. Static initialization would fire at class-load time —
    // before the SDK is ready — and silently produce permanent no-ops.
    private final Meter meter =
      GlobalOpenTelemetry.getMeter("oracle.ucp");

    // db.client.connection.usage (LongGauge, state=used|idle)
    // See class-level Javadoc for deviation rationale.
    private final LongGauge connectionUsage =
      meter.gaugeBuilder("db.client.connection.usage")
        .setDescription("The number of connections that are currently in the state described by the state attribute.")
        .setUnit("{connection}").ofLongs().build();

    // db.client.connection.max (LongGauge)
    // Recorded on pool lifecycle events only — maxPoolSize rarely changes.
    private final LongGauge connectionMax =
      meter.gaugeBuilder("db.client.connection.max")
        .setDescription("The maximum number of open connections allowed.")
        .setUnit("{connection}").ofLongs().build();

    // db.client.connection.idle.min (LongGauge)
    // Sourced from minPoolSize() (minimum total size, not idle-only).
    // Recorded on pool lifecycle events only.
    private final LongGauge connectionIdleMin =
      meter.gaugeBuilder("db.client.connection.idle.min")
        .setDescription(
          "Approximation of the minimum number of idle open connections allowed. " +
          "Sourced from UCP's minPoolSize (minimum total pool size), which is the " +
          "closest value UCP's event API exposes. May differ from true idle minimum " +
          "when connections are actively borrowed.")
        .setUnit("{connection}").ofLongs().build();

    // db.client.connection.wait_time (DoubleHistogram, seconds)
    // DoubleHistogram preserves fractional seconds (e.g. 250 ms = 0.25 s).
    // Recorded on CONNECTION_BORROWED only when wait time > 0.
    // UCP value is in ms — divided by 1000.0 before recording.
    private final DoubleHistogram waitTime =
      meter.histogramBuilder("db.client.connection.wait_time")
        .setDescription(
          "Approximation of borrow wait time based on UCP's pool-wide cumulative " +
          "average. Recorded on CONNECTION_BORROWED events only when wait time > 0.")
        .setUnit("s").build();

    // db.client.connection.established (LongGauge)
    // LongGauge rather than LongCounter: UCP exposes an absolute lifetime total,
    // and LongGauge.set() records the value even when it is 0 (delta=0 on a
    // counter produces no data point, making the metric invisible).
    private final LongGauge connectionEstablished =
      meter.gaugeBuilder("db.client.connection.established")
        .setDescription("Cumulative number of physical connections opened since pool start.")
        .setUnit("{connection}").ofLongs().build();

    // db.client.connection.closed (LongGauge) — same reasoning as above.
    private final LongGauge connectionClosed =
      meter.gaugeBuilder("db.client.connection.closed")
        .setDescription("Cumulative number of physical connections closed since pool start.")
        .setUnit("{connection}").ofLongs().build();

    // Per-pool state — pre-built Attributes objects reused on every event to
    // avoid per-call allocation under high load. Entries are removed on
    // POOL_DESTROYED; abandoned pools linger for the JVM lifetime.
    private static final class PoolState {
      final Attributes attrs;
      final Attributes attrsUsed;
      final Attributes attrsIdle;

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

    // Serialization guard — OTel instrument fields are not serializable.
    private void writeObject(ObjectOutputStream ignored) throws IOException {
      throw new NotSerializableException(
        OtelUCPEventListener.class.getName() +
          ": OTel instrument fields are not serializable.");
    }

    @Override
    public boolean isDesiredEvent(EventType eventType) {
      return true;
    }

    @Override
    public void onUCPEvent(EventType eventType, UCPEventContext ctx) {
      if (eventType == null || ctx == null) {
        return;
      }

      String poolName = ctx.poolName();
      // ConcurrentHashMap does not permit null keys.
      if (poolName == null) {
        return;
      }

      // POOL_DESTROYED: remove state atomically before recording the final
      // snapshot to avoid allocating a new PoolState only to discard it.
      if (eventType == EventType.POOL_DESTROYED) {
        PoolState state = poolStates.remove(poolName);
        if (state != null) {
          recordSnapshot(eventType, ctx, state);
        }
        return;
      }

      PoolState state = poolStates.computeIfAbsent(
        poolName, k -> new PoolState(k, POOL_NAME, STATE));

      recordSnapshot(eventType, ctx, state);
    }

    /**
     * Records all metric observations for a single event.
     * Extracted from {@link #onUCPEvent} to keep routing and recording logic separate.
     */
    private void recordSnapshot(EventType eventType, UCPEventContext ctx, PoolState state) {

      // db.client.connection.usage — direct snapshot on every event.
      connectionUsage.set(ctx.borrowedConnectionsCount(), state.attrsUsed);
      connectionUsage.set(ctx.availableConnectionsCount(), state.attrsIdle);

      // db.client.connection.max / idle.min — pool lifecycle events only.
      // Maintenance events (POOL_REFRESHED, POOL_RECYCLED, POOL_PURGED) are
      // excluded; they contribute only a live connection snapshot.
      switch (eventType) {
        case POOL_CREATED:
        case POOL_STARTING:
        case POOL_STARTED:
        case POOL_STOPPED:
        case POOL_DESTROYED:
          connectionMax.set(ctx.maxPoolSize(), state.attrs);
          connectionIdleMin.set(ctx.minPoolSize(), state.attrs);
          break;
        default:
          break;
      }

      // db.client.connection.wait_time — CONNECTION_BORROWED only, when > 0.
      if (eventType == EventType.CONNECTION_BORROWED) {
        double avgWaitMs = ctx.getAverageConnectionWaitTime();
        if (avgWaitMs > 0) {
          waitTime.record(avgWaitMs / 1000.0, state.attrs);
        }
      }

      connectionEstablished.set(ctx.createdConnections(), state.attrs);
      connectionClosed.set(ctx.closedConnections(), state.attrs);
    }
  }
}