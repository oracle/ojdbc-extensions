# Local Guide for JFR and OpenTelemetry UCP Providers

This guide is for running the UCP observability providers locally from this repository.

Available providers:

- `jfr-ucp-listener`
- `otel-ucp-listener`

## Files used in this walkthrough

- JFR sample:
  - `src/main/java/oracle/ucp/provider/observability/stress/JfrSampleTestUCP.java`
- OTel simple sample:
  - `src/main/java/oracle/ucp/provider/observability/stress/OtelSampleTestUCP.java`
- OTel stress sample:
  - `src/main/java/oracle/ucp/provider/observability/stress/OtelUCPTest.java`
- Grafana dashboard for the stress test:
  - `grafana-otel-stress-dashboard.json`

## Prerequisites

- JDK 11 or newer
- Maven
- Access to an Oracle database reachable from the machine
- If using ATP/ADB, a valid wallet directory
- For JFR viewing: JDK Mission Control
- For OTel dashboarding: Prometheus and Grafana on macOS

## Recommended order

Follow the steps in this order:

1. Update the sample with your local database connection details
2. Build the module
3. If needed, install Prometheus and Grafana
4. Run the JFR sample
5. Run the OTel sample
6. Configure Prometheus
7. Start Grafana
8. Add Prometheus as a Grafana data source
9. Run the OTel stress sample and import the Grafana dashboard

## Step 1: Update the local connection details

The sample classes use hardcoded local database values. Before running anything, update the sample you plan to use with your own:

- JDBC URL
- `TNS_ADMIN` wallet location if needed
- database user
- database password
- pool name if you want a different label in JFR or metrics

Minimum required connection setup:

```java
pds.setURL("jdbc:oracle:thin:@<service>?TNS_ADMIN=<wallet-path>");
pds.setUser("<db-user>");
pds.setPassword("<db-password>");
```

## Step 2: Build the module

From the repo root:

```bash
mvn -pl ojdbc-provider-ucp-observability -DskipTests package
```

## Step 3: Install Prometheus and Grafana on macOS

If Prometheus and Grafana are not already installed:

```bash
brew update
brew install prometheus grafana
```

## Step 4: Run the JFR provider

The JFR sample uses `jfr-ucp-listener` and emits UCP events into a flight recording.

Run:

```bash
mvn -pl ojdbc-provider-ucp-observability \
  -Dexec.mainClass=oracle.ucp.provider.observability.stress.JfrSampleTestUCP \
  -Dexec.classpathScope=runtime \
  -Dexec.jvmArgs="-XX:StartFlightRecording=filename=recording.jfr,settings=profile" \
  org.codehaus.mojo:exec-maven-plugin:3.5.0:java
```

What this sample does:

- creates and starts a pool
- borrows connections
- closes and returns connections
- triggers purge, recycle, and refresh
- stops and destroys the pool

How to verify:

- confirm `recording.jfr` was created
- open it in JDK Mission Control
- go to `Event Browser -> ucp`

Expected event groups:

- pool lifecycle
- connection lifecycle
- maintenance events

## Step 5: Run the OpenTelemetry provider

The OTel samples use `otel-ucp-listener` and expose metrics on `localhost:9464`.

### Option A: simple OTel demo

Run:

```bash
mvn -pl ojdbc-provider-ucp-observability \
  -Dexec.mainClass=oracle.ucp.provider.observability.stress.OtelSampleTestUCP \
  -Dexec.classpathScope=runtime \
  org.codehaus.mojo:exec-maven-plugin:3.5.0:java
```

### Option B: stress OTel demo

Run:

```bash
mvn -pl ojdbc-provider-ucp-observability \
  -Dexec.mainClass=oracle.ucp.provider.observability.stress.OtelUCPTest \
  -Dexec.classpathScope=runtime \
  org.codehaus.mojo:exec-maven-plugin:3.5.0:java
```

How to verify:

- open `http://localhost:9464/metrics`
- or run:

```bash
curl http://localhost:9464/metrics | grep db_client_connection
```

Expected metrics include:

- `db_client_connection_usage`
- `db_client_connection_max`
- `db_client_connection_idle_min`
- `db_client_connection_wait_time_seconds`
- `db_client_connection_established`
- `db_client_connection_closed`

## Step 6: Configure Prometheus

Create a local `prometheus.yml` file:

```yaml
global:
  scrape_interval: 5s

scrape_configs:
  - job_name: ucp
    static_configs:
      - targets: ["localhost:9464"]
```

Start Prometheus from the directory containing that file:

```bash
prometheus --config.file=prometheus.yml
```

Prometheus UI:

- `http://localhost:9090`

## Step 7: Start Grafana

Run:

```bash
brew services start grafana
```

Grafana UI:

- `http://localhost:3000`

First login:

- username: `admin`
- password: `admin`

Grafana will ask for a password change on first sign-in.

## Step 8: Add Prometheus as a Grafana data source

In Grafana:

1. Open `Connections`
2. Add a `Prometheus` data source
3. Set the URL to `http://localhost:9090`
4. Save and test

## Step 9: Import the OTel stress dashboard

This step is intended for the stress sample:

```bash
mvn -pl ojdbc-provider-ucp-observability \
  -Dexec.mainClass=oracle.ucp.provider.observability.stress.OtelUCPTest \
  -Dexec.classpathScope=runtime \
  org.codehaus.mojo:exec-maven-plugin:3.5.0:java
```

Import this dashboard file:

- `ojdbc-provider-ucp-observability/grafana-otel-stress-dashboard.json`

In Grafana:

1. Open `Dashboards`
2. Click `New`
3. Click `Import`
4. Upload `grafana-otel-stress-dashboard.json`
5. Select the Prometheus data source
6. Save the dashboard

This dashboard is built for the stress sample pool name `OtelTestPool` and visualizes:

- used connections
- idle connections
- total live connections
- average wait time
- created physical connections
- closed physical connections
- current pool size stats

## Provider activation note

The samples set the provider directly on the pool:

```java
pds.setUCPEventListenerProvider("jfr-ucp-listener");
```

or:

```java
pds.setUCPEventListenerProvider("otel-ucp-listener");
```

There is also a JVM-wide option:

```bash
java -DUCPEventListenerProvider=jfr-ucp-listener ...
```

For local demos, use the sample code as-is and keep the pool-level setting.

## Troubleshooting

- No JFR events:
  - confirm the sample uses `jfr-ucp-listener`
  - confirm flight recording was enabled on the JVM
  - confirm the pool actually started and emitted events

- No OTel metrics:
  - confirm the sample uses `otel-ucp-listener`
  - confirm the OTel SDK is registered before the pool starts
  - confirm port `9464` is free

- Prometheus shows no UCP metrics:
  - confirm the OTel sample is still running
  - confirm Prometheus is scraping `localhost:9464`
  - open `http://localhost:9464/metrics` directly and verify data exists

- Grafana dashboard is empty:
  - confirm the Prometheus data source is working
  - confirm the imported dashboard is being used with `OtelUCPTest`
  - confirm the pool name is still `OtelTestPool`

- Database connection failures:
  - verify wallet path
  - verify JDBC URL and service name
  - verify user and password
