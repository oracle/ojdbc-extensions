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

import java.sql.SQLException;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import oracle.jdbc.provider.observability.ObservabilityConfiguration;
import oracle.jdbc.provider.observability.tracers.ObservabilityTracer;

import static oracle.jdbc.provider.observability.tracers.otel.OtelSemanticConventions.*;

/**
 * Open Telemetry tracer. Exports round trip event and execution events to
 * Open Telemetry. with support for both stable and experimental semantic
 * conventions.
 * <p>
 * <b>Semantic Convention Modes:</b>
 * </p>
 * <ul>
 *   <li><b>Legacy mode (default)</b>: Emits experimental conventions only when
 *       {@code OTEL_SEMCONV_STABILITY_OPT_IN} is not set or empty</li>
 *   <li><b>Stable mode</b>: Emits stable conventions only when
 *       {@code OTEL_SEMCONV_STABILITY_OPT_IN=database}</li>
 *   <li><b>Dual mode</b>: Emits both old and new conventions when
 *       {@code OTEL_SEMCONV_STABILITY_OPT_IN=database/dup}</li>
 * </ul>
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
      if (userContext instanceof Span) {
        final Span span = (Span) userContext;
        Boolean isErrorObj = traceContext.isCompletedExceptionally();
        boolean hasError = isErrorObj != null && isErrorObj;
        span.setStatus(hasError ? StatusCode.ERROR : StatusCode.OK);
        if (hasError && isStableConventionsEnabled()) {
          Throwable throwable = traceContext.getThrowable();
          if (throwable != null) {
            span.setAttribute(ERROR_TYPE_ATTRIBUTE, throwable.getClass().getName());
            if (throwable instanceof SQLException) {
              SQLException sqlEx = (SQLException) throwable;
              span.setAttribute(DB_RESPONSE_STATUS_CODE_ATTRIBUTE, String.format("ORA-%05d", sqlEx.getErrorCode()));
            }
          }
        }
        span.end(Instant.now());
      } else {
        logger.log(Level.WARNING, "Unknown or null user context received from the driver on " +
                "database operation: " + traceContext.databaseOperation());
      }
      return null;
    }
  }

  @Override
  public Object traceExecutionEvent(JdbcExecutionEvent event, Object userContext, Object... params) {
    if (EXECUTION_EVENTS_PARAMETERS.get(event) == params.length) {
      Tracer tracer = GlobalOpenTelemetry.get().getTracer(OTelTracer.class.getName());
      boolean emitStable = isStableConventionsEnabled();
      boolean emitOld = isOldConventionsEnabled();
      if (event == TraceEventListener.JdbcExecutionEvent.VIP_RETRY) {
        SpanBuilder spanBuilder = tracer.spanBuilder(event.getDescription());

        // Emit NEW stable conventions
        if (emitStable) {
          spanBuilder.setAttribute(DB_SYSTEM_ATTRIBUTE, DB_SYSTEM_VALUE_ORACLE);

          if (params[0] != null) {
            spanBuilder.setAttribute(ERROR_TYPE_ATTRIBUTE, params[0].toString());
          }
          if (params[7] != null) {
            spanBuilder.setAttribute(SERVER_ADDRESS_ATTRIBUTE, params[7].toString());
          }

          // Add sensitive information (URL and SQL) if enabled
          if (configuration.getSensitiveDataEnabled()) {
            logger.log(Level.FINEST, "Sensitive information on");
            if (params[3] != null) {
              spanBuilder.setAttribute(SERVER_PORT_ATTRIBUTE, params[3].toString());
            }
            if (params[1] != null) {
              spanBuilder.setAttribute(ORACLE_VIP_PROTOCOL_ATTRIBUTE, params[1].toString());
            }
            if (params[2] != null) {
              spanBuilder.setAttribute(ORACLE_VIP_FAILED_HOST_ATTRIBUTE, params[2].toString());
            }
            if (params[4] != null) {
              spanBuilder.setAttribute(ORACLE_VIP_SERVICE_NAME_ATTRIBUTE, params[4].toString());
            }
            if (params[5] != null) {
              spanBuilder.setAttribute(ORACLE_VIP_SID_ATTRIBUTE, params[5].toString());
            }
            if (params[6] != null) {
              spanBuilder.setAttribute(ORACLE_VIP_CONNECTION_DESCRIPTOR_ATTRIBUTE, params[6].toString());
            }
          }
        }
        if (emitOld) {
          spanBuilder.setAttribute(LEGACY_ERROR_MESSAGE_ATTRIBUTE, params[0].toString());

          if (params[7] != null) {
            spanBuilder.setAttribute(LEGACY_VIP_ADDRESS_ATTRIBUTE, params[7].toString());
          }

          // Add sensitive information (URL and connection details) if enabled
          if (configuration.getSensitiveDataEnabled()) {
            logger.log(Level.FINEST, "Sensitive information on");
            if (params[1] != null) {
              spanBuilder.setAttribute(LEGACY_PROTOCOL_ATTRIBUTE, params[1].toString());
            }
            if (params[2] != null) {
              spanBuilder.setAttribute(LEGACY_HOST_ATTRIBUTE, params[2].toString());
            }
            if (params[3] != null) {
              spanBuilder.setAttribute(LEGACY_PORT_ATTRIBUTE, params[3].toString());
            }
            if (params[4] != null) {
              spanBuilder.setAttribute(LEGACY_SERVICE_NAME_ATTRIBUTE, params[4].toString());
            }
            if (params[5] != null) {
              spanBuilder.setAttribute(LEGACY_SID_ATTRIBUTE, params[5].toString());
            }
            if (params[6] != null) {
              spanBuilder.setAttribute(LEGACY_CONNECTION_DATA_ATTRIBUTE, params[6].toString());
            }
          }
        }
        // start and end span.
        spanBuilder.startSpan().end();
      } else if (event == TraceEventListener.JdbcExecutionEvent.AC_REPLAY_STARTED
          || event == TraceEventListener.JdbcExecutionEvent.AC_REPLAY_SUCCESSFUL) {
        SpanBuilder spanBuilder = tracer.spanBuilder(event.getDescription());
        SQLException sqlEx = (SQLException) params[1];
        // Emit stable conventions
        if (emitStable) {
          spanBuilder.setAttribute(DB_SYSTEM_ATTRIBUTE, DB_SYSTEM_VALUE_ORACLE);
          if (params[0] != null) {
            spanBuilder.setAttribute(ERROR_TYPE_ATTRIBUTE, params[0].toString());
          }
          if (sqlEx != null) {
            spanBuilder.setAttribute(DB_RESPONSE_STATUS_CODE_ATTRIBUTE,
              String.format("ORA-%05d", sqlEx.getErrorCode()));
          }
          if (params[2] != null) {
            try {
              spanBuilder.setAttribute(DB_OPERATION_BATCH_SIZE_ATTRIBUTE,
                Integer.parseInt(params[2].toString()));
            } catch (NumberFormatException e) {
              logger.log(Level.FINE, "Could not parse retry count: " + params[2]);
            }
          }
        }

        // Emit old conventions
        if (emitOld) {
          if (params[0] != null) {
            spanBuilder.setAttribute(LEGACY_ERROR_MESSAGE_CAPITAL_ATTRIBUTE, params[0].toString());
          }
          if (sqlEx != null) {
            spanBuilder.setAttribute(LEGACY_ERROR_CODE_ATTRIBUTE, sqlEx.getErrorCode());
            spanBuilder.setAttribute(LEGACY_SQL_STATE_ATTRIBUTE, sqlEx.getSQLState());
          }
          if (params[2] != null) {
            spanBuilder.setAttribute(LEGACY_REPLAY_RETRY_COUNT_ATTRIBUTE, params[2].toString());
          }
        }
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
   * Creates an OpenTelemetry span with attributes based on the semantic convention mode.
   * <p>
   * Emits span attributes according to the configured mode:
   * </p>
   * <ul>
   *   <li><b>Stable conventions</b></li>
   *   <li><b>Legacy conventions</b></li>
   *   <li><b>Dual mode</b></li>
   * </ul>
   * <p>
   * Sensitive data (user, SQL text, row counts) is only included when
   * {@link ObservabilityConfiguration#getSensitiveDataEnabled()} returns {@code true}.
   * </p>
   * <p>
   * The span becomes a nested child if called within the context of an existing span.
   * </p>
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
    SpanBuilder spanBuilder = tracer.spanBuilder(spanName);

    boolean emitStable = isStableConventionsEnabled();
    boolean emitOld = isOldConventionsEnabled();

    // Thread attributes are common to both old and new conventions
    spanBuilder
      .setAttribute(THREAD_ID_ATTRIBUTE, Thread.currentThread().getId())
      .setAttribute(THREAD_NAME_ATTRIBUTE, Thread.currentThread().getName());

    // Emit NEW stable semantic conventions
    if (emitStable) {
      spanBuilder.setAttribute(DB_SYSTEM_ATTRIBUTE, DB_SYSTEM_VALUE_ORACLE);

      String namespace = buildDbNamespace(traceContext);
      if (namespace != null && !namespace.isEmpty()) {
        spanBuilder.setAttribute(DB_NAMESPACE_ATTRIBUTE, namespace);
      }

      if (traceContext.getServerAddress() != null && !traceContext.getServerAddress().isEmpty()) {
        spanBuilder.setAttribute(SERVER_ADDRESS_ATTRIBUTE, traceContext.getServerAddress());
      }

      if (traceContext.databaseOperation() != null && !traceContext.databaseOperation().isEmpty()) {
        spanBuilder.setAttribute(DB_OPERATION_NAME_ATTRIBUTE, traceContext.databaseOperation());
      }

      if (traceContext.getSqlId() != null && !traceContext.getSqlId().isEmpty()) {
        spanBuilder.setAttribute(ORACLE_SQL_ID_ATTRIBUTE, traceContext.getSqlId());
      }

      if (traceContext.getSessionID() != null && !traceContext.getSessionID().isEmpty()) {
        spanBuilder.setAttribute(ORACLE_SESSION_ID_ATTRIBUTE, traceContext.getSessionID());
      }

      if (traceContext.getServerPID() != null && !traceContext.getServerPID().isEmpty()) {
        spanBuilder.setAttribute(ORACLE_SERVER_PID_ATTRIBUTE, traceContext.getServerPID());
      }

      if (traceContext.getShardName() != null && !traceContext.getShardName().isEmpty()) {
        spanBuilder.setAttribute(ORACLE_SHARD_NAME_ATTRIBUTE, traceContext.getShardName());
      }

      int serverPort = traceContext.getServerPort();
      if (serverPort > 0 && serverPort != 1521) { // 1521 is default Oracle port
        spanBuilder.setAttribute(SERVER_PORT_ATTRIBUTE, serverPort);
      }

      if (traceContext.getSqlType() != null && !traceContext.getSqlType().isEmpty()) {
        spanBuilder.setAttribute(DB_QUERY_SUMMARY_ATTRIBUTE, traceContext.getSqlType());
      }

      // Add sensitive information (URL and SQL) if it is enabled
      if (configuration.getSensitiveDataEnabled()) {
        logger.log(Level.FINEST, "Sensitive information on");
        if (traceContext.user() != null && !traceContext.user().isEmpty()) {
          spanBuilder.setAttribute(DB_USER_ATTRIBUTE, traceContext.user());
        }
        if (traceContext.actualSqlText() != null) {
          spanBuilder.setAttribute(DB_QUERY_TEXT_ATTRIBUTE, traceContext.actualSqlText());
        }
        long numRows = traceContext.getNumRows();
        if (numRows > 0) {
          spanBuilder.setAttribute(DB_RESPONSE_RETURNED_ROWS_ATTRIBUTE, numRows);
        }
      }
    }

    // Emit old conventions
    if (emitOld) {
      spanBuilder
        .setAttribute(LEGACY_CONNECTION_ID_ATTRIBUTE, traceContext.getConnectionId())
        .setAttribute(LEGACY_DATABASE_OPERATION_ATTRIBUTE, traceContext.databaseOperation())
        .setAttribute(LEGACY_SQL_ID_ATTRIBUTE, traceContext.getSqlId())
        .setAttribute(LEGACY_DATABASE_TENANT_ATTRIBUTE, traceContext.tenant());

      // Add sensitive information (URL and SQL) if it is enabled
      if (configuration.getSensitiveDataEnabled()) {
        logger.log(Level.FINEST, "Sensitive information on");
        spanBuilder.setAttribute(LEGACY_ORIGINAL_SQL_TEXT_ATTRIBUTE, traceContext.originalSqlText())
          .setAttribute(LEGACY_ACTUAL_SQL_TEXT_ATTRIBUTE, traceContext.actualSqlText())
          .setAttribute(LEGACY_DATABASE_USER_ATTRIBUTE, traceContext.user());
      }
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
      // Ignore exception
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
   * Builds the db.namespace attribute according to Oracle Database
   * semantic conventions.
   * Format: {instance_name}|{database_name}|{service_name}
   * Missing components and their separators are omitted.
   */
  private String buildDbNamespace(TraceContext traceContext) {
    StringBuilder ns = new StringBuilder();

    if (traceContext.getInstanceName() != null && !traceContext.getInstanceName().isEmpty()) {
      ns.append(traceContext.getInstanceName());
    }

    if (traceContext.getDatabaseName() != null && !traceContext.getDatabaseName().isEmpty()) {
      if (ns.length() > 0) ns.append('|');
      ns.append(traceContext.getDatabaseName());
    }

    if (traceContext.getServiceName() != null && !traceContext.getServiceName().isEmpty()) {
      if (ns.length() > 0) ns.append('|');
      ns.append(traceContext.getServiceName());
    }

    return ns.length() > 0 ? ns.toString() : null;
  }

  /**
   * Returns true if stable database semantic conventions should be emitted.
   * This checks if OTEL_SEMCONV_STABILITY_OPT_IN contains "database" or "database/dup"
   * in its comma-separated list of values.
   */
  private boolean isStableConventionsEnabled() {
    return hasDatabaseValue("database") || hasDatabaseValue("database/dup");
  }

  /**
   * Returns true if old conventions should be emitted.
   * This is true when:
   * - OTEL_SEMCONV_STABILITY_OPT_IN is empty/null (default behavior), OR
   * - OTEL_SEMCONV_STABILITY_OPT_IN contains "database/dup" (dual mode)
   */
  private boolean isOldConventionsEnabled() {
    if (configuration.getSemconvOptIn() == null || configuration.getSemconvOptIn().isEmpty()) {
      return true;
    }
    return hasDatabaseValue("database/dup");
  }

  /**
   * Helper method to check if a specific database value is present in the
   * comma-separated OTEL_SEMCONV_STABILITY_OPT_IN list.
   *
   * @param targetValue the value to search for (e.g., "database" or "database/dup")
   * @return true if the value is found in the comma-separated list
   */
  private boolean hasDatabaseValue(String targetValue) {
    if (configuration.getSemconvOptIn() == null || configuration.getSemconvOptIn().isEmpty()) {
      return false;
    }

    for (String value : configuration.getSemconvOptIn().split(",")) {
      if (targetValue.equals(value.trim())) {
        return true;
      }
    }
    return false;
  }

}
