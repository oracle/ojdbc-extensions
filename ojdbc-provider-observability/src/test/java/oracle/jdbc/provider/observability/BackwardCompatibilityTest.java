/*
 ** Copyright (c) 2025 Oracle and/or its affiliates.
 **
 ** The Universal Permissive License (UPL), Version 1.0
 **
 ** Subject to the condition set forth below, permission is hereby granted to any
 ** person obtaining a copy of this software, associated documentation and/or data
 ** (collectively the "Software"), free of charge and under any and all copyright
 ** rights in the Software, and any and all patent rights owned or freely
 ** licensable by each licensor hereunder covering either (i) the unmodified
 ** Software as contributed to or provided by such licensor, or (ii) the Larger
 ** Works (as defined below), to deal in both
 **
 ** (a) the Software, and
 ** (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 ** one is included with the Software (each a "Larger Work" to which the Software
 ** is contributed by such licensors),
 **
 ** without restriction, including without limitation the rights to copy, create
 ** derivative works of, display, perform, and distribute the Software and make,
 ** use, sell, offer for sale, import, export, have made, and have sold the
 ** Software and the Larger Work(s), and to sublicense the foregoing rights on
 ** either these or other terms.
 **
 ** This license is subject to the following condition:
 ** The above copyright notice and either this complete permission notice or at
 ** a minimum a reference to the UPL must be included in all copies or
 ** substantial portions of the Software.
 **
 ** THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 ** IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 ** FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 ** AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 ** LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 ** OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 ** SOFTWARE.
 */
package oracle.jdbc.provider.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.jupiter.api.Test;

import oracle.jdbc.spi.OracleResourceProvider.Parameter;
import oracle.jdbc.spi.TraceEventListenerProvider;

public class BackwardCompatibilityTest {
  private static final String INSTANCE_NAME = "test-instance";
  
 @Test
  public void testConfiguration() throws Exception {
    
    // System properties
    System.setProperty("oracle.jdbc.provider.opentelemetry.enabled", "true");
    System.setProperty("oracle.jdbc.provider.opentelemetry.sensitive-enabled", "true");

    TraceEventListenerProvider provider = new OpenTelemetryTraceEventListenerProvider();
    Map<Parameter, CharSequence> parameters = new HashMap<>();
    provider.getParameters().forEach(parameter -> {
      parameters.put(parameter, (CharSequence)INSTANCE_NAME);
    });
    ObservabilityTraceEventListener listener = (ObservabilityTraceEventListener)provider.getTraceEventListener(parameters);

    ObservabilityConfiguration configuration = ObservabilityTraceEventListener.getObservabilityConfiguration(INSTANCE_NAME);

    assertEquals(true, configuration.getEnabled());
    assertEquals("OTEL", configuration.getEnabledTracers());
    assertEquals(true, configuration.getSensitiveDataEnabled());

    assertEquals(1, configuration.getEnabledTracersAsList().size());
    assertEquals("OTEL", configuration.getEnabledTracersAsList().get(0));

    // MBean
    ObjectName objectName = new ObjectName(listener.getMBeanObjectName());
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    String enabled = server.getAttribute(objectName, "Enabled").toString();
    String enabledTracers = server.getAttribute(objectName, "EnabledTracers").toString();
    String sensitiveDataEnabled = server.getAttribute(objectName, "SensitiveDataEnabled").toString();

    assertEquals(enabled, "true");
    assertEquals(enabledTracers, "OTEL");
    assertEquals(sensitiveDataEnabled, "true");

    server.setAttribute(objectName, new Attribute("Enabled", false));
    server.setAttribute(objectName, new Attribute("SensitiveDataEnabled", false));

    assertEquals(false, configuration.getEnabled());
    assertEquals(false, configuration.getSensitiveDataEnabled());

    assertEquals("OTEL", configuration.getEnabledTracers());
    assertEquals(false, configuration.getSensitiveDataEnabled());

    assertEquals(1, configuration.getEnabledTracersAsList().size());
    assertEquals("OTEL", configuration.getEnabledTracersAsList().get(0));

    // Singleton
    configuration.setEnabled(true);
    configuration.setSensitiveDataEnabled(true);

    enabled = server.getAttribute(objectName, "Enabled").toString();
    enabledTracers = server.getAttribute(objectName, "EnabledTracers").toString();
    sensitiveDataEnabled = server.getAttribute(objectName, "SensitiveDataEnabled").toString();

    assertEquals("true", enabled);
    assertEquals("OTEL", enabledTracers);
    assertEquals("true", sensitiveDataEnabled);

    assertEquals(1, configuration.getEnabledTracersAsList().size());
    assertEquals("OTEL", configuration.getEnabledTracersAsList().get(0));

  }
}
