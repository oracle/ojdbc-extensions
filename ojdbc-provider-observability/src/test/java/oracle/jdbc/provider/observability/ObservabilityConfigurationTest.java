package oracle.jdbc.provider.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.management.ManagementFactory;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.jupiter.api.Test;

import oracle.jdbc.provider.observability.configuration.ObservabilityConfiguration;
import oracle.jdbc.provider.observability.tracers.TracerType;
import oracle.jdbc.spi.TraceEventListenerProvider;

public class ObservabilityConfigurationTest {

  // Set system properties before starting 
  static {
    System.setProperty("oracle.jdbc.provider.observability.enabledTracers", "JFR");
    System.setProperty("oracle.jdbc.provider.observability.sensitiveDataEnabled", "true");
    System.out.println(ObservabilityConfiguration.getInstance().getEnabledTracers());
  }

  @Test
  public void testConfiguration() throws Exception {

    System.out.println(ObservabilityConfiguration.getInstance().getEnabledTracers());
    TraceEventListenerProvider provider = new ObservabilityTraceEventListenerProvider();
    provider.getTraceEventListener(null);

    System.out.println(ObservabilityConfiguration.getInstance().getEnabledTracers());
    assertEquals("JFR", ObservabilityConfiguration.getInstance().getEnabledTracers());
    assertEquals(true, ObservabilityConfiguration.getInstance().getSensitiveDataEnabled());

    assertEquals(1, ObservabilityConfiguration.getInstance().getEnabledTracersSet().size());
    assertEquals(TracerType.JFR, ObservabilityConfiguration.getInstance().getEnabledTracersSet().get(0));

    // MBean
    ObjectName objectName = new ObjectName(
    "com.oracle.jdbc.provider.observability:type=ObservabilityConfiguration");
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    String enabledTracers = server.getAttribute(objectName, "EnabledTracers").toString();
    String sensitiveDataEnabled = server.getAttribute(objectName, "SensitiveDataEnabled").toString();

    assertEquals(enabledTracers, "JFR");
    assertEquals(sensitiveDataEnabled, "true");

    server.setAttribute(objectName, new Attribute("EnabledTracers", "OTEL,JFR"));
    server.setAttribute(objectName, new Attribute("SensitiveDataEnabled", false));

    assertEquals("OTEL,JFR", ObservabilityConfiguration.getInstance().getEnabledTracers());
    assertEquals(false, ObservabilityConfiguration.getInstance().getSensitiveDataEnabled());

    assertEquals("OTEL,JFR", ObservabilityConfiguration.getInstance().getEnabledTracers());
    assertEquals(false, ObservabilityConfiguration.getInstance().getSensitiveDataEnabled());

    assertEquals(2, ObservabilityConfiguration.getInstance().getEnabledTracersSet().size());
    assertEquals(TracerType.OTEL, ObservabilityConfiguration.getInstance().getEnabledTracersSet().get(0));
    assertEquals(TracerType.JFR, ObservabilityConfiguration.getInstance().getEnabledTracersSet().get(1));

    // Singleton
    ObservabilityConfiguration.getInstance().setEnabledTracers("OTEL");
    ObservabilityConfiguration.getInstance().setSensitiveDataEnabled(true);

    enabledTracers = server.getAttribute(objectName, "EnabledTracers").toString();
    sensitiveDataEnabled = server.getAttribute(objectName, "SensitiveDataEnabled").toString();

    assertEquals("OTEL", enabledTracers);
    assertEquals("true", sensitiveDataEnabled);

    assertEquals(1, ObservabilityConfiguration.getInstance().getEnabledTracersSet().size());
    assertEquals(TracerType.OTEL, ObservabilityConfiguration.getInstance().getEnabledTracersSet().get(0));

  }

}
