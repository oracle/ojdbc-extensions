/*
 ** Copyright (c) 2024 Oracle and/or its affiliates.
 **
 ** The Universal Permissive License (UPL), Version 1.0
 **
 ** Subject to the condition set forth below, permission is hereby granted to any
 ** person obtaining a copy of this software, associated documentation and/or data
 ** (collectively the "Software"), free of charge and under any and all copyright
 ** rights in the Software, and any and all patent rights owned or freely
 ** licensable by each licensor hereunder covering either (i) the unmodified
 ** Software as contributed to or provided by such licensor, or (ii) the Larger
 ** Works (as defined below), to deal in both
 **
 ** (a) the Software, and
 ** (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 ** one is included with the Software (each a "Larger Work" to which the Software
 ** is contributed by such licensors),
 **
 ** without restriction, including without limitation the rights to copy, create
 ** derivative works of, display, perform, and distribute the Software and make,
 ** use, sell, offer for sale, import, export, have made, and have sold the
 ** Software and the Larger Work(s), and to sublicense the foregoing rights on
 ** either these or other terms.
 **
 ** This license is subject to the following condition:
 ** The above copyright notice and either this complete permission notice or at
 ** a minimum a reference to the UPL must be included in all copies or
 ** substantial portions of the Software.
 **
 ** THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 ** IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 ** FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 ** AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 ** LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 ** OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 ** SOFTWARE.
 */
package oracle.jdbc.provider.oson.sample;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import oracle.jdbc.datasource.impl.OracleDataSource;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import oracle.jdbc.provider.Configuration;
import oracle.jdbc.provider.oson.sample.model.Emp;
import oracle.jdbc.provider.oson.sample.model.Phone;

public class JacksonOsonSampleUtil {

  public static final String URL = Configuration.getRequired("jackson_oson_url");
  public static final String USER = Configuration.getRequired("jackson_oson_username");
  public static final String PASSWORD = Configuration.getRequired("jackson_oson_password");

  public static Connection createConnection() throws SQLException {
    OracleDataSource ods = new OracleDataSource();
    ods.setURL(URL);
    ods.setUser(USER);
    ods.setPassword(PASSWORD);
    return ods.getConnection();
  }

  public static void createTable(Connection conn) throws SQLException {
    try (Statement stmt = conn.createStatement()) {
      stmt.addBatch("drop table if exists jackson_oson_sample");
      stmt.addBatch("create table jackson_oson_sample(id number, json_value JSON) tablespace tbs1");
      stmt.executeBatch();
    }
  }

  public static Emp createEmp() {
    List<Phone> phones = new ArrayList<Phone>();
    phones.add(new Phone("333-222-1111", Phone.Type.WORK));
    phones.add(new Phone("666-555-4444", Phone.Type.MOBILE));
    phones.add(new Phone("999-888-7777", Phone.Type.HOME));
    return new Emp("Bob", "software engineer", new BigDecimal(4000), "bob@bob.org", phones);
  }

  public static ObjectNode createEmpUsingObjectNode(ObjectMapper objectMapper) {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("name", "Bob");
    node.put("job", "software engineer");
    node.put("salary", new BigDecimal(4000));
    node.put("email", "bob@bob.org");
    ArrayNode phones = objectMapper.createArrayNode();
    ObjectNode phone = objectMapper.createObjectNode();
    phone.put("number", "333-222-1111");
    phone.put("type", "WORK");
    phones.add(phone);
    phone = objectMapper.createObjectNode();
    phone.put("number", "666-555-4444");
    phone.put("type", "MOBILE");
    phones.add(phone);
    phone = objectMapper.createObjectNode();
    phone.put("number", "999-888-7777");
    phone.put("type", "HOME");
    phones.add(phone);
    node.put("phoneNumbers", phones);
    return node;
  }

}
