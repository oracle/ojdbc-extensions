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

import oracle.jdbc.TraceEventListener;

import java.sql.SQLException;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
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

import static oracle.jdbc.provider.opentelemetry.OtelSemanticConventions.*;

/**
 * <p>
 * TraceEventListener implementation that receives notifications whenever events
 * are generated in the driver and publishes these events into Open Telemetry.
 * </p>
 * <p>
 * These events include:
 * </p>
 * <ul>
 * <li>roundtrips to the database server</li>
 * <li>AC begin and success</li>
 * <li>VIP down event</li>
 * </ul>
 *
 * The system properties
 * {@value #OPEN_TELEMENTRY_TRACE_EVENT_LISTENER_ENABLED}
 * and
 * {@value #OPEN_TELEMENTRY_TRACE_EVENT_LISTENER_SENSITIVE_ENABLED}
 * can be used to enable/disable this listener and the use of sensitive data
 * by this listener.
 * The environment variable {@value #OTEL_SEMCONV_STABILITY_OPT_IN} controls which
 * semantic convention version to emit:
 * <ul>
 *  <li><b>"database"</b> - emit only the new stable Oracle Database semantic conventions</li>
 *  <li><b>"database/dup"</b> - emit both old and new conventions (for migration)</li>
 *  <li><b>empty/not set (default)</b> - emit only old experimental conventions</li>
 * </ul>
 * A MBean registered by the
 * {@link OpenTelemetryTraceEventListenerProvider} can be used to change these values
 * at runtime.
 */
public class OpenTelemetryTraceEventListener
    implements TraceEventListener, OpenTelemetryTraceEventListenerMBean {

  /**
   * Name of the property used to enable or disable this listener.
   */
  public static final String OPEN_TELEMENTRY_TRACE_EVENT_LISTENER_ENABLED = "oracle.jdbc.provider.opentelemetry.enabled";
  /**
   * Name of the property used to enable or disable sensitive data for this
   * listener.
   */
  public static final String OPEN_TELEMENTRY_TRACE_EVENT_LISTENER_SENSITIVE_ENABLED = "oracle.jdbc.provider.opentelemetry.sensitive-enabled";

  private static final String TRACE_KEY = "clientcontext.ora$opentelem$tracectx";

  /**
   * OpenTelemetry semantic convention stability opt-in environment variable.
   * Accepts a comma-separated list of values including:
   * "database" (new stable conventions only),
   * "database/dup" (both old and new conventions),
   * or empty/null (old conventions only)
   */
  public static final String OTEL_SEMCONV_STABILITY_OPT_IN = "OTEL_SEMCONV_STABILITY_OPT_IN";

  // Number of parameters expected for each execution event
  private static final Map<JdbcExecutionEvent, Integer> EXECUTION_EVENTS_PARAMETERS = new EnumMap<JdbcExecutionEvent, Integer>(
      JdbcExecutionEvent.class) {
    {
      put(JdbcExecutionEvent.AC_REPLAY_STARTED, 3);
      put(JdbcExecutionEvent.AC_REPLAY_SUCCESSFUL, 3);
      put(JdbcExecutionEvent.VIP_RETRY, 8);
    }
  };

  private static Logger logger = Logger.getLogger(OpenTelemetryTraceEventListener.class.getName());

  private Tracer tracer;

  /**
   * <p>
   * Singleton enumeration containing the TraceEventListener's configuration. Two
   * configuration parameters are available:
   * <ul>
   * <li><b>enabled</b> - enables/disables the traces,</li>
   * <li><b>sensitive data enabled</b> - enables/disables sensitive data like SQL
   * statements in the traces.</li>
   * </ul>
   * </p>
   * <p>
   * By default traces are enabled and sensitive data is disabled.
   * </p>
   */
  private enum Configuration {
    INSTANCE(true, false, "");

    private AtomicBoolean enabled;
    private AtomicBoolean sensitiveDataEnabled;
    private volatile String semconvOptIn;

    private Configuration(boolean enabled, boolean sensitiveDataEnabled, String defaultOptIn) {
      String enabledStr = System.getProperty(OPEN_TELEMENTRY_TRACE_EVENT_LISTENER_ENABLED);
      String sensitiveStr = System.getProperty(OPEN_TELEMENTRY_TRACE_EVENT_LISTENER_SENSITIVE_ENABLED);
      String optInStr = System.getenv(OTEL_SEMCONV_STABILITY_OPT_IN);

      this.enabled = new AtomicBoolean(enabledStr == null ? enabled : Boolean.parseBoolean(enabledStr));
      this.sensitiveDataEnabled = new AtomicBoolean(
          sensitiveStr == null ? sensitiveDataEnabled : Boolean.parseBoolean(sensitiveStr));

      this.semconvOptIn = optInStr == null ? defaultOptIn : optInStr;
    }

    private boolean isEnabled() {
      return enabled.get();
    }

    private void setEnabled(boolean enabled) {
      this.enabled.set(enabled);
    }

    private boolean isSensitiveDataEnabled() {
      return sensitiveDataEnabled.get();
    }

    private void setSensitiveDataEnabled(boolean enabled) {
      this.sensitiveDataEnabled.set(enabled);
    }

    private String getSemconvOptIn() {
      return semconvOptIn == null ? "" : semconvOptIn;
    }

    private void setSemconvOptIn(String optIn) {
      this.semconvOptIn = optIn == null ? "" : optIn;
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
      if (semconvOptIn == null || semconvOptIn.isEmpty()) {
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
      if (semconvOptIn == null || semconvOptIn.isEmpty()) {
        return false;
      }

      for (String value : semconvOptIn.split(",")) {
        if (targetValue.equals(value.trim())) {
          return true;
        }
      }
      return false;
    }
  }

  public OpenTelemetryTraceEventListener() {
    this(GlobalOpenTelemetry.get().getTracer(OpenTelemetryTraceEventListener.class.getName()));
  }

  public OpenTelemetryTraceEventListener(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  /**
   * Indicates whether traces will be exported by the TraceEventListener.
   */
  public boolean isEnabled() {
    return Configuration.INSTANCE.isEnabled();
  }

  @Override
  /**
   * Sets whether the TraceEventListener should export traces
   */
  public void setEnabled(boolean enabled) {
    Configuration.INSTANCE.setEnabled(enabled);
  }

  @Override
  /**
   * Indicates whether the traces should contain sensitive data.
   */
  public boolean isSensitiveDataEnabled() {
    return Configuration.INSTANCE.isSensitiveDataEnabled();
  }

  @Override
  public String getSemconvOptIn() {
    return Configuration.INSTANCE.getSemconvOptIn();
  }

  @Override
  /**
   * Sets whether the traces should contain sensitive data.
   */
  public void setSensitiveDataEnabled(boolean enabled) {
    Configuration.INSTANCE.setSensitiveDataEnabled(enabled);
  }

  @Override
  public void setSemconvOptIn(String optIn) {
    Configuration.INSTANCE.setSemconvOptIn(optIn);
  }

  @Override
  /**
   * If traces are enabled, exports traces to Open Telemetry for every round
   * trip.
   */
  public Object roundTrip(Sequence sequence, TraceContext traceContext, Object userContext) {
    if (!isEnabled())
      return null;
    if (sequence == Sequence.BEFORE) {
      // Create the Span before the round-trip.
      final Span span = initAndGetSpan(traceContext, traceContext.databaseOperation());
      try (Scope ignored = span.makeCurrent()) {
        traceContext.setClientInfo(TRACE_KEY, getTraceValue(span));
      } catch (Exception ex) {
        // Ignore exception caused by connection state.
      }
      // Return the Span instance to the driver. The driver holds this instance and
      // supplies it
      // as user context parameter on the next round-trip call.
      return span;
  } else {
    // End the Span after the round-trip.
    if (userContext instanceof Span) {
      final Span span = (Span) userContext;
      boolean emitStable = Configuration.INSTANCE.isStableConventionsEnabled();
      Boolean isErrorObj = traceContext.isCompletedExceptionally();
      boolean hasError = isErrorObj != null && isErrorObj;
      span.setStatus(hasError ? StatusCode.ERROR : StatusCode.OK);
      if (hasError && emitStable) {
        Throwable throwable = traceContext.getThrowable();
        if (throwable != null) {
          span.setAttribute(ERROR_TYPE_ATTRIBUTE, throwable.getClass().getName());
          if (throwable instanceof SQLException) {
            SQLException sqlEx = (SQLException) throwable;
            span.setAttribute(DB_RESPONSE_STATUS_CODE_ATTRIBUTE, String.format("ORA-%05d", sqlEx.getErrorCode()));
          }
        }
      }
      endSpan(span);
    }
    return null;
  }

  }

  @Override
  /**
   * If traces are enabled, exports execution event to Open Telemetry
   */
  public Object onExecutionEventReceived(JdbcExecutionEvent event, Object userContext, Object... params) {
    // Noop if not enabled or parameter count is not correct
    if (!isEnabled())
      return null;

    boolean emitStable = Configuration.INSTANCE.isStableConventionsEnabled();
    boolean emitOld = Configuration.INSTANCE.isOldConventionsEnabled();

    if (EXECUTION_EVENTS_PARAMETERS.get(event) == params.length) {
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
          if (Configuration.INSTANCE.isSensitiveDataEnabled()) {
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
          if (Configuration.INSTANCE.isSensitiveDataEnabled()) {
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
        return spanBuilder.startSpan();

      } else if (event == TraceEventListener.JdbcExecutionEvent.AC_REPLAY_STARTED
          || event == TraceEventListener.JdbcExecutionEvent.AC_REPLAY_SUCCESSFUL) {
        SpanBuilder spanBuilder = tracer.spanBuilder(event.getDescription());
        SQLException sqlEx = (SQLException) params[1];
        // Emit NEW stable conventions
        if (emitStable) {
          spanBuilder.setAttribute(DB_SYSTEM_ATTRIBUTE, DB_SYSTEM_VALUE_ORACLE);
          if (params[0] != null) {
            spanBuilder.setAttribute(ERROR_TYPE_ATTRIBUTE, params[0].toString());
          }

          if (sqlEx != null) {
            spanBuilder.setAttribute(DB_RESPONSE_STATUS_CODE_ATTRIBUTE, String.format("ORA-%05d",
                    sqlEx.getErrorCode()));
          }

          if (params[2] != null) {
            spanBuilder.setAttribute(DB_OPERATION_BATCH_SIZE_ATTRIBUTE, params[2].toString());
          }
        }
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

  @Override
  public boolean isDesiredEvent(JdbcExecutionEvent event) {
    // Accept all events
    return true;
  }

  private Span initAndGetSpan(TraceContext traceContext, String spanName) {
    /*
     * If this is in the context of current span, the following becomes a nested or
     * child span to the current span. I.e. the current span in context becomes
     * parent to this child span.
     */
    SpanBuilder spanBuilder = tracer.spanBuilder(spanName);
    boolean emitStable = Configuration.INSTANCE.isStableConventionsEnabled();
    boolean emitOld = Configuration.INSTANCE.isOldConventionsEnabled();

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
      if (this.isSensitiveDataEnabled()) {
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
        .setAttribute(LEGACY_DATABASE_USER_ATTRIBUTE, traceContext.user())
        .setAttribute(LEGACY_SQL_ID_ATTRIBUTE, traceContext.getSqlId())
        .setAttribute(LEGACY_DATABASE_TENANT_ATTRIBUTE, traceContext.tenant());

      // Add sensitive information (URL and SQL) if it is enabled
      if (this.isSensitiveDataEnabled()) {
        logger.log(Level.FINEST, "Sensitive information on");
        spanBuilder.setAttribute(LEGACY_ORIGINAL_SQL_TEXT_ATTRIBUTE, traceContext.originalSqlText())
          .setAttribute(LEGACY_ACTUAL_SQL_TEXT_ATTRIBUTE, traceContext.actualSqlText());
      }
    }

    // Indicates that the span covers server-side handling of an RPC or other remote
    // request.
    SpanKind spanKind = emitStable ? SpanKind.CLIENT : SpanKind.SERVER;
    return spanBuilder.setSpanKind(spanKind).startSpan();  }



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

  private void endSpan(Span span) {
    span.end(Instant.now());
  }

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
