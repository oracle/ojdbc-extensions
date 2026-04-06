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
 * <p>Metrics follow the OpenTelemetry semantic conventions for database client
 * connection pools: https://opentelemetry.io/docs/specs/semconv/database/database-metrics/
 * Where the spec defines an instrument for data that UCP exposes, the spec name
 * and instrument type are used exactly.
 * </p>
 *
 * <h2>Spec-aligned metrics</h2>
 * <ul>
 *   <li>{@code db.client.connection.usage} LongGauge — current used/idle connections
 *       (state attribute: {@code used} | {@code idle}).
 *       Note: the spec defines this metric as {@code db.client.connection.count} with
 *       instrument type UpDownCounter. We deviate on both: we use {@code usage} as the
 *       name (to avoid the reserved Prometheus {@code _count} suffix which causes the
 *       OTel exporter to report an unknown type), and LongGauge as the instrument
 *       (because UCP provides absolute snapshots, not incremental deltas).</li>
 *   <li>{@code db.client.connection.max} LongGauge — configured maximum pool size.
 *       Recorded only on pool lifecycle events (POOL_CREATED, POOL_STARTING,
 *       POOL_STARTED, POOL_STOPPED, POOL_DESTROYED) since this is a configuration
 *       constant that rarely changes, avoiding unnecessary overhead on high-frequency
 *       connection events.</li>
 *   <li>{@code db.client.connection.idle.min} LongGauge — configured minimum pool size.
 *       Note: UCP's {@link UCPEventContext#minPoolSize()} returns the minimum total
 *       pool size, not a dedicated minimum-idle value. This is the closest approximation
 *       UCP's event API allows. Recorded only on pool lifecycle events for the same
 *       reason as {@code db.client.connection.max}.</li>
 *   <li>{@code db.client.connection.wait_time} Histogram (s) — approximated from UCP's
 *       cumulative average wait time; recorded on CONNECTION_BORROWED only, and only
 *       when the average is greater than zero to avoid polluting the histogram with
 *       zero-value observations at pool startup.</li>
 * </ul>
 *
 * <h2>UCP-specific metrics (no spec equivalent)</h2>
 * <ul>
 *   <li>{@code db.client.connection.established} LongGauge — cumulative physical connections
 *       ever opened since pool start</li>
 *   <li>{@code db.client.connection.closed} LongGauge — cumulative physical connections
 *       ever closed since pool start</li>
 * </ul>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>{@code db.client.connection.wait_time}: The spec intends per-borrow wait durations.
 *       {@link UCPEventContext#getAverageConnectionWaitTime()} returns a cumulative pool-wide
 *       average since pool start, not a per-borrow value. This is the closest approximation
 *       UCP's event API allows.</li>
 *   <li>{@code db.client.connection.idle.min}: The spec intends the minimum number of idle
 *       connections. UCP's {@link UCPEventContext#minPoolSize()} returns the minimum total
 *       pool size (borrowed + idle), not a dedicated idle floor. These differ when minimum
 *       pool size is non-zero and connections are actively borrowed.</li>
 *   <li>Serialization: {@code OtelUCPEventListener} implements {@link java.io.Serializable}
 *       as required by the {@link UCPEventListener} contract, but its OTel instrument fields
 *       are not serializable. Attempting to serialize this instance will throw
 *       {@link NotSerializableException}.</li>
 *   <li>Pool state memory: {@code OtelUCPEventListener} retains a {@code PoolState} entry
 *       per pool name and removes it on {@code POOL_DESTROYED}. If a pool is abandoned
 *       without {@code POOL_DESTROYED} firing (e.g. abrupt JVM shutdown), that entry will
 *       linger for the lifetime of the JVM.</li>
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
    // config is intentionally unused: OTel SDK configuration (exporters,
    // export intervals, resource attributes, etc.) is managed externally
    // via the OTel SDK setup (e.g. OpenTelemetrySdkAutoConfiguration or
    // programmatic SDK builder), not through UCP's provider config map.
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

    // -------------------------------------------------------------------------
    // Attribute keys — static: stateless, shared across all pools.
    // -------------------------------------------------------------------------

    private static final AttributeKey<String> POOL_NAME =
        AttributeKey.stringKey("db.client.connection.pool.name");

    private static final AttributeKey<String> STATE =
        AttributeKey.stringKey("db.client.connection.state");

    // -------------------------------------------------------------------------
    // Meter and instruments — instance fields, intentionally NOT static.
    //
    // GlobalOpenTelemetry.getMeter() is called when OtelUCPEventListener is
    // constructed, which happens inside getListener(), which UCP calls at pool
    // activation time — after the application has registered its OTel SDK.
    // If these were static, they would be initialized at class-load time,
    // before the SDK is registered, and would silently become permanent no-ops.
    //
    // Inverse risk: if the OTel SDK is reinitialized after the listener is
    // constructed (uncommon in production; typical in tests that reset the SDK
    // between runs), these instruments remain bound to the original SDK instance
    // and will export to the old exporter. Tests should obtain a fresh provider
    // instance after each SDK reset rather than reusing the singleton.
    // -------------------------------------------------------------------------

    // Note: meter is retained as an instance field rather than a constructor-local
    // variable for readability — the instrument declarations below read as a
    // cohesive block. The Meter object itself is lightweight (no pooled resources),
    // so the marginal GC cost of holding the reference is negligible.
    private final Meter meter =
      GlobalOpenTelemetry.getMeter("oracle.ucp");

    // Spec-aligned: db.client.connection.usage  (LongGauge, state=used|idle)
    // Spec says UpDownCounter named db.client.connection.count. We deviate on
    // both: name changed to avoid the reserved Prometheus _count suffix (which
    // causes the OTel Prometheus exporter to report an unknown metric type), and
    // LongGauge used because UCP provides absolute snapshots at each event —
    // not incremental deltas. LongGauge.set() maps directly to that model without
    // drift risk. A single instrument is registered; the state attribute
    // differentiates used vs idle at the data level.
    private final LongGauge connectionUsage =
      meter.gaugeBuilder("db.client.connection.usage")
        .setDescription("The number of connections that are currently in the state described by the state attribute.")
        .setUnit("{connection}").ofLongs().build();

    // Spec-aligned: db.client.connection.max  (LongGauge)
    // Spec says UpDownCounter, but maxPoolSize is a configuration value set at
    // pool creation and rarely changes. LongGauge records the actual current
    // value directly, which is semantically correct for a configured constant.
    // Recorded only on pool lifecycle events to avoid per-connection overhead.
    private final LongGauge connectionMax =
      meter.gaugeBuilder("db.client.connection.max")
        .setDescription("The maximum number of open connections allowed.")
        .setUnit("{connection}").ofLongs().build();

    // Spec-aligned: db.client.connection.idle.min  (LongGauge)
    // Same reasoning as db.client.connection.max above.
    // Note: UCP's minPoolSize() is the minimum total pool size, not a dedicated
    // idle floor. See class-level Javadoc for the full limitation note.
    // Recorded only on pool lifecycle events to avoid per-connection overhead.
    private final LongGauge connectionIdleMin =
      meter.gaugeBuilder("db.client.connection.idle.min")
        .setDescription(
          "Approximation of the minimum number of idle open connections allowed. " +
          "Sourced from UCP's minPoolSize (minimum total pool size), which is the " +
          "closest value UCP's event API exposes. May differ from true idle minimum " +
          "when connections are actively borrowed.")
        .setUnit("{connection}").ofLongs().build();

    // Spec-aligned: db.client.connection.wait_time  (DoubleHistogram, unit: s)
    // DoubleHistogram (not Long) because fractional seconds matter:
    // e.g. 250 ms = 0.25 s would be truncated to 0 with a LongHistogram.
    // Recorded on CONNECTION_BORROWED events only when wait time > 0 to avoid
    // polluting the histogram with zero-value observations at pool startup.
    // UCP value is in ms — divided by 1000.0 before recording.
    // See class-level Javadoc for the full limitation note.
    private final DoubleHistogram waitTime =
      meter.histogramBuilder("db.client.connection.wait_time")
        .setDescription(
          "Approximation of borrow wait time based on UCP's pool-wide cumulative " +
          "average. Recorded on CONNECTION_BORROWED events only when wait time > 0.")
        .setUnit("s").build();

    // UCP-specific: db.client.connection.established  (LongGauge)
    // Cumulative count of physical connections ever opened since pool start.
    // Although monotonically increasing, we use LongGauge because UCP exposes
    // this as an absolute lifetime total. When UCP reuses cached connections
    // and never opens new physical sockets, the value stays at 0 — a LongCounter
    // with delta=0 produces no data point at all, making the metric invisible.
    // LongGauge.set() always records the current value, even when it is 0.
    private final LongGauge connectionEstablished =
      meter.gaugeBuilder("db.client.connection.established")
        .setDescription("Cumulative number of physical connections opened since pool start.")
        .setUnit("{connection}").ofLongs().build();

    // UCP-specific: db.client.connection.closed  (LongGauge)
    // Same reasoning as db.client.connection.established above.
    private final LongGauge connectionClosed =
      meter.gaugeBuilder("db.client.connection.closed")
        .setDescription("Cumulative number of physical connections closed since pool start.")
        .setUnit("{connection}").ofLongs().build();

    // -------------------------------------------------------------------------
    // Per-pool state — holds pre-built Attributes objects reused on every event
    // to avoid allocating new objects on every onUCPEvent call under high load.
    // PoolState is a plain data holder — attribute keys passed in explicitly
    // to avoid hidden coupling to the enclosing class's static fields.
    //
    // Entries are removed on POOL_DESTROYED. If a pool is abandoned without
    // POOL_DESTROYED firing, its entry will linger for the JVM lifetime.
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Serialization guard
    //
    // UCPEventListener extends Serializable, but this class holds OTel
    // instrument fields that are not serializable. Explicitly block
    // serialization rather than letting it fail with an opaque stack trace.
    // -------------------------------------------------------------------------

    private void writeObject(ObjectOutputStream ignored) throws IOException {
      throw new NotSerializableException(
        OtelUCPEventListener.class.getName() +
          ": OTel instrument fields are not serializable.");
    }

    // -------------------------------------------------------------------------
    // Event filter — explicit opt-in to all events.
    //
    // All UCP event types are processed:
    //   - Pool lifecycle (POOL_CREATED, POOL_STARTING, POOL_STARTED,
    //     POOL_STOPPED)                  → connectionMax, connectionIdleMin,
    //                                       plus live snapshot
    //   - POOL_DESTROYED                 → connectionMax, connectionIdleMin,
    //                                       live snapshot + poolStates cleanup
    //   - CONNECTION_BORROWED            → waitTime (when > 0), plus live snapshot
    //   - All other connection events    → live snapshot only
    //     (CONNECTION_CREATED, CONNECTION_RETURNED, CONNECTION_CLOSED)
    //   - Maintenance events             → live snapshot only
    //     (POOL_REFRESHED, POOL_RECYCLED, POOL_PURGED)
    //
    // Maintenance events have no dedicated metric handling but still contribute
    // a live connectionUsage / connectionEstablished / connectionClosed snapshot,
    // which is useful for observing pool state immediately after a refresh/recycle.
    //
    // Overriding isDesiredEvent() makes this opt-in explicit rather than relying
    // on the default implementation.
    // -------------------------------------------------------------------------

    @Override
    public boolean isDesiredEvent(EventType eventType) {
      return true;
    }

    // -------------------------------------------------------------------------
    // Event handler
    // -------------------------------------------------------------------------

    @Override
    public void onUCPEvent(EventType eventType, UCPEventContext ctx) {
      if (eventType == null || ctx == null) {
        return;
      }

      String poolName = ctx.poolName();
      // ConcurrentHashMap does not permit null keys — guard explicitly rather
      // than relying on UCP to always provide a non-null pool name.
      if (poolName == null) {
        return;
      }

      // POOL_DESTROYED is handled first: remove the PoolState entry atomically
      // before recording the final snapshot. This avoids the alternative pattern
      // of computeIfAbsent followed by an immediate remove, which would allocate
      // a new PoolState only to discard it. The removed state is used to record
      // the final metric snapshot; if it is null (pool was never seen), we skip.
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
     * Records all metric observations for a single event. Extracted from
     * {@link #onUCPEvent} to keep the event-routing logic and the metric
     * recording logic separately readable.
     */
    private void recordSnapshot(EventType eventType, UCPEventContext ctx, PoolState state) {

      // --- db.client.connection.usage (LongGauge, direct snapshot) ------------

      connectionUsage.set(ctx.borrowedConnectionsCount(), state.attrsUsed);
      connectionUsage.set(ctx.availableConnectionsCount(), state.attrsIdle);

      // --- db.client.connection.max / idle.min --------------------------------
      // Configuration constants — only recorded on pool lifecycle events to
      // avoid redundant work on high-frequency connection borrow/return events.
      // Maintenance events (POOL_REFRESHED, POOL_RECYCLED, POOL_PURGED) are
      // intentionally excluded — they only contribute a live connection snapshot.

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

      // --- db.client.connection.wait_time (CONNECTION_BORROWED only) -----------
      // Cached in a local variable to avoid calling getAverageConnectionWaitTime()
      // twice and to guard against any future context mutability.
      // Only recorded when > 0 to avoid polluting the histogram with zero-value
      // observations before any real waits occur.
      // See limitation note in Javadoc — UCP exposes a cumulative average,
      // not a per-borrow value.

      if (eventType == EventType.CONNECTION_BORROWED) {
        double avgWaitMs = ctx.getAverageConnectionWaitTime();
        if (avgWaitMs > 0) {
          waitTime.record(avgWaitMs / 1000.0, state.attrs);
        }
      }

      // --- UCP-specific metrics -----------------------------------------------

      connectionEstablished.set(ctx.createdConnections(), state.attrs);
      connectionClosed.set(ctx.closedConnections(), state.attrs);
    }
  }
}