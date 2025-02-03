package oracle.jdbc.provider.observability;

import oracle.jdbc.TraceEventListener;
import oracle.jdbc.provider.observability.configuration.ObservabilityConfiguration;
import oracle.jdbc.provider.observability.tracers.JFRTracer;
import oracle.jdbc.provider.observability.tracers.OTelTracer;
import oracle.jdbc.provider.observability.tracers.ObservabilityTracer;

public class ObservabilityTraceEventListener implements TraceEventListener {


  public enum Tracers {
    OTEL(new OTelTracer()),
    JFR(new JFRTracer());

    private ObservabilityTracer tracer;

    Tracers(ObservabilityTracer tracer) {
      this.tracer = tracer;
    }

    public ObservabilityTracer getTracer() {
      return tracer;
    }
  }

  @Override
  public Object roundTrip(Sequence sequence, TraceContext traceContext, Object userContext) {
    Object[] currentUserContext = getCurrentUserContext(userContext);
    for (Tracers tracer : ObservabilityConfiguration.getInstance().getEnabledTracersSet()) {
      Object newUserContext = tracer.getTracer().traceRoundtrip(sequence, traceContext, currentUserContext[tracer.ordinal()]);
      currentUserContext[tracer.ordinal()] = newUserContext;
    }
    return currentUserContext;
  }


  @Override
  public Object onExecutionEventReceived(JdbcExecutionEvent event, Object userContext, Object... params) {
    Object[] currentUserContext = getCurrentUserContext(userContext);
    for (Tracers tracer : ObservabilityConfiguration.getInstance().getEnabledTracersSet()) {
      Object newUserContext = tracer.getTracer().traceExecutionEvent(event, currentUserContext[tracer.ordinal()], params);
      currentUserContext[tracer.ordinal()] = newUserContext;
    }
    return currentUserContext;
  }

  @Override
  public boolean isDesiredEvent(JdbcExecutionEvent event) {
    // Accept all events
    return true;
  }

  @SuppressWarnings("unchecked")
  private Object[] getCurrentUserContext(Object userContext) {
    Object[] currentUserContext;
    if (userContext != null && (userContext instanceof Object[])) {
      currentUserContext = (Object[]) userContext;
    } else {
      currentUserContext = new Object[Tracers.values().length];
    }
    return currentUserContext;
  }

}
