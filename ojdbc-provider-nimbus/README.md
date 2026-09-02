# Oracle JDBC Providers for Nimbus 

This module contains providers for integration between Oracle JDBC and 
[Nimbus OAuth 2.0 SDK with OpenID Connect extensions](https://connect2id.com/products/nimbus-oauth-openid-connect-sdk).

## Resource Providers
This module only contains an
<a href="#access-token-provider">access token provider</a>.

## Installation

The access token provider in this module is distributed as a single jar on the
Maven Central Repository. The jar is compiled for JDK 8, and is forward 
compatible with later JDK versions. The coordinates for the latest release are:
```xml
<dependency>
  <groupId>com.oracle.database.jdbc</groupId>
  <artifactId>ojdbc-provider-nimbus</artifactId>
  <version>1.1.0</version>
</dependency>
```

## Command-Line Setup Helper

The Nimbus provider jar includes an interactive setup helper for generating
provider configuration from the command line.

The helper generates resource-provider connection properties for the Access
Token provider. It does not contact any authorization server or validate
credentials; it only prints the values you configure.

### Running the helper

The helper is launched from the provider jar with the `--setup` flag:

```bash
java -jar ojdbc-provider-nimbus-1.1.0.jar --setup
```

Running the jar without `--setup` prints a short info banner (name,
version, a one-line description, and a link to docs) and exits
immediately, without reading from standard input.

For direct `java -jar` execution, `ojdbc-provider-common-1.1.0.jar` must be
present in the same directory as this jar.

## Access Token Provider
The Nimbus Access Token Provider provides Oracle JDBC with an access token that 
authorizes logins to an Autonomous Database. This is a
[Resource Provider](https://docs.oracle.com/en/database/oracle/oracle-database/26/jajdb/oracle/jdbc/spi/OracleResourceProvider.html)
identified by the name `ojdbc-provider-nimbus-token`.

Oracle JDBC automatically uses this provider when it (and its dependencies) 
are included on the class/module path of a Java application, and 
connection properties are configured as shown in the example below. The 
configuration must not include a traditional database username or password, 
because it will [override the access token configuration](https://docs.oracle.com/en/database/oracle/oracle-database/26/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_PROVIDER_ACCESS_TOKEN).

This provider should be configured to authenticate as an OAuth 2.0 client that 
has been mapped to a `USER` or `DATA ROLE` in Oracle Database. Follow any of the
links below for more information on the setup:
- [Mapping a USER to an Identity in Azure Entra ID](https://docs.oracle.com/en/cloud/paas/autonomous-database/serverless/adbsb/manage-users-azure-ad.html)
- [Mapping a DATA ROLE to an Identity in Azure Entra ID or Oracle Cloud](https://docs.oracle.com/en/database/oracle/oracle-database/26/ddscg/iam-managed-users-connecting-directly-5.html)

This provider won't work if you're [mapping a USER to an Identity in Oracle Cloud](https://docs.oracle.com/en/cloud/paas/autonomous-database/serverless/adbsb/manage-users-iam.html).
For that scenario, you should use the [OCI Token Provider](../ojdbc-provider-oci/README.md#access-token-provider) 
from the ojdbc-provider-oci module instead.

Any application using version 19 or newer of Oracle JDBC 
(ojdbc8/ojdbc11/etc) can use this provider without changing code of the 
application or updating any libraries it depends on. The only changes
required are to include the provider on the class/module path, and to configure
Oracle JDBC as shown in the example below.


```properties
# Configures Oracle JDBC to identity the name of the Nimbus Token Provider.
oracle.jdbc.provider.accessToken=ojdbc-provider-nimbus-token

# The URI of an authorization server endpoint for requesting access tokens
oracle.jdbc.provider.accessToken.tokenEndpoint=https://example.com/oauth2/v1/token

# Configures the grant type, which determines which credentials are used to
# authenticate with an authorization server when requesting an access token.
oracle.jdbc.provider.accessToken.grantType=client_credentials

# Configures a scope to identify a registered client, or application, that
# represents an instance of Oracle Database.
oracle.jdbc.provider.accessToken.scope=https://tenant-name.cloud-name.example.com/scope.name

# The ID of an OAuth 2.0 client used when requesting access tokens
oracle.jdbc.provider.accessToken.clientId=aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee

# The secret of an OAuth 2.0 client used when requesting access tokens
oracle.jdbc.provider.accessToken.clientSecret=$3cr3t
```
These properties may be stored in an
[Oracle JDBC connection properties file](https://docs.oracle.com/en/database/oracle/oracle-database/26/jajdb/oracle/jdbc/OracleConnection.html#CONNECTION_PROPERTY_CONFIG_FILE),
or other places where datasource/connection properties get 
configured. Configuration with JVM system properties is not supported.

The full set of properties that configure this provider are listed in the table
below.
<table>
<thead><tr>
<th>Parameter Name</th>
<th>Description</th>
<th>Accepted Values</th>
<th>Default Value</th>
</tr></thead><tbody><tr><td>
oracle.jdbc.provider.accessToken.tokenEndpoint
</td><td>
The 
<a href="https://datatracker.ietf.org/doc/html/rfc6749#autoid-31">
token request endpoint
</a>
of an OAuth 2.0 authorization server. Access tokens will be requested from this 
endpoint.
</td><td>
This should be the token request endpoint of an authorization server, such as:
<pre>
https://example.com/oauth2/v1/token
</pre>
</td><td>
<i>No default value. A value must be configured for this parameter.</i>
</td></tr><tr><td>
oracle.jdbc.provider.accessToken.grantType
</td><td>
The 
<a href="https://datatracker.ietf.org/doc/html/rfc6749#section-4">
type of grant
</a>
to use as authorization when requesting an access token.
</td><td>
Any of the following values are accepted: <code>client_credentials</code>,
<code>password</code>, or <code>authorization_code</code>.
</td><td>
<i>No default value. A value must be configured for this parameter.</i>
</td></tr><tr><td>
oracle.jdbc.provider.accessToken.scope
</td><td>
A 
<a href="https://datatracker.ietf.org/doc/html/rfc6749#autoid-33">
scope 
</a>
identifying resources of an Oracle Database that the client will 
access. This parameter may be configured with multiple scopes, each one 
separated by a space.
</td><td>
The scope should identify a registered client, or application, that represents
an instance of Oracle Database. It might look like:
<pre>
https://tenant-name.cloud-name.example.com/scope.name
</pre>
Multiple scopes can be separated by spaces:
<pre>
https://tenant-name.cloud-name.example.com/scope.one https://tenant-name.cloud-name.example.com/scope.two
</pre>
</td><td>
<i>No default value. In most cases, a value must be configured for this parameter.</i>
</td></tr><tr><td>
oracle.jdbc.provider.accessToken.clientId
</td><td>
The 
<a href="https://datatracker.ietf.org/doc/html/rfc6749#autoid-17">
identifier of an OAuth 2.0 client
</a>. This ID is used when requesting an access token.
</td><td>
<i>
This should be a unique string assigned to a registered client.
</i>
</td><td>
<i>No default value. A value must be configured for this parameter.</i>
</td></tr><tr><td>
oracle.jdbc.provider.accessToken.clientSecret
</td><td>
The 
<a href="https://datatracker.ietf.org/doc/html/rfc6749#section-2.3.1">
secret of an OAuth 2.0 client
</a>. The secret is used to authenticate the client.
</td><td>
<i>
This should be a secret of a registered client.
</i>
</td><td>
<i>No default value. A value must be configured for this parameter when using 
the client_credentials grant type. It most cases, it will be required for the
password grant type as well.</i>
</td></tr><tr><td>
oracle.jdbc.provider.accessToken.username
</td><td>
The 
<a href="https://datatracker.ietf.org/doc/html/rfc6749#autoid-45">
username of a resource owner
</a>. The username is used when requesting an access token with the password
grant type.
</td><td>
<i>
This should be the username of a resource owner.
</i>
</td><td>
<i>No default value. A value must be configured for this parameter when using 
the password grant type.</i>
</td></tr><tr><td>
oracle.jdbc.provider.accessToken.password
</td><td>
The 
<a href="https://datatracker.ietf.org/doc/html/rfc6749#autoid-45">
password of a resource owner
</a>. The password is used when requesting an access token with the password
grant type.
</td><td>
<i>
This should be the password of a resource owner.
</i>
</td><td>
<i>No default value. A value must be configured for this parameter when using 
the password grant type.</i>
</td></tr><tr><td>
oracle.jdbc.provider.accessToken.authorizationEndpoint
</td><td>
The 
<a href="https://datatracker.ietf.org/doc/html/rfc6749#autoid-23">
authorization endpoint
</a>
of an OAuth 2.0 authorization server. When the authorization_code grant type is
used, the system default web is opened and sent to a web page at this endpoint. 
The web page will typically prompt a user to log in with their credentials.
</td><td>
This should be the authorization endpoint of an authorization server, such as:
<pre>
https://example.com/oauth2/v1/authorize
</pre>
</td><td>
<i>No default value. A value must be configured for this parameter when using
the authorization_code grant type.</i>
</td></tr><tr><td>
oracle.jdbc.provider.accessToken.redirectUri
</td><td>
The 
<a href="https://datatracker.ietf.org/doc/html/rfc6749#autoid-25">
redirection endpoint
</a>
where a web browser is redirected to when `grantType=authorization_code`. A 
server will begin running locally just before the web browser is opened, and it
will wait for a single request made to the redirection endpoint.
</td><td>
This should be a URI of the form http://localhost:{port-number} such as:
<pre>
http://localhost:1977
</pre>
Most authorization servers will require a redirect URI which has been registered
for an OAuth 2.0 client.
</td><td>
<i>No default value. A value must be configured for this parameter when using
the authorization_code grant type.</i>
</td></tr></tbody></table>

#### Caching Mechanism
The Access Token Provider employs a caching mechanism to efficiently manage and reuse access tokens. By utilizing Oracle JDBC's cache for JWTs, access tokens are
cached and updated one minute before they expire, ensuring no blocking of threads. This cache reduces latency when creating JDBC connections, as a thread opening
a connection does not have to wait for a new token to be requested.You can check this in more detail
at [Oracle's documentation](https://docs.oracle.com/en/database/oracle/oracle-database/26/jajdb/oracle/jdbc/AccessToken.html).
