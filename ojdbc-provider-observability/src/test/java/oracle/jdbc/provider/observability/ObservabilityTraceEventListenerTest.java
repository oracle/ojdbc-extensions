/*
 ** Copyright (c) 2025 Oracle and/or its affiliates.
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
package oracle.jdbc.provider.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
//import oracle.jdbc.provider.TestProperties;

public class ObservabilityTraceEventListenerTest {

  String url = ""; //TestProperties.getOrAbort(ObservabilityTestProperties.OBSERVABILITY_URL);
  String userName = ""; //TestProperties.getOrAbort(ObservabilityTestProperties.OBSERVABILITY_USERNAME);
  String password = ""; //TestProperties.getOrAbort(ObservabilityTestProperties.OBSERVABILITY_PASSWORD);

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

    System.setProperty("oracle.jdbc.provider.observability.enabledTracers", "JFR");
    System.setProperty("oracle.jdbc.provider.observability.sensitiveDataEnabled", String.valueOf(sensitiveDataEnabled));
    Configuration configuration = Configuration.getConfiguration("default");
    String connectionId = null;
    try (Recording recording = new Recording(configuration)) {
      recording.start();
      String jfrUrl = url + "?oracle.jdbc.provider.traceEventListener=observability-trace-event-listener-provider&oracle.jdbc.provider.traceEventListener.unique_identifier=test-jfr" + sensitiveDataEnabled;
      try (Connection connection = DriverManager.getConnection(jfrUrl, userName, password);
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
        int countRoundTrips = 0;
        while (recordingFile.hasMoreEvents()) {
          RecordedEvent event = recordingFile.readEvent();
          if (event.getEventType().getCategoryNames().contains("Round trips")) {
            countRoundTrips++;
            switch (event.getEventType().getName()) {
              case SESSION_KEY:
                assertEquals(connectionId, event.getString("connectionID"));
                assertNotNull(event.getString("databaseOperation"));
                assertNull(event.getString("sqlID"));
                assertNull(event.getString("originalSQLText"));
                assertNull(event.getString("actualSQLText"));
                assertEquals(sensitiveDataEnabled, event.getString("databaseUser") != null);
                break;
              case AUTH_CALL:
                assertEquals(connectionId, event.getString("connectionID"));
                assertNotNull(event.getString("databaseOperation"));
                assertNull(event.getString("sqlID"));
                assertNull(event.getString("originalSQLText"));
                assertNull(event.getString("actualSQLText"));
                assertEquals(sensitiveDataEnabled, event.getString("databaseUser") != null);

                break;
              case EXECUTE_QUERY:
                assertEquals(connectionId, event.getString("connectionID"));
                assertNotNull(event.getString("databaseOperation"));
                assertNotNull(event.getString("sqlID"));
                assertEquals(sensitiveDataEnabled, event.getString("originalSQLText") != null);
                assertEquals(sensitiveDataEnabled, event.getString("actualSQLText") != null);
                assertEquals(sensitiveDataEnabled, event.getString("databaseUser") != null);
                break;
              case LOGOFF:
                assertEquals(connectionId, event.getString("connectionID"));
                assertNotNull(event.getString("databaseOperation"));
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
        assertTrue(countRoundTrips > 0, "Application should have performed at least one round trip");
      }
    }

  }

  @ParameterizedTest(name = "OTELTraceTest - {arguments}")
  @ValueSource(booleans = {true, false})
  public void OTELTraceTest(boolean sensitiveDataEnabled) throws Exception {
    Mockito.clearInvocations(tracer, spanBuilder);
    System.setProperty("oracle.jdbc.provider.observability.enabledTracers", "OTEL");
    System.setProperty("oracle.jdbc.provider.observability.sensitiveDataEnabled", String.valueOf(sensitiveDataEnabled));
    String otelUrl = url + "?oracle.jdbc.provider.traceEventListener=observability-trace-event-listener-provider&oracle.jdbc.provider.traceEventListener.unique_identifier=test-otel-" + sensitiveDataEnabled ;
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
    } else {
      Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Database User", userName);
      Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Original SQL Text", "SELECT 'OK' FROM DUAL");
      Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Actual SQL Text", "SELECT 'OK' FROM DUAL");
    }
    Mockito.verify(span, atLeast(4)).end();

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
