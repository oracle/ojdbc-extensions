package oracle.jdbc.provider.opentelemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.management.ManagementFactory;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.jupiter.api.Test;

import oracle.jdbc.provider.observability.configuration.ObservabilityConfiguration;
import oracle.jdbc.provider.observability.tracers.TracerType;
import oracle.jdbc.spi.TraceEventListenerProvider;

public class BackwardCompatibilityTest {
 @Test
  public void testConfiguration() throws Exception {
    
    // System properties
    System.setProperty("oracle.jdbc.provider.opentelemetry.enabled", "true");
    System.setProperty("oracle.jdbc.provider.opentelemetry.sensitive-enabled", "true");

    TraceEventListenerProvider provider = new OpenTelemetryTraceEventListenerProvider();
    provider.getTraceEventListener(null);

    assertEquals(true, ObservabilityConfiguration.getInstance().getEnabled());
    assertEquals("OTEL", ObservabilityConfiguration.getInstance().getEnabledTracers());
    assertEquals(true, ObservabilityConfiguration.getInstance().getSensitiveDataEnabled());

    assertEquals(1, ObservabilityConfiguration.getInstance().getEnabledTracersSet().size());
    assertEquals(TracerType.OTEL, ObservabilityConfiguration.getInstance().getEnabledTracersSet().get(0));

    // MBean
    ObjectName objectName = new ObjectName(
    "com.oracle.jdbc.extension.opentelemetry:type=OpenTelemetryTraceEventListener");
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    String enabled = server.getAttribute(objectName, "Enabled").toString();
    String enabledTracers = server.getAttribute(objectName, "EnabledTracers").toString();
    String sensitiveDataEnabled = server.getAttribute(objectName, "SensitiveDataEnabled").toString();

    assertEquals(enabled, "true");
    assertEquals(enabledTracers, "OTEL");
    assertEquals(sensitiveDataEnabled, "true");

    server.setAttribute(objectName, new Attribute("Enabled", false));
    server.setAttribute(objectName, new Attribute("SensitiveDataEnabled", false));

    assertEquals(false, ObservabilityConfiguration.getInstance().getEnabled());
    assertEquals(false, ObservabilityConfiguration.getInstance().getSensitiveDataEnabled());

    assertEquals("OTEL", ObservabilityConfiguration.getInstance().getEnabledTracers());
    assertEquals(false, ObservabilityConfiguration.getInstance().getSensitiveDataEnabled());

    assertEquals(1, ObservabilityConfiguration.getInstance().getEnabledTracersSet().size());
    assertEquals(TracerType.OTEL, ObservabilityConfiguration.getInstance().getEnabledTracersSet().get(0));

    // Singleton
    ObservabilityConfiguration.getInstance().setEnabled(true);
    ObservabilityConfiguration.getInstance().setSensitiveDataEnabled(true);

    enabled = server.getAttribute(objectName, "Enabled").toString();
    enabledTracers = server.getAttribute(objectName, "EnabledTracers").toString();
    sensitiveDataEnabled = server.getAttribute(objectName, "SensitiveDataEnabled").toString();

    assertEquals("true", enabled);
    assertEquals("OTEL", enabledTracers);
    assertEquals("true", sensitiveDataEnabled);

    assertEquals(1, ObservabilityConfiguration.getInstance().getEnabledTracersSet().size());
    assertEquals(TracerType.OTEL, ObservabilityConfiguration.getInstance().getEnabledTracersSet().get(0));

  }
}
