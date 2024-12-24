# Oracle JDBC Java Flight Recorder Provider

This module contains a provider for integration between Oracle JDBC and
Java Flight Recorder.

This provider implements the TraceEventListener interface provided by the JDBC
driver which will be notified for every round trip and generate Java Flight 
Recorder events. The following attributes are added the the traces for each event:
 * **Roundtrips**
    * Connection ID
    * Database Operation
    * Database Tenant
    * SQL ID
    * Database User *(only present if sensitive data is enabled)*
    * Original SQL Text *(only present if sensitive data is enabled)*
    * Actual SQL Text *(only present if sensitive data is enabled)*

## Installation

This provider is distributed as a single jar on the Maven Central Repository. The 
jar is compiled for JDK 11, and is forward compatible with later JDK versions. 
The coordinates for the latest release are:

```xml
<dependency>
  <groupId>com.oracle.database.jdbc</groupId>
  <artifactId>ojdbc-provider-jfr</artifactId>
  <version>1.0.1</version>
</dependency>
```

## Usage 

To use the Oracle JDBC provider for Java Flight Recorder just add the artifact to the
application's classpath and set the following connection property :

```java
oracle.jdbc.provider.traceEventListener=java-flight-recorder-trace-event-listener-provider
```
