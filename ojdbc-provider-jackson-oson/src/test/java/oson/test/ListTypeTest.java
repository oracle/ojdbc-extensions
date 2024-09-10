package oson.test;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import oracle.jdbc.OracleType;
import oracle.jdbc.jackson.oson.provider.JacksonOsonConverter;
import oracle.jdbc.pool.OracleDataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class ListTypeTest {
  static Connection conn = null;
  
  @BeforeAll
  public void setup() {
    try {
      OracleDataSource ods = new OracleDataSource();
      ods.setURL("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=phoenix93464.dev3sub2phx.databasede3phx.oraclevcn.com)(PORT=5521))(CONNECT_DATA=(SERVICE_NAME=cdb1_pdb1.regress.rdbms.dev.us.oracle.com)))");
      ods.setUser("system");
      ods.setPassword("manager");
      conn = ods.getConnection();

      //setup db tables
      Statement stmt = conn.createStatement();
      stmt.execute("drop table if exists emp_json");
      stmt.execute("create table emp_json(c1 number, c2 JSON) tablespace tbs1");
      stmt.close();

    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  @Test
	@Order(1)
  public void insertIntoDatabase() throws IOException, SQLException {
    List<Phone> phones = new ArrayList<>();
    phones.add(new Phone("333-222-1111", Phone.Type.WORK));
    phones.add(new Phone("666-555-4444", Phone.Type.MOBILE));
    phones.add(new Phone("999-888-7777", Phone.Type.HOME));

    PreparedStatement
      pstmt = conn.prepareStatement("insert into emp_json (c1,c2) values(?,?)");
    pstmt.setInt(1, 1);

    pstmt.setObject(2, phones, OracleType.JSON);
    pstmt.execute();
    pstmt.close();
  }
  
  @Test
	@Order(2)
  public void retieveFromDatabase() throws SQLException, IOException {

    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery("select c1, c2 from emp_json order by c1");
    while(rs.next()) {
    	
    	JavaType type = TypeFactory.defaultInstance().constructParametricType(List.class, Phone.class); 
    	List<Phone> phones = (List<Phone>) JacksonOsonConverter.convertValue(
    			rs.getObject(2, JsonNode.class), type);
    	
    	for(int i=0; i<phones.size(); i++) {
    		System.out.println(phones.get(i).toString());
    	}
    }
    rs.close();
    stmt.close();

  }



}

class Phone {

  public enum Type {MOBILE, HOME, WORK}

  String number;

  Type type;

  public Phone() {
  }

  public Phone(String number, Type type) {
    this.number = number;
    this.type = type;
  }

  public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	@Override
  public String toString() {
    return "Phone {" +
      "number='" + number + '\'' +
      ", type=" + type +
      '}';
  }

}