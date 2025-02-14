package oracle.jdbc.provider.opentelemetry;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import oracle.jdbc.TraceEventListener;
import oracle.jdbc.provider.observability.ObservabilityTraceEventListener;
import oracle.jdbc.provider.observability.ObservabilityTraceEventListenerProvider;
import oracle.jdbc.provider.observability.configuration.ObservabilityConfiguration;

/**
 * <p>
 * This class implements the TraceEventListenerProvider interface exposed by the
 * Oracle JDBC driver. It provides TraceEventListeners of type {@link
 * oracle.jdbc.provider.observability.ObservabilityTraceEventListener}.
 * </p>
 * <p>
 * The provider registers a MBean (with objectName {@value #MBEAN_OBJECT_NAME})
 * that allows to configure the TraceEventListener by setting attributes. The
 * following attributes are available:
 * <ul>
 * <li><b>Enabled</b>: enables/disables exporting traces to Open Telemetry
 * <em>(true by default)</em></li>
 * <li><b>SensitiveDataEnabled</b>: enables/disables exporting sensiteve data
 * to Open Telemetry<em>(false by default)</em></li>
 * </ul>
 */
public class OpenTelemetryTraceEventListenerProvider extends ObservabilityTraceEventListenerProvider {

  private static final String PROVIDER_NAME = "open-telemetry-trace-event-listener-provider";
  private static final String MBEAN_OBJECT_NAME = "com.oracle.jdbc.extension.opentelemetry:type=OpenTelemetryTraceEventListener";

  /**
   * Name of the property used to enable or disable this listener.
   */
  public static final String OPEN_TELEMENTRY_TRACE_EVENT_LISTENER_ENABLED = "oracle.jdbc.provider.opentelemetry.enabled";
  /**
   * Name of the property used to enable or disable sensitive data for this
   * listener.
   */
  public static final String OPEN_TELEMENTRY_TRACE_EVENT_LISTENER_SENSITIVE_ENABLED = "oracle.jdbc.provider.opentelemetry.sensitive-enabled";

  /**
   * Logger
   */
  private Logger logger = Logger.getLogger(OpenTelemetryTraceEventListenerProvider.class.getName());

  private static ObjectName objectName;

  static {
    try {
      objectName = new ObjectName(MBEAN_OBJECT_NAME);
    } catch (MalformedObjectNameException e) {
      objectName = null;
    }
  }

  /**
   * Constructs a new instance of OpenTelemetryTraceEventListenerProvider. This
   * constructor will be called by the driver's service provider to create a new
   * instance.
   */
  public OpenTelemetryTraceEventListenerProvider() { }

  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  @Override
  public Collection<? extends Parameter> getParameters() {
    return Collections.emptyList();
  }

  @Override
  public TraceEventListener getTraceEventListener(Map<Parameter, CharSequence> map) {
    try {
      if (!server.isRegistered(objectName)) {
        boolean enabled = Boolean.valueOf(System.getProperty(OPEN_TELEMENTRY_TRACE_EVENT_LISTENER_ENABLED, "true"));
        String sensitiveDataEnabled = System.getProperty(OPEN_TELEMENTRY_TRACE_EVENT_LISTENER_SENSITIVE_ENABLED, "false");
        ObservabilityConfiguration.getInstance().setEnabled(enabled);
        ObservabilityConfiguration.getInstance().setEnabledTracers("OTEL");
        ObservabilityConfiguration.getInstance().setSensitiveDataEnabled(Boolean.parseBoolean(sensitiveDataEnabled));

        server.registerMBean(ObservabilityConfiguration.getInstance(), objectName);
      }
    } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
      logger.log(Level.WARNING, "Could not register MBean", e);
    }
    return ObservabilityTraceEventListener.getInstance();
  }

}
