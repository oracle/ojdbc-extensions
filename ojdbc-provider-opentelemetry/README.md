# Oracle JDBC Open Telemetry Provider

This module contains a provider for integration between Oracle JDBC and
Open Telemetry.

This provider implements the TraceEventListener interface provided by the JDBC
driver which will be notified whenever events are generated in the driver and 
will publish these events into Open Telemetry. These events include:
 * Roundtrips to the database server
 * AC begin and sucess
 * VIP down event

## Semantic Conventions

This provider supports both **stable** and **experimental** (legacy) OpenTelemetry
semantic conventions for Oracle Database instrumentation.

### Semantic Convention Migration

The provider supports three modes controlled by the `OTEL_SEMCONV_STABILITY_OPT_IN`
environment variable:

* **Empty/Not Set (default)** - Emits only experimental (legacy) conventions for backward compatibility
* **`database`** - Emits only the new stable Oracle Database semantic conventions
* **`database/dup`** - Emits both old and new conventions (dual mode for migration)

See the [OpenTelemetry Oracle Database Semantic Conventions](https://opentelemetry.io/docs/specs/semconv/database/database-metrics/)
for details on the stable conventions.

### Roundtrip Events (Stable Conventions)

When `OTEL_SEMCONV_STABILITY_OPT_IN=database` or `database/dup`:

* **Required/Recommended Attributes**
    * `db.system.name` - Always set to "oracle.db"
    * `db.namespace` - Format: `{instance_name}|{database_name}|{service_name}`
    * `db.operation.name` - Database operation being executed (e.g., "SELECT", "INSERT")
    * `db.query.summary` - Low cardinality query summary
    * `server.address` - Database server hostname
    * `server.port` - Database server port (if non-default)
    * `oracle.db.query.sql.id` - Oracle SQL_ID
    * `oracle.db.session.id` - Oracle session ID
    * `oracle.db.server.pid` - Oracle server process ID
    * `oracle.db.shard.name` - Oracle shard name (if applicable)
    * `thread.id` - Current thread ID
    * `thread.name` - Current thread name

* **Opt-In Attributes** *(only present if sensitive data is enabled)*
    * `db.user` - Database user name
    * `db.query.text` - Actual SQL query text
    * `db.response.returned_rows` - Number of rows returned

* **Error Attributes** *(only present on errors)*
    * `error.type` - Exception class name
    * `db.response.status_code` - Oracle error code (format: "ORA-XXXXX")

### Roundtrip Events (Legacy Conventions)

When `OTEL_SEMCONV_STABILITY_OPT_IN` is empty/not set or `database/dup`:

* `Connection ID`
* `Database Operation`
* `Database User`
* `Database Tenant`
* `SQL ID`
* `Original SQL Text` *(only present if sensitive data is enabled)*
* `Actual SQL Text` *(only present if sensitive data is enabled)*

### Application Continuity (AC) Replay Events

**Stable Conventions** (`database` or `database/dup`):
* `db.system.name` - Always set to "oracle.db"
* `error.type` - Error message that triggered replay
* `db.response.status_code` - Oracle error code (format: "ORA-XXXXX")
* `db.operation.batch.size` - Current replay retry count

**Legacy Conventions** (default or `database/dup`):
* `Error Message`
* `Error code`
* `SQL state`
* `Current replay retry count`

### VIP Retry Events (RAC Failover)

**Stable Conventions** (`database` or `database/dup`):
* `db.system.name` - Always set to "oracle.db"
* `error.type` - Error message that triggered VIP retry
* `server.address` - VIP address being retried

**Opt-In VIP Debug Attributes** *(only present if sensitive data is enabled)*:
* `server.port` - Server port number
* `oracle.db.vip.protocol` - Connection protocol
* `oracle.db.vip.failed_host` - Host that failed during VIP retry
* `oracle.db.vip.service_name` - Oracle service name
* `oracle.db.vip.sid` - Oracle System Identifier (SID)
* `oracle.db.vip.connection_descriptor` - Full connection descriptor

**Legacy Conventions** (default or `database/dup`):
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
jar is compiled for JDK 8, and is forward compatible with later JDK versions. 
The coordinates for the latest release are:

```xml
<dependency>
  <groupId>com.oracle.database.jdbc</groupId>
  <artifactId>ojdbc-provider-opentelemetry</artifactId>
  <version>1.0.6</version>
</dependency>
```

## Usage 

To use the Oracle JDBC provider for Open Telemetry just add the artifact to the
application's classpath and set the following connection property :

```java
oracle.jdbc.provider.traceEventListener=open-telemetry-trace-event-listener-provider
```

### Enabling Stable Semantic Conventions

To use the new stable OpenTelemetry semantic conventions, set the environment variable:
```bash
# Use only stable conventions
export OTEL_SEMCONV_STABILITY_OPT_IN=database

# OR use both old and new conventions (dual mode for migration)
export OTEL_SEMCONV_STABILITY_OPT_IN=database/dup
```

## Configuration

The Oracle JDBC provider for Open Telemetry can be configured using system properties 
or a MBean. Two parameters can be configured: 
  * **Enabled**: when enabled (*true*) traces will be exported to Open 
 Telemetry. This property is **enabled by default**.
  * **Sensitive data enabled**: when enabled (*true*) attributes containing
 sensitive information like SQL statements and connection URL will be included
 in the traces. This property is **disabled by default**.

The system properties are "oracle.jdbc.provider.opentelemetry.enabled" and 
"oracle.jdbc.provider.opentelemetry.sensitive-enabled" respectively and the MBean 
with object name "com.oracle.jdbc.extension.opentelemetry:type=OpenTelemetryTraceEventListener"
exposes two attributes "Enabled" and "SensitiveDataEnabled".

 The sample code below shows how to retrieve the value of an attribute:
```java
ObjectName objectName = new ObjectName(
    "com.oracle.jdbc.extension.opentelemetry:type=OpenTelemetryTraceEventListener");
MBeanServer server = ManagementFactory.getPlatformMBeanServer();
boolean isEnabled = Boolean.valueOf(server.getAttribute(objectName, "Enabled").toString())
    .booleanValue(); 
```