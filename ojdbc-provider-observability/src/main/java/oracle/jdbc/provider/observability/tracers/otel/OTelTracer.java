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
package oracle.jdbc.provider.observability.tracers.otel;

import java.lang.management.ManagementFactory;
import java.security.Timestamp;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import oracle.jdbc.TraceEventListener;
import oracle.jdbc.TraceEventListener.JdbcExecutionEvent;
import oracle.jdbc.TraceEventListener.Sequence;
import oracle.jdbc.TraceEventListener.SpanEventType;
import oracle.jdbc.TraceEventListener.TraceContext;
import oracle.jdbc.provider.observability.ObservabilityConfiguration;
import oracle.jdbc.provider.observability.tracers.ObservabilityTracer;
import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManagerMBeanImpl;
import oracle.jdbc.DatabaseFunction;

/**
 * Open Telemetry tracer. Exports round trip event and execution events to
 * Open Telemetry.
 */
public class OTelTracer implements ObservabilityTracer {

  /**
   * Key used to send the current Open Telemetry Trace Context to the server 
   * using {@link TraceContext#setClientInfo(String, String)}.
   */
  private static final String TRACE_KEY = "clientcontext.ora$opentelem$tracectx";

  /**
   * The trace context is sent to the server in two lines, this is the first 
   * line it contains the version and the span context.
   */
  private static final String TRACE_FORMAT = "traceparent: %s-%s-%s-%s\r\n";

  /**
   * Trace context version.
   */
  private static final String TRACE_VERSION = "00";

  /**
   * Format of the second line sent to the server containing the trace context.
   * It contains the trace state.
   */
  private static final String TRACE_STATE_FORMAT = "tracestate: %s\r\n";

  /**
   * Logger.
   */
  private static Logger logger = Logger.getLogger(OTelTracer.class.getPackageName());

  /**
   * Configuraiton
   */
  private final ObservabilityConfiguration configuration;

  /**
   * Constructor. This tracer always uses {@link GlobalOpenTelemetry} to get 
   * the Open Telemetry tracer.
   * 
   * @param configuration the configuration.
   */
  public OTelTracer(ObservabilityConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public String getName() {
    return "OTEL";
  }

  @Override
  public Object traceRoundTrip(Sequence sequence, TraceContext traceContext, Object userContext) {
    if (sequence == Sequence.BEFORE) {
      // Create the Span before the round-trip.
      final Span span = initAndGetSpan(traceContext, traceContext.databaseOperation());
      makeSpanCurrentAndSendContextToServer(traceContext, span);
      // Return the Span instance to the driver. The driver holds this instance and
      // supplies it as user context parameter on the next round-trip call.
      return span;
    } else {
      // End the Span after the round-trip.
      if (userContext != null) {
        final Span span = (Span) userContext;
        span.setStatus(traceContext.isCompletedExceptionally() ? StatusCode.ERROR : StatusCode.OK);
        if (traceContext.getThrowable() != null) {
          if (traceContext.getThrowable() instanceof SQLException) {
            span.setAttribute("db.response.status_code", ((SQLException)traceContext.getThrowable()).getErrorCode());
          }
          span.recordException(traceContext.getThrowable());
        }
        span.end();
      } else {
        logger.log(Level.WARNING, "Unknown or null user context received from the driver on " +
            "database operation: " + traceContext.databaseOperation());
      }
      return null;
    }
  }

  @Override
  public Consumer<TraceContext> SpanEvent(SpanEventType spanEventType, TraceContext traceContext) {
    logger.log(Level.FINEST, "Received Span Event: " + spanEventType.toString());
    Tracer tracer = GlobalOpenTelemetry.get().getTracer(OTelTracer.class.getName());
    Instant startTimeInstant = Instant.now();
    final Span span = tracer.spanBuilder(
      SpanEventType.ROUND_TRIP.equals(spanEventType) ? 
        (DatabaseFunction.EXECUTE_QUERY.equals(traceContext.databaseFunction()) ?
          traceContext.databaseOperation() + ": " + traceContext.getSqlType() :
          traceContext.databaseOperation()) :
        spanEventType.toString())
      .setAttribute("db.event.type", spanEventType.toString())
      .setSpanKind(SpanKind.CLIENT)
      .setStartTimestamp(startTimeInstant)
      .startSpan();
    final Scope scope = span.makeCurrent();
    try {
      // If the connection is not closed, send the current context to the server
      if (!traceContext.isConnectionClosed()) {
        final String traceParent = initAndGetTraceParent(span);
        final String traceState = initAndGetTraceState(span);
        logger.log(Level.FINEST, "Sending client info " + traceParent + traceState);
        traceContext.setClientInfo(TRACE_KEY, traceParent + traceState);
      } else {
        logger.log(Level.FINEST, "Connection is closed.");
      }
    } catch (Exception ex) {
      logger.log(Level.WARNING, "An error occured while sending the current Open Telemetry context to the server. " 
          + ex.getMessage(), ex);
    }
    return (t) -> {
      Instant endInstant = Instant.now();
      logger.log(Level.FINEST, "End Span Event: " + spanEventType.toString());
      trace(span, traceContext);
      span.end(endInstant);
      Double durationInSeconds = ChronoUnit.MILLIS.between(startTimeInstant, endInstant) / 1000.0;
      configuration.getOTelMetrics().recordDuration(durationInSeconds, getAttributesForTraceContext(traceContext));
      switch (spanEventType) {
        case ROUND_TRIP:
          if (DatabaseFunction.EXECUTE_QUERY.equals(traceContext.databaseFunction()) ||
            DatabaseFunction.FETCH_ROW.equals(traceContext.databaseFunction())) {
            configuration.getOTelMetrics().recordReturnedRows(traceContext.getNumRows(), getAttributesForTraceContext(traceContext));
          }
          break;
        case REQUEST;
          configuration.getOTelMetrics().recordUseTime(durationInSeconds, getAttributesForTraceContext(traceContext));
          break;
        case CONNECTION:
          configuration.getOTelMetrics().recordCreateTime(durationInSeconds, getAttributesForTraceContext(traceContext));
          break
        default:
          break;
      }
      if (SpanEventType.REQUEST.equals(spanEventType)) {
        
      }

      scope.close();
    };

  }

  @Override
  public Object traceExecutionEvent(JdbcExecutionEvent event, Object userContext, Object... params) {
    if (EXECUTION_EVENTS_PARAMETERS.get(event) == params.length) {
      Tracer tracer = GlobalOpenTelemetry.get().getTracer(OTelTracer.class.getName());
      if (event == TraceEventListener.JdbcExecutionEvent.VIP_RETRY) {
        Thread.dumpStack();
        SpanBuilder spanBuilder = tracer
            .spanBuilder(event.getDescription())
            .setAttribute("Error message", params[0] != null ? params[0].toString() : "")
            .setAttribute("VIP Address", params[7] != null ? params[7].toString() : "");
        // Add sensitive information (URL and SQL) if it is enabled
        if (configuration.getSensitiveDataEnabled()) {
          logger.log(Level.FINEST, "Sensitive information on");
          spanBuilder.setAttribute("Protocol", params[1] != null ? params[1].toString() : "")
              .setAttribute("Host", params[2] != null ? params[2].toString() : "")
              .setAttribute("Port", params[3] != null ? params[3].toString() : "")
              .setAttribute("Service name", params[4] != null ? params[4].toString(): "")
              .setAttribute("SID", params[5] != null ? params[5].toString() : "")
              .setAttribute("Connection data", params[6] != null ? params[6].toString() : "");
        }
        // start and end span.
        spanBuilder.startSpan().end();
      } else if (event == TraceEventListener.JdbcExecutionEvent.AC_REPLAY_STARTED
          || event == TraceEventListener.JdbcExecutionEvent.AC_REPLAY_SUCCESSFUL) {
        SpanBuilder spanBuilder = tracer
            .spanBuilder(event.getDescription())
            .setAttribute("Error Message", (params[0] != null ? params[0].toString() : ""))
            .setAttribute("Error code", (params[1] != null ? ((SQLException) params[1]).getErrorCode() : 0))
            .setAttribute("SQL state",  (params[1] != null ? ((SQLException) params[1]).getSQLState() : ""))
            .setAttribute("Current replay retry count",  (params[2] != null ? params[2].toString() : ""));
        spanBuilder.startSpan().end();
      } else {
        logger.log(Level.WARNING, "Unknown event received : " + event.toString());
      }
    } else {
      // log wrong number of parameters returned for execution event
      logger.log(Level.WARNING, "Wrong number of parameters received for event " + event.toString());
    }
    // return the previous userContext
    return userContext;
  }

  /**
   * Creates a Open Telemetry Span and sets it's attributes according to the
   * trace context and the configuration.
   * 
   * @param traceContext the trace context.
   * @param spanName then span name.
   * @return returns the Span.
   */
  private Span initAndGetSpan(TraceContext traceContext, String spanName) {
    /*
     * If this is in the context of current span, the following becomes a nested or
     * child span to the current span. I.e. the current span in context becomes
     * parent to this child span.
     */
    Tracer tracer = GlobalOpenTelemetry.get().getTracer(OTelTracer.class.getName());
    SpanBuilder spanBuilder = tracer
        .spanBuilder(spanName)
        .setAttribute("thread.id", Thread.currentThread().getId())
        .setAttribute("thread.name", Thread.currentThread().getName())
        .setAttribute("Connection ID", traceContext.getConnectionId())
        .setAttribute("Database Operation", traceContext.databaseFunction() != null ? traceContext.databaseOperation() : "")
        .setAttribute("Database Tenant", traceContext.tenant())
        .setAttribute("db.namespace", traceContext.getDatabaseName())
        .setAttribute("server.port", traceContext.getServerPort())
        .setAttribute("db.query.summary", traceContext.getSqlType())
        .setAttribute("db.response.returned_rows", traceContext.getNumRows())
        .setAttribute("server.address", traceContext.getServerAddress())
        .setAttribute("db.connection_string", traceContext.getConnectionUrl())
        .setAttribute("db.instance.id", traceContext.getInstanceName())
        .setAttribute("db.system", "oracle.db")
        .setAttribute("SQL ID", traceContext.getSqlId());

    // Add sensitive information (URL and SQL) if it is enabled
    if (configuration.getSensitiveDataEnabled()) {
      logger.log(Level.FINEST, "Sensitive information on");
      spanBuilder
          .setAttribute("Database User", traceContext.user())
          .setAttribute("Original SQL Text", traceContext.originalSqlText())
          .setAttribute("db.query.text", traceContext.actualSqlText())
          .setAttribute("db.user", traceContext.user())
          .setAttribute("Actual SQL Text", traceContext.actualSqlText());
    } else {
      spanBuilder
          .setAttribute("db.query.text", traceContext.getSqlType() + " " + traceContext.getDatabaseName() + "***");
    }

    // According to the semantic conventions the Span Kind should be CLIENT,
    // used to be SERVER.
    return spanBuilder.setSpanKind(SpanKind.CLIENT).startSpan();

  }

  /**
   * Sets the span as the current Open Telemetry Span and sends context information
   * to the database server.
   * 
   * @param traceContext the trace context
   * @param span the currect spans
   */
  private void makeSpanCurrentAndSendContextToServer(TraceContext traceContext, Span span) {
    final String traceParent = initAndGetTraceParent(span);
    final String traceState = initAndGetTraceState(span);

    try (Scope ignored = span.makeCurrent()) {
      // Send the current context to the server
      traceContext.setClientInfo(TRACE_KEY, traceParent + traceState);
    } catch (Exception ex) {
      logger.log(Level.WARNING, "An error occured while sending the current Open Telemetry context to the server. " 
          + ex.getMessage(), ex);
    }
  }

  /**
   * Formats the current Open Telemetry context in a format that the server can
   * understand.
   * 
   * @param span the current Span
   * @return the current Open Telemetry context formatted so that the server
   * can understand.
   */
  private String initAndGetTraceParent(Span span) {
    final SpanContext spanContext = span.getSpanContext();
    final String traceId = spanContext.getTraceId();
    // parent-id is known as the span-id
    final String parentId = spanContext.getSpanId();
    final String traceFlags = spanContext.getTraceFlags().toString();

    return String.format(TRACE_FORMAT, TRACE_VERSION, traceId, parentId, traceFlags);
  }

    /**
   * Formats the current Open Telemetry Span state in a format that the server 
   * can understand.
   * 
   * @param span the current Span
   * @return the current Open Telemetry Span state formatted so that the server
   * can understand.
   */
  private String initAndGetTraceState(Span span) {
    final TraceState traceState = span.getSpanContext().getTraceState();
    final StringBuilder stringBuilder = new StringBuilder();

    traceState.forEach((k, v) -> stringBuilder.append(k).append("=").append(v));
    return String.format(TRACE_STATE_FORMAT, stringBuilder);
  }

    /**
   * Creates a Open Telemetry Span and sets it's attributes according to the
   * trace context and the configuration.
   * 
   * @param traceContext the trace context.
   * @param spanName then span name.
   * @return returns the Span.
   */
  private void trace(Span span, TraceContext traceContext) {
    /*
     * If this is in the context of current span, the following becomes a nested or
     * child span to the current span. I.e. the current span in context becomes
     * parent to this child span.
     */
    span.setAllAttributes(getAttributesForTraceContext(traceContext));
    span.setStatus(traceContext.isCompletedExceptionally() != null && traceContext.isCompletedExceptionally() ? StatusCode.ERROR : StatusCode.OK);
    if (traceContext.getThrowable() != null) {
      span.recordException(traceContext.getThrowable());
    }
  }

  public Attributes getAttributesForTraceContext(TraceContext traceContext) {
    AttributesBuilder attributesBuilder = Attributes.builder();
    attributesBuilder.put("thread.id", Thread.currentThread().getId())
      .put("thread.name", Thread.currentThread().getName())
      .put("Connection ID", traceContext.getConnectionId())
      .put("Database Operation", traceContext.databaseFunction() != null ? traceContext.databaseOperation() : "")
      .put("Database Tenant", traceContext.tenant())
      .put("db.namespace", traceContext.getDatabaseName())
      .put("server.port", traceContext.getServerPort())
      .put("db.query.summary", DatabaseFunction.EXECUTE_QUERY.equals(traceContext.databaseFunction()) ? traceContext.getSqlType() : "")
      .put("db.response.returned_rows", traceContext.getNumRows())
      .put("server.address", traceContext.getServerAddress())
      .put("db.connection_string", traceContext.getConnectionUrl())
      .put("db.instance.id", traceContext.getInstanceName())
      .put("db.service.name", traceContext.getServiceName())
      .put("db.schema", traceContext.getSchema())
      .put("db.shard.name", traceContext.getShardName())
      .put("db.server.pid", traceContext.getServerPID())
      .put("db.session.id", traceContext.getSessionID())
      .put("db.system", "oracle.db")
      .put("Component", "JDBC")
      .put("SQL ID", traceContext.getSqlId());
      
    if (traceContext.getThrowable() != null) {
      if (traceContext.getThrowable() instanceof SQLException) {
        attributesBuilder.put("db.response.status_code", ((SQLException)traceContext.getThrowable()).getErrorCode());
      }
    }

    // Add sensitive information (URL and SQL) if it is enabled
    if (configuration.getSensitiveDataEnabled()) {
      logger.log(Level.FINEST, "Sensitive information on");
      attributesBuilder
        .put("Database User", traceContext.user())
        .put("Original SQL Text", traceContext.originalSqlText())
        .put("db.query.text", traceContext.actualSqlText())
        .put("db.user", traceContext.user())
        .put("Actual SQL Text", traceContext.actualSqlText());
    }

    return attributesBuilder.build();
  }


}
