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

  private static final String INSTANCE_NAME = "configuration-single";
  private static final String INSTANCE_NAME_1 = "configuration-test-one";
  private static final String INSTANCE_NAME_2 = "configuration-test-two";

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
    ObservabilityTraceEventListener listener = createTraceEventListener(provider, INSTANCE_NAME);

    // Get the configuration object
    ObservabilityConfiguration configuration = listener.getObservabilityConfiguration();

    // Verify that listener one is configured with values from system properties
    verifyConfiguration(configuration, listener.getMBeanObjectName(), "JFR", 1, true);

    // Update configuration using MBean 
    server.setAttribute(listener.getMBeanObjectName(), new Attribute("EnabledTracers", "OTEL, JFR"));
    server.setAttribute(listener.getMBeanObjectName(), new Attribute("SensitiveDataEnabled", false));

    // check that the values have been updated using the instance of the configuration
    verifyConfiguration(configuration, listener.getMBeanObjectName(), "OTEL,JFR", 2, false);

    // Update the configuration using the instance of the configuration
    configuration.setEnabledTracers("OTEL");
    configuration.setSensitiveDataEnabled(true);

    // Check  that the values returned by the MBean correspond to the values set using the instance
    verifyConfiguration(configuration, listener.getMBeanObjectName(), "OTEL", 1, true);

  }

  @Test
  public void testConfigurationWith2Instances() throws Exception {
    
    // Create a TraceEventListner named test-instance
    TraceEventListenerProvider provider = new ObservabilityTraceEventListenerProvider();
    ObservabilityTraceEventListener listener1 = createTraceEventListener(provider, INSTANCE_NAME_1);
    ObservabilityTraceEventListener listener2 = createTraceEventListener(provider, INSTANCE_NAME_2);

    // Verify that listener one is configured with values from system properties
    verifyConfiguration(listener1.getObservabilityConfiguration(), 
        listener1.getMBeanObjectName(), "JFR", 1, true);

    // Verify that listener two is configured with values from system properties
    verifyConfiguration(listener2.getObservabilityConfiguration(), 
        listener2.getMBeanObjectName(), "JFR", 1, true);

    // Update configuration one using MBean 
    server.setAttribute(listener1.getMBeanObjectName(), new Attribute("EnabledTracers", "OTEL,JFR"));
    server.setAttribute(listener1.getMBeanObjectName(), new Attribute("SensitiveDataEnabled", false));
    
    // Verify that listener one's configuration with the values set using the MBean
    verifyConfiguration(listener1.getObservabilityConfiguration(), 
        listener1.getMBeanObjectName(), "OTEL,JFR", 2, false);

    // Verify that listener two's configuration did not change
    verifyConfiguration(listener2.getObservabilityConfiguration(), 
    listener2.getMBeanObjectName(), "JFR", 1, true);

    // Update the configuration using the instance of the configuration
    ObservabilityConfiguration configuration2 = listener2.getObservabilityConfiguration();
    configuration2.setEnabledTracers("OTEL");
    configuration2.setSensitiveDataEnabled(false);

    // Verify that listener one's configuration did not change
    verifyConfiguration(listener1.getObservabilityConfiguration(), 
        listener1.getMBeanObjectName(), "OTEL,JFR", 2, false);

    // Verify that listener two's configuration has been updated
    verifyConfiguration(listener2.getObservabilityConfiguration(), 
    listener2.getMBeanObjectName(), "OTEL", 1, false);

  }

  @Test
  public void testDefaultUniqueIdentifier() throws Exception {
    
    // Create a TraceEventListner named test-instance
    TraceEventListenerProvider provider = new ObservabilityTraceEventListenerProvider();
    ObservabilityTraceEventListener listener = (ObservabilityTraceEventListener)provider.getTraceEventListener(new HashMap<>());

    // Get the configuration object
    ObservabilityConfiguration configuration = listener.getObservabilityConfiguration();

    // Verify that listener one is configured with values from system properties
    verifyConfiguration(configuration, listener.getMBeanObjectName(), "JFR", 1, true);

    // Update configuration using MBean 
    server.setAttribute(listener.getMBeanObjectName(), new Attribute("EnabledTracers", "OTEL, JFR"));
    server.setAttribute(listener.getMBeanObjectName(), new Attribute("SensitiveDataEnabled", false));

    // check that the values have been updated using the instance of the configuration
    verifyConfiguration(configuration, listener.getMBeanObjectName(), "OTEL,JFR", 2, false);

    // Update the configuration using the instance of the configuration
    configuration.setEnabledTracers("OTEL");
    configuration.setSensitiveDataEnabled(true);

    // Check  that the values returned by the MBean correspond to the values set using the instance
    verifyConfiguration(configuration, listener.getMBeanObjectName(), "OTEL", 1, true);

  }

  private ObservabilityTraceEventListener createTraceEventListener(TraceEventListenerProvider provider,
      String instanceName) {
    Map<Parameter, CharSequence> parameters = new HashMap<>();
    provider.getParameters().forEach(parameter -> {
      parameters.put(parameter, (CharSequence)instanceName);
    });
    return (ObservabilityTraceEventListener)provider.getTraceEventListener(parameters);
  }

  private void verifyConfiguration(ObservabilityConfiguration configuration, 
      ObjectName mBeanObjectName,
      String expectedTracers, 
      int expectedTracerCount, 
      boolean expectedSensitiveDataEnabled) throws Exception{

    String[] expectedTracersArray = expectedTracers.split(",");
    // Verify that the configuration matches the configuration set using system properties
    assertEquals(expectedTracers, configuration.getEnabledTracers());
    assertEquals(expectedSensitiveDataEnabled, configuration.getSensitiveDataEnabled());
    assertEquals(expectedTracerCount, configuration.getEnabledTracersAsList().size());
    assertEquals(expectedTracerCount, expectedTracersArray.length, "Wrong number of tracers.");
    for (int i = 0; i < expectedTracerCount; i++) {
      assertEquals(expectedTracersArray[i], configuration.getEnabledTracersAsList().get(i));
    }

    // Get configuration using MBean and check that it matches the configuration set using system properties
    String enabledTracers = server.getAttribute(mBeanObjectName, "EnabledTracers").toString();
    String sensitiveDataEnabled = server.getAttribute(mBeanObjectName, "SensitiveDataEnabled").toString();
    assertEquals(enabledTracers, expectedTracers);
    assertEquals(Boolean.parseBoolean(sensitiveDataEnabled), expectedSensitiveDataEnabled);

  }

}
