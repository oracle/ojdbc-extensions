# Oracle JDBC Providers for Spring

This module contains providers for integrations between Oracle JDBC and 
[Spring](https://spring.io/).

## Installation

Providers in this module are distributed as a single jar on the
Maven Central Repository. The jar is compiled for JDK 17 and is forward 
compatible with later JDK versions. The coordinates for the latest release are:
```xml
<dependency>
  <groupId>com.oracle.database.jdbc</groupId>
  <artifactId>ojdbc-provider-spring</artifactId>
  <version>1.1.0</version>
</dependency>
```
Providers in this module are compatible with applications that use Oracle JDBC
23.26.4.0.0 or newer.

## End User Security Context Provider
The End User Security Context Provider provides a security context that enables
[Deep Data Security features in Oracle Database](https://docs.oracle.com/en/database/oracle/oracle-database/26/ddscg/understand-oracle-deep-data-security.html).
This is a [Resource Provider](https://docs.oracle.com/en/database/oracle/oracle-database/26/jajdb/oracle/jdbc/spi/OracleResourceProvider.html)
identified by the name `ojdbc-provider-spring-end-user-security-context`.

When this provider is activated, it provides instances of
[oracle.jdbc.EndUserSecurityContext](https://docs.oracle.com/en/database/oracle/oracle-database/26/jajdb/oracle/jdbc/EndUserSecurityContext.html)
to Oracle JDBC. An `EndUserSecurityContext` allows operations on the database to
execute under the security policy of a specific end user. 

### OAuth 2.0 Authorization
The End User Security Context Provider requests an OAuth 2.0 access token using
a [Spring OAuth 2.0 client registration](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/core.html#oauth2Client-client-registration).
This token authorizes the application to access the database. For certain grant
types, this token will authorize the application to act on behalf of an end 
user.

#### Client Registration Example
An example OAuth 2.0 client registration is shown below using a Spring 
application.yaml file:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: "https://login.microsoftonline.com/${TENANT_ID}/v2.0"
      client:
        provider:
          azure:
            token-uri: "https://login.microsoftonline.com/${TENANT_ID}/oauth2/v2.0/token"
        registration:
          azure:
            authorization-grant-type: urn:ietf:params:oauth:grant-type:jwt-bearer
            client-id: "${SPRING_APP_CLIENT_ID}"
            client-secret: "${SPRING_APP_CLIENT_SECRET}"
            scope: "${DATABASE_APP_ID_URI}/.default"
  datasource:
    url: jdbc:oracle:thin:@tcps://example.com:1522/example-service-name
    username: db_user
    password: db_password
    hikari:
      data-source-properties:
        oracle.jdbc.provider.endUserSecurityContext: ojdbc-provider-spring-end-user-security-context
        oracle.jdbc.provider.endUserSecurityContext.registrationId: azure
        oracle.jdbc.provider.endUserSecurityContext.requestParameters: requested_token_use=on_behalf_of
```
In this example, an OAuth 2.0 client registration is defined by properties 
under the `spring.security.oauth2` namespace. The client registration has an ID
of "azure", and that ID is referenced by the 
`oracle.jdbc.provider.endUserSecurityContext.registrationId` datasource 
property. This configures the provider to use the "azure" registration when
requesting a database access token. 

Additional datasource properties that appear in the example are 
`oracle.jdbc.provider.endUserSecurityContext` and 
`oracle.jdbc.provider.endUserSecurityContext.requestParameters`. The
`oracle.jdbc.provider.endUserSecurityContext` property sets the name of this
provider. Oracle JDBC will not use this provider unless this property is set. 
The `oracle.jdbc.provider.endUserSecurityContext.requestParameters` property 
configures extra parameters to include in the token request. For Azure, the 
`requested_token_use` parameter signals that the application is acting on 
behalf of a user.

#### JWT Bearer and Token Exchange Grant Types 
The End User Security Context Provider may be configured to
use the `urn:ietf:params:oauth:grant-type:jwt-bearer` or
`urn:ietf:params:oauth:grant-type:token-exchange` grant types. When configured
to use these grant types, the provider sends a request for a database access 
token that includes a token received from a downstream client of the Spring 
application. The system looks like this:
```
┌──────────┐   Client Token    ┌────────────┐    DB Token   ┌──────────────────┐
│  CLIENT  │ ────────────────▶ │ SPRING APP │ ────────────▶ │ ORACLE DATABASE  │
└──────────┘                   └────────────┘               └──────────────────┘
                                   │    ▲
                      Client Token │    │ DB Token
                                   ▼    │
                              ┌──────────────┐
                              │AUTHORIZATION │
                              │    SERVER    │
                              └──────────────┘
```
To obtain the downstream client's token, the provider checks Spring's
[SecurityContextHolder](https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html#servlet-authentication-securitycontextholder).
If no suitable token is present in the `SecurityContextHolder`, then the 
provider cannot request a database access token, and so it will not
provide an `EndUserSecurityContext` to Oracle JDBC. 

When no `EndUserSecurityContext` is provided to Oracle JDBC, it executes 
database operations as a plain database user, such as the one configured as 
Spring's datasource user. This means the datasource user effectively represents
an unauthenticated (or "anonymous") client of the Spring application.
Accordingly, a very limited set of database roles and privileges should be 
granted to this user.

#### Client Credentials Grant Type

In addition to the JWT bearer and token exchange grant types, 
`client_credentials` is also supported. When using client credentials, a 
downstream client's token is not required for the provider to request a database 
access token. For client credentials, an `EndUserSecurityContext` can be 
provided to Oracle JDBC even when no authenticated user is present in Spring's
`SecurityContextHolder`.

A token issued for client credentials does not authorize the application to act on behalf 
of any downstream client. This makes client credentials more suitable for cases
in which the Spring application manages user identities on its own, without
relying on an external service like Azure's Entra or OCI's Identity Domains. In
this scenario, Deep Data Security features can still be activated by mapping the
`GrantedAuthority` of an application-managed user to a `DATA ROLE` or 
`END USER CONTEXT` attributes. The next section describes that in more detail.

### GrantedAuthority Mapping
The `EndUserSecurityContext` provided to Oracle JDBC may include `DATA ROLE` 
names and `END USER CONTEXT` attributes. These roles and attributes can be 
derived from a 
[GrantedAuthority](https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html#servlet-authentication-granted-authority)
present in Spring's `SecurityContextHolder`.

#### Mapping Data Roles
The `oracle.jdbc.provider.endUserSecurityContext.authorityRolePrefix`
parameter specifies the prefix for a `GrantedAuthority` that contains
a `DATA ROLE` name.

The name of a `DATA ROLE` should appear after the specified prefix in the String
representation of a `GrantedAuthority`. For example, if the prefix is configured 
as "ORACLE_DATA_ROLE_", and a `GrantedAuthority` has a String representation of 
"ORACLE_DATA_ROLE_ADMIN", then the provider will map that to a `DATA ROLE` named
"ADMIN".

#### Mapping End User Context Attributes
The 
`oracle.jdbc.provider.endUserSecurityContext.authorityAttributesPrefix`
parameter specifies the prefix for a `GrantedAuthority` that 
contains `END USER CONTEXT` attributes. 

A JSON object containing one or more nested `END USER CONTEXT` objects should 
appear after the specified prefix in the String representation of a 
`GrantedAuthority`. For example, if the prefix is configured as
"ORACLE_CONTEXT_ATTRIBUTES_", then a `GrantedAuthority` might have the String
representation seen below, including the line breaks:
```
ORACLE_CONTEXT_ATTRIBUTES_{
  "app_schema.user_details" : {
    "first_name" : "George",
    "last_name" : "Washington"
  },
  "app_schema.location_info" : {
    "City" : "Mount Vernon",
    "State" : "Virginia",
    "Country" : "United States"
  }
}
```
The provider would recognize this <code>GrantedAuthority</code> as containing
two <code>END USER CONTEXT</code> objects named "user_details" and 
"location_info", each one declared within a database schema named "app_schema". 

### Configuration
The full set of parameters that configure this provider are listed in the table
below.
<table>
<thead><tr>
<th>Parameter Name</th>
<th>Description</th>
<th>Accepted Values</th>
<th>Default Value</th>
</tr></thead><tbody><tr><td>
oracle.jdbc.provider.endUserSecurityContext.registrationId
</td><td>
The ID of an 
<a href="https://docs.spring.io/spring-security/reference/servlet/oauth2/client/core.html#oauth2Client-client-registration">
OAuth 2.0 client registration
</a>.
The provider requests database access tokens using this client registration.
</td><td>
This should be the ID of an OAuth 2.0 client registration configured using 
Spring.
</td><td>
<i>No default value. A value must be configured for this parameter.</i>
</td></tr><tr><td>
oracle.jdbc.provider.endUserSecurityContext.dataRoles
</td><td>
A fixed set of <code>DATA ROLE</code> names to enable for all end users.
</td><td>
This should be one or more <code>DATA ROLE</code> names, separated by commas:
<pre>
common_user_role, common_app_role, read_only_role
</pre>
</td><td>
<i>No default value. This parameter is optional.</i>
</td></tr><tr><td>
oracle.jdbc.provider.endUserSecurityContext.endUserContextAttributes
</td><td>
A fixed set of <code>END USER CONTEXT</code> attributes that apply to all end
users.
</td><td>
This should be a JSON object containing the attributes of one or more 
<code>END USER CONTEXT</code> objects:
<pre>
{
  "app_schema.user_details" : {
    "first_name" : "George",
    "last_name" : "Washington"
  },
  "app_schema.location_info" : {
    "City" : "Mount Vernon",
    "State" : "Virginia",
    "Country" : "United States"
  }
}
</pre>
The example above would be recognized as the attributes of two
<code>END USER CONTEXT</code> objects, one named "user_details" and the
other named "location_info", both declared within a database schema named 
"app_schema".
</td><td>
<i>No default value. This parameter is optional.</i>
</td></tr><tr><td>
oracle.jdbc.provider.endUserSecurityContext.authorityRolePrefix
</td><td>
The prefix used in the <code>String</code> representation of a
<code>GrantedAuthority</code> that the provider should recognize as a 
<code>DATA ROLE</code>. The name of the <code>DATA ROLE</code> should appear
after the prefix. For example, this parameter might be set to 
"ORACLE_DATA_ROLE_" and a <code>GrantedAuthority</code> could have the
<code>String</code> representation of <code>ORACLE_DATA_ROLE_ADMIN</code>. 
The provider recognizes this <code>GrantedAuthority</code> as a 
<code>DATA ROLE</code> named "ADMIN", and enables that 
<code>DATA ROLE</code> for the end user.
</td><td>
This should be a unique string that is unlikely to collide with a
<code>String</code> representation used by any other 
<code>GrantedAuthority</code> that is not intended to be recognized as a 
<code>DATA ROLE</code>.
</td><td>
<i>No default value. This parameter is optional.</i>
</td></tr><tr><td>
oracle.jdbc.provider.endUserSecurityContext.authorityAttributesPrefix
</td><td>
The prefix used in the <code>String</code> representation of a
<code>GrantedAuthority</code> that the provider should recognize as 
a JSON object containing the attributes of one or more 
<code>END USER CONTEXT</code>. The JSON object should appear
after the prefix. For example, this parameter might be set to 
"ORACLE_CONTEXT_ATTRIBUTES_" and a <code>GrantedAuthority</code> 
might have the following <code>String</code> representation, including the line
breaks:
<pre>
ORACLE_CONTEXT_ATTRIBUTES_{
  "app_schema.user_details" : {
    "first_name" : "George",
    "last_name" : "Washington"
  },
  "app_schema.location_info" : {
    "City" : "Mount Vernon",
    "State" : "Virginia",
    "Country" : "United States"
  }
}
</pre>
The provider recognizes this <code>GrantedAuthority</code> as the attributes
of two <code>END USER CONTEXT</code> named "user_details" and "location_info" 
declared within a database schema named "app_schema", and it applies these
attributes to the end user.
</td><td>
This should be a unique string that is unlikely to collide with a
<code>String</code> representation used by any other 
<code>GrantedAuthority</code> that is not intended to be recognized as 
<code>END USER CONTEXT</code> attributes.
</td><td>
<i>No default value. This parameter is optional.</i>
</td></tr><tr><td>
oracle.jdbc.provider.endUserSecurityContext.requestParameters
</td><td>
URL parameters to include in requests for a database access token.
</td><td>
This should be a string of URL encoded name=value parameters. It can be
a single name=value pair:
<pre>
requested_token_use=on_behalf_of
</pre>
Or, it may be multiple name=value pairs, each delimited by an `&`:
<pre>
requested_token_use=on_behalf_of&another_name=another_value
</pre>
The name and value are decoded by 
<a href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/URLDecoder.html">
java.net.URLDecoder
</a>. This requires an escape for most non-alphanumeric characters. For instance,
a space character should be escaped as <code>%20</code>.
</td><td>
<i>No default value. This parameter is optional.</i>
</td></tr><tr><td>
oracle.jdbc.provider.endUserSecurityContext.enableTokenForwarding
</td><td>
Enables tokens from Spring's <code>SecurityContextHolder</code> to be sent to the 
database. This creates a risk of exposing the token to an unintended 
audience. A more secure way to propagate a downstream client's identity is
to use a grant type such as 
<code>urn:ietf:params:oauth:grant-type:jwt-bearer</code> or
<code>urn:ietf:params:oauth:grant-type:token-exchange</code>.
</td><td>
<code>true</code> (case-insensitive) enables forwarding. All other values,
including <code>false</code>, disable it.
</td><td>
<code>false</code>
</td></tr></tbody></table>

## Command-Line Setup Helper

The Spring provider jar includes an interactive setup helper for generating
provider configuration from the command line.

The helper generates resource-provider connection properties for the End
User Security Context provider. It does not connect to an
authorization server, or a database; it only prints the values you
configure.

### Running the helper

The helper is launched from the provider jar with the `--setup` flag:

```bash
java -jar ojdbc-provider-spring-1.1.0.jar --setup
```

Running the jar without `--setup` prints a short info banner (name,
version, a one-line description, and a link to the docs) and exits
immediately, without reading from standard input.

For direct `java -jar` execution, `ojdbc-provider-common-1.1.0.jar` must be
present in the same directory as this jar.

### What the helper does

Once running, the helper's menu offers to add a resource provider. Choosing
it asks for the OAuth 2.0 client registration ID, then a set of optional
values (data roles, end user context attributes, and the authority
prefixes used to derive either of those from Spring Security authorities
at runtime), then generates the matching `oracle.jdbc.provider.*`
connection properties.

The values this helper generates are the same `oracle.jdbc.provider.*`
properties documented below.
You can repeat this as many times as you like, then export everything at
once, either printed to the terminal or appended to a file.


