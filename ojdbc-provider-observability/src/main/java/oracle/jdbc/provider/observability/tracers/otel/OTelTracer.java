package oracle.jdbc.provider.observability.tracers.otel;

import java.sql.SQLException;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.login.Configuration;

import io.opentelemetry.api.GlobalOpenTelemetry;
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
import oracle.jdbc.TraceEventListener.TraceContext;
import oracle.jdbc.provider.observability.configuration.ObservabilityConfiguration;
import oracle.jdbc.provider.observability.tracers.ObservabilityTracer;

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
   * Logger.
   */
  private static Logger logger = Logger.getLogger(OTelTracer.class.getName());

  /**
   * Constructor. This tracer always uses {@link GlobalOpenTelemetry} to get 
   * the Open Telemetry tracer.
   */
  public OTelTracer() { }

  @Override
  public Object traceRoundtrip(Sequence sequence, TraceContext traceContext, Object userContext) {
    if (sequence == Sequence.BEFORE) {
      // Create the Span before the round-trip.
      final Span span = initAndGetSpan(traceContext, traceContext.databaseOperation());
      try (Scope ignored = span.makeCurrent()) {
        traceContext.setClientInfo(TRACE_KEY, getTraceValue(span));
      } catch (Exception ex) {
        logger.log(Level.WARNING, ex.getMessage(), ex);
      }
      // Return the Span instance to the driver. The driver holds this instance and
      // supplies it
      // as user context parameter on the next round-trip call.
      return span;
    } else {
      // End the Span after the round-trip.
      if (userContext instanceof Span) {
        final Span span = (Span) userContext;
        span.setStatus(traceContext.isCompletedExceptionally() ? StatusCode.ERROR : StatusCode.OK);
        span.end(Instant.now());
      }
      return null;
    }
  }

  @Override
  public Object traceExecutionEvent(JdbcExecutionEvent event, Object userContext, Object... params) {
    if (EXECUTION_EVENTS_PARAMETERS.get(event) == params.length) {
      Tracer tracer = GlobalOpenTelemetry.get().getTracer(OTelTracer.class.getName());
      if (event == TraceEventListener.JdbcExecutionEvent.VIP_RETRY) {
        SpanBuilder spanBuilder = tracer
            .spanBuilder(event.getDescription())
            .setAttribute("Error message", params[0].toString())
            .setAttribute("VIP Address", params[7].toString());
        // Add sensitive information (URL and SQL) if it is enabled
        if (ObservabilityConfiguration.getInstance().getSensitiveDataEnabled()) {
          logger.log(Level.FINEST, "Sensitive information on");
          spanBuilder.setAttribute("Protocol", params[1].toString())
              .setAttribute("Host", params[2].toString())
              .setAttribute("Port", params[3].toString())
              .setAttribute("Service name", params[4].toString())
              .setAttribute("SID", params[5].toString())
              .setAttribute("Connection data", params[6].toString());
        }
        return spanBuilder.startSpan();
      } else if (event == TraceEventListener.JdbcExecutionEvent.AC_REPLAY_STARTED
          || event == TraceEventListener.JdbcExecutionEvent.AC_REPLAY_SUCCESSFUL) {
        SpanBuilder spanBuilder = tracer
            .spanBuilder(event.getDescription())
            .setAttribute("Error Message", params[0].toString())
            .setAttribute("Error code", ((SQLException) params[1]).getErrorCode())
            .setAttribute("SQL state", ((SQLException) params[1]).getSQLState())
            .setAttribute("Current replay retry count", params[2].toString());
        return spanBuilder.startSpan();
      } else {
        logger.log(Level.WARNING, "Unknown event received : " + event.toString());
      }
    } else {
      // log wrong number of parameters returned for execution event
      logger.log(Level.WARNING, "Wrong number of parameters received for event " + event.toString());
    }
    return null;
  }

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
        .setAttribute("Database Operation", traceContext.databaseOperation())
        .setAttribute("Database User", traceContext.user())
        .setAttribute("Database Tenant", traceContext.tenant())
        .setAttribute("SQL ID", traceContext.getSqlId());

    // Add sensitive information (URL and SQL) if it is enabled
    if (ObservabilityConfiguration.getInstance().getSensitiveDataEnabled()) {
      logger.log(Level.FINEST, "Sensitive information on");
      spanBuilder
          .setAttribute("Original SQL Text", traceContext.originalSqlText())
          .setAttribute("Actual SQL Text", traceContext.actualSqlText());
    }

    // According to the semantic conventions the Span Kind should be CLIENT,
    // used to be SERVER.
    return spanBuilder.setSpanKind(SpanKind.CLIENT).startSpan();

  }

  /**
   * Builds the Open Telemetry trace context to be sent to the database server.
   * @param span the currect spans
   * @return the current trace context formatted so that the server can read it.
   */
  private String getTraceValue(Span span) {
    final String traceParent = initAndGetTraceParent(span);
    final String traceState = initAndGetTraceState(span);
    return traceParent + traceState;
  }

  private String initAndGetTraceParent(Span span) {
    final SpanContext spanContext = span.getSpanContext();
    // The current specification assumes the version is set to 00.
    final String version = "00";
    final String traceId = spanContext.getTraceId();
    // parent-id is known as the span-id
    final String parentId = spanContext.getSpanId();
    final String traceFlags = spanContext.getTraceFlags().toString();

    return String.format("traceparent: %s-%s-%s-%s\r\n",
        version, traceId, parentId, traceFlags);
  }

  private String initAndGetTraceState(Span span) {
    final TraceState traceState = span.getSpanContext().getTraceState();
    final StringBuilder stringBuilder = new StringBuilder();

    traceState.forEach((k, v) -> stringBuilder.append(k).append("=").append(v));
    return String.format("tracestate: %s\r\n", stringBuilder);
  }

}
