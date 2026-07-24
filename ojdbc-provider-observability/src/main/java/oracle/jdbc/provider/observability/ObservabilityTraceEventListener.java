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

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.RuntimeOperationsException;

import oracle.jdbc.TraceEventListener;
import oracle.jdbc.provider.observability.ObservabilityConfiguration.ObservabilityConfigurationType;
import oracle.jdbc.provider.observability.tracers.ObservabilityTracer;
import oracle.jdbc.provider.observability.tracers.jfr.JFRTracer;
import oracle.jdbc.provider.observability.tracers.otel.OTelTracer;

/**
 * <p>
 * TraceEventListener implementation that receives notifications whenever events
 * are generated in the driver and publishes these events different tracers 
 * depending on the configuration.
 * </p>
 * <p>
 * These events include:
 * </p>
 * <ul>
 * <li>roundtrips to the database server</li>
 * <li>AC begin and success</li>
 * <li>VIP down event</li>
 * </ul>
 * <p>
 * This extension implements two tracers:
 * </p>
  * <ul>
 * <li>OTEL: which exports traces to Open Telemetry</li>
 * <li>JFR: which exports traces to Java Flight recorder</li>
 * </ul>
 * <p>
 * The {@link ObservabilityConfiguration} class allows to configure which tracers 
 * are enabled and whether sensitive data should be exported or not.
 * </p>
 * <p>
 * The {@link ObservabilityConfiguration} is a registered MBean the object name
 * can be retrieved by calling {@link ObservabilityTraceEventListener#getMBeanObjectName()}.
 * This MBean allows to configure the TraceEventListener by setting attributes. 
 * The following attributes are available:
 * </p>
 * <ul>
 * <li><b>EnabledTracers</b>: comma separated list of tracers <em>"OTEL,JFR" by
 * default.</em></li>
 * <li><b>SensitiveDataEnabled</b>: enables/disables exporting sensiteve data
 * <em>(false by default)</em></li>
 * </ul>
 */
public class ObservabilityTraceEventListener implements TraceEventListener {
  /**
   * MBean object name format
   */
  private static final String MBEAN_OBJECT_NAME = "com.oracle.jdbc.provider.observability:type=ObservabilityConfiguration,uniqueIdentifier=%s";
  private static final String MBEAN_OBJECT_NAME_OTEL = "com.oracle.jdbc.extension.opentelemetry:type=OpenTelemetryTraceEventListener,uniqueIdentifier=%s";
  private static final int MAX_CONFIGURATION_COUNT = 100;
  private static final LinkedList<String> LRU_CONFIGURATION = new LinkedList<String>();

  /**
   * Default unique identifier, if parameter not set.
   */
  static final CharSequence DEFAULT_UNIQUE_IDENTIFIER = "default";

  /**
   * MBean server
   */
  private static final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

  /**
   * Logger
   */
  private static final Logger logger = Logger.getLogger(
      ObservabilityTraceEventListener.class.getPackageName());

  /**
   * Configuration for this instance of Trace Event Listener
   */
  private final ObservabilityConfiguration configuration;

  /**
   * Static map linking the name of the listener to its instance.
   */
  private static final Map<String, ObservabilityTraceEventListener> INSTANCES 
      = new HashMap<>();
  /**
   * Lock that assures that the INSTANCES and LRU_CONFIGURATIONS are only 
   * accessed by one thread.
   */
  private static final ReentrantLock INSTANCES_LOCK = new ReentrantLock();

  private ObjectName mBeanObjectName;

  /**
   * Create a trace event listener identified by the given name. 
   * @param uniqueIdentifier the name of the trace event listener.
   * @param configurationType configuration type for backward compatibility.
   */
  private ObservabilityTraceEventListener(String uniqueIdentifier, 
      ObservabilityConfigurationType configurationType) { 
    // Create the  configuration for this instance and register MBean
    final String mBeanName = ObservabilityConfigurationType.OTEL.equals(configurationType) ?
        String.format(MBEAN_OBJECT_NAME_OTEL, uniqueIdentifier) :
        String.format(MBEAN_OBJECT_NAME, uniqueIdentifier);
    this.configuration = new ObservabilityConfiguration(configurationType);
    try {
      mBeanObjectName = new ObjectName(mBeanName);
      if (!server.isRegistered(mBeanObjectName)) {
        server.registerMBean(configuration, mBeanObjectName);
        logger.log(Level.FINEST, "MBean and tracers registered");
      }
    } catch (InstanceAlreadyExistsException | MBeanRegistrationException | 
        NotCompliantMBeanException | MalformedObjectNameException e) {
      logger.log(Level.WARNING, "Could not register MBean", e);
    }
    // Register known tracers
    configuration.registerTracer(new OTelTracer(configuration));
    configuration.registerTracer(new JFRTracer(configuration));
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object roundTrip(Sequence sequence, TraceContext traceContext, Object userContext) {
    if (!configuration.getEnabled()) { return null;}

    // Cast the userContext to the map this listener uses, or create a new one
    // if it is being used for the first time. This is the return value of the 
    // method, and will be send back by the driver on the next event.
    Map<String, Object> currentUserContext = userContext == null ? 
        new HashMap<>() : (Map<String, Object>)userContext;

    // loop through all the enabled tracers
    for (String tracerName : configuration.getEnabledTracersAsList()) {
      ObservabilityTracer tracer = configuration.getTracer(tracerName);
      if (tracer != null) {
        // call the tracer's round trip event with the tracer's context and store
        // the new user context returned by the tracer in the user context map
        Object newUserContext = tracer.traceRoundTrip(sequence, traceContext, currentUserContext.get(tracerName));
        currentUserContext.put(tracerName, newUserContext);
      } else {
        // the listener does not fail if the tracer is unknow, it is possible to
        // enable a tracer and register it later
        logger.log(Level.WARNING, "Could not find registered tracer with name: " + tracer);
      }
    }

    // return the new user context
    return currentUserContext;
  }


  @Override
  @SuppressWarnings("unchecked")
  public Object onExecutionEventReceived(JdbcExecutionEvent event, Object userContext, Object... params) {
    if (!configuration.getEnabled()) { return null;}

    // Cast the userContext to the map this listener uses, or create a new one
    // if it is being used for the first time. This is the return value of the 
    // method, and will be send back by the driver on the next event.
    Map<String, Object> currentUserContext = userContext == null ? 
        new HashMap<>() : (Map<String, Object>)userContext;
    
    // loop through all the enabled tracers
    for (String tracerName : configuration.getEnabledTracersAsList()) {
      ObservabilityTracer tracer = configuration.getTracer(tracerName);
      if (tracer != null) {
        // call the tracer's execution event with the tracer's context and store
        // the new user context returned by the tracer in the user context map
        Object newUserContext = tracer.traceExecutionEvent(event, currentUserContext.get(tracerName), params);
        currentUserContext.put(tracerName, newUserContext);
      } else {
        // the listener does not fail if the tracer is unknow, it is possible to
        // enable a tracer and register it later
        logger.log(Level.WARNING, "Could not find registered tracer with name: " + tracer);
      }
    }

    // return the new user context
    return currentUserContext;
  }

  @Override
  public boolean isDesiredEvent(JdbcExecutionEvent event) {
    // Accept all events
    return true;
  }


  /**
   * Returns the MBean object name assiciated with the configuration of the 
   * listener.
   * 
   * @return the MBean object name.
   */
  public ObjectName getMBeanObjectName() {
    return mBeanObjectName;
  }

  /**
   * Returns the listener's configuration.
   * 
   * @return the configuration instance associated to the listener.
   */
  public ObservabilityConfiguration getObservabilityConfiguration() {
    return configuration;
  }


  /**
   * Returns the trace event listener identified by the given unique idetifier.
   * 
   * @param uniqueIdentifier the unique identifier, if no unique identifier was 
   * provided as a connection property, the unique identifier is "default".
   * @return the trace event listener identified by the given unique idetifier, 
   * or {@code null} if no trace event listener with that unique idetifier was 
   * found.
   */
  public static ObservabilityTraceEventListener getTraceEventListener(String uniqueIdentifier) {
    INSTANCES_LOCK.lock();
    try {
      return INSTANCES.get(uniqueIdentifier);
    } finally {
      INSTANCES_LOCK.unlock();
    }
  }

  /**
   * Gets or creates an instance of {@link ObservabilityTraceEventListener} 
   * associated to the name.
   * @param uniqueIdentifier the name of the listener instance.
   * @param configurationType configuration type for backward compatibility.
   * 
   * @return an instance of {@link ObservabilityTraceEventListener}.
   */
  static ObservabilityTraceEventListener getOrCreateInstance(String uniqueIdentifier, 
      ObservabilityConfigurationType configurationType) {
    INSTANCES_LOCK.lock();
    try {
      ObservabilityTraceEventListener instance = INSTANCES.computeIfAbsent(uniqueIdentifier, n -> {
        return new ObservabilityTraceEventListener(n, configurationType);
      });
      registerConfigurationUsage(uniqueIdentifier, configurationType);
      return instance;
    } finally {
      INSTANCES_LOCK.unlock();
    }
  }

  private static void registerConfigurationUsage(String uniqueIdentifier, 
      ObservabilityConfigurationType configurationType) {
    INSTANCES_LOCK.lock();
    try {
      LRU_CONFIGURATION.remove(uniqueIdentifier);
      LRU_CONFIGURATION.addFirst(uniqueIdentifier);
      if (LRU_CONFIGURATION.size() > MAX_CONFIGURATION_COUNT) {
        String leastRecentlyUsed = LRU_CONFIGURATION.removeLast();
        // unregister MBean try both configuraiton types
        final String mBeanNameOtel = String.format(MBEAN_OBJECT_NAME_OTEL, uniqueIdentifier);
        final String mBeanNameDefault = String.format(MBEAN_OBJECT_NAME, uniqueIdentifier);
        try {
          ObjectName mBeanObjectName = new ObjectName(mBeanNameDefault);
          if (server.isRegistered(mBeanObjectName)) {
            server.unregisterMBean(mBeanObjectName);
            logger.log(Level.FINEST, "MBean and tracers unregistered");
          }
          mBeanObjectName = new ObjectName(mBeanNameOtel);
          if (server.isRegistered(mBeanObjectName)) {
            server.unregisterMBean(mBeanObjectName);
            logger.log(Level.FINEST, "MBean and tracers unregistered");
          }

        } catch (RuntimeOperationsException | MBeanRegistrationException | 
            MalformedObjectNameException | InstanceNotFoundException e) {
          logger.log(Level.WARNING, "Could not unregister MBean", e);
        }
        // remove configuration
        INSTANCES.remove(leastRecentlyUsed);
      }
    } finally {
      INSTANCES_LOCK.unlock();
    }
  }

}
