package oracle.jdbc.provider.observability.configuration;

public interface ObservabilityConfigurationMBean {
  
  public String getEnabledTracers();
  
  public void setEnabledTracers(String tracers);

  public boolean getSensitiveDataEnabled();

  public void setSensitiveDataEnabled(boolean sensitiveDataEnabled);
}
