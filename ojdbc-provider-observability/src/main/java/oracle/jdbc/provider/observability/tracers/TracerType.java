package oracle.jdbc.provider.observability.tracers;

import oracle.jdbc.provider.observability.tracers.jfr.JFRTracer;
import oracle.jdbc.provider.observability.tracers.otel.OTelTracer;

/**
 * This interface defines the constants that are used to identify the 
 * {@link ObservabilityTracer}.
 */
public enum TracerType {
  /**
   * Open Telemetry tracer.
   */
  OTEL(new OTelTracer()),

  /**
   * Java Flight Recorder tracer.
   */
  JFR(new JFRTracer());

  private ObservabilityTracer tracer;

  TracerType(ObservabilityTracer tracer) {
    this.tracer = tracer;
  }

  /**
   * Returns the {@link ObservabilityTracer} for this {@link TracerType}.
   * @return the {@link ObservabilityTracer}.
   */
  public ObservabilityTracer getTracer() {
    return tracer;
  }
}