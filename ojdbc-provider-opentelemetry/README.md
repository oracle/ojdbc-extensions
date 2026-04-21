# Oracle JDBC Open Telemetry Provider

This module contains a provider for integration between Oracle JDBC and
Open Telemetry.

This provider implements the TraceEventListener interface provided by the JDBC
driver which will be notified whenever events are generated in the driver and 
will publish these events into Open Telemetry. These events include:
 * roundtrips to the database server
 * AC begin and sucess
 * VIP down event

The following attributes are added the the traces for each event:
 * **Roundtrips**
    * Connection ID
    * Database Operation
    * Database User
    * Database Tenant
    * SQL ID
    * Original SQL Text *(only present if sensitive data is enabled)*
    * Actual SQL Text *(only present if sensitive data is enabled)*
  * **AC begin and success**
    * Error Message
    * Error code
    * SQL state
    * Current replay retry count
  * **VIP down event**
    * Error message
    * VIP address
    * Protocol *(only present if sensitive data is enabled)*
    * Host *(only present if sensitive data is enabled)*
    * Port *(only present if sensitive data is enabled)*
    * Service name *(only present if sensitive data is enabled)*
    * SID *(only present if sensitive data is enabled)*
    * Connection data *(only present if sensitive data is enabled)*

## Installation

This provider is distributed as single jar on the Maven Central Repository. The 
jar is compiled for JDK 8, and is forward compatible with later JDK versions. 
The coordinates for the latest release are:

```xml
<dependency>
  <groupId>com.oracle.database.jdbc</groupId>
  <artifactId>ojdbc-provider-opentelemetry</artifactId>
  <version>1.0.7</version>
</dependency>
```

## Usage 

To use the Oracle JDBC provider for Open Telemetry just add the artifact to the
application's classpath and set the following connection property :

```java
oracle.jdbc.provider.traceEventListener=open-telemetry-trace-event-listener-provider
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