# Oracle JDBC Providers for AWS

This module contains providers for integration between Oracle JDBC and
Amazon Web Services (AWS).

## Centralized Config Providers

<dl>
<dt><a href="#aws-config-config-provider">AWS S3 Configuration
Provider</a></dt>
<dd>Provides connection properties managed by the S3 service</dd>
<dt><a href="#aws-secrets-manager-config-provider">AWS Secrets Manager Configuration 
Provider</a></dt>
<dd>Provides connection properties managed by the Secrets Manager service</dd>
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
  <version>1.0.2</version>
</dependency>
```

## Authentication

Providers use AWS SDK which supports
[Default credentials provider chain](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html),
which looks for credentials in a set of defined locations and use those
credentials to authenticate requests to AWS.

The default credentials provider chain searches for credentials in one of the following locations using a predefined sequence:

1. Java system properties
2. Environment variables
3. Web identity token from AWS Security Token Service
4. The shared credentials and config files
5. Amazon ECS container credentials
6. Amazon EC2 instance IAM role-provided credentials

## AWS S3 Config Provider
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
    "value": "test-secret",
    "key_name": "db-password"
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
        - gcpsecretmanager
        - awssecretsmanager
- value
    - Mandatory
    - Possible values
        - OCID of the secret (if ocivault)
        - Azure Key Vault URI (if azurevault)
        - Base64 Encoded password (if base64)
        - GCP resource name (if gcpsecretmanager)
        - AWS Secret name (if awssecretsmanager)
        - Text
- key_name
    - Optional
    - Possible values
        - Name of the key, if stored as key-value pairs in AWS Secrets Manager
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

## Caching configuration

Config providers in this module store the configuration in caches to minimize
the number of RPC requests to remote location. See
[Caching configuration](../ojdbc-provider-azure/README.md#caching-configuration) for more
details of the caching mechanism.


