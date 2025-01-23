package oracle.jdbc.provider.observability.configuration;

import java.util.EnumSet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import oracle.jdbc.provider.observability.ObservabilityTraceEventListener;

public class ObservabilityConfiguration implements ObservabilityConfigurationMBean {

  private final ReentrantLock observabilityConfiguraitonLock = new ReentrantLock();

  private static final ObservabilityConfiguration INSTANCE = new ObservabilityConfiguration();

  private ObservabilityConfiguration() {  }

  private boolean sensitiveDataEnabled;
  private String tracers;

  private EnumSet<ObservabilityTraceEventListener.Tracers> enabledTracers;

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

  public EnumSet<ObservabilityTraceEventListener.Tracers> getEnabledTracersSet() {
    return enabledTracers.clone();
  }

}
