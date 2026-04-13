# Local Guide for JFR and OpenTelemetry UCP Providers

This guide explains how to run the UCP observability demos locally from this repository.

Providers covered:

- `jfr-ucp-listener`
- `otel-ucp-listener`

Main sample classes:

- JFR: `src/main/java/oracle/ucp/provider/observability/stress/JfrSampleTestUCP.java`
- OTel: `src/main/java/oracle/ucp/provider/observability/stress/OtelUCPTest.java`

Dashboard file:

- `grafana-otel-stress-dashboard.json`

## 1. Tool setup

Before running anything, make sure the local machine has the following:

- JDK 11 or newer
- Maven
- IntelliJ IDEA
- Oracle database access
- wallet directory if using ATP/ADB
- JDK Mission Control for viewing `.jfr` recordings
- Prometheus
- Grafana

### Install Prometheus and Grafana on macOS

```bash
brew update
brew install prometheus grafana
```

### Start Grafana

```bash
brew services start grafana
```

Grafana UI:

- `http://localhost:3000`

First login:

- username: `admin`
- password: `admin`

### Prepare Prometheus

Create a local `prometheus.yml` file with:

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

### Add Prometheus to Grafana

In Grafana:

1. Open `Connections`
2. Add a `Prometheus` data source
3. Set the URL to `http://localhost:9090`
4. Save and test

## 2. Project setup

Open the repository in IntelliJ.

### Build the full project

From the repo root:

```bash
mvn clean install -DskipTests
```

This builds the full repository locally and makes sure all modules are resolved.

### Reload Maven in IntelliJ

After the build, reload the Maven project.

In IntelliJ:

1. Open the Maven tool window
2. Click `Reload All Maven Projects`
3. Wait for indexing to finish

## 3. Sample configuration

Before running the samples, update the database connection values in the sample class you want to use.

Fields to update:

- JDBC URL
- `TNS_ADMIN` wallet location if needed
- database user
- database password

Minimum required configuration looks like:

```java
pds.setURL("jdbc:oracle:thin:@<service>?TNS_ADMIN=<wallet-path>");
pds.setUser("<db-user>");
pds.setPassword("<db-password>");
```

## 4. Run the JFR demo

Open `JfrSampleTestUCP.java` in IntelliJ and run its `main` method.

Add this VM option in the run configuration:

```text
-XX:StartFlightRecording=filename=recording.jfr,settings=profile
```

What this demo does:

- creates and starts a pool
- borrows connections
- closes and returns connections
- triggers purge, recycle, and refresh
- stops and destroys the pool

How to verify:

- confirm `recording.jfr` was created
- open it in JDK Mission Control
- go to `Event Browser -> ucp`

Expected JFR event groups:

- pool lifecycle
- connection lifecycle
- maintenance events

## 5. Run the OpenTelemetry demo

Open `OtelUCPTest.java` in IntelliJ and run its `main` method.

This demo uses:

- provider: `otel-ucp-listener`
- pool name: `OtelTestPool`
- Prometheus endpoint: `http://localhost:9464/metrics`

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

## 6. Import the Grafana dashboard

Use the OTel stress sample first, then import:

- `ojdbc-provider-ucp-observability/grafana-otel-stress-dashboard.json`

In Grafana:

1. Open `Dashboards`
2. Click `New`
3. Click `Import`
4. Upload `grafana-otel-stress-dashboard.json`
5. Select the Prometheus data source
6. Save the dashboard

This dashboard is built for pool name `OtelTestPool`.

It visualizes:

- used connections
- idle connections
- total live connections
- average wait time
- created physical connections
- closed physical connections
- current pool size stats

## 7. Customize the demos

The sample classes are only starting points. They can be edited freely, or copied to create custom local scenarios.

Typical changes:

- increase or decrease pool sizes
- change borrow and return patterns
- add more threads
- increase wait times
- add SQL execution loops
- adjust pool names for easier dashboard reading
- create a new stress scenario derived from `OtelUCPTest.java`

Best starting points:

- `JfrSampleTestUCP.java` for JFR experiments
- `OtelUCPTest.java` for OTel stress and dashboard experiments
