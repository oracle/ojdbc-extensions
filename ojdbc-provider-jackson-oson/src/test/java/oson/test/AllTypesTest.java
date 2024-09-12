package oson.test;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import oracle.jdbc.OracleType;
import oracle.jdbc.datasource.impl.OracleDataSource;
import oracle.jdbc.provider.oson.OsonFactory;
import oracle.jdbc.provider.oson.OsonGenerator;
import oracle.jdbc.provider.oson.OsonParser;
import oson.model.AllOracleTypes;
import oracle.sql.json.OracleJsonDatum;
import oracle.sql.json.OracleJsonObject;
import oracle.sql.json.OracleJsonValue;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.time.*;


@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class AllTypesTest {

	Connection conn = null;
	OsonFactory osonFactory = new OsonFactory();
	ObjectMapper om = new ObjectMapper(osonFactory);
	OsonGenerator osonGen = null;
	byte[] osonTocheck = null;
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	
	AllOracleTypes allTypes = new AllOracleTypes(
			1,
			1L,
			BigInteger.valueOf(5L),
			"string sample",
			'c',
			true,
			1.2,
			1.34f,
			BigDecimal.valueOf(1.345),
			Date.valueOf("2024-03-12"),
			Period.of(12, 5, 0),
			Duration.ofDays(30),
			LocalDateTime.of(2024, 3, 12, 1, 30),
			OffsetDateTime.of(2024, 3, 12, 1, 30, 21, 11, ZoneOffset.ofHours(5))
		);
	
	@BeforeAll
	public void setup() {
		try {
			om.findAndRegisterModules();
			osonGen = (OsonGenerator) osonFactory.createGenerator(out);
			
			OracleDataSource ods = new OracleDataSource();
			ods.setURL("jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=phoenix93464.dev3sub2phx.databasede3phx.oraclevcn.com)(PORT=5521))(CONNECT_DATA=(SERVICE_NAME=cdb1_pdb1.regress.rdbms.dev.us.oracle.com)))");
			ods.setUser("system");
			ods.setPassword("manager");
			conn = ods.getConnection();
			
			//setup db tables
			Statement stmt = conn.createStatement();
			stmt.execute("drop table if exists all_types_json");
			stmt.execute("create table all_types_json(c1 number, c2 JSON) tablespace tbs1");
			stmt.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
		
	@Test
	@Order(1)
	public void convertToOson() throws StreamWriteException, DatabindException, IOException {
		
		om.writeValue(osonGen, allTypes);
		osonGen.close();
		osonTocheck = out.toByteArray();

		System.out.println("Binary encoded OSON: " + osonTocheck.length + " bytes");

	}
	
	@Test
	@Order(2)
	public void convertFromOson() throws Exception {
		try (OsonParser parser = (OsonParser) osonFactory.createParser(osonTocheck) ) {
			
			AllOracleTypes ne = om.readValue(parser, AllOracleTypes.class);
			System.out.println("POJO after conversion from OSON: " + ne.toString());
		}
		
	}
	
	@Test
	@Order(3)
	public void insertIntoDB() throws Exception {
		try (OsonParser parser = (OsonParser) osonFactory.createParser(osonTocheck) ) {
			
			// insert into db
			PreparedStatement pstmt = conn.prepareStatement("insert into all_types_json (c1,c2) values(?,?)");
			pstmt.setInt(1, 1);
			pstmt.setObject(2,  allTypes, OracleType.JSON);
			pstmt.execute();
			pstmt.close();
		}
		
	}
	
	@Test
	@Order(4)
	public void retieveFromDatabase() throws Exception {
    
    //retrieve from db
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("select c1, c2 from all_types_json order by c1");
		while(rs.next()) {

      			OracleJsonValue oson_value = rs.getObject(2, OracleJsonValue.class);
      			byte[] osonBytes = rs.getObject(2, OracleJsonDatum.class).shareBytes();

      			if (oson_value != null && oson_value instanceof OracleJsonObject) {
        			OracleJsonObject oson_obj = (OracleJsonObject) oson_value;
        
        			//OracleJsonObject value
        			System.out.println("OracleJsonObject: " + oson_obj);
      			}
      			if(osonBytes!=null && osonBytes.length>0) {
      				System.out.println("Oson bytes length: " + osonBytes.length);
      	
      				// JsonNode mapping
      				JsonNode node = om.readValue(osonBytes, JsonNode.class);
      				System.out.println("JsonNode: " + node.toPrettyString());
      			}
      			if(osonBytes!=null && osonBytes.length>0) {
      				System.out.println("Oson bytes length: " + osonBytes.length);
      	
      				// POJO mapping
      				AllOracleTypes types = om.readValue(osonBytes, AllOracleTypes.class);
      				System.out.println("POJO after retrieval from DB and conversion: " + types.toString());
      			}
		}
		rs.close();
    stmt.close();
		
	}
}
