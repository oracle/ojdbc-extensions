package oracle.jdbc.provider.observability.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import oracle.jdbc.provider.observability.ObservabilityTraceEventListener;

public class ObservabilityConfiguration implements ObservabilityConfigurationMBean {

  private static final ReentrantLock observabilityConfiguraitonLock = new ReentrantLock();

  private static final ObservabilityConfiguration INSTANCE;

  static {
    INSTANCE = new ObservabilityConfiguration();
    //INSTANCE.setSensitiveDataEnabled(true);
    INSTANCE.setEnabledTracers(System.getProperty("oracle.jdbc.provider.observability.tracer", "OTEL,JFR"));
  }

  private ObservabilityConfiguration() {  }

  private boolean sensitiveDataEnabled;
  private String tracers;

  private List<ObservabilityTraceEventListener.Tracers> enabledTracers = new ArrayList<>();

  @Override
  public String getEnabledTracers() {
    try {
      observabilityConfiguraitonLock.lock();
      return tracers;
    } finally {
      observabilityConfiguraitonLock.unlock();
    }
  }

  @Override
  public void setEnabledTracers(String tracers) {
    try {
      observabilityConfiguraitonLock.lock();
      enabledTracers.clear();
      String[] items = tracers.split(",");
      for (String item : items) {
        if (item != null) {
          enabledTracers.add(ObservabilityTraceEventListener.Tracers.valueOf(item.toUpperCase()));
        }
      }
      this.tracers = enabledTracers.stream().map((item) -> item.toString()).collect(Collectors.joining(","));
    } finally {
      observabilityConfiguraitonLock.unlock();
    }

  }

  @Override
  public boolean getSensitiveDataEnabled() {
    return sensitiveDataEnabled;
  }

  @Override
  public void setSensitiveDataEnabled(boolean sensitiveDataEnabled) {
    this.sensitiveDataEnabled = sensitiveDataEnabled;
  }

  public static ObservabilityConfiguration getInstance() {
    return INSTANCE;
  }

  public List<ObservabilityTraceEventListener.Tracers> getEnabledTracersSet() {
    return enabledTracers;
  }

}
