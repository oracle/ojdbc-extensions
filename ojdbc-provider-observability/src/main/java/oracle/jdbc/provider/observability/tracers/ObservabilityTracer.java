package oracle.jdbc.provider.observability.tracers;

import oracle.jdbc.TraceEventListener.JdbcExecutionEvent;
import oracle.jdbc.TraceEventListener.Sequence;
import oracle.jdbc.TraceEventListener.TraceContext;
import oracle.jdbc.provider.observability.ObservabilityTraceEventListener;

/**
 * This interface must be implemented by all Observability tracers.
 */
public interface ObservabilityTracer {

  /**
   * Called by {@link ObservabilityTraceEventListener} when a round trip event
   * is received.
   * 
   * @param sequence BEFORE if before the round trip, AFTER if after the round 
   * trip
   * @param traceContext Information about the round trip. Valid only during the
   * call
   * @param userContext Result of previous call on this Connection or null if no
   * previous call or if observability was disabled since the previous call.
   * @return a user context object that is passed to the next call on this 
   * Connection. May be null.
   */
  Object traceRoundtrip(Sequence sequence, TraceContext traceContext, Object userContext);

  /**
   * Called by {@link ObservabilityTraceEventListener} when an execution event
   * is received.
   * 
   * @param event the event.
   * @param userContext the result of the previous call or null if no previous 
   * call has been made.
   * @param params event specific parameters.
   * @return a user context object that is passed to the next call for the same 
   * type of event. May be null.
   */
  Object traceExecutionEvent(JdbcExecutionEvent event, Object userContext, Object... params);

}
