# Oracle JDBC UCP Observability Providers

Implementations of the `UCPEventListenerProvider` SPI that expose Oracle Universal Connection
Pool (UCP) telemetry through two industry-standard observability backends:

| Provider | Listener name | Backend |
|---|---|---|
| `JFRUCPEventListenerProvider` | `jfr-ucp-listener` | Java Flight Recorder (JFR) |
| `OtelUCPEventListenerProvider` | `otel-ucp-listener` | OpenTelemetry (metrics) |

Both providers are registered automatically via `java.util.ServiceLoader` — no code changes
are required in the application.

---

## Contents

- [Installation](#installation)
- [Activation](#activation)
- [JFR Provider](#jfr-provider)
  - [Recorded event types](#recorded-event-types)
  - [Enabling a JFR recording](#enabling-a-jfr-recording)
  - [Analysing events in JDK Mission Control](#analysing-events-in-jdk-mission-control)
- [OpenTelemetry Provider](#opentelemetry-provider)
  - [SDK initialisation requirement](#sdk-initialisation-requirement)
  - [Exported metrics](#exported-metrics)
  - [Prometheus / Grafana quick-start](#prometheus--grafana-quick-start)
- [Supported UCP event types](#supported-ucp-event-types)
- [Requirements](#requirements)

---

## Installation

Add the module to your Maven project:

```xml
<dependency>
  <groupId>com.oracle.database.jdbc</groupId>
  <artifactId>ojdbc-provider-ucp-observability</artifactId>
  <version>1.0.6</version>
</dependency>
```
---

## Activation

Each provider is activated by registering its listener name on the UCP data source **before**
the pool is started.

### Programmatic activation

```java
public class MyApp {
  public static void main(String[] args) throws Exception {
    PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
    pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
    pds.setURL("jdbc:oracle:thin:@//host:1521/service");

    // Activate your provider
    pds.setUCPEventListenerProvider("jfr-ucp-listener");
    // pds.setUCPEventListenerProvider("otel-ucp-listener");
  }
}
```

---

## JFR Provider

`JFRUCPEventListenerProvider` converts every UCP pool and connection event into a committed
JFR event. Events are emitted with zero overhead when no JFR recording is active, making the provider safe to ship in production at all times.

### Recorded event types

All events live under the `ucp` JFR category.

#### Pool lifecycle

| JFR event class | Fired when |
|---|---|
| `PoolCreatedEvent` | A new UCP pool instance is created |
| `PoolStartingEvent` | Pool initialisation begins |
| `PoolStartedEvent` | Pool is fully started and ready |
| `PoolStoppedEvent` | Pool has been stopped |
| `PoolDestroyedEvent` | Pool instance is destroyed |

#### Connection lifecycle

| JFR event class | Fired when |
|---|---|
| `ConnectionCreatedEvent` | A physical connection is opened to the database |
| `ConnectionBorrowedEvent` | A connection is checked out by the application |
| `ConnectionReturnedEvent` | A connection is returned to the pool |
| `ConnectionClosedEvent` | A physical connection is closed |

#### Maintenance operations

| JFR event class | Fired when |
|---|---|
| `PoolRefreshedEvent` | The pool replaces all connections with fresh ones |
| `PoolRecycledEvent` | The pool recycles stale connections |
| `PoolPurgedEvent` | All connections are forcibly removed from the pool |

### Fields recorded on every event

| Field | Type | Description |
|---|---|---|
| `ucpTimestamp` | `long` | Epoch millisecond timestamp from the UCP event context |
| `poolName` | `String` | Name of the UCP pool that fired the event |
| `maxPoolSize` | `int` | Maximum pool capacity at event time |
| `minPoolSize` | `int` | Minimum pool capacity at event time |
| `borrowedConnectionsCount` | `int` | Checked-out connections at event time |
| `availableConnectionsCount` | `int` | Idle connections at event time |
| `totalConnections` | `int` | Total physical connections at event time |
| `createdConnections` | `long` | Cumulative connections created since pool start |
| `closedConnections` | `long` | Cumulative connections closed since pool start |
| `averageConnectionWaitTime` | `long` | Average borrow wait time in milliseconds |

### Enabling a JFR recording

**At JVM start** — capture everything from pool creation:

```bash
java \
  -XX:StartFlightRecording=filename=ucp.jfr,settings=profile \
  -jar myapp.jar
```

**At runtime via `jcmd`** — attach to a running process:

```bash
# Start recording
jcmd <pid> JFR.start name=ucp settings=profile

# Dump to file
jcmd <pid> JFR.dump name=ucp filename=ucp.jfr

# Stop
jcmd <pid> JFR.stop name=ucp
```

To limit recording to UCP events only and reduce file size, create a custom settings file
and enable only the `ucp.*` event namespace:

```bash
jcmd <pid> JFR.start name=ucp settings=ucp-custom.jfc
```

Where `ucp-custom.jfc` enables `<event name="ucp.*"><setting name="enabled">true</setting></event>`.

### Analysing events in JDK Mission Control

Open the `.jfr` file in [JDK Mission Control (JMC)](https://www.oracle.com/java/technologies/jdk-mission-control.html).
UCP events appear under **Event Browser → ucp** and can be correlated with GC pauses,
thread activity, and I/O latency on the same timeline.

---

## OpenTelemetry Provider

`OtelUCPEventListenerProvider` publishes UCP pool and connection metrics through the
[OpenTelemetry API](https://opentelemetry.io/docs/languages/java/). The provider is
**event-driven**: every time a UCP event fires, the snapshot data it carries is recorded
directly into OTel instruments.

The provider depends only on `opentelemetry-api` — it does **not** pull in the SDK or any
exporter. The application is responsible for initialising an OpenTelemetry SDK and
registering it with `GlobalOpenTelemetry` **before** the pool is started.

### SDK initialisation requirement

The provider obtains its `Meter` on first listener instantiation using
`GlobalOpenTelemetry.getMeter("oracle.ucp")`. The SDK must therefore be registered before
the pool is started. A minimal setup using the Prometheus exporter:

```java
public class MyApp {
  public static void main(String[] args) throws Exception {
    // 1. Register the SDK BEFORE pool creation
    PrometheusHttpServer prometheusServer = PrometheusHttpServer.builder()
            .setPort(9464)
            .build();

    SdkMeterProvider meterProvider = SdkMeterProvider.builder()
            .registerMetricReader(prometheusServer)
            .build();

    OpenTelemetrySdk.builder()
            .setMeterProvider(meterProvider)
            .buildAndRegisterGlobal();

    // 2. Then create and start the pool
    PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
    pds.setUCPEventListenerProvider("otel-ucp-listener");
  }
}
```

### Exported metrics

All metrics use the `db.client.connection` prefix following
[OpenTelemetry semantic conventions for database clients](https://opentelemetry.io/docs/specs/semconv/database/database-metrics/).

#### Histograms — snapshot fields recorded on every event

Each UCP event carries a snapshot of pool state at the moment it fired. These eight fields
are recorded as histogram observations on **every** event regardless of type, giving a
time-series view of pool state correlated with event activity.

| Metric name | Unit | Description |
|---|---|---|
| `db.client.connection.borrowed_count` | `{connection}` | Snapshot of checked-out connections at event time |
| `db.client.connection.available_count` | `{connection}` | Snapshot of idle connections at event time |
| `db.client.connection.total_count` | `{connection}` | Snapshot of total connections (borrowed + available) at event time |
| `db.client.connection.created_count` | `{connection}` | Cumulative connections created since pool start at event time |
| `db.client.connection.closed_count` | `{connection}` | Cumulative connections closed since pool start at event time |
| `db.client.connection.wait_time` | `ms` | Average connection wait time in milliseconds at event time |
| `db.client.connection.max_pool_size` | `{connection}` | Configured maximum pool size at event time |
| `db.client.connection.min_pool_size` | `{connection}` | Configured minimum pool size at event time |

#### Event counters

One counter is incremented by 1 each time the corresponding UCP event fires.

| Metric name | Incremented on |
|---|---|
| `db.client.connection.pool.created` | `POOL_CREATED` |
| `db.client.connection.pool.starting` | `POOL_STARTING` |
| `db.client.connection.pool.started` | `POOL_STARTED` |
| `db.client.connection.pool.stopped` | `POOL_STOPPED` |
| `db.client.connection.pool.destroyed` | `POOL_DESTROYED` |
| `db.client.connection.pool.refreshed` | `POOL_REFRESHED` |
| `db.client.connection.pool.recycled` | `POOL_RECYCLED` |
| `db.client.connection.pool.purged` | `POOL_PURGED` |
| `db.client.connection.created` | `CONNECTION_CREATED` |
| `db.client.connection.borrowed` | `CONNECTION_BORROWED` |
| `db.client.connection.returned` | `CONNECTION_RETURNED` |
| `db.client.connection.closed` | `CONNECTION_CLOSED` |

#### Common attributes

All metrics carry the following attribute:

| Attribute key | Example value | Description |
|---|---|---|
| `db.client.connection.pool.name` | `"MyPool"` | Name of the UCP pool that emitted the event |

### Prometheus / Grafana quick-start

**1. Add the Prometheus exporter (test / application scope)**

```xml
<dependencies>
  <dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk</artifactId>
    <version>1.44.1</version>
  </dependency>
  <dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-prometheus</artifactId>
    <version>1.44.1-alpha</version>
  </dependency>
</dependencies>
```

**2. Configure Prometheus to scrape the application**

```yaml
# prometheus.yml
scrape_configs:
  - job_name: ucp
    static_configs:
      - targets: ["localhost:9464"]
```

**3. Useful Grafana PromQL queries**

```promql
# Average borrowed connections over the last minute
rate(db_client_connection_borrowed_count_sum[1m])
  / rate(db_client_connection_borrowed_count_count[1m])

# Average available connections over the last minute
rate(db_client_connection_available_count_sum[1m])
  / rate(db_client_connection_available_count_count[1m])

# Average connection wait time over the last minute
rate(db_client_connection_wait_time_milliseconds_sum[1m])
  / rate(db_client_connection_wait_time_milliseconds_count[1m])

# 95th-percentile connection wait time
histogram_quantile(0.95,
  rate(db_client_connection_wait_time_milliseconds_bucket[1m]))

# Total borrow events (cumulative counter)
db_client_connection_borrowed_total

# Borrow rate per minute
rate(db_client_connection_borrowed_total[1m])
```

---

## Supported UCP event types

Both providers handle all twelve event types defined by `UCPEventListener.EventType`:

| Event type | Category |
|---|---|
| `POOL_CREATED` | Pool lifecycle |
| `POOL_STARTING` | Pool lifecycle |
| `POOL_STARTED` | Pool lifecycle |
| `POOL_STOPPED` | Pool lifecycle |
| `POOL_DESTROYED` | Pool lifecycle |
| `CONNECTION_CREATED` | Connection lifecycle |
| `CONNECTION_BORROWED` | Connection lifecycle |
| `CONNECTION_RETURNED` | Connection lifecycle |
| `CONNECTION_CLOSED` | Connection lifecycle |
| `POOL_REFRESHED` | Maintenance |
| `POOL_RECYCLED` | Maintenance |
| `POOL_PURGED` | Maintenance |

---

## Requirements

| Requirement | Minimum version |
|---|---|
| Oracle JDBC driver | 23.26.0.0.0 |
| Oracle UCP | 23.26.1.0.0 |
| Java (compile) | 11 |
| Java (runtime) | 11+ |
| OpenTelemetry API *(OTel provider only)* | 1.44.1 |

The JFR provider has no runtime dependencies beyond the Oracle UCP jar and a JDK that
supports JFR (JDK 11+). The OpenTelemetry provider requires `opentelemetry-api` on the
classpath; the SDK and exporter are the application's responsibility.
---

## See also

- [Oracle UCP Developer's Guide](https://docs.oracle.com/en/database/oracle/oracle-database/23/jjucp/)
- [OpenTelemetry Java documentation](https://opentelemetry.io/docs/languages/java/)
- [JDK Mission Control download](https://www.oracle.com/java/technologies/jdk-mission-control.html)
- [Oracle JDBC Driver Extensions — root README](../README.md)