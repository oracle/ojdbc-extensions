package oracle.jdbc.provider.observability;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Collections;
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

public class ObservabilityTraceEventListenerProvider implements TraceEventListenerProvider {

  private static final String PROVIDER_NAME = "observability-trace-event-listener-provider";
  private static final String MBEAN_OBJECT_NAME = "com.oracle.jdbc.extension.opentelemetry:type=ObservabilityTraceEventListener";

  private static final MBeanServer server = ManagementFactory.getPlatformMBeanServer();;
  private static ObjectName objectName;

  Logger logger = Logger.getLogger(ObservabilityTraceEventListenerProvider.class.getName());

  static {
    try {
      objectName = new ObjectName(MBEAN_OBJECT_NAME);
    } catch (MalformedObjectNameException e) {
      objectName = null;
    }
  }

  @Override
  public TraceEventListener getTraceEventListener(Map<Parameter, CharSequence> map) {
    ObservabilityTraceEventListener observabilityBean;
    try {
      if (objectName != null && server.isRegistered(objectName)) {
        observabilityBean = (ObservabilityTraceEventListener) server
            .instantiate(ObservabilityTraceEventListener.class.getName());
        return observabilityBean;
      }
    } catch (ReflectionException | MBeanException e) {
      logger.log(Level.WARNING, "Could not retrieve MBean from server", e);
    }
    observabilityBean = new ObservabilityTraceEventListener();
    try {
      server.registerMBean(observabilityBean, objectName);
    } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
      logger.log(Level.WARNING, "Could not register MBean", e);
    }
    return observabilityBean;
  }

  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  @Override
  public Collection<? extends Parameter> getParameters() {
    return Collections.emptyList();
  }

}
