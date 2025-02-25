package oracle.jdbc.provider.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.MeterBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import oracle.jdbc.DatabaseFunction;
import oracle.jdbc.driver.OracleConnection;
import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.observability.configuration.ObservabilityConfiguration;

public class ObservabilityTraceEventListenerTest {
  String url = TestProperties.getOrAbort(ObservabilityTestProperties.OBSERVABILITY_URL);
  String userName = TestProperties.getOrAbort(ObservabilityTestProperties.OBSERVABILITY_USERNAME);
  String password = TestProperties.getOrAbort(ObservabilityTestProperties.OBSERVABILITY_PASSWORD);

  // JFR
  private static final String SESSION_KEY = "oracle.jdbc.provider.observability.RoundTrip.SESSION_KEY";
  private static final String AUTH_CALL = "oracle.jdbc.provider.observability.RoundTrip.AUTH_CALL";
  private static final String EXECUTE_QUERY = "oracle.jdbc.provider.observability.RoundTrip.EXECUTE_QUERY";
  private static final String LOGOFF = "oracle.jdbc.provider.observability.RoundTrip.LOGOFF";

  // OTEL
  private static final OpenTelemetry openTelemetry = Mockito.mock(OpenTelemetry.class);
  private static final Span span = Mockito.mock(Span.class);
  private static final SpanContext spanContext = Mockito.mock(SpanContext.class);
  private static final TraceFlags traceFlags = Mockito.mock(TraceFlags.class);
  private static final TraceState traceState = Mockito.mock(TraceState.class);
  private static final SpanBuilder spanBuilder = Mockito.mock(SpanBuilder.class);
  private static final TracerProvider tracerProvider = Mockito.mock(TracerProvider.class);
  private static final Tracer tracer = Mockito.mock(Tracer.class);
  private static final MeterBuilder meterBuilder = Mockito.mock(MeterBuilder.class);

  static {
    GlobalOpenTelemetry.set(openTelemetry);
    configureOTEL();
  }

  @ParameterizedTest(name = "JFRTraceTest - {arguments}")
  @ValueSource(booleans = {true, false})
  public void JFRTraceTest(boolean sensitiveDataEnabled) throws Exception {

    ObservabilityConfiguration.getInstance().setEnabledTracers("JFR");
    ObservabilityConfiguration.getInstance().setSensitiveDataEnabled(sensitiveDataEnabled);
    Configuration configuration = Configuration.getConfiguration("default");
    String connectionId = null;
    try (Recording recording = new Recording(configuration)) {
      recording.start();
      try (Connection connection = DriverManager.getConnection(url, userName, password);
          Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery("SELECT 'OK' FROM DUAL")) {
        connectionId = ((OracleConnection)connection).getNetConnectionId();
        while (resultSet.next()) {
          assertEquals("OK", resultSet.getString(1));
        }
      }
      recording.stop();
      recording.dump(Path.of("dump" + sensitiveDataEnabled + ".jfr"));

      try (RecordingFile recordingFile = new RecordingFile(Path.of("dump" + sensitiveDataEnabled + ".jfr"))) {
        while (recordingFile.hasMoreEvents()) {
          RecordedEvent event = recordingFile.readEvent();
          if (event.getEventType().getCategoryNames().contains("Round trips")) {
            switch (event.getEventType().getName()) {
              case SESSION_KEY:
                assertEquals(connectionId, event.getString("connectionID"));
                assertNotNull(event.getString("databaseOperation"));
                assertNull(event.getString("tenant"));
                assertNull(event.getString("sqlID"));
                assertNull(event.getString("originalSQLText"));
                assertNull(event.getString("actualSQLText"));
                assertEquals(sensitiveDataEnabled, event.getString("databaseUser") != null);
                break;
              case AUTH_CALL:
                assertEquals(connectionId, event.getString("connectionID"));
                assertNotNull(event.getString("databaseOperation"));
                assertNull(event.getString("tenant"));
                assertNull(event.getString("sqlID"));
                assertNull(event.getString("originalSQLText"));
                assertNull(event.getString("actualSQLText"));
                assertEquals(sensitiveDataEnabled, event.getString("databaseUser") != null);

                break;
              case EXECUTE_QUERY:
                assertEquals(connectionId, event.getString("connectionID"));
                assertNotNull(event.getString("databaseOperation"));
                assertNotNull(event.getString("tenant"));
                assertNotNull(event.getString("sqlID"));
                assertEquals(sensitiveDataEnabled, event.getString("originalSQLText") != null);
                assertEquals(sensitiveDataEnabled, event.getString("actualSQLText") != null);
                assertEquals(sensitiveDataEnabled, event.getString("databaseUser") != null);
                break;
              case LOGOFF:
                assertEquals(connectionId, event.getString("connectionID"));
                assertNotNull(event.getString("databaseOperation"));
                assertNotNull(event.getString("tenant"));
                assertNull(event.getString("sqlID"));
                assertNull(event.getString("originalSQLText"));
                assertNull(event.getString("actualSQLText"));
                assertEquals(sensitiveDataEnabled, event.getString("databaseUser") != null);
                break;            
              default:
                fail("Unexpected event");
            }
          }
        }
      }
    }
      
  }

  @ParameterizedTest(name = "OTELTraceTest - {arguments}")
  @ValueSource(booleans = {true, false})
  public void OTELTraceTest(boolean sensitiveDataEnabled) throws Exception {
    Mockito.clearInvocations(tracer, spanBuilder);
    ObservabilityConfiguration.getInstance().setEnabledTracers("OTEL");
    ObservabilityConfiguration.getInstance().setSensitiveDataEnabled(sensitiveDataEnabled);
    String otelUrl = url + "?oracle.jdbc.provider.traceEventListener=observability-trace-event-listener-provider";
    String connectionId = null;
    try (Connection connection = DriverManager.getConnection(otelUrl, userName, password);
          Statement statement = connection.createStatement();
          ResultSet resultSet = statement.executeQuery("SELECT 'OK' FROM DUAL")) {
      connectionId = ((OracleConnection)connection).getNetConnectionId();
      while (resultSet.next()) {
        assertEquals("OK", resultSet.getString(1));
      }
    }

    Mockito.verify(tracer, atLeastOnce()).spanBuilder(DatabaseFunction.SESSION_KEY.getDescription());
    Mockito.verify(tracer, atLeastOnce()).spanBuilder(DatabaseFunction.AUTH_CALL.getDescription());
    Mockito.verify(tracer, atLeastOnce()).spanBuilder(DatabaseFunction.EXECUTE_QUERY.getDescription());
    Mockito.verify(tracer, atLeastOnce()).spanBuilder(DatabaseFunction.LOGOFF.getDescription());
    Mockito.verify(spanBuilder, atLeastOnce()).startSpan();
    Mockito.verify(spanBuilder, Mockito.atLeast(4)).setAttribute("thread.id", Thread.currentThread().getId());
    Mockito.verify(spanBuilder, Mockito.atLeast(4)).setAttribute("thread.name", Thread.currentThread().getName());
    Mockito.verify(spanBuilder, Mockito.atLeast(1)).setAttribute("Connection ID", connectionId);
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Database Operation", DatabaseFunction.SESSION_KEY.getDescription());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Database Operation", DatabaseFunction.AUTH_CALL.getDescription());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Database Operation", DatabaseFunction.EXECUTE_QUERY.getDescription());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Database Operation", DatabaseFunction.LOGOFF.getDescription());
    if (sensitiveDataEnabled) {
      Mockito.verify(spanBuilder, Mockito.times(4)).setAttribute("Database User", userName);
      Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Original SQL Text", "SELECT 'OK' FROM DUAL");
      Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Actual SQL Text", "SELECT 'OK' FROM DUAL");
    }
    Mockito.verify(span, atLeast(4)).end(Mockito.any());


  }

  private static void configureOTEL() {
    Mockito.when(openTelemetry.getTracerProvider()).thenReturn(tracerProvider);
    Mockito.when(tracerProvider.get(Mockito.anyString())).thenReturn(tracer);
    Mockito.when(openTelemetry.meterBuilder(Mockito.anyString())).thenReturn(meterBuilder);
    Mockito.when(spanContext.getTraceFlags()).thenReturn(traceFlags);
    Mockito.when(spanContext.getTraceState()).thenReturn(traceState);
    Mockito.when(span.getSpanContext()).thenReturn(spanContext);
    Mockito.when(spanBuilder.setAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(spanBuilder);
    Mockito.when(spanBuilder.setAttribute(Mockito.anyString(), Mockito.anyLong())).thenReturn(spanBuilder);
    Mockito.when(spanBuilder.setAttribute(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(spanBuilder);
    Mockito.when(spanBuilder.setAttribute(Mockito.anyString(), Mockito.any())).thenReturn(spanBuilder);
    Mockito.when(spanBuilder.setSpanKind(Mockito.any(SpanKind.class))).thenReturn(spanBuilder);
    Mockito.when(spanBuilder.startSpan()).thenReturn(span);
    Mockito.when(tracer.spanBuilder(Mockito.anyString())).thenReturn(spanBuilder);
  }

}
