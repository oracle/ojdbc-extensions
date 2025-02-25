package oracle.jdbc.provider.observability;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.Attribute;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import oracle.jdbc.TraceEventListener;
import oracle.jdbc.provider.observability.configuration.ObservabilityConfiguration;
import oracle.jdbc.spi.TraceEventListenerProvider;

/**
 * Implementation of Oracle JDBC {@link TraceEventListenerProvider} for 
 * {@link ObservabilityTraceEventListener}.
 */
public class ObservabilityTraceEventListenerProvider implements TraceEventListenerProvider {

  private static final String PROVIDER_NAME = "observability-trace-event-listener-provider";
  private static final String MBEAN_OBJECT_NAME = "com.oracle.jdbc.provider.observability:type=ObservabilityConfiguration";
  private static ObjectName objectName;

  /**
   * Logger
   */
  private Logger logger = Logger.getLogger(ObservabilityTraceEventListenerProvider.class.getName());

  /**
   * MBean server
   */
  protected static final MBeanServer server = ManagementFactory.getPlatformMBeanServer();;


  static {
    try {
      objectName = new ObjectName(MBEAN_OBJECT_NAME);
    } catch (MalformedObjectNameException e) {
      objectName = null;
    }
  }

  /**
   * Constructs a new instance of ObservabilityTraceEventListenerProvider. This
   * constructor will be called by the driver's service provider to create a new
   * instance.
   */
  public ObservabilityTraceEventListenerProvider() { }

  @Override
  public TraceEventListener getTraceEventListener(Map<Parameter, CharSequence> map) {
    try {
      if (!server.isRegistered(objectName)) {
        server.registerMBean(ObservabilityConfiguration.getInstance(), objectName);
      }
    } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
      logger.log(Level.WARNING, "Could not register MBean", e);
    }
    return ObservabilityTraceEventListener.getInstance();
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
