package oson.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import oracle.jdbc.OracleType;
import oracle.jdbc.datasource.impl.OracleDataSource;
import oracle.jdbc.provider.oson.OsonFactory;
import oracle.jdbc.provider.oson.OsonGenerator;
import oson.model.Emp;
import oson.model.Phone;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


// Test direct insertion and retrieval of json data into a table and map to POJO
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class DBTest {

	Connection conn = null;
	OsonFactory osonFactory = new OsonFactory();
	ObjectMapper om = new ObjectMapper(osonFactory);
	OsonGenerator osonGen = null;
	byte[] objectBytes =null;
	
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
		Emp e = new Emp("Bob", "software engineer", new BigDecimal(4000), "bob@bob.org", phones);
		
		// insert into db
		PreparedStatement pstmt = conn.prepareStatement("insert into emp_json (c1,c2) values(?,?)");
		pstmt.setInt(1, 1);
		pstmt.setObject(2, e, OracleType.JSON);
		pstmt.execute();
		pstmt.close();
	}
	
	@Test
	@Order(2)
	public void retieveFromDatabase() throws Exception {
    
    //retrieve from db
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("select c1, c2 from emp_json order by c1");
		while(rs.next()) {
      Emp e = rs.getObject(2, Emp.class);
      System.out.println(e.toString());
		}
		rs.close();
    stmt.close();
		
	}
}
