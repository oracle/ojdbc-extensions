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

import oracle.jdbc.spi.TraceEventListenerProvider;
import oracle.jdbc.spi.OracleResourceProvider.Parameter;

public class ObservabilityConfigurationTest {

  private static final String INSTANCE_NAME = "configuration-test-instance";

  MBeanServer server = ManagementFactory.getPlatformMBeanServer();

  // Set system properties before starting 
  static {
    System.setProperty("oracle.jdbc.provider.observability.enabledTracers", "JFR");
    System.setProperty("oracle.jdbc.provider.observability.sensitiveDataEnabled", "true");
  }

  @Test
  public void testConfiguration() throws Exception {
    
    // Create a TraceEventListner named test-instance
    TraceEventListenerProvider provider = new ObservabilityTraceEventListenerProvider();
    Map<Parameter, CharSequence> parameters = new HashMap<>();
    provider.getParameters().forEach(parameter -> {
      parameters.put(parameter, (CharSequence)INSTANCE_NAME);
    });
    ObservabilityTraceEventListener listener = (ObservabilityTraceEventListener)provider.getTraceEventListener(parameters);

    // Get the configuration object
    ObservabilityConfiguration configuration = ObservabilityTraceEventListener.getObservabilityConfiguration(INSTANCE_NAME);

    // Verify that the configuration matches the configuration set using system properties
    assertEquals("JFR", configuration.getEnabledTracers());
    assertEquals(true, configuration.getSensitiveDataEnabled());
    assertEquals(1, configuration.getEnabledTracersAsList().size());
    assertEquals("JFR", configuration.getEnabledTracersAsList().get(0));

    // Get the MBean for the configuration
    ObjectName objectName = new ObjectName(listener.getMBeanObjectName());

    // Get configuration using MBean and check that it matches the configuration set using system properties
    String enabledTracers = server.getAttribute(objectName, "EnabledTracers").toString();
    String sensitiveDataEnabled = server.getAttribute(objectName, "SensitiveDataEnabled").toString();
    assertEquals(enabledTracers, "JFR");
    assertEquals(sensitiveDataEnabled, "true");

    // Update configuration using MBean 
    server.setAttribute(objectName, new Attribute("EnabledTracers", "OTEL,JFR"));
    server.setAttribute(objectName, new Attribute("SensitiveDataEnabled", false));

    // check that the values have been updated using the instance of the configuration
    assertEquals("OTEL,JFR", configuration.getEnabledTracers());
    assertEquals(false, configuration.getSensitiveDataEnabled());
    assertEquals(2, configuration.getEnabledTracersAsList().size());
    assertEquals("OTEL", configuration.getEnabledTracersAsList().get(0));
    assertEquals("JFR", configuration.getEnabledTracersAsList().get(1));

    // Update the configuration using the instance of the configuration
    configuration.setEnabledTracers("OTEL");
    configuration.setSensitiveDataEnabled(true);

    // Check  that the values returned by the MBean correspond to the values set using the instance
    enabledTracers = server.getAttribute(objectName, "EnabledTracers").toString();
    sensitiveDataEnabled = server.getAttribute(objectName, "SensitiveDataEnabled").toString();
    assertEquals("OTEL", enabledTracers);
    assertEquals("true", sensitiveDataEnabled);
    assertEquals(1, configuration.getEnabledTracersAsList().size());
    assertEquals("OTEL", configuration.getEnabledTracersAsList().get(0));

  }

}
