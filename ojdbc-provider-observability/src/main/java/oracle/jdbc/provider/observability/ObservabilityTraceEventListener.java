package oracle.jdbc.provider.observability;

import java.util.EnumMap;

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
    EnumMap<Tracers, Object> currentUserContext = getCurrentUserContext(userContext);
    for (Tracers tracer : ObservabilityConfiguration.getInstance().getEnabledTracersSet()) {
      Object newUserContext = tracer.getTracer().traceRoudtrip(sequence, traceContext, currentUserContext.get(tracer));
      currentUserContext.put(tracer, newUserContext);
    }
    return currentUserContext;
  }


  @Override
  public Object onExecutionEventReceived(JdbcExecutionEvent event, Object userContext, Object... params) {
    EnumMap<Tracers, Object> currentUserContext = getCurrentUserContext(userContext);
    for (Tracers tracer : ObservabilityConfiguration.getInstance().getEnabledTracersSet()) {
      Object newUserContext = tracer.getTracer().traceExecutionEvent(event, currentUserContext.get(tracer), params);
      currentUserContext.put(tracer, newUserContext);
    }
    return currentUserContext;
  }

  @Override
  public boolean isDesiredEvent(JdbcExecutionEvent event) {
    // Accept all events
    return true;
  }

  @SuppressWarnings("unchecked")
  private EnumMap<Tracers, Object> getCurrentUserContext(Object userContext) {
    EnumMap<Tracers, Object> currentUserContext;
    if (userContext != null && (userContext instanceof EnumMap<?,?>)) {
      currentUserContext = (EnumMap<Tracers, Object>) userContext;
    } else {
      currentUserContext = new EnumMap<Tracers, Object>(Tracers.class);
    }
    return currentUserContext;
  }

}
