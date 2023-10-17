/*
 ** Copyright (c) 2023 Oracle and/or its affiliates.
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

package oracle.jdbc.provider.opentelemetry;

import oracle.jdbc.TraceEventListener;
import oracle.jdbc.spi.TraceEventListenerProvider;

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

/**
 * <p>
 * This class implements the TraceEventListenerProvider interface exposed by the
 * Oracle JDBC driver. It provides TraceEventListeners of type {@linkplain
 * oracle.jdbc.provider.opentelemetry.OpenTelemetryTraceEventListener
 * OpenTelemetryTraceEventListener}.
 * </p>
 * <p>
 * In order to use this provider the Oracle JDBC Connection Property
 * {@code oracle.jdbc.provider.traceEventListener} must be set to
 * {@value #PROVIDER_NAME}, and the provider must either be declared in a
 * {@code META-INF/services/oracle.jdbc.spi.TraceEventListenerProvider} file in
 * the class path, or by a {@code module-info.java} file with a
 * "<code>provides oracle.jdbc.spi.TraceEventListenerProvider</code>" directive
 * in the module path.
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
public class OpenTelemetryTraceEventListenerProvider implements TraceEventListenerProvider {

  private static final String PROVIDER_NAME = "open-telemetry-trace-event-listener-provider";
  private static final String MBEAN_OBJECT_NAME = "com.oracle.jdbc.extension.opentelemetry:type=OpenTelemetryTraceEventListener";

  private static final MBeanServer server = ManagementFactory.getPlatformMBeanServer();;
  private static ObjectName objectName;

  Logger logger = Logger.getLogger(OpenTelemetryTraceEventListenerProvider.class.getName());

  static {
    try {
      objectName = new ObjectName(MBEAN_OBJECT_NAME);
    } catch (MalformedObjectNameException e) {
      objectName = null;
    }
  }

  @Override
  public TraceEventListener getTraceEventListener(Map<Parameter, CharSequence> map) {
    OpenTelemetryTraceEventListener openTelemetryBean;
    try {
      if (objectName != null && server.isRegistered(objectName)) {
        openTelemetryBean = (OpenTelemetryTraceEventListener) server
            .instantiate(OpenTelemetryTraceEventListener.class.getName());
        return openTelemetryBean;
      }
    } catch (ReflectionException | MBeanException e) {
      logger.log(Level.WARNING, "Could not retrieve MBean from server", e);
    }
    openTelemetryBean = new OpenTelemetryTraceEventListener();
    try {
      server.registerMBean(openTelemetryBean, objectName);
    } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
      logger.log(Level.WARNING, "Could not register MBean", e);
    }
    return openTelemetryBean;
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
