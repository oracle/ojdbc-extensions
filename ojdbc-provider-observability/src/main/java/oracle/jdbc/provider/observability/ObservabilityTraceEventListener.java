package oracle.jdbc.provider.observability;

import oracle.jdbc.TraceEventListener;
import oracle.jdbc.provider.observability.configuration.ObservabilityConfiguration;
import oracle.jdbc.provider.observability.tracers.TracerType;

/**
 * <p>
 * TraceEventListener implementaiton that receives notifications whenever events
 * are generated in the driver and publishes these events different tracers 
 * depending on the configuraiton.
 * </p>
 * <p>
 * These events include:
 * </p>
 * <ul>
 * <li>roundtrips to the database server</li>
 * <li>AC begin and success</li>
 * <li>VIP down event</li>
 * </ul>
 * <p>
 * The available tracers are defined in the enumeration {@link TracerType}, and
 * can be enabled using the method 
 * {@link ObservabilityConfiguration#setEnabledTracers(String)}. The method
 * {@link ObservabilityConfiguration#setSensitiveDataEnabled(boolean)} allows
 * to enabled/disable exporting sensitive data to the tracers.
 * </p>
 */
public class ObservabilityTraceEventListener implements TraceEventListener {

  /**
   * Private constructor.
   */
  private ObservabilityTraceEventListener() { }

  /**
   * Singleton instance.
   */
  private static final ObservabilityTraceEventListener INSTANCE = 
      new ObservabilityTraceEventListener();

  /**
   * Returns the singleton instance of {@link ObservabilityTraceEventListener}.
   * @return the singleton instance of {@link ObservabilityTraceEventListener}.
   */
  public static final ObservabilityTraceEventListener getInstance() {
    return INSTANCE;
  }

  @Override
  public Object roundTrip(Sequence sequence, TraceContext traceContext, Object userContext) {
    if (!ObservabilityConfiguration.getInstance().getEnabled()) { return null;}
    Object[] currentUserContext = getCurrentUserContext(userContext);
    for (TracerType tracer : ObservabilityConfiguration.getInstance().getEnabledTracersSet()) {
      Object newUserContext = tracer.getTracer().traceRoundtrip(sequence, traceContext, currentUserContext[tracer.ordinal()]);
      currentUserContext[tracer.ordinal()] = newUserContext;
    }
    return currentUserContext;
  }


  @Override
  public Object onExecutionEventReceived(JdbcExecutionEvent event, Object userContext, Object... params) {
    if (!ObservabilityConfiguration.getInstance().getEnabled()) { return null;}
    Object[] currentUserContext = getCurrentUserContext(userContext);
    for (TracerType tracer : ObservabilityConfiguration.getInstance().getEnabledTracersSet()) {
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
      currentUserContext = new Object[TracerType.values().length];
    }
    return currentUserContext;
  }

}
