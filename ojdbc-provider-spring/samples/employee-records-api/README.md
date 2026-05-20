# Employee Records Sample Application

This is a sample Spring Boot application that uses the Deep Data Security
feature of Oracle Database. Set up instructions can be found
in [the quick-start guide](https://docs.oracle.com/en/database/oracle/oracle-database/26/ddscg/configure-oracle-deep-data-security-sample-application.html).

This sample application uses Spring Boot 3.3.x to expose a REST API. The API
lists employees (id, name, salary, phone) from Oracle HR.EMPLOYEES using
JPA/Hibernate with HikariCP.

The Oracle JDBC EndUserSecurityContext provider for Spring Boot is configured to
propagate the end-user's security context to the database, where Deep Data
Security data grants will enforce that the database query returns only the
current user's salary, and not the salaries of any other user.



## Project Structure

- src/main/java/com/example/employees
  - EmployeeRecordsApiApplication.java (Bootstrap)
  - api/EmployeeController.java (REST)
  - service/EmployeeService.java (mapping to DTO)
  - repository/EmployeeRepository.java (JPA)
  - domain/EmployeeEntity.java (JPA mapping)
  - dto/EmployeeDto.java (payload)
  - config/SecurityConfig.java (JWT resource server)
- src/main/resources/application.properties (config)
- pom.xml (dependencies/build)


## Prerequisites:
- Java 17+
- Maven 3.9+
- Oracle Database and Entra ID set up according to [the quick-start guide](https://docs.oracle.com/en/database/oracle/oracle-database/26/ddscg/configure-oracle-deep-data-security-sample-application.html)

## Build, Run, and Test
See instructions in [the quick-start guide](https://docs.oracle.com/en/database/oracle/oracle-database/26/ddscg/build-run-and-verify.html)

