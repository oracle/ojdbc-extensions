package oracle.jdbc.provider.observability.tracers;

import jdk.jfr.Event;
import oracle.jdbc.TraceEventListener.JdbcExecutionEvent;
import oracle.jdbc.TraceEventListener.Sequence;
import oracle.jdbc.TraceEventListener.TraceContext;
import oracle.jdbc.provider.observability.tracers.JFREventFactory.RoundTripEvent;

/**
 * {@link ObservabilityTracer} for tracing Java Flight Recorder events.
 */
public class JFRTracer implements ObservabilityTracer{

  /**
   * Creates a new instance.
   */
  public JFRTracer() {}

  @Override
  public Object traceRoundtrip(Sequence sequence, TraceContext traceContext, Object userContext) {
    if (sequence.equals(Sequence.BEFORE)) {
      RoundTripEvent event = JFREventFactory.createJFREvent(traceContext);
      event.begin();
      return event;
    } else {
      if (userContext != null) {
        RoundTripEvent event = (RoundTripEvent) userContext;
        event.setValues(traceContext);
        event.end();
        event.commit();
      }
    }
    return null;
  }

  @Override
  public Object traceExecutionEvent(JdbcExecutionEvent event, Object userContext, Object... params) {
    Event executionEvent = JFREventFactory.createExecutionEvent(event, params);
    executionEvent.begin();
    executionEvent.commit();
    return null;
  }

}
