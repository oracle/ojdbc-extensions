/*
 ** Copyright (c) 2026 Oracle and/or its affiliates.
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
 ** one is included with the Software (each a "Larger Work" to which the
 ** Software is contributed by such licensors),
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

package oracle.ucp.provider.observability;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Runtime configuration for UCP provider listeners.
 */
public final class UCPObservabilityConfiguration
  implements UCPObservabilityConfigurationMBean {

  /**
   * Runtime listener name for the Java Flight Recorder backend.
   */
  public static final String JFR = "JFR";

  /**
   * Runtime listener name for the OpenTelemetry backend.
   */
  public static final String OTEL = "OTEL";

  private static final String OBJECT_NAME =
    "com.oracle.ucp.provider.observability:type=UCPObservabilityConfiguration";

  private static final Logger LOGGER =
    Logger.getLogger(UCPObservabilityConfiguration.class.getName());

  private static final UCPObservabilityConfiguration INSTANCE =
    new UCPObservabilityConfiguration();

  private volatile boolean enabled = true;
  private volatile Set<String> enabledListeners =
    immutableListeners(JFR, OTEL);

  static {
    INSTANCE.registerMBean();
  }

  /**
   * Creates the JVM-wide provider configuration.
   */
  private UCPObservabilityConfiguration() {}

  /**
   * Returns the JVM-wide provider configuration.
   *
   * @return provider configuration
   */
  public static UCPObservabilityConfiguration getInstance() {
    return INSTANCE;
  }

  /**
   * Returns whether provider emission is globally enabled.
   *
   * @return true when provider emission is enabled
   */
  @Override
  public boolean getEnabled() {
    return enabled;
  }

  /**
   * Enables or disables all provider emission.
   *
   * @param enabled true to enable emission, false to disable it
   */
  @Override
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Returns the enabled listener backends as a comma-separated list.
   *
   * @return enabled listener backend names
   */
  @Override
  public String getEnabledListeners() {
    return enabledListeners.stream().collect(Collectors.joining(","));
  }

  /**
   * Sets the enabled listener backends.
   *
   * <p>The accepted backend names are {@link #JFR} and {@link #OTEL}.
   * Unknown names are ignored. The parsed set is replaced atomically for
   * lock-free reads from UCP event threads.
   *
   * @param listeners comma-separated listener backend names
   */
  @Override
  public void setEnabledListeners(String listeners) {
    enabledListeners = parseListeners(listeners);
  }

  /**
   * Returns whether the named listener backend may emit telemetry.
   *
   * @param listenerName listener backend name
   * @return true when provider emission is globally enabled and the backend is
   * enabled
   */
  public boolean isListenerEnabled(String listenerName) {
    return enabled && enabledListeners.contains(listenerName);
  }

  /**
   * Resets the JVM-wide configuration to defaults for tests.
   */
  static void resetForTest() {
    INSTANCE.enabled = true;
    INSTANCE.enabledListeners = immutableListeners(JFR, OTEL);
  }

  /**
   * Registers the runtime configuration MBean with the platform MBean server.
   */
  private void registerMBean() {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();

    try {
      ObjectName objectName = new ObjectName(OBJECT_NAME);
      if (!server.isRegistered(objectName)) {
        server.registerMBean(this, objectName);
      }
    }
    catch (InstanceAlreadyExistsException | MBeanRegistrationException |
           MalformedObjectNameException | NotCompliantMBeanException e) {
      LOGGER.log(Level.WARNING,
        "Could not register UCP provider configuration MBean", e);
    }
  }

  /**
   * Parses a comma-separated listener list into an immutable backend set.
   *
   * @param listeners comma-separated listener backend names
   * @return immutable set containing accepted backend names
   */
  private static Set<String> parseListeners(String listeners) {
    LinkedHashSet<String> parsed = new LinkedHashSet<>();

    if (listeners != null) {
      for (String listener : listeners.split(",")) {
        String normalized =
          listener.trim().toUpperCase(Locale.ROOT);

        if (JFR.equals(normalized) || OTEL.equals(normalized)) {
          parsed.add(normalized);
        }
      }
    }

    return Collections.unmodifiableSet(parsed);
  }

  /**
   * Creates an immutable backend set preserving insertion order.
   *
   * @param listeners listener backend names
   * @return immutable listener backend set
   */
  private static Set<String> immutableListeners(String... listeners) {
    return Collections.unmodifiableSet(
      new LinkedHashSet<>(Arrays.asList(listeners)));
  }
}
