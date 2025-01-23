package oracle.jdbc.provider.observability.tracers;

import oracle.jdbc.TraceEventListener.JdbcExecutionEvent;
import oracle.jdbc.TraceEventListener.Sequence;
import oracle.jdbc.TraceEventListener.TraceContext;

public interface ObservabilityTracer {

  Object traceRoudtrip(Sequence sequence, TraceContext traceContext, Object userContext);

  Object traceExecutionEvent(JdbcExecutionEvent event, Object userContext, Object... params);

}
