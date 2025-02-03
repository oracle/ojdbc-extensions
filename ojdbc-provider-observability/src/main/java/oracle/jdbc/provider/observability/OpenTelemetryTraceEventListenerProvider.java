package oracle.jdbc.provider.observability;

import java.util.Collection;
import java.util.Collections;

public class OpenTelemetryTraceEventListenerProvider extends ObservabilityTraceEventListenerProvider {

  private static final String PROVIDER_NAME = "open-telemetry-trace-event-listener-provider";

  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  @Override
  public Collection<? extends Parameter> getParameters() {
    return Collections.emptyList();
  }

}
