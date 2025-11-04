# Oracle JDBC Configuration Provider Parser for Pkl

This module provides a parser that integrates **Oracle JDBC** with **Pkl**, a modern configuration language.  
It implements the `OracleConfigurationParser` interface to parse and read `.pkl` files for database configuration.

With the Oracle JDBC Pkl Parser, developers can store JDBC configurations in `.pkl` files and load them dynamically through Oracle JDBC Driver Extensions.

> **Note:** This parser works only with providers that extend `OracleConfigurationParsableProvider`, such as `file`, `https`, `ociobject`, `awss3`, and others.
> 
## Installation

All providers in this module are distributed as single jar on the Maven Central
Repository. The jar is compiled for JDK 17, and is forward compatible with later
JDK versions. The coordinates for the latest release are:

```xml
<dependency>
  <groupId>com.oracle.database.jdbc</groupId>
  <artifactId>ojdbc-provider-pkl</artifactId>
  <version>1.0.6</version>
</dependency>
```

## Usage 

To use the Oracle JDBC Pkl Parser:

1. Prepare a .pkl configuration file (see examples below).
2. Add this artifact to your application's classpath.
3. Reference the .pkl file in the JDBC URL.

The parser type is inferred from the file extension. In this case, it’s "pkl".
If the file name doesn’t include an extension, you can specify the parser explicitly using the parser option.
All other options (like key, label, etc.) follow the same format as other providers.

Example using the file configuration provider:

```java
jdbc:oracle:thin:@config-file://{pkl-file-name}[?parser=pkl&key=prefix&label=value&option1=value1&option2=value2...]
```

## Writing .pkl Configuration

There are two approaches to filling out a template: **amends** and **import**.

### 1. Using `amends`

#### myJdbcConfig.pkl

```yaml
amends "https://raw.githubusercontent.com/oracle/ojdbc-extensions/refs/heads/main/ojdbc-provider-pkl/template/JdbcConfig.pkl"

connect_descriptor = "dbhost:1521/orclpdb1"
user = "scott"

password {
  type = "ocivault"
  value = "ocid1.vaultsecret..."
  authentication {
    ["method"] = "OCI_DEFAULT"
  }
}

jdbc {
  autoCommit = false
  `oracle.jdbc.loginTimeout` = 60.s
}
```

#### URL (using file provider):

```java
jdbc:oracle:thin:@config-file://myJdbcConfig.pkl
```

### 2. Using `import`

#### myJdbcConfig.pkl

```yaml
import "https://raw.githubusercontent.com/oracle/ojdbc-extensions/refs/heads/main/ojdbc-provider-pkl/template/JdbcConfig.pkl"
 
config1 = (JdbcConfig) {
  connect_descriptor = "dbhost:1521/orclpdb1"
 
  user = "scott"
 
  password {
    type = "ocivault"
    value = "ocid1.vaultsecret..."
    authentication {
      ["method"] = "OCI_DEFAULT"
    }
  }
 
  jdbc {
    autoCommit = false
    `oracle.jdbc.loginTimeout` = 60.s
  }
}
```

#### URL (using file provider):

```java
jdbc:oracle:thin:@config-file://myJdbcConfig.pkl?key=config1
```