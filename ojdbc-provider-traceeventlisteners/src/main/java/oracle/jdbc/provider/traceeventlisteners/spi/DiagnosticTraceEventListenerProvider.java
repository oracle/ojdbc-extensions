package oracle.jdbc.provider.traceeventlisteners.spi;

import oracle.jdbc.TraceEventListener;

public interface DiagnosticTraceEventListenerProvider {
  TraceEventListener getTraceEventListener(boolean enableSensitiveData);
  
}
