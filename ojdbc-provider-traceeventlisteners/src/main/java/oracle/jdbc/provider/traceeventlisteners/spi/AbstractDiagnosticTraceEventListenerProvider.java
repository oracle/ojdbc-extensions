package oracle.jdbc.provider.traceeventlisteners.spi;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import oracle.jdbc.TraceEventListener;
import oracle.jdbc.spi.TraceEventListenerProvider;

public abstract class AbstractDiagnosticTraceEventListenerProvider implements TraceEventListenerProvider {

  private static final String MBEAN_OBJECT_NAME = "oracle.jdbc.provider:type=DiagnosticTraceEventListener,name=%s,classLoaderInfo=%s";
  private static final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
  Logger logger = Logger.getLogger(AbstractDiagnosticTraceEventListenerProvider.class.getName());

  private static final Parameter ENABLE_PARAMETER = new Parameter() {
    @Override
    public boolean isSensitive() {
      return false;
    }
    @Override
    public String name() {
      return "enable";
    }
  };

  private static final Parameter ENABLE_SENSITIVE_DATA_PARAMETER = new Parameter() {
    @Override
    public boolean isSensitive() {
      return false;
    }
    @Override
    public String name() {
      return "enableSensitiveData";
    }
  };

    @Override
  public String getName() {
    return getProviderName();
  }

  @Override
  public Collection<? extends Parameter> getParameters() {
    return Arrays.asList(ENABLE_PARAMETER, ENABLE_SENSITIVE_DATA_PARAMETER);
  }

  @Override
  public TraceEventListener getTraceEventListener(Map<Parameter, CharSequence> parametersMap) {
    boolean enabled = getBooleanParameterValue(ENABLE_PARAMETER, parametersMap);
    boolean enableSensitiveData = getBooleanParameterValue(ENABLE_SENSITIVE_DATA_PARAMETER, parametersMap);
    
    return getTraceEventListener(enabled, enableSensitiveData);
  }

  protected abstract String getProviderName();
  protected abstract TraceEventListener getTraceEventListener(boolean enabled, boolean enableSensitiveData);

  private boolean getBooleanParameterValue(Parameter parameter, Map<Parameter, CharSequence> parametersMap) {
    CharSequence value = parametersMap.get(parameter);
    if (value != null) {
      return Boolean.valueOf(value.toString());
    }
    return false;
  }

  protected TraceEventListener getTraceEventListenerFromMBean(boolean enabled, boolean enableSensitiveData) {
    TraceEventListener traceEventListenerMBean;
    ObjectName objectName = getObjectName();
    
    try {
      if (objectName != null && server.isRegistered(objectName)) {
        traceEventListenerMBean = (TraceEventListener) server
            .instantiate(TraceEventListener.class.getName());
        return traceEventListenerMBean;
      }
    } catch (ReflectionException | MBeanException e) {
      logger.log(Level.WARNING, "Could not retrieve MBean from server", e);
    }

    traceEventListenerMBean = getTraceEventListener(enabled, enableSensitiveData);
    try {
      server.registerMBean(traceEventListenerMBean, objectName);
    } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
      logger.log(Level.WARNING, "Could not register MBean", e);
    }

    return traceEventListenerMBean;
  }

  private ObjectName getObjectName() {
    ClassLoader classLoader = this.getClass().getClassLoader();
    String classLoaderName = classLoader == null ? "nullLoader" : classLoader.getClass().getSimpleName();
    String classLoaderInfo = classLoaderName + "@" +
          Integer.toHexString((classLoader == null ? 0 : classLoader.hashCode()));
    String className = this.getClass().getSimpleName();
    String objectName = String.format(MBEAN_OBJECT_NAME, classLoaderInfo, className);
    try {
      return new ObjectName(objectName);
    } catch (MalformedObjectNameException e) {
      return null;
    }
  }


}
