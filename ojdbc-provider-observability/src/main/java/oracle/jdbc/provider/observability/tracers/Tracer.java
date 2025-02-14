package oracle.jdbc.provider.observability.tracers;

/**
 * This interface defines the constants that are used to identify the 
 * {@link ObservabilityTracer}.
 */
public enum Tracer {
  /**
   * Open Telemetry tracer.
   */
  OTEL(new OTelTracer()),

  /**
   * Java Flight Recorder tracer.
   */
  JFR(new JFRTracer());

  private ObservabilityTracer tracer;

  Tracer(ObservabilityTracer tracer) {
    this.tracer = tracer;
  }

  /**
   * Returns the {@link ObservabilityTracer} for this {@link Tracer}.
   * @return the {@link ObservabilityTracer}.
   */
  public ObservabilityTracer getTracer() {
    return tracer;
  }
}