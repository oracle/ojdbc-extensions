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

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

/**
 * <p>
 * TraceEventListener implementaiton that receives notifications whenever events
 * are generated in the driver and publishes these events into Open Telemetry.
 * </p>
 * <p>
 * These events include:
 * <ul>
 * <li>roundtrips to the database server</li>
 * <li>AC begin and sucess</li>
 * <li>VIP down event</li>
 * </ul>
 * </p>
 */
public class OpenTelemetryTraceEventListener
    implements TraceEventListener, OpenTelemetryTraceEventListenerMBean {

  private static final String TRACE_KEY = "clientcontext.ora$opentelem$tracectx";

  // Number of parameters expected for each execution event
  private static final Map<JdbcExecutionEvent, Integer> EXECUTION_EVENTS_PARAMETERS = new EnumMap<JdbcExecutionEvent, Integer>(
      JdbcExecutionEvent.class) {
    {
      put(JdbcExecutionEvent.AC_REPLAY_STARTED, 3);
      put(JdbcExecutionEvent.AC_REPLAY_SUCCESSFUL, 3);
      put(JdbcExecutionEvent.VIP_RETRY, 8);
    }
  };

  private Tracer tracer;

  /**
   * <p>
   * Singleton enumeration containing the TraceEventListener's configuration.
   * Two configuration parameters are available:
   * (i) <b>enabled</b> - enables/disables the traces, and
   * (ii) <b>sensitive data enabled</b> - enables/disables sensitive data like SQL
   * statements in the traces.
   * </p>
   * <p>
   * By default traces are enabled and sensitive data is disabled.
   * </p>
   */
  private enum Configuration {
    INSTANCE(true, false);

    private AtomicBoolean enabled;
    private AtomicBoolean sensitiveDataEnabled;

    private Configuration(boolean enabled, boolean sensitiveDataEnabled) {
      this.enabled = new AtomicBoolean(enabled);
      this.sensitiveDataEnabled = new AtomicBoolean(sensitiveDataEnabled);
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
  /**
   * Sets whether the traces should contain sensitive data.
   */
  public void setSensitiveDataEnabled(boolean enabled) {
    Configuration.INSTANCE.setSensitiveDataEnabled(enabled);
  }

  @Override
  /**
   * If traces are enabled, exports traces to Open Telemetry for every round
   * trip.
   */
  public Object roundTrip(Sequence sequence, TraceContext traceContext, Object userContext) {
    if (!Configuration.INSTANCE.isEnabled())
      return null;
    if (sequence == Sequence.BEFORE) {
      // Create the Span before the round-trip.
      final Span span = initAndGetSpan(traceContext, traceContext.databaseOperation());
      try (Scope ignored = span.makeCurrent()) {
        traceContext.setClientInfo(TRACE_KEY, getTraceValue(span));
      } catch (Exception ex) {
        System.out.println(ex.getMessage());
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
    if (!Configuration.INSTANCE.isEnabled() || (EXECUTION_EVENTS_PARAMETERS.get(event) != params.length))
      return null;
    SpanBuilder spanBuilder = tracer
        .spanBuilder(event.getDescription());
    if (event == TraceEventListener.JdbcExecutionEvent.VIP_RETRY && params.length == 8) {
      spanBuilder.setAttribute("Error message", params[0].toString())
          .setAttribute("VIP Address", params[7].toString());
      // Add sensitive information (URL and SQL) if it is enabled
      if (Configuration.INSTANCE.isSensitiveDataEnabled()) {
        spanBuilder.setAttribute("Protocol", params[1].toString())
            .setAttribute("Host", params[2].toString())
            .setAttribute("Port", params[3].toString())
            .setAttribute("Service name", params[4].toString())
            .setAttribute("SID", params[5].toString())
            .setAttribute("Connection data", params[6].toString());
      }

    } else {
      if ((event == TraceEventListener.JdbcExecutionEvent.AC_REPLAY_STARTED
          || event == TraceEventListener.JdbcExecutionEvent.AC_REPLAY_SUCCESSFUL) && params.length == 3) {
        spanBuilder.setAttribute("Error Message", params[0].toString())
            .setAttribute("Error code", ((SQLException) params[1]).getErrorCode())
            .setAttribute("SQL state", ((SQLException) params[1]).getSQLState())
            .setAttribute("Current replay retry count", params[2].toString());
      } else {
        for (int i = 0; i < params.length; i++) {
          spanBuilder.setAttribute("Parameter " + i, params[i].toString());
        }
      }
    }
    return spanBuilder.startSpan();
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
    SpanBuilder spanBuilder = tracer
        .spanBuilder(spanName);
    spanBuilder
        .setAttribute("thread.id", Thread.currentThread().getId())
        .setAttribute("thread.name", Thread.currentThread().getName())
        // Set the relevant attributes only. Here all attributes are set for demo
        // purpose.
        .setAttribute("Connection ID", traceContext.getConnectionId());
    spanBuilder
        .setAttribute("Database Operation", traceContext.databaseOperation())
        .setAttribute("Database User", traceContext.user())
        .setAttribute("Database Tenant", traceContext.tenant())
        .setAttribute("SQL ID", traceContext.getSqlId());

    // Add sensitive information (URL and SQL) if it is enabled
    if (this.isSensitiveDataEnabled()) {
      spanBuilder.setAttribute("Original SQL Text", traceContext.originalSqlText())
          .setAttribute("Actual SQL Text", traceContext.actualSqlText());
    }

    // Indicates that the span covers server-side handling of an RPC or other remote
    // request.
    return spanBuilder.setSpanKind(SpanKind.SERVER).startSpan();

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
