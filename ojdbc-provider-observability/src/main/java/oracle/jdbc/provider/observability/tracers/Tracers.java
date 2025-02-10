package oracle.jdbc.provider.observability.tracers;

/**
 * This interface defines the constants that are used to identify the 
 * {@link ObservabilityTracer}.
 */
public enum Tracers {
  /**
   * Open Telemetry tracer.
   */
  OTEL(new OTelTracer()),

  /**
   * Java Flight Recorder tracer.
   */
  JFR(new JFRTracer());

  private ObservabilityTracer tracer;

  Tracers(ObservabilityTracer tracer) {
    this.tracer = tracer;
  }

  public ObservabilityTracer getTracer() {
    return tracer;
  }
}