package oracle.jdbc.provider.traceeventlisteners.spi;

import oracle.jdbc.TraceEventListener;

/**
 * 
 */
public abstract class AbstractDiagnosticTraceEventListener implements 
  DiagnosticTraceEventListenerMBean, TraceEventListener {

  protected boolean enabled;
  protected boolean sensitiveDataEnabled;

  /**
   * Reads the system properties defined by the {@link getEnabledSystemProperty}
   * and {@link getEnableSensitiveDataSystemProperty} methods and enables de 
   * diagnostic trace event listener and tracing sensitive data. The system 
   * properties override the parameters send to this constructor.
   * 
   * @param enabled set true to enable the diagnostic trace event listener.
   * @param sensitiveDataEnabled set to true to enable tracing sensitive data.
   */
  protected AbstractDiagnosticTraceEventListener(boolean enabled, boolean sensitiveDataEnabled) {
    this.enabled = getBooleanProperty(getEnabledSystemProperty(), enabled);
    this.sensitiveDataEnabled = getBooleanProperty(getEnableSensitiveDataSystemProperty(), sensitiveDataEnabled);
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    
  }

  @Override
  public boolean isSensitiveDataEnabled() {
    return this.sensitiveDataEnabled;
  }

  @Override
  public void setSensitiveDataEnabled(boolean sensitiveDataEnabled) {
    this.sensitiveDataEnabled = sensitiveDataEnabled;
  }

  protected abstract String getEnabledSystemProperty();
  protected abstract String getEnableSensitiveDataSystemProperty();

  /**
   * Gets property value from system property or environment variable given the 
   * property name. If no system property or environment variable is found the
   * default value is returned.
   * @param propertyName The property name.
   * @param defaultValue The default value.
   * @return the property value from system property, environment variable or 
   * the default value.
   */
  private boolean getBooleanProperty (String propertyName, boolean defaultValue) {
    boolean value = defaultValue;
    if (propertyName != null) {
      String propertyValue = System.getProperty(propertyName);
      if (propertyValue != null) {
        value = Boolean.valueOf(propertyValue);
      } else {
        propertyValue = System.getenv(propertyName);
        if (propertyValue != null) {
          value = Boolean.valueOf(propertyValue);
        }
      }
    } 
    return value;
  }

}
