# Oracle UCP Observability Providers

This module contains providers that add observability capabilities to Oracle
Universal Connection Pool (UCP). Three listener providers are available:

* JFR: exports UCP pool and connection lifecycle events to Java Flight Recorder.
* OTEL: publishes UCP connection pool metrics through OpenTelemetry.
* Observability: exports the same UCP event stream to both JFR and OTEL.

| Provider | Listener name | Backend |
|---|---|---|
| Observability | `ucp-observability-listener` | Java Flight Recorder (JFR) and OpenTelemetry metrics |
| JFR | `jfr-ucp-listener` | Java Flight Recorder (JFR) |
| OpenTelemetry | `otel-ucp-listener` | OpenTelemetry metrics |

These providers implement the `UCPEventListenerProvider` interface provided by
UCP. They are notified when UCP emits pool, connection, and maintenance events,
and expose those events as either JFR events or OpenTelemetry metrics.

The exported telemetry includes:

* pool lifecycle events, such as creation, startup, shutdown, and destruction
* connection lifecycle events, such as creation, borrowing, return, and closure
* maintenance events, such as refresh, recycle, and purge
* pool state metrics, such as borrowed, available, total, created, and closed connections
* UCP-reported average borrow wait time

All providers are discovered automatically via `java.util.ServiceLoader`;
activate one by setting the UCP listener provider name on the pool or through
the JVM system property.

---

## Installation

This provider is distributed as a single jar on the Maven Central Repository.
The jar is compiled for JDK 11 and is forward compatible with later JDK
versions. Add the UCP observability provider artifact to the application
classpath:

```xml
<dependency>
  <groupId>com.oracle.database.jdbc</groupId>
  <artifactId>ucp-provider-observability</artifactId>
  <version>1.1.0</version>
</dependency>
```

---

## Activation

To use one of the UCP observability providers, add the artifact to the
application classpath and configure UCP with the listener provider name. Two
activation modes are supported, in priority order:

**1. Pool-level property** (highest priority) — set directly on the data source before the pool starts:

```java
PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
pds.setUCPEventListenerProvider("ucp-observability-listener");
```

**2. JVM system property** — applies globally to all pools in the JVM:

```bash
java -DUCPEventListenerProvider=ucp-observability-listener -jar myapp.jar
```

Or programmatically:

```java
System.setProperty("UCPEventListenerProvider", "ucp-observability-listener");
```

If no provider is configured, UCP uses its default no-op behavior.

Use `ucp-observability-listener` to enable both JFR and OpenTelemetry. Use
`jfr-ucp-listener` for JFR only, or `otel-ucp-listener` for OpenTelemetry only.

---

## JFR Provider

The JFR provider converts UCP pool, connection, and maintenance events into
custom Java Flight Recorder events. Activate it for a UCP pool by configuring
the `jfr-ucp-listener` or `ucp-observability-listener` listener. After it is
activated, the provider emits UCP events to Java Flight Recorder, where they can
be captured, viewed, and filtered in JDK Mission Control.

### Recorded event types

The following UCP event categories are exported as custom JFR events.

#### Pool lifecycle

| JFR event class | Fired when |
|---|---|
| `PoolCreatedEvent` | Pool is successfully created |
| `PoolStartingEvent` | Pool startup begins |
| `PoolStartedEvent` | Pool is successfully started |
| `PoolStoppedEvent` | Pool is successfully stopped |
| `PoolDestroyedEvent` | Pool is successfully destroyed |

#### Connection lifecycle

| JFR event class | Fired when |
|---|---|
| `ConnectionCreatedEvent` | New connection is created |
| `ConnectionBorrowedEvent` | Connection is borrowed from pool |
| `ConnectionReturnedEvent` | Connection is returned to pool |
| `ConnectionClosedEvent` | Connection is closed |

#### Maintenance

| JFR event class | Fired when |
|---|---|
| `PoolRefreshedEvent` | Refresh completes |
| `PoolRecycledEvent` | Recycle completes |
| `PoolPurgedEvent` | Purge completes |

### Fields recorded on every event

Each JFR event records a snapshot of the pool state reported by UCP when the
event is emitted.

| Field | Type | Description |
|---|---|---|
| `ucpTimestamp` | `long` | Event occurrence time in milliseconds since epoch |
| `poolName` | `String` | Unique identifier for the pool instance |
| `maxPoolSize` | `int` | Maximum allowed connections |
| `minPoolSize` | `int` | Minimum maintained connections |
| `borrowedConnections` | `int` | Currently checked-out connections |
| `availableConnections` | `int` | Ready-to-use connections |
| `totalConnections` | `int` | Current active connections (borrowed + available) |
| `createdConnections` | `int` | Total connections ever created |
| `closedConnections` | `int` | Total connections closed |
| `avgWaitTime` | `long` | Average milliseconds a thread waited to obtain a connection |

### Enabling a JFR recording

JFR events can be collected either when the JVM starts or dynamically from a
running process.

**At JVM start:**

```bash
java -XX:StartFlightRecording=filename=ucp.jfr,settings=profile -jar myapp.jar
```

**At runtime via `jcmd`:**

```bash
jcmd <pid> JFR.start name=ucp settings=profile
jcmd <pid> JFR.dump name=ucp filename=ucp.jfr
jcmd <pid> JFR.stop name=ucp
```

---

## OpenTelemetry Provider

The OpenTelemetry provider publishes UCP connection pool metrics through the
[OpenTelemetry API](https://opentelemetry.io/docs/languages/java/). It is
event-driven and does not create background polling threads. This module depends
only on `opentelemetry-api`; the OpenTelemetry SDK and exporter are configured
by the application.

### Exported metrics

The provider emits metrics when UCP events are received. Some metrics use the
OpenTelemetry database client connection namespace, while UCP-specific metrics
use the `oracle.ucp` namespace.

#### Database client connection metrics (`db.client.connection` prefix)

| Metric name | Instrument | Unit | Description |
|---|---|---|---|
| `db.client.connection.count` | LongGauge | `{connection}` | Connections per state (`used` / `idle`). Recorded on every event. |
| `db.client.connection.max` | LongGauge | `{connection}` | Configured maximum pool size. |
| `db.client.connection.min` | LongGauge | `{connection}` | Configured minimum pool size. |

#### UCP-specific

| Metric name | Instrument | Unit | Description |
|---|---|---|---|
| `oracle.ucp.connection.established` | LongGauge | `{connection}` | Cumulative physical connections opened. |
| `oracle.ucp.connection.closed` | LongGauge | `{connection}` | Cumulative physical connections closed. |
| `oracle.ucp.connection.borrow_wait_time.avg` | DoubleGauge | `s` | Cumulative pool-wide average time spent waiting to borrow a connection, as reported by UCP. |

#### Metric attributes

Metric attributes are labels attached to the exported metric points. They allow
monitoring backends such as Prometheus and Grafana to group, filter, and chart
values by pool and connection state.

| Attribute | Applied to | Description |
|---|---|---|
| `db.client.connection.pool.name` | All metrics | UCP connection pool name |
| `db.client.connection.state` | `db.client.connection.count` only | Connection state: `used` or `idle` |

## Supported UCP event types

| Event type | Category | Description |
|---|---|---|
| `POOL_CREATED` | Pool lifecycle | Pool is successfully created |
| `POOL_STARTING` | Pool lifecycle | Pool startup begins |
| `POOL_STARTED` | Pool lifecycle | Pool is successfully started |
| `POOL_STOPPED` | Pool lifecycle | Pool is successfully stopped |
| `POOL_DESTROYED` | Pool lifecycle | Pool is successfully destroyed |
| `CONNECTION_CREATED` | Connection lifecycle | New connection is created |
| `CONNECTION_BORROWED` | Connection lifecycle | Connection is borrowed from pool |
| `CONNECTION_RETURNED` | Connection lifecycle | Connection is returned to pool |
| `CONNECTION_CLOSED` | Connection lifecycle | Connection is closed |
| `POOL_REFRESHED` | Maintenance | Refresh completes |
| `POOL_RECYCLED` | Maintenance | Recycle completes |
| `POOL_PURGED` | Maintenance | Purge completes |

---

## Requirements

| Requirement | Minimum version |
|---|---|
| Oracle UCP | 23.26.1.0.0 |
| Java | 11+ |
| OpenTelemetry API *(OTel provider only)* | 1.44.1 |

---

## See also

- [Oracle UCP Developer's Guide](https://docs.oracle.com/en/database/oracle/oracle-database/23/jjucp/)
- [OpenTelemetry Java documentation](https://opentelemetry.io/docs/languages/java/)
- [JDK Mission Control download](https://www.oracle.com/java/technologies/jdk-mission-control.html)
- [Oracle JDBC Driver Extensions — root README](../README.md)
