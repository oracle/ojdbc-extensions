# Oracle JDBC Observability Provider

This module contains a provider that adds tracing capabilities to the Oracle 
JDBC driver. Two tracers are available:
  * OTEL: adds Open Telemetry tracing capabilities.
  * JFR: exports events to Java Flight Recorder.

This provider implements the TraceEventListener interface provided by the JDBC
driver which will be notified whenever events are generated in the driver and 
will publish these events into Open Telemetry. These events include:
 * roundtrips to the database server
 * AC begin and success
 * VIP down event

## Semantic Conventions (OpenTelemetry Tracer)

The OpenTelemetry tracer supports both **stable** and **experimental** (legacy)
OpenTelemetry semantic conventions for Oracle Database instrumentation.

### Semantic Convention Migration

The OpenTelemetry tracer supports three modes controlled by the `OTEL_SEMCONV_STABILITY_OPT_IN`
environment variable:

* **Empty/Not Set (default)** - Emits only experimental (legacy) conventions for backward compatibility
* **`database`** - Emits only the new stable Oracle Database semantic conventions
* **`database/dup`** - Emits both old and new conventions (dual mode for gradual migration)

This configuration can be set via environment variable or changed at runtime through the MBean interface.

See the [OpenTelemetry Database Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/database/database-spans/)
for details on the stable conventions.

### Roundtrip Events

#### Stable Conventions
When `OTEL_SEMCONV_STABILITY_OPT_IN=database` or `database/dup`:

* **Required/Recommended Attributes**
    * `db.system.name` - Always set to `"oracle.db"` (identifies Oracle Database)
    * `db.namespace` - The Oracle service name from the connection
    * `db.operation.name` - Database operation being executed (e.g. `ExecuteQuery`)
    * `db.query.summary` - Low cardinality SQL command type (e.g., `SELECT`, `INSERT`)
    * `server.address` - Database server hostname (e.g., `db.example.com`)
    * `server.port` - Database server port (if non-default, i.e., not 1521)
    * `oracle.db.instance.id` - Oracle database instance identifier
    * `oracle.db.pdb` - Oracle Pluggable Database (PDB) name
    * `oracle.db.query.sql.id` - Oracle SQL identifier (e.g., `8vq9m5kx3n2wh`)
    * `oracle.db.session.id` - Oracle session identifier (e.g., `1234`)
    * `oracle.db.server.pid` - Oracle server process ID on the database host (e.g., `56789`)
    * `oracle.db.shard.name` - Oracle shard database name (only populated when using Oracle Sharding)
    * `thread.id` - JVM thread ID that executed the operation
    * `thread.name` - JVM thread name that executed the operation

* **Opt-In Attributes** *(only present if sensitive data is enabled)*
    * `db.user` - Database username used for the connection (e.g., `APP_USER`, `SCOTT`)
    * `db.query.text` - Full SQL statement text sent to the database
    * `db.response.returned_rows` - Number of rows returned

* **Error Attributes** *(only present on errors)*
    * `error.type` - Exception class name (e.g., `java.sql.SQLSyntaxErrorException`)
    * `db.response.status_code` - Oracle error code in format `ORA-XXXXX` (e.g., `ORA-00942`, `ORA-01017`)

#### Legacy Conventions
When `OTEL_SEMCONV_STABILITY_OPT_IN` is empty/not set or `database/dup`:

* `Connection ID` - JDBC connection identifier
* `Database Operation` - Database operation being executed (e.g., `Commit`, `ExecuteQuery`)
* `Database Tenant` - Tenant name
* `SQL ID` - Oracle SQL identifier (e.g., `8vq9m5kx3n2wh`)
* `thread.id` - JVM thread ID that executed the operation
* `thread.name` - JVM thread name that executed the operation
* `Database User` *(only present if sensitive data is enabled)*
* `Original SQL Text` *(only present if sensitive data is enabled)*
* `Actual SQL Text` *(only present if sensitive data is enabled)*

### Application Continuity (AC) Replay Events

#### Stable Conventions
When `OTEL_SEMCONV_STABILITY_OPT_IN=database` or `database/dup`:

* `db.system.name` - Always set to `"oracle.db"` (identifies Oracle Database)
* `error.type` - The error message text that triggered the replay attempt
* `db.response.status_code` - Oracle error code that triggered replay in format `ORA-XXXXX` (e.g., `ORA-03135`, `ORA-25408`)
* `oracle.db.ac.retry_count` - Application Continuity replay attempt number

#### Legacy Conventions
When `OTEL_SEMCONV_STABILITY_OPT_IN` is empty/not set or `database/dup`:

* `Error Message` - Error message text that triggered the replay attempt
* `Error code`
* `SQL state`
* `Current replay retry count` - Application Continuity replay attempt number

### VIP Retry Events 

#### Stable Conventions
When `OTEL_SEMCONV_STABILITY_OPT_IN=database` or `database/dup`:

* `db.system.name` - Always set to `"oracle.db"` (identifies Oracle Database)
* `error.type` - The error message that triggered the VIP retry
* `server.address` - The VIP (Virtual IP) address that is being retried after failure

**Opt-In VIP Debug Attributes** *(only present if sensitive data is enabled)*:
* `server.port` - Database server port number being retried
* `oracle.db.vip.protocol` - Connection protocol being used
* `oracle.db.vip.failed_host` - The hostname that failed, triggering the VIP retry
* `oracle.db.vip.service_name` - Oracle service name for the failed connection
* `oracle.db.vip.sid` - Oracle System Identifier (SID) for the failed connection
* `oracle.db.vip.connection_descriptor` - Complete JDBC connection descriptor string

#### Legacy Conventions
When `OTEL_SEMCONV_STABILITY_OPT_IN` is empty/not set or `database/dup`:

* `Error message`
* `VIP Address`
* `Protocol` *(only present if sensitive data is enabled)*
* `Host` *(only present if sensitive data is enabled)*
* `Port` *(only present if sensitive data is enabled)*
* `Service name` *(only present if sensitive data is enabled)*
* `SID` *(only present if sensitive data is enabled)*
* `Connection data` *(only present if sensitive data is enabled)*

## Installation

This provider is distributed as single jar on the Maven Central Repository. The 
jar is compiled for JDK 11, and is forward compatible with later JDK versions. 
The coordinates for the latest release are:

```xml
<dependency>
  <groupId>com.oracle.database.jdbc</groupId>
  <artifactId>ojdbc-provider-observability</artifactId>
  <version>1.0.6</version>
</dependency>
```

## Usage 

To use the Oracle JDBC observability provider just add the artifact to the
application's classpath and set the following connection property:

```java
oracle.jdbc.provider.traceEventListener=observability-trace-event-listener-provider
```

A unique identifier connection property allows to identify the trace event 
listener. Connections setting the same unique identifier use the same trace 
event listener and share the same configuration. 

```java
oracle.jdbc.provider.traceEventListener.unique_identifier=<unique identifier>
```

If no unique identifier is provided, the unique idetifier "default" is used.

### Enabling Stable Semantic Conventions (OpenTelemetry)

To use the new stable OpenTelemetry semantic conventions, set the environment variable:
```bash
# Use only stable conventions
export OTEL_SEMCONV_STABILITY_OPT_IN=database

# OR use both old and new conventions (dual mode for migration)
export OTEL_SEMCONV_STABILITY_OPT_IN=database/dup
```

## Configuration

The provider can be configured by: 
* using system properties,
```java
System.setProperty("oracle.jdbc.provider.observability.enabledTracers", "OTEL,JFR");
System.setProperty("oracle.jdbc.provider.observability.sensitiveDataEnabled", "true");
```
* or using the MBean.
```java
ObservabilityTraceEventListener listener = ObservabilityTraceEventListener.getTraceEventListener("<unique identifier>");
ObjectName objectName = new ObjectName(listener.getMBeanObjectName());
MBeanServer server = ManagementFactory.getPlatformMBeanServer();
server.setAttribute(objectName, new Attribute("EnabledTracers", "OTEL,JFR"));
server.setAttribute(objectName, new Attribute("SensitiveDataEnabled", "true"));
```
* it is also possible to use the ObservabilityConfiguration object directly by
calling the 
```java
ObservabilityConfiguration configuration = ObservabilityTraceEventListener.getObservabilityConfiguration("<name>");
configuration.setEnabledTracers("OTEL,JFR");
configuration.setSensitiveDataEnabled(true);
```

## Backward compatibility

### Usage

The provider can also be used by setting the following connection property:

```java
oracle.jdbc.provider.traceEventListener=open-telemetry-trace-event-listener-provider
```

When this property is used only the OTEL tracer can be used.

### Configuration

To ensure backward compatibility Oracle JDBC provider for Open Telemetry configuration
properties and MBean have been kept. When these properties and MBean are used only
the Open Telemetry tracer will be enabled.

The Oracle JDBC provider for Open Telemetry can be configured using system properties 
or a MBean. Two parameters can be configured: 
  * **Enabled**: when enabled (*true*) traces will be exported to Open 
 Telemetry. This property is **enabled by default**.
  * **Sensitive data enabled**: when enabled (*true*) attributes containing
 sensitive information like SQL statements and connection URL will be included
 in the traces. This property is **disabled by default**.

The system properties are "oracle.jdbc.provider.opentelemetry.enabled" and 
"oracle.jdbc.provider.opentelemetry.sensitive-enabled" respectively and the MBean 
exposes two attributes "Enabled" and "SensitiveDataEnabled".

 The sample code below shows how to retrieve the value of an attribute:
```java
ObservabilityTraceEventListener listener = ObservabilityTraceEventListener.getTraceEventListener("<name>");
ObjectName objectName = new ObjectName(listener.getMBeanObjectName());
MBeanServer server = ManagementFactory.getPlatformMBeanServer();
boolean isEnabled = Boolean.valueOf(server.getAttribute(objectName, "Enabled").toString())
    .booleanValue(); 
```