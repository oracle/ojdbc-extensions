/*
 ** Copyright (c) 2023 Oracle and/or its affiliates.
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

package oracle.jdbc.provider.opentelemetry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;

import java.sql.SQLException;
import java.time.Instant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import oracle.jdbc.TraceEventListener.JdbcExecutionEvent;
import oracle.jdbc.TraceEventListener.Sequence;
import oracle.jdbc.TraceEventListener.TraceContext;

public class OpenTelemetryTraceEventListenerTest {

  private Span span = Mockito.mock(Span.class);
  private SpanContext spanContext = Mockito.mock(SpanContext.class);
  private TraceFlags traceFlags = Mockito.mock(TraceFlags.class);
  private TraceState traceState = Mockito.mock(TraceState.class);
  private SpanBuilder spanBuilder = Mockito.mock(SpanBuilder.class);
  private Tracer tracer = Mockito.mock(Tracer.class);
  private TraceContext traceContext = Mockito.mock(TraceContext.class);
  private OpenTelemetryTraceEventListener traceEventListener = new OpenTelemetryTraceEventListener(tracer);

  @BeforeEach
  public void setupMocks() throws Exception {
    Mockito.when(spanContext.getTraceFlags()).thenReturn(traceFlags);
    Mockito.when(spanContext.getTraceState()).thenReturn(traceState);
    Mockito.when(span.getSpanContext()).thenReturn(spanContext);
    Mockito.when(spanBuilder.setAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(spanBuilder);
    Mockito.when(spanBuilder.setAttribute(Mockito.anyString(), Mockito.anyLong())).thenReturn(spanBuilder);
    Mockito.when(spanBuilder.setAttribute(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(spanBuilder);
    Mockito.when(spanBuilder.setSpanKind(Mockito.any(SpanKind.class))).thenReturn(spanBuilder);
    Mockito.when(spanBuilder.startSpan()).thenReturn(span);
    Mockito.when(tracer.spanBuilder(Mockito.anyString())).thenReturn(spanBuilder);
    Mockito.when(traceContext.actualSqlText()).thenReturn("Actual SQL");
    Mockito.when(traceContext.getConnectionId()).thenReturn("ConnectionId");
    Mockito.when(traceContext.databaseOperation()).thenReturn("Execute statement");
    Mockito.when(traceContext.getClientInfo(Mockito.anyString())).thenReturn("ClientInfo");
    Mockito.when(traceContext.getSqlId()).thenReturn("SqlID");
    Mockito.when(traceContext.isCompletedExceptionally()).thenReturn(false);
    Mockito.when(traceContext.originalSqlText()).thenReturn("Original SQL");
    Mockito.when(traceContext.tenant()).thenReturn("tenant");
    Mockito.when(traceContext.user()).thenReturn("user");
  }

  @Test
  void testPropertiesDisabled() throws Exception {
    System.setProperty(OpenTelemetryTraceEventListener.OPEN_TELEMETRY_TRACE_EVENT_LISTENER_ENABLED, "false");
    System.setProperty(OpenTelemetryTraceEventListener.OPEN_TELEMETRY_TRACE_EVENT_LISTENER_SENSITIVE_ENABLED, "false");
    OpenTelemetryTraceEventListener traceEventListener = new OpenTelemetryTraceEventListener(tracer);
    Assertions.assertFalse(traceEventListener.isEnabled(), "Set to false using system property");
    Assertions.assertFalse(traceEventListener.isSensitiveDataEnabled(), "Set to false using system property");
  }

  @Test
  void testPropertiesEnabled() throws Exception {
    System.setProperty(OpenTelemetryTraceEventListener.OPEN_TELEMETRY_TRACE_EVENT_LISTENER_ENABLED, "true");
    System.setProperty(OpenTelemetryTraceEventListener.OPEN_TELEMETRY_TRACE_EVENT_LISTENER_SENSITIVE_ENABLED, "true");
    OpenTelemetryTraceEventListener traceEventListener = new OpenTelemetryTraceEventListener(tracer);
    Assertions.assertTrue(traceEventListener.isEnabled(), "Set to false using system property");
    Assertions.assertTrue(traceEventListener.isSensitiveDataEnabled(), "Set to false using system property");
  }

  @Test
  public void roundTripEnabledSensitiveSuccessTest() throws Exception {
    traceEventListener.setEnabled(true);
    traceEventListener.setSensitiveDataEnabled(true);
    Object userContext = traceEventListener.roundTrip(Sequence.BEFORE, traceContext, null);
    Mockito.verify(tracer, atLeastOnce()).spanBuilder(traceContext.databaseOperation());
    Mockito.verify(spanBuilder, atLeastOnce()).startSpan();
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("thread.id", Thread.currentThread().getId());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("thread.name", Thread.currentThread().getName());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Connection ID", traceContext.getConnectionId());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Database Operation", traceContext.databaseOperation());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Database User", traceContext.user());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Database Tenant", traceContext.tenant());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("SQL ID", traceContext.getSqlId());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Original SQL Text", traceContext.originalSqlText());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Actual SQL Text", traceContext.actualSqlText());
    userContext = traceEventListener.roundTrip(Sequence.AFTER, traceContext, userContext);
    Mockito.verify(span, Mockito.times(1)).setStatus(StatusCode.OK);
    Mockito.verify(span, Mockito.times(1)).end(any(Instant.class));
  }

  @Test
  public void roundTripNotEnabledSensitiveSuccessTest() throws Exception {
    traceEventListener.setEnabled(false);
    traceEventListener.setSensitiveDataEnabled(true);
    Object userContext = traceEventListener.roundTrip(Sequence.BEFORE, traceContext, null);
    Mockito.verify(tracer, never()).spanBuilder(traceContext.databaseOperation());
    Mockito.verify(spanBuilder, never()).startSpan();
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("thread.id", Thread.currentThread().getId());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("thread.name", Thread.currentThread().getName());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Connection ID", traceContext.getConnectionId());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Database Operation", traceContext.databaseOperation());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Database User", traceContext.user());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Database Tenant", traceContext.tenant());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("SQL ID", traceContext.getSqlId());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Original SQL Text", traceContext.originalSqlText());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Actual SQL Text", traceContext.actualSqlText());
    userContext = traceEventListener.roundTrip(Sequence.AFTER, traceContext, userContext);
    Mockito.verify(span, Mockito.times(0)).setStatus(StatusCode.OK);
    Mockito.verify(span, Mockito.times(0)).end(any(Instant.class));
  }

  @Test
  public void roundTripEnabledNotSensitiveSuccessTest() throws Exception {
    traceEventListener.setEnabled(true);
    traceEventListener.setSensitiveDataEnabled(false);
    Object userContext = traceEventListener.roundTrip(Sequence.BEFORE, traceContext, null);
    Mockito.verify(tracer, atLeastOnce()).spanBuilder(traceContext.databaseOperation());
    Mockito.verify(spanBuilder, atLeastOnce()).startSpan();
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("thread.id", Thread.currentThread().getId());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("thread.name", Thread.currentThread().getName());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Connection ID", traceContext.getConnectionId());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Database Operation", traceContext.databaseOperation());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Database User", traceContext.user());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Database Tenant", traceContext.tenant());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("SQL ID", traceContext.getSqlId());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Original SQL Text", traceContext.originalSqlText());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Actual SQL Text", traceContext.actualSqlText());
    userContext = traceEventListener.roundTrip(Sequence.AFTER, traceContext, userContext);
    Mockito.verify(span, Mockito.times(1)).setStatus(StatusCode.OK);
    Mockito.verify(span, Mockito.times(1)).end(any(Instant.class));
  }

  @Test
  public void roundTripNotEnabledNotSensitiveSuccessTest() throws Exception {
    traceEventListener.setEnabled(false);
    traceEventListener.setSensitiveDataEnabled(true);
    Object userContext = traceEventListener.roundTrip(Sequence.BEFORE, traceContext, null);
    Mockito.verify(spanBuilder, never()).startSpan();
    Mockito.verify(tracer, never()).spanBuilder(traceContext.databaseOperation());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("thread.id", Thread.currentThread().getId());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("thread.name", Thread.currentThread().getName());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Connection ID", traceContext.getConnectionId());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Database Operation", traceContext.databaseOperation());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Database User", traceContext.user());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Database Tenant", traceContext.tenant());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("SQL ID", traceContext.getSqlId());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Original SQL Text", traceContext.originalSqlText());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Actual SQL Text", traceContext.actualSqlText());
    userContext = traceEventListener.roundTrip(Sequence.AFTER, traceContext, userContext);
    Mockito.verify(span, Mockito.times(0)).setStatus(StatusCode.OK);
    Mockito.verify(span, Mockito.times(0)).end(any(Instant.class));
  }

  @Test
  public void executionEventEnabledSensitiveACReplayStarted() throws Exception {
    traceEventListener.setEnabled(true);
    traceEventListener.setSensitiveDataEnabled(true);
    SQLException exception = Mockito.mock(SQLException.class);
    Mockito.when(exception.getMessage()).thenReturn("SQL message");
    Mockito.when(exception.getErrorCode()).thenReturn(123);
    Mockito.when(exception.getSQLState()).thenReturn("SQL State");
    int retryCount = 2;
    Object[] params = new Object[] { exception.getMessage(), exception, retryCount };
    traceEventListener.onExecutionEventReceived(JdbcExecutionEvent.AC_REPLAY_STARTED, traceContext,
        params);
    Mockito.verify(spanBuilder, atLeastOnce()).startSpan();
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Error Message", exception.getMessage());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Error code", exception.getErrorCode());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("SQL state", exception.getSQLState());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Current replay retry count",
        String.valueOf(retryCount));
  }

  @Test
  public void executionEventEnabledSensitiveACReplaySuccessful() throws Exception {
    traceEventListener.setEnabled(true);
    traceEventListener.setSensitiveDataEnabled(true);
    SQLException exception = Mockito.mock(SQLException.class);
    Mockito.when(exception.getMessage()).thenReturn("SQL message");
    Mockito.when(exception.getErrorCode()).thenReturn(123);
    Mockito.when(exception.getSQLState()).thenReturn("SQL State");
    int retryCount = 2;
    Object[] params = new Object[] { exception.getMessage(), exception, retryCount };
    traceEventListener.onExecutionEventReceived(JdbcExecutionEvent.AC_REPLAY_SUCCESSFUL,
        traceContext, params);
    Mockito.verify(spanBuilder, atLeastOnce()).startSpan();
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Error Message", exception.getMessage());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Error code", exception.getErrorCode());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("SQL state", exception.getSQLState());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Current replay retry count",
        String.valueOf(retryCount));
  }

  @Test
  public void executionEventEnabledSensitiveVIPRetry() throws Exception {
    traceEventListener.setEnabled(true);
    traceEventListener.setSensitiveDataEnabled(true);
    Object[] params = new Object[] { "Error message", "Protocol", "Host", "Port", "Service name", "SID",
        "Connection data", "VIP Address" };
    traceEventListener.onExecutionEventReceived(JdbcExecutionEvent.VIP_RETRY,
        traceContext, params);
    Mockito.verify(spanBuilder, atLeastOnce()).startSpan();
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Error message", params[0].toString());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Protocol", params[1].toString());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Host", params[2].toString());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Port", params[3].toString());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Service name", params[4].toString());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("SID", params[5].toString());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Connection data", params[6].toString());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("VIP Address", params[7].toString());
  }

  @Test
  public void executionEventEnabledNotSensitiveACReplayStarted() throws Exception {
    traceEventListener.setEnabled(true);
    traceEventListener.setSensitiveDataEnabled(false);
    SQLException exception = Mockito.mock(SQLException.class);
    Mockito.when(exception.getMessage()).thenReturn("SQL message");
    Mockito.when(exception.getErrorCode()).thenReturn(123);
    Mockito.when(exception.getSQLState()).thenReturn("SQL State");
    int retryCount = 2;
    Object[] params = new Object[] { exception.getMessage(), exception, retryCount };
    traceEventListener.onExecutionEventReceived(JdbcExecutionEvent.AC_REPLAY_STARTED, traceContext,
        params);
    Mockito.verify(spanBuilder, atLeastOnce()).startSpan();
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Error Message", exception.getMessage());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Error code", exception.getErrorCode());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("SQL state", exception.getSQLState());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Current replay retry count",
        String.valueOf(retryCount));
  }

  @Test
  public void executionEventEnabledNotSensitiveACReplaySuccessful() throws Exception {
    traceEventListener.setEnabled(true);
    traceEventListener.setSensitiveDataEnabled(false);
    SQLException exception = Mockito.mock(SQLException.class);
    Mockito.when(exception.getMessage()).thenReturn("SQL message");
    Mockito.when(exception.getErrorCode()).thenReturn(123);
    Mockito.when(exception.getSQLState()).thenReturn("SQL State");
    int retryCount = 2;
    Object[] params = new Object[] { exception.getMessage(), exception, retryCount };
    traceEventListener.onExecutionEventReceived(JdbcExecutionEvent.AC_REPLAY_SUCCESSFUL,
        traceContext, params);
    Mockito.verify(spanBuilder, atLeastOnce()).startSpan();
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Error Message", exception.getMessage());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Error code", exception.getErrorCode());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("SQL state", exception.getSQLState());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Current replay retry count",
        String.valueOf(retryCount));
  }

  @Test
  public void executionEventEnabledNotSensitiveVIPRetry() throws Exception {
    traceEventListener.setEnabled(true);
    traceEventListener.setSensitiveDataEnabled(false);
    Object[] params = new Object[] { "Error message", "Protocol", "Host", "Port", "Service name", "SID",
        "Connection data", "VIP Address" };
    traceEventListener.onExecutionEventReceived(JdbcExecutionEvent.VIP_RETRY,
        traceContext, params);
    Mockito.verify(spanBuilder, atLeastOnce()).startSpan();
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("Error message", params[0].toString());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Protocol", params[1].toString());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Host", params[2].toString());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Port", params[3].toString());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Service name", params[4].toString());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("SID", params[5].toString());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Connection data", params[6].toString());
    Mockito.verify(spanBuilder, Mockito.times(1)).setAttribute("VIP Address", params[7].toString());
  }

  @Test
  public void executionEventNotEnabledSensitiveACReplayStarted() throws Exception {
    traceEventListener.setEnabled(false);
    traceEventListener.setSensitiveDataEnabled(true);
    SQLException exception = Mockito.mock(SQLException.class);
    Mockito.when(exception.getMessage()).thenReturn("SQL message");
    Mockito.when(exception.getErrorCode()).thenReturn(123);
    Mockito.when(exception.getSQLState()).thenReturn("SQL State");
    int retryCount = 2;
    Object[] params = new Object[] { exception.getMessage(), exception, retryCount };
    traceEventListener.onExecutionEventReceived(JdbcExecutionEvent.AC_REPLAY_STARTED, traceContext,
        params);
    Mockito.verify(spanBuilder, never()).startSpan();
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Error Message", exception.getMessage());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Error code", exception.getErrorCode());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("SQL state", exception.getSQLState());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Current replay retry count",
        String.valueOf(retryCount));
  }

  @Test
  public void executionEventNotEnabledSensitiveACReplaySuccessful() throws Exception {
    traceEventListener.setEnabled(false);
    traceEventListener.setSensitiveDataEnabled(true);
    SQLException exception = Mockito.mock(SQLException.class);
    Mockito.when(exception.getMessage()).thenReturn("SQL message");
    Mockito.when(exception.getErrorCode()).thenReturn(123);
    Mockito.when(exception.getSQLState()).thenReturn("SQL State");
    int retryCount = 2;
    Object[] params = new Object[] { exception.getMessage(), exception, retryCount };
    traceEventListener.onExecutionEventReceived(JdbcExecutionEvent.AC_REPLAY_SUCCESSFUL,
        traceContext, params);
    Mockito.verify(spanBuilder, never()).startSpan();
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Error Message", exception.getMessage());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Error code", exception.getErrorCode());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("SQL state", exception.getSQLState());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Current replay retry count",
        String.valueOf(retryCount));
  }

  @Test
  public void executionEventNotEnabledSensitiveVIPRetry() throws Exception {
    traceEventListener.setEnabled(false);
    traceEventListener.setSensitiveDataEnabled(true);
    Object[] params = new Object[] { "Error message", "Protocol", "Host", "Port", "Service name", "SID",
        "Connection data", "VIP Address" };
    traceEventListener.onExecutionEventReceived(JdbcExecutionEvent.VIP_RETRY,
        traceContext, params);
    Mockito.verify(spanBuilder, never()).startSpan();
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Error message", params[0].toString());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Protocol", params[1].toString());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Host", params[2].toString());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Port", params[3].toString());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Service name", params[4].toString());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("SID", params[5].toString());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Connection data", params[6].toString());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("VIP Address", params[7].toString());
  }

  @Test
  public void executionEventNotEnabledNotSensitiveACReplayStarted() throws Exception {
    traceEventListener.setEnabled(false);
    traceEventListener.setSensitiveDataEnabled(false);
    SQLException exception = Mockito.mock(SQLException.class);
    Mockito.when(exception.getMessage()).thenReturn("SQL message");
    Mockito.when(exception.getErrorCode()).thenReturn(123);
    Mockito.when(exception.getSQLState()).thenReturn("SQL State");
    int retryCount = 2;
    Object[] params = new Object[] { exception.getMessage(), exception, retryCount };
    traceEventListener.onExecutionEventReceived(JdbcExecutionEvent.AC_REPLAY_STARTED, traceContext,
        params);
    Mockito.verify(spanBuilder, never()).startSpan();
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Error Message", exception.getMessage());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Error code", exception.getErrorCode());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("SQL state", exception.getSQLState());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Current replay retry count",
        String.valueOf(retryCount));
  }

  @Test
  public void executionEventNotEnabledNotSensitiveACReplaySuccessful() throws Exception {
    traceEventListener.setEnabled(false);
    traceEventListener.setSensitiveDataEnabled(false);
    SQLException exception = Mockito.mock(SQLException.class);
    Mockito.when(exception.getMessage()).thenReturn("SQL message");
    Mockito.when(exception.getErrorCode()).thenReturn(123);
    Mockito.when(exception.getSQLState()).thenReturn("SQL State");
    int retryCount = 2;
    Object[] params = new Object[] { exception.getMessage(), exception, retryCount };
    traceEventListener.onExecutionEventReceived(JdbcExecutionEvent.AC_REPLAY_SUCCESSFUL,
        traceContext, params);
    Mockito.verify(spanBuilder, never()).startSpan();
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Error Message", exception.getMessage());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Error code", exception.getErrorCode());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("SQL state", exception.getSQLState());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Current replay retry count",
        String.valueOf(retryCount));
  }

  @Test
  public void executionEventNotEnabledNotSensitiveVIPRetry() throws Exception {
    traceEventListener.setEnabled(false);
    traceEventListener.setSensitiveDataEnabled(false);
    Object[] params = new Object[] { "Error message", "Protocol", "Host", "Port", "Service name", "SID",
        "Connection data", "VIP Address" };
    traceEventListener.onExecutionEventReceived(JdbcExecutionEvent.VIP_RETRY,
        traceContext, params);
    Mockito.verify(spanBuilder, never()).startSpan();
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Error message", params[0].toString());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Protocol", params[1].toString());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Host", params[2].toString());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Port", params[3].toString());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Service name", params[4].toString());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("SID", params[5].toString());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("Connection data", params[6].toString());
    Mockito.verify(spanBuilder, Mockito.times(0)).setAttribute("VIP Address", params[7].toString());
  }

  @Test
  public void executionEventEnabledWrongParameterCountACReplayStarted() throws Exception {
    traceEventListener.setEnabled(true);
    traceEventListener.setSensitiveDataEnabled(true);
    Object[] params = new Object[] { "Only one marameter" };
    traceEventListener.onExecutionEventReceived(JdbcExecutionEvent.AC_REPLAY_STARTED, traceContext,
        params);
    Mockito.verify(spanBuilder, never()).startSpan();
  }

  @Test
  public void executionEventEnabledWrongParameterCountACReplaySuccessful() throws Exception {
    traceEventListener.setEnabled(true);
    traceEventListener.setSensitiveDataEnabled(true);
    Object[] params = new Object[] { "Only one marameter" };
    traceEventListener.onExecutionEventReceived(JdbcExecutionEvent.AC_REPLAY_SUCCESSFUL,
        traceContext, params);
    Mockito.verify(spanBuilder, never()).startSpan();
  }

  @Test
  public void executionEventEnabledWrongParameterCountVIPRetry() throws Exception {
    traceEventListener.setEnabled(true);
    traceEventListener.setSensitiveDataEnabled(true);
    Object[] params = new Object[] { "Only one marameter" };
    traceEventListener.onExecutionEventReceived(JdbcExecutionEvent.VIP_RETRY,
        traceContext, params);
    Mockito.verify(spanBuilder, never()).startSpan();
  }

}
