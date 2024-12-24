package oracle.jdbc.provider.traceeventlisteners;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.ServiceLoader;

import oracle.jdbc.provider.Configuration;
import oracle.jdbc.provider.traceeventlisteners.spi.DiagnosticTraceEventListenerProvider;

public class TraceEventListenersSample {
  private static final String CONNECTION_URL = Configuration
      .getRequired("trace-event-listener-connection-url");
  private static final String USERNAME = Configuration
      .getRequired("trace-event-listener-username");
  private static final String PASSWORD = Configuration
      .getRequired("trace-event-listener-password");


  public static void main(String[] args) throws SQLException {
    Properties properties = new Properties();
    properties.setProperty("user", USERNAME);
    properties.setProperty("password", PASSWORD);
    properties.setProperty("oracle.jdbc.provider.traceEventListener", "multi-trace_event-listener-provider");
    //properties.setProperty("oracle.jdbc.provider.traceEventListener.sensitive", "true");
    properties.setProperty("oracle.jdbc.provider.traceEventListener.enableSensitiveData", "true");
    ServiceLoader<DiagnosticTraceEventListenerProvider> loader = ServiceLoader.load(DiagnosticTraceEventListenerProvider.class);
    loader.forEach(p -> System.out.println(p.getClass().getName()));
    try (Connection connection = DriverManager.getConnection(CONNECTION_URL, properties);
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT 1 FROM dual")) {
        while (resultSet.next()) {
            System.out.println("Read value: " + resultSet.getInt(1));
        }
    } catch (SQLException sqlException) {
        System.out.println(sqlException.getMessage());
    }

  }
}
