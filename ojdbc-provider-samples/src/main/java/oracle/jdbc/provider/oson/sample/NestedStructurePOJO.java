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
