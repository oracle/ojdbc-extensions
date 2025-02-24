package oracle.jdbc.provider.observability.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import oracle.jdbc.provider.observability.tracers.TracerType;

/**
 * Implementation of {@link ObservabilityConfigurationMBean} that allows to 
 * configure the Oracle JDBC Observability Provider.
 */
public class ObservabilityConfiguration implements ObservabilityConfigurationMBean {

  /**
   * System property used to enabled/disable tracers. The value of this system property should be a comma separated list
   * of {@link TracerType} to enable. By default all tracers will be enabled.
   */
  private static final String ENABLED_TRACERS = "oracle.jdbc.provider.observability.enabledTracers";

  /**
   * System property used to enable/disable exporting sensitive data. Set the property to true to enable sensitive data. 
   * By default exporting sensitive data is disabled.
   */
  private static final String SENSITIVE_DATA_ENABLED = "oracle.jdbc.provider.observability.sensitiveDataEnabled";

  private static final ReentrantLock observabilityConfiguraitonLock = new ReentrantLock();

  private static final ObservabilityConfiguration INSTANCE;

  private boolean enabled = true;
  private boolean sensitiveDataEnabled;
  private String tracers;

  private List<TracerType> enabledTracers = new ArrayList<>();


  static {
    INSTANCE = new ObservabilityConfiguration();
  }

  private ObservabilityConfiguration() {  
    String enabledTracers = System.getProperty(ENABLED_TRACERS, "OTEL,JFR");
    String sensitiveDataEnabled = System.getProperty(SENSITIVE_DATA_ENABLED, "false");
    setEnabledTracers(enabledTracers);
    setSensitiveDataEnabled(Boolean.valueOf(sensitiveDataEnabled));
  }

  /**
   * Returns true if the provider is enabled, otherwise false.
   * 
   * @return true if the provider is enabled, otherwise false.
   */
  @Override
  public boolean getEnabled() {
    try {
      observabilityConfiguraitonLock.lock();
      return  enabled;
    } finally {
      observabilityConfiguraitonLock.unlock();
    }
  }

  /**
   * Enables/disables the provider.
   * 
   * @param enabled true to enable the provider, otherwise false.
   */
  @Override
  public void setEnabled(boolean enabled) {
    try {
      observabilityConfiguraitonLock.lock();
      this.enabled = enabled;
    } finally {
      observabilityConfiguraitonLock.unlock();
    }
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
   * Enables the tracers. Available tracers are defined in enum {@link TracerType}.
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
          enabledTracers.add(TracerType.valueOf(item.toUpperCase()));
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
    try {
      observabilityConfiguraitonLock.lock();
      return sensitiveDataEnabled;
    } finally {
      observabilityConfiguraitonLock.unlock();
    }
  }

  /**
   * Enables/disables sensitive data.
   * 
   * @param sensitiveDataEnabled true to enable sensitive data, otherwise false.
   */
  @Override
  public void setSensitiveDataEnabled(boolean sensitiveDataEnabled) {
    try {
      observabilityConfiguraitonLock.lock();
      this.sensitiveDataEnabled = sensitiveDataEnabled;
    } finally {
      observabilityConfiguraitonLock.unlock();
    }
  }

  /**
   * Returns the singleton instance of {@link ObservabilityConfiguration}.
   * @return the singleton instance of {@link ObservabilityConfiguration}.
   */
  public static ObservabilityConfiguration getInstance() {
    try {
      observabilityConfiguraitonLock.lock();
      return INSTANCE;
    } finally {
      observabilityConfiguraitonLock.unlock();
    }
  }

  /**
   * Returns a list of enabled {@link TracerType}.
   * @return then list of nabled {@link TracerType}.
   */
  public List<TracerType> getEnabledTracersSet() {
    try {
      observabilityConfiguraitonLock.lock();
      return enabledTracers;
    } finally {
      observabilityConfiguraitonLock.unlock();
    }
  }


}
