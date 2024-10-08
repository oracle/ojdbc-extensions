# OSON Provider for Jackson
## Overview
The **OSON Provider for Jackson** is a module that provides support for serializing and deserializing Oracle JSON (OSON) data using Jackson. 
It includes custom generators, parsers, serializers, and deserializers for POJO mapping and Java types, ensuring efficient 
JSON data handling in Oracle environments. 
The module implements [**oracle.jdbc.spi.JsonProvider**](https://docs.oracle.com/en/database/oracle/oracle-database/23/jajdb/oracle/jdbc/spi/OsonProvider.html) 
in order to use the Oracle JDBC driver's JSON processing capabilities.
## Features
- **OSON Generators**: Custom logic for generating Oracle JSON (OSON) representations of Java objects.
- **OSON Parsers**: Parsing Oracle-specific JSON(OSON bytes) data into Java objects.
- **Serializers**: Custom serializers for transforming Java types into OSON bytes.
- **Deserializers**: Custom deserializers for mapping OSON bytes back into Java POJOs.
- **POJO Mapping**: Seamless mapping between Plain Old Java Objects (POJOs) and Oracle OSON formats.
- **Java Types Handling**: Support for various complex and basic Java types during serialization and deserialization.
- **Jackson Annotation support**: Support for Jackson Annotations. Note: When **@Format** annotation is used, the values are processed as Strings.

## Installation

All providers in this module are distributed as single jar on the Maven Central
Repository. The jar is compiled for JDK 8, and is forward compatible with later
JDK versions. The coordinates for the latest release are:
```xml
<dependency>
  <groupId>com.oracle.database.jdbc</groupId>
  <artifactId>ojdbc-provider-jackson-oson</artifactId>
  <version>1.0.1</version>
</dependency>
```
## Building the provider module
1. Clone the repository:
   ```bash
   git clone https://github.com/oracle/ojdbc-extensions.git
2. Navigate to the project directory:
   ```bash
   cd oracle-provider-jackson-oson
3. Build the module and it's dependencies:
   ```bash
   mvn clean -pl ojdbc-provider-jackson-oson -amd install

## Usage
Usage Examples for Oracle Jackson OSON Provider Extensions can be found at [ojdbc-provider-samples/src/main/java/](../ojdbc-provider-samples/src/main/java)
