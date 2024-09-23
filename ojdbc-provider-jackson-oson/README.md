# Oracle Provider Jackson OSON
## Overview
The **Oracle Provider Jackson OSON** is a module that provides support for serializing and deserializing Oracle JSON (OSON) data using Jackson. It includes custom generators, parsers, serializers, and deserializers for POJO mapping and Java types, ensuring efficient JSON data handling in Oracle environments.
## Features
- **OSON Generators**: Custom logic for generating JSON representations of Java objects.
- **OSON Parsers**: Parsing Oracle-specific JSON data into Java objects.
- **Serializers**: Custom serializers for transforming Java types into JSON format.
- **Deserializers**: Custom deserializers for mapping JSON data back into Java POJOs.
- **POJO Mapping**: Seamless mapping between Plain Old Java Objects (POJOs) and Oracle OSON formats.
- **Java Types Handling**: Support for various complex and basic Java types during serialization and deserialization.
## Installation
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
Usage Eamples for Oracle Jackson OSON Provider Extensions can be found at [ojdbc-provider-samples/src/main/java/](../ojdbc-provider-samples/src/main/java)
