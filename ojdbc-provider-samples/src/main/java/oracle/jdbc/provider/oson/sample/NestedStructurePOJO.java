package oracle.jdbc.provider.oson.sample;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleType;
import oracle.jdbc.provider.oson.sample.model.Movie;
import oracle.jdbc.provider.oson.sample.model.Image;

public class NestedStructurePOJO {
  public static void main(String[] args) throws SQLException {
    Properties properties = new Properties();
    properties.setProperty(OracleConnection.CONNECTION_PROPERTY_PROVIDER_JSON, "jackson-json-provider");
    properties.setProperty(OracleConnection.CONNECTION_PROPERTY_JSON_DEFAULT_GET_OBJECT_TYPE, "oracle.sql.json.OracleJsonValue");
    try (Connection con = JacksonOsonSampleUtil.createConnectionWithProperties(properties)) {
        Statement stmt = con.createStatement();
        stmt.execute("drop table if exists movie");
        stmt.execute("create json collection table movie");
        
        Movie matrix = new Movie(
                123, 
                "The Matrix", 
                "Sci-fi", 
                BigDecimal.valueOf(353212323), 
                OffsetDateTime.now(),
                Arrays.asList(
                    new Image("poster.png", "The Matrix"),
                    new Image("showtimes.png", "Matrix Showtimes")
                )
        );
        
        Movie shawshank = new Movie(
                456, 
                "The Shawshank Redemption", 
                "Drama", 
                BigDecimal.valueOf(453212345), 
                OffsetDateTime.now(),
                null
        );
        
        PreparedStatement insert = con.prepareStatement("insert into movie values (:1)");
        insert.setObject(1, matrix, OracleType.JSON);
        insert.execute();
        
        insert.setObject(1, shawshank, OracleType.JSON);
        insert.execute();
        
        ResultSet rs = stmt.executeQuery("select * from movie m where m.data.title = 'The Matrix'");
        rs.next();
        Movie result = rs.getObject(1, Movie.class);
        System.out.println("Objects are equal: " + matrix.equals(result));
    }   
  }
}
