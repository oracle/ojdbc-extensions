# Oracle JDBC UCP Observability Providers

Implementations of the `UCPEventListenerProvider` SPI that expose Oracle UCP telemetry through two observability backends:

| Provider | Listener name | Backend |
|---|---|---|
| `JFRUCPEventListenerProvider` | `jfr-ucp-listener` | Java Flight Recorder (JFR) |
| `OtelUCPEventListenerProvider` | `otel-ucp-listener` | OpenTelemetry (metrics) |

Both providers are registered automatically via `java.util.ServiceLoader` — no code changes required.

---

## Contents

- [Installation](#installation)
- [Activation](#activation)
- [JFR Provider](#jfr-provider)
- [OpenTelemetry Provider](#opentelemetry-provider)
- [Supported UCP event types](#supported-ucp-event-types)
- [Requirements](#requirements)

---

## Installation

```xml
<dependencies>
  <dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc-provider-ucp-observability</artifactId>
    <version>1.0.6</version>
  </dependency>
</dependencies>
```

---

## Activation

Two ways to activate a provider, in priority order:

**1. Pool-level property** (highest priority) — set directly on the data source before the pool starts:

```java
public class MyApp {
  public static void main(String[] args) throws Exception {
    PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
    pds.setUCPEventListenerProvider("jfr-ucp-listener");
    // pds.setUCPEventListenerProvider("otel-ucp-listener");
  }
}
```

**2. JVM system property** — applies globally to all pools in the JVM:

```bash
java -DUCPEventListenerProvider=jfr-ucp-listener -jar myapp.jar
```

Or programmatically:

```java
public class MyApp {
  public static void main(String[] args) throws Exception {
    System.setProperty("UCPEventListenerProvider", "jfr-ucp-listener");
    // System.setProperty("UCPEventListenerProvider", "otel-ucp-listener");
  }
}
```

If no provider is configured, a no-op provider is used automatically with zero overhead.

---

## JFR Provider

Converts every UCP pool and connection event into a committed JFR event. Zero overhead when no recording is active.

### Recorded event types

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

| Field | Type | Description |
|---|---|---|
| `ucpTimestamp` | `long` | Event occurrence time in milliseconds since epoch |
| `poolName` | `String` | Unique identifier for the pool instance |
| `maxPoolSize` | `int` | Maximum allowed connections |
| `minPoolSize` | `int` | Minimum maintained connections |
| `borrowedConnectionsCount` | `int` | Currently checked-out connections |
| `availableConnectionsCount` | `int` | Ready-to-use connections |
| `totalConnections` | `int` | Current active connections (borrowed + available) |
| `createdConnections` | `int` | Total connections ever created |
| `closedConnections` | `int` | Total connections closed |
| `averageConnectionWaitTime` | `long` | Average milliseconds a thread waited to obtain a connection |

### Enabling a JFR recording

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

### Analysing events in JDK Mission Control

Open the `.jfr` file in [JDK Mission Control](https://www.oracle.com/java/technologies/jdk-mission-control.html). UCP events appear under **Event Browser → ucp**.

---

## OpenTelemetry Provider

Publishes UCP metrics through the [OpenTelemetry API](https://opentelemetry.io/docs/languages/java/). Event-driven — no background threads or polling. Depends only on `opentelemetry-api`; the SDK and exporter are the application's responsibility.

### SDK initialisation

The SDK must be registered with `GlobalOpenTelemetry` **before** the pool is started. Example using the Prometheus exporter:

```java
public class MyApp {
  public static void main(String[] args) throws Exception {
    // 1. Register the SDK before pool creation
    PrometheusHttpServer prometheusServer = PrometheusHttpServer.builder().setPort(9464).build();

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

#### Spec-aligned (`db.client.connection` prefix)

| Metric name | Instrument | Unit | Description |
|---|---|---|---|
| `db.client.connection.usage` | LongGauge | `{connection}` | Connections per state (`used` / `idle`). Recorded on every event. |
| `db.client.connection.max` | LongGauge | `{connection}` | Configured maximum pool size. |
| `db.client.connection.idle.min` | LongGauge | `{connection}` | Configured minimum pool size. |
| `db.client.connection.wait_time` | DoubleHistogram | `s` | Average borrow wait time. Recorded on `CONNECTION_BORROWED` only, when > 0. |

#### UCP-specific

| Metric name | Instrument | Unit | Description |
|---|---|---|---|
| `db.client.connection.established` | LongGauge | `{connection}` | Cumulative physical connections opened. |
| `db.client.connection.closed` | LongGauge | `{connection}` | Cumulative physical connections closed. |

#### Common attributes

| Attribute | Description |
|---|---|
| `db.client.connection.pool.name` | Pool name |
| `db.client.connection.state` | `used` / `idle` (on `usage` metric only) |

### Prometheus / Grafana quick-start

**1. Add the Prometheus exporter:**

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

**2. Configure Prometheus:**

```yaml
scrape_configs:
  - job_name: ucp
    static_configs:
      - targets: ["localhost:9464"]
```

**3. Useful PromQL queries:**

```promql
db_client_connection_usage{db_client_connection_state="used"}
db_client_connection_usage{db_client_connection_state="idle"}
sum by (db_client_connection_pool_name) (db_client_connection_usage)
histogram_quantile(0.95, rate(db_client_connection_wait_time_seconds_bucket[1m]))
db_client_connection_usage{db_client_connection_state="used"} / db_client_connection_max
```

---

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
