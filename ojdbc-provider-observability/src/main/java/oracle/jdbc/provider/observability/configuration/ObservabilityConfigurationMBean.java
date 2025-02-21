package oracle.jdbc.provider.observability.configuration;

import oracle.jdbc.provider.observability.tracers.TracerType;

/**
 * MBean that allows to configure the Oracle JDBC Observability Provider.
 */
public interface ObservabilityConfigurationMBean {

  /**
   * Returns true if the provider is enabled, otherwise false.
   * 
   * @return true if the provider is enabled, otherwise false.
   */
  public boolean getEnabled();

  /**
   * Enables/disables the provider.
   * 
   * @param enabled true to enable the provider, otherwise false.
   */
  public void setEnabled(boolean enabled);

  /**
   * Returns a comma separated list of enabled tracers.
   * 
   * @return a comma separated list of enabled tracers.
   */
  public String getEnabledTracers();
  
  /**
   * Enables the tracers. Available tracers are defined in enum {@link TracerType}.
   *
   * @param tracers comma separated list of enabled tracers.
   */
  public void setEnabledTracers(String tracers);

  /**
   * Returns true if sensitive data is enabled, otherwise false.
   * 
   * @return true if sensitive data is enabled, otherwise false.
   */
  public boolean getSensitiveDataEnabled();

  /**
   * Enables/disables sensitive data.
   * 
   * @param sensitiveDataEnabled true to enable sensitive data, otherwise false.
   */
  public void setSensitiveDataEnabled(boolean sensitiveDataEnabled);
}
