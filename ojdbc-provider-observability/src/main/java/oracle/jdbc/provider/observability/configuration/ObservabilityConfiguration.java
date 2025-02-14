package oracle.jdbc.provider.observability.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import oracle.jdbc.provider.observability.tracers.Tracer;

/**
 * Implementation of {@link ObservabilityConfigurationMBean} that allows to 
 * configure the Oracle JDBC Observability Provider.
 */
public class ObservabilityConfiguration implements ObservabilityConfigurationMBean {

  private static final ReentrantLock observabilityConfiguraitonLock = new ReentrantLock();

  private static final ObservabilityConfiguration INSTANCE;

  static {
    INSTANCE = new ObservabilityConfiguration();
    //INSTANCE.setSensitiveDataEnabled(true);
    INSTANCE.setEnabledTracers(System.getProperty("oracle.jdbc.provider.observability.tracer", "OTEL,JFR"));
  }

  private ObservabilityConfiguration() {  }

  private boolean enabled = true;
  private boolean sensitiveDataEnabled;
  private String tracers;

  private List<Tracer> enabledTracers = new ArrayList<>();

  /**
   * Returns true if the provider is enabled, otherwise false.
   * 
   * @return true if the provider is enabled, otherwise false.
   */
  @Override
  public boolean getEnabled() {
    return  enabled;
  }

  /**
   * Enables/disables the provider.
   * 
   * @param enabled true to enable the provider, otherwise false.
   */
  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns a comma separated list of enabled tracers.
   */
  @Override
  public String getEnabledTracers() {
    try {
      observabilityConfiguraitonLock.lock();
      return tracers;
    } finally {
      observabilityConfiguraitonLock.unlock();
    }
  }

  /**
   * Enables the tracers. Available tracers are defined in enum {@link Tracer}.
   *
   * @param tracers comma separated list of enabled tracers.
   */
  @Override
  public void setEnabledTracers(String tracers) {
    try {
      observabilityConfiguraitonLock.lock();
      enabledTracers.clear();
      String[] items = tracers.split(",");
      for (String item : items) {
        if (item != null) {
          enabledTracers.add(Tracer.valueOf(item.toUpperCase()));
        }
      }
      this.tracers = enabledTracers.stream().map((item) -> item.toString()).collect(Collectors.joining(","));
    } finally {
      observabilityConfiguraitonLock.unlock();
    }

  }

  /**
   * Returns true if sensitive data is enabled, otherwise false.
   */
  @Override
  public boolean getSensitiveDataEnabled() {
    return sensitiveDataEnabled;
  }

  /**
   * Enables/disables sensitive data.
   * 
   * @param sensitiveDataEnabled true to enable sensitive data, otherwise false.
   */
  @Override
  public void setSensitiveDataEnabled(boolean sensitiveDataEnabled) {
    this.sensitiveDataEnabled = sensitiveDataEnabled;
  }

  /**
   * Returns the singleton instance of {@link ObservabilityConfiguration}.
   * @return the singleton instance of {@link ObservabilityConfiguration}.
   */
  public static ObservabilityConfiguration getInstance() {
    return INSTANCE;
  }

  /**
   * Returns a list of enabled {@link Tracer}.
   * @return then list of nabled {@link Tracer}.
   */
  public List<Tracer> getEnabledTracersSet() {
    return enabledTracers;
  }


}
