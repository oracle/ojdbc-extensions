# Employee Records Sample Application

This is a sample Spring Boot application that uses the Deep Data Security
feature of Oracle Database. Setup instructions can be found
in [the quick-start guide](https://docs.oracle.com/en/database/oracle/oracle-database/26/ddscg/configure-oracle-deep-data-security-sample-application.html).

This sample application uses Spring Boot to expose a REST API. The API
lists employees (id, name, salary, phone) from an HR.EMPLOYEES table in Oracle
Database. The table is protected by a Deep Data Security `DATA GRANT` that
allows an employee to see their own records, but not the records of
other employees:
```sql
CREATE OR REPLACE DATA GRANT hr.employees_own_record_oid AS
  SELECT
  ON hr.employees
  WHERE hr.employees.employee_id = (
    SELECT hr.azure_accounts.employee_id
    FROM hr.azure_accounts
    WHERE hr.azure_accounts.oid = ORA_END_USER_CONTEXT.USER.TOKEN.oid)
  TO employee_role;
```
The `DATA_GRANT` follows 
[Azure's recommended practice](https://learn.microsoft.com/en-us/entra/identity-platform/id-token-claims-reference#use-claims-to-reliably-identify-a-user)
of using a token's "oid" claim to identify a user. An `hr.azure_accounts`
table stores the relation between a user's object ID (oid) and their employee 
ID:
```sql
CREATE TABLE IF NOT EXISTS hr.azure_accounts (
  oid VARCHAR(36) PRIMARY KEY,
  employee_id NUMBER,
  FOREIGN KEY (employee_id) REFERENCES hr.employees(employee_id));
```
To setup the table, [obtain the object ID](https://learn.microsoft.com/en-us/partner-center/account-settings/find-ids-and-domain-names#find-the-user-object-id)
for an Azure account you're able to login as. Then insert the object ID into 
the `hr.azure_accounts` table along with an `employee_id` from the 
`hr.employees` table. For instance, an `employee_id` of `999` can be associated 
to an Azure account with an object ID of 
`aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee` like this:
```sql
INSERT INTO hr.employees VALUES (999, 'FirstName', 'LastName',
  'you@example.com', 'your.manager@example.com',
  '999-99-9999', 99999999, '999-9999');
  
INSERT INTO hr.azure_accounts VALUES (
  'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee',
  999);
```

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
See [the quick-start guide](https://docs.oracle.com/en/database/oracle/oracle-database/26/ddscg/build-run-and-verify.html).

Additional instructions for running a frontend client are described in the next 
section.

## Frontend Client

This Spring application is meant to function as a backend service for a frontend
client. A rudimentary frontend client can be found in
[FrontEndClient.java](FrontEndClient.java).

The frontend client performs an interactive login by opening a web browser. The 
login results in an access token that is sent to the Spring 
application's employee records API.

The frontend client should be registered as a public client that can access the
API of the Spring application. Its OAuth 2.0 client ID should be set as 
`HRAPP_FRONTEND_CLIENT_ID` in the `.env` file that configures this sample 
application.

The client can be run like this:
```sh
set -a && source .env && set +a 
java FrontEndClient.java 
```
The frontend client simply prints the JSON response received from the Spring 
application's API, which might look like this:
```
[{"id":999,"name":"First Last","salary":99999999,"phone":"999-9999"}]
```