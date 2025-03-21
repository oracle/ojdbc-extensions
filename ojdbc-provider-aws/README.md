# Oracle JDBC Providers for AWS

This module contains providers for integration between Oracle JDBC and
Amazon Web Services (AWS).

## Centralized Config Providers

<dl>
<dt><a href="#aws-s3-configuration-provider">AWS S3 Configuration
Provider</a></dt>
<dd>Provides connection properties managed by the S3 service</dd>
<dt><a href="#aws-secrets-manager-config-provider">AWS Secrets Manager Configuration 
Provider</a></dt>
<dd>Provides connection properties managed by the Secrets Manager service</dd>
<dt><a href="#common-parameters-for-centralized-config-providers">Common Parameters for Centralized Config Providers</a></dt>
<dd>Common parameters supported by the config providers</dd>
<dt><a href="#caching-configuration">Caching configuration</a></dt>
<dd>Caching mechanism adopted by Centralized Config Providers</dd>
</dl>

Visit any of the links above to find information and usage examples for a
particular provider.

## Installation

All providers in this module are distributed as single jar on the Maven Central
Repository. The jar is compiled for JDK 8, and is forward compatible with later
JDK versions. The coordinates for the latest release are:
```xml
<dependency>
  <groupId>com.oracle.database.jdbc</groupId>
  <artifactId>ojdbc-provider-aws</artifactId>
  <version>1.0.4</version>
</dependency>
```

## AWS S3 Configuration Provider
The Oracle DataSource uses a new prefix `jdbc:oracle:thin:@config-awss3:` to be able to identify that the configuration parameters should be loaded using AWS S3.
Users only need to indicate the S3 URI of the object that contains the JSON payload.

A URL with either of the following formats is valid:
<pre>
jdbc:oracle:thin:@config-awss3://{S3-URI}
</pre>
or
<pre>
jdbc:oracle:thin:@config-aws{S3-URI}
</pre>

The {S3-URI} can be obtained from the Amazon S3 console and follows this naming convention: s3://bucket-name/file-name.

### JSON Payload format

There are 3 fixed values that are looked at the root level.

- connect_descriptor (required)
- user (optional)
- password (optional)

The rest are dependent on the driver, in our case `/jdbc`. The key-value pairs that are with sub-prefix `/jdbc` will be applied to a DataSource. The key values are constant keys which are equivalent to the properties defined in the [OracleConnection](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/OracleConnection.html) interface.

For example, let's suppose an url like:

<pre>
jdbc:oracle:thin:@config-awss3://s3://mybucket/payload_ojdbc_objectstorage.json
</pre>

And the JSON Payload for the file **payload_ojdbc_objectstorage.json** in **mybucket** as following:

```json
{
  "connect_descriptor": "(description=(retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1521)(host=adb.us-phoenix-1.oraclecloud.com))(connect_data=(service_name=xsxsxs_dbtest_medium.adb.oraclecloud.com))(security=(ssl_server_dn_match=yes)))",
  "user": "scott",
  "password": { 
    "type": "awssecretsmanager",
    "value": "test-secret"
  },
  "jdbc": {
    "oracle.jdbc.ReadTimeout": 1000,
    "defaultRowPrefetch": 20,
    "autoCommit": "false"
  }
}
```

The sample code below executes as expected with the previous configuration.

```java
    OracleDataSource ds = new OracleDataSource();
    ds.setURL("jdbc:oracle:thin:@config-awss3://s3://mybucket/payload_ojdbc_objectstorage.json");
    Connection cn = ds.getConnection();
    Statement st = cn.createStatement();
    ResultSet rs = st.executeQuery("select sysdate from dual");
    if (rs.next())
      System.out.println("select sysdate from dual: " + rs.getString(1));
```

### Password JSON Object

For the JSON type of provider (AWS S3, AWS Secrets Manager, HTTP/HTTPS, File) the password is an object itself with the following spec:

- type
    - Mandatory
    - Possible values
        - ocivault
        - azurevault
        - base64
        - awssecretsmanager
- value
    - Mandatory
    - Possible values
        - OCID of the secret (if ocivault)
        - Azure Key Vault URI (if azurevault)
        - Base64 Encoded password (if base64)
        - AWS Secret name (if awssecretsmanager)
- authentication
    - Optional
    - Possible Values
        - method
        - optional parameters (depends on the cloud provider).

## AWS Secrets Manager Config Provider
Apart from AWS S3, users can also store JSON Payload in the content of AWS Secrets Manager secret. Users need to indicate the secret name:

<pre>
jdbc:oracle:thin:@config-awssecretsmanager://{secret-name}
</pre>

The JSON Payload retrieved by AWS Secrets Manager Provider follows the same format in [AWS S3 Configuration Provider](#json-payload-format).

## Common Parameters for Centralized Config Providers
AWS S3 Configuration Provider and AWS Secrets Manager Configuration Provider
share the same sets of parameters for authentication configuration.

### Configuring Authentication

The Centralized Config Providers in this module use the
[Default credentials provider chain](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html) to provide authorization and authentication to S3 and Secrets Manager services.
The user can provide an optional parameter `AUTHENTICATION` (case-ignored) which is mapped with the following Credential Class.

<table>
<thead><tr>
<th>'AUTHENTICATION' Param Value</th>
<th>Method</th>
<th>Optional Configuration</th>
<th>Optional Parameters</th>
</tr></thead>
<tbody>
<tr>
  <td><b>AWS_DEFAULT</b> or &lt;Empty&gt;</td>
  <td>Default Credentials Provider Chain</td>
  <td>see below Default Credentials Provider Chain</td>
  <td>AWS_REGION (see AWS Region below)</td>
</tr>
</tbody>
</table>

### Default Credentials Provider Chain

The default credentials provider chain provided by AWS SDK is implemented by the 
[DefaultCredentialsProvider](https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/auth/credentials/DefaultCredentialsProvider.html) class,
which searches for credentials in one of the following locations using a predefined sequence:

1. Java system properties
2. Environment variables
3. Web identity token from AWS Security Token Service
4. The shared credentials and config files
5. Amazon ECS container credentials
6. Amazon EC2 instance IAM role-provided credentials

For more details, please refer to [Default credentials provider chain](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html).

### AWS Region

In this project, region can be specified from two places: 
1. `AWS_REGION` as an optional parameter in URL
2. [Default region provider chain](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/region-selection.html#automatically-determine-the-aws-region-from-the-environment).

If `AWS_REGION` is specified in the URL, the provider uses it as the value of Region for authentication. Otherwise, the value from default region provider chain will be applied.

## Caching configuration

Config providers in this module store the configuration in caches to minimize
the number of RPC requests to remote location. See
[Caching configuration](../ojdbc-provider-azure/README.md#caching-configuration) for more
details of the caching mechanism.


