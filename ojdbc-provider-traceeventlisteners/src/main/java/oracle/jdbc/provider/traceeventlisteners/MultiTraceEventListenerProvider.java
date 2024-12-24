package oracle.jdbc.provider.traceeventlisteners;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import javax.management.ObjectName;

import oracle.jdbc.TraceEventListener;
import oracle.jdbc.provider.traceeventlisteners.spi.AbstractDiagnosticTraceEventListenerProvider;
import oracle.jdbc.provider.traceeventlisteners.spi.DiagnosticTraceEventListenerProvider;

public class MultiTraceEventListenerProvider extends AbstractDiagnosticTraceEventListenerProvider  {

  private static final String PROVIDER_NAME = "multi-trace_event-listener-provider";

  @Override
  protected String getProviderName() {
    return PROVIDER_NAME;
  }

  @Override
  protected TraceEventListener getTraceEventListener(boolean enabled, boolean enableSensitiveData) {
    List<TraceEventListener> traceEventListenersList = new ArrayList<>();
    ServiceLoader<DiagnosticTraceEventListenerProvider> loader = ServiceLoader.load(DiagnosticTraceEventListenerProvider.class);
    loader.forEach(provider -> 
        traceEventListenersList.add(provider.getTraceEventListener(enableSensitiveData))
    );
    return new MultiTraceEventListener(traceEventListenersList);
  }

}
