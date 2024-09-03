# Oracle JDBC Driver Extensions

Implementations of service provider interfaces (SPIs) that extend the
Oracle JDBC Driver for integration with cloud services and other specialized
APIs. These SPI implementations are referred as "providers" for short.

Each module of this project contains a set of providers. Three of these modules 
contain providers for a particular cloud platform:
<dl>
<dt><a href="ojdbc-provider-oci/README.md">Oracle JDBC OCI Providers</a></dt>
<dd>Providers for integration with Oracle Cloud Infrastructure (OCI)</dd>
<dt><a href="ojdbc-provider-azure/README.md">Oracle JDBC Azure Providers</a></dt>
<dd>Providers for integration with Microsoft Azure</dd>
<dt><a href="ojdbc-provider-gcp/README.md">Oracle JDBC GCP Providers</a></dt>
<dd>Providers for integration with Google Cloud Platform</dd>
</dl>
And the last one contains a provider for Open Telemetry:
<dl>
<dt><a href="ojdbc-provider-opentelemetry/README.md">Oracle JDBC Open Telemetry Provider</a></dt>
<dd>Provider for integration with Open Telemetry</dd>
<dl>
Visit any of the links above to learn about providers which are available for 
a particular platform.

## General Information

Starting in the 23.3 release, the Oracle JDBC Driver defines SPIs for providing
the driver with connection properties and other resources. The SPIs are defined
in the newly added `oracle.jdbc.spi` package. At runtime, the driver will locate
implementations of these interfaces using
[java.util.ServiceLoader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html).

The Oracle JDBC Driver Extensions project is focused on implementing these SPIs
for integration with widely used services, such as cloud computing platforms.

There are two general types of providers in this project:
<dl>
<dt>Centralized Config Providers</dt>
<dd>
A Centralized Config Provider provides Oracle JDBC with all the information it 
needs to create a database connection, including a connection string, user
credentials, and connection properties.
</dd>
<dt>Resource Providers</dt>
<dd>
A Resource Provider provides Oracle JDBC with an individual piece of 
information, such as a connection string, a username, or a password. Providers
of this type may use high level objects to represent a resource, such as an 
<code>SSLContext</code> that contains keys, certificates, and other information 
for TLS communication.
</dd>
</dl>

## General Usage

When a provider is present in a JVM's class path, Oracle JDBC can be configured 
to use it.

The providers are designed to be added into an existing system without requiring 
code changes. This means that no application code needs to be modified, and
no updates are required for the various frameworks, libraries, and tools that 
consume the JDBC driver.

The requirements for using a provider are:
<ul>
<li>The class path includes the provider's jar.</li>
<li>The class path includes Oracle JDBC 23.3 or newer.</li>
<li>Oracle JDBC is configured to use the provider.</li>
</ul>

The next sections describe how Oracle JDBC is configured to use the two general
types of providers.

### Using a Centralized Config Provider

A Centralized Config Provider is identified and configured by a JDBC URL of the
following form:
<pre>
jdbc:oracle:thin:@config-{provider-name}://[path][?parameters]
</pre>
For example, the URL below identifies and configures the Centralized Config Provider for Azure:
<pre>
jdbc:oracle:thin:@config-azure://myappconfig?key=/sales_app1/&label=dev
</pre>

URLs of this form may be used with any framework, library, or tool that already
accepts a JDBC URL. For instance, the URL might appear in the 
application.properties file of a Spring application:

```properties
spring.datasource.url=jdbc:oracle:thin:@config-azure://myappconfig?key=/sales_app1/&label=dev
```

### Using Resource Providers

Providers of individual resources are identified and configured using connection 
properties of the following form:

```
oracle.jdbc.provider.{resource-type}={provider-name}
oracle.jdbc.provider.{resource-type}.{parameter-name}={parameter-value}
```

For example, the connection properties below would identify and configure the 
OAUTH Token Provider for OCI:

```properties
oracle.jdbc.provider.accessToken=ojdbc-provider-oci-token
oracle.jdbc.provider.accessToken.scope=urn:oracle:db::id::ocid1.compartment.oc1..aaaaaaaajx2fpr7szach4vpdsjegvkbjirronlnwkxiivwmp6qfrissxgyia
```

These connection properties can appear in a 
[connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE),
or anywhere else that JDBC connection properties might be configured.

## Installation

Providers are installed by declaring a dependency on one or more modules of
this project:

[ojdbc-provider-oci](ojdbc-provider-oci/README.md#installation)

[ojdbc-provider-azure](ojdbc-provider-azure/README.md#installation)

[ojdbc-provider-gcp](ojdbc-provider-gcp/README.md#installation)

[ojdbc-provider-opentelemetry](ojdbc-provider-opentelemetry/README.md#installation)

Each module listed above is distributed on the Maven Central Repository as a
separate jar file. Coordinates can be found by visiting the links above.

## Examples

Examples for Oracle JDBC Driver Extensions can be found at [ojdbc-provider-samples/src/main/java](./ojdbc-provider-samples/src/main/java)

## Help

Are you having trouble with Oracle JDBC Driver Extensions? We want to help!

For help programming with Oracle JDBC Driver Extensions:

- Ask questions on Stack Overflow tagged with [[ojdbc-providers]](https://stackoverflow.com/tags/ojdbc-providers).
  The development team monitors Stack Overflow regularly.
- Ask a question on Discord channel #java_jdbc.

Issues may be opened as described in our [contribution guide](./CONTRIBUTING.md).

## Contributing

This project welcomes contributions from the community. Before submitting a pull
request, please [review our contribution guide](./CONTRIBUTING.md)

## Security

Please consult the [security guide](./SECURITY.md) for our responsible security 
vulnerability disclosure process

## Building from Source

For general usage, it is not necessary to compile and build the whole project.
Pre-built artifacts are available on Maven Central.

If you want to try out the latest changes, this project can be built with:
```sh
mvn clean install -DskipTests
```
The command above will publish jars to your local Maven cache.

## License

Copyright (c) 2023 Oracle and/or its affiliates. Released under the Universal Permissive License v1.0 as shown at <https://oss.oracle.com/licenses/upl/>.
