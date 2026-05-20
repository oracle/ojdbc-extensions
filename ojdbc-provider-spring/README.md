# Oracle JDBC Providers for Spring

This module contains providers for integrations between Oracle JDBC and 
[Spring](https://spring.io/).

## Installation

Providers in this module are distributed as a single jar on the
Maven Central Repository. The jar is compiled for JDK 17, and is forward 
compatible with later JDK versions. The coordinates for the latest release are:
```xml
<dependency>
  <groupId>com.oracle.database.jdbc</groupId>
  <artifactId>ojdbc-provider-spring</artifactId>
  <version>1.0.6</version>
</dependency>
```

## End User Security Context Provider
The End User Security Context Provider provides a security context that enables
Deep Data Security features in Oracle Database. This is a
[Resource Provider](https://docs.oracle.com/en/database/oracle/oracle-database/26/jajdb/oracle/jdbc/spi/OracleResourceProvider.html)
identified by the name `ojdbc-provider-spring-end-user-security-context`.

This provider must be configured to authenticate as an OAuth 2.0 client. The
client should be mapped to a `DATA ROLE` or `APPLICATION IDENTITY` defined in
an Oracle Database. In the example below, Spring Boot properties under 
`spring.security` define an
[OAuth 2.0 client registration](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/core.html#oauth2Client-client-registration)
for Azure, and properties under `spring.datasource` configure this provider to 
use that registration.
```yaml
spring:
  security:
    oauth2:
      client:
        provider:
          azure:
            token-uri: "https://login.microsoftonline.com/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx/oauth2/v2.0/token"
        registration:
          azure:
            authorization-grant-type: client_credentials
            client-id: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
            client-secret: "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
            scope: "https://xxxxxxxxxxxxxxxxx.onmicrosoft.com/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx/.default"
  datasource:
    url: >
      jdbc:oracle:thin:@example
        ?oracle.jdbc.provider.endUserSecurityContext=ojdbc-provider-spring-end-user-security-context
        &oracle.jdbc.provider.endUserSecurityContext.registrationId=azure_client
    username: db_user
    password: db_password
```
This provider is activated by parameters seen in the JDBC URL above, where
`oracle.jdbc.provider.endUserSecurityContext` configures Oracle JDBC to load an
[EndUserSecurityContextProvider](https://docs.oracle.com/en/database/oracle/oracle-database/26/jajdb/oracle/jdbc/spi/EndUserSecurityContextProvider.html)
with the name of this provider:
`ojdbc-provider-spring-end-user-security-context`. This configuration will have
an [oracle.jdbc.EndUserSecurityContext](https://docs.oracle.com/en/database/oracle/oracle-database/26/jajdb/oracle/jdbc/EndUserSecurityContext.html) provided to Oracle JDBC
each time it executes a database operation.

Almost every network request JDBC sends to the database is considered a database 
operation. This includes SQL execution, row fetching, commit/rollback,
and LOB data access. When an `EndUserSecurityContext` is provided to Oracle 
JDBC, all of these operations become subject to `DATA GRANT` policies defined in
Oracle Database. 

A `DATA GRANT` policy can apply to specific controls based on which user is
accessing the database, and which application they are accessing it through. 
This offers a more granular approach to data security when compared to the
traditional model, in which all application users access the database as the 
same database user.

### Application Authorization and User Identity
An `EndUserSecurityContext` provided to Oracle JDBC encapsulates two security 
tokens:
<dl>
<dt>Database Access Token</dt>
<dd>
This token authorizes a Spring application to access the database. The provider
requests this token using an OAuth 2.0 client registration identified by the
<code>oracle.jdbc.provider.endUserSecurityContext.registrationId</code>
parameter.
</dd>
<dt>End User Token</dt>
<dd>
This token identifies the end user of a Spring application. 
It is typically received in the Authorization header of an incoming HTTP 
request. The provider retrieves this token using Spring's
<a href="https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html#servlet-authentication-securitycontextholder">
SecurityContextHolder
</a>.
</dd>
</dl>
These two tokens allow Oracle Database to make authorization decisions based
on the application user who is accessing data, and which application they are 
accessing it from.

### Application Managed Data Roles and Attributes
The `EndUserSecurityContext` provided to Oracle JDBC may include application
managed <code>DATA ROLE</code> names or <code>END USER CONTEXT</code> attributes
that are derived from a
<a href="https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html#servlet-authentication-granted-authority">
GrantedAuthority
</a> present in Spring's
<a href="https://docs.spring.io/spring-security/reference/servlet/authentication/architecture.html#servlet-authentication-securitycontextholder">
SecurityContextHolder
</a>.
<dl>
<dt>Application Managed Data Roles</dt>
<dd>
If one or more <code>GrantedAuthority</code> objects have <code>String</code> 
representations beginning with a specified prefix, then the provider will 
recognize them as application managed <code>DATA ROLE</code> names that 
should be enabled for the end user.
<p>
The name of a <code>DATA ROLE</code> should appear
after the specified prefix. For example: If the prefix is configured as 
"ORACLE_DATA_ROLE_", then a <code>GrantedAuthority</code> might have a
<code>String</code> representation of <code>ORACLE_DATA_ROLE_ADMIN</code>. The 
provider will recognize that <code>GrantedAuthority</code> as a 
<code>DATA ROLE</code> named "ADMIN".
</dd>
<dt>End User Context Attributes</dt>
<dd>
If one or more <code>GrantedAuthority</code> objects have <code>String</code> 
representations beginning with a specified prefix, then the provider will 
recognize them as attributes of an <code>END USER CONTEXT</code>.
<p>
A JSON object containing the attributes of one or more 
<code>END USER CONTEXT</code> should appear
after the <code>ORACLE_CONTEXT_ATTRIBUTE_</code> prefix. For example: If the 
prefix is configured as "ORACLE_CONTEXT_ATTRIBUTE_", then a 
<code>GrantedAuthority</code> might have the following <code>String</code> 
representation, including the line breaks:
<pre>
ORACLE_CONTEXT_ATTRIBUTE_{
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
The provider will recognize that <code>GrantedAuthority</code> as the attributes
of two <code>END USER CONTEXT</code> objects named "user_details" and 
"location_info", each declared within a database schema named "app_schema". 
</dd>
</dl>

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
<code>END USER CONTEXT</code> named "user_details" and "location_info" declared
within a database schema named "app_schema".
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
The provider will recognize this <code>GrantedAuthority</code> as a 
<code>DATA ROLE</code> named "ADMIN", and will enable that 
<code>DATA ROLE</code> for the end user.
</td><td>
This should be a unique string that is unlikely to collide with a
<code>String</code> representation used by any other 
<code>GrantedAuthority</code> that is not intended to be recognized as a 
<code>DATA ROLE</code>.
</td><td>
<i>No default value. This parameter is optional.</i>
</td></tr><tr><td>
oracle.jdbc.provider.endUserSecurityContext.authorityAttributePrefix
</td><td>
The prefix used in the <code>String</code> representation of a
<code>GrantedAuthority</code> that the provider should recognize as 
a JSON object containing the attributes of one or more 
<code>END USER CONTEXT</code>. The JSON object should appear
after the prefix. For example, this parameter might be set to 
"ORACLE_CONTEXT_ATTRIBUTE_" and a <code>GrantedAuthority</code> 
might have the following <code>String</code> representation, including the line
breaks:
<pre>
ORACLE_CONTEXT_ATTRIBUTE_{
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
The provider will recognize this <code>GrantedAuthority</code> as the attributes
of two <code>END USER CONTEXT</code> named "user_details" and "location_info" 
declared within a database schema named "app_schema", and it will apply these
attributes to the end user.
</td><td>
This should be a unique string that is unlikely to collide with a
<code>String</code> representation used by any other 
<code>GrantedAuthority</code> that is not intended to be recognized as 
<code>END USER CONTEXT</code> attributes.
</td><td>
ORACLE_CONTEXT_ATTRIBUTE_
</td></tr></tbody></table>
