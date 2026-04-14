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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import oracle.jdbc.provider.observability.tracers.ObservabilityTracer;
import oracle.jdbc.provider.observability.tracers.jfr.JFRTracer;
import oracle.jdbc.provider.observability.tracers.otel.OTelTracer;

/**
 * <p>
 * Implementation of {@link ObservabilityConfigurationMBean} that allows to 
 * configure the Oracle JDBC Observability Provider. The system properties that
 * can be used to configure the Observability Provider depend on the provider 
 * used:
 * </p>
 * If {@link ObservabilityTraceEventListenerProvider} is being used:
 * <ul>
 * <li>{@link ObservabilityConfiguration#ENABLED_TRACERS}: comma separated list 
 * of enabled tracers, default "JFR,OTEL"</li>
 * <li>{@link ObservabilityConfiguration#SENSITIVE_DATA_ENABLED}: true if 
 * sensitive data is enabled, default false</li>
 * </ul>
 * If {@link OpenTelemetryTraceEventListenerProvider} is being used:
 * <ul>
 * <li>{@link ObservabilityConfiguration#OPEN_TELEMETRY_TRACE_EVENT_LISTENER_ENABLED}: 
 * true if OTEL tracer is enabled, otherwise false. Default true.</li>
 * <li>{@link ObservabilityConfiguration#OPEN_TELEMETRY_TRACE_EVENT_LISTENER_SENSITIVE_ENABLED}: 
 * true if sensitive data is enabled, default false</li>
 * </ul>
 */
public class ObservabilityConfiguration implements ObservabilityConfigurationMBean {

  /**
   * <p>
   * System property used to enabled/disable tracers. The value of this system property should be a comma separated list
   * of tracers to enable. 
   * </p>
   * <p>
   * This extension implements two tracers:
   * </p>
   * <ul>
   * <li>OTEL: which exports traces to Open Telemetry {@link OTelTracer}</li>
   * <li>JFR: which exports traces to Java Flight recorder {@link JFRTracer}</li>
   * </ul>
   * <p>
   * By default all tracers will be enabled.
   */
  public static final String ENABLED_TRACERS = "oracle.jdbc.provider.observability.enabledTracers";

  /**
   * System property used to enable/disable exporting sensitive data. Set the property to true to enable sensitive data. 
   * By default exporting sensitive data is disabled.
   */
  public static final String SENSITIVE_DATA_ENABLED = "oracle.jdbc.provider.observability.sensitiveDataEnabled";

  /**
   * This property is kept for backward compatibility. It is used to enable/disable the previous verison of the provider: 
   * "open-telemetry-trace-event-listener-provider".
   */
  public static final String OPEN_TELEMETRY_TRACE_EVENT_LISTENER_ENABLED = "oracle.jdbc.provider.opentelemetry.enabled";
  
  /**
   * This property is kept for backward compatibility. It allows to enable/disable sensitive data when using the previous
   * version of the provider: "open-telemetry-trace-event-listener-provider". 
   */
  public static final String OPEN_TELEMETRY_TRACE_EVENT_LISTENER_SENSITIVE_ENABLED = "oracle.jdbc.provider.opentelemetry.sensitive-enabled";

  /**
   * OpenTelemetry semantic convention stability opt-in environment variable.
   * Accepts a comma-separated list of values including:
   * "database" (new stable conventions only),
   * "database/dup" (both old and new conventions),
   * or empty/null (old conventions only)
   */
  public static final String OTEL_SEMCONV_STABILITY_OPT_IN = "OTEL_SEMCONV_STABILITY_OPT_IN";


  /**
   * Default values
   */
  private static final String DEFAULT_ENABLED_TRACERS = "OTEL,JFR";
  private static final String DEFAULT_SENSITIVE_DATA_ENABLED = "false";
  private static final String DEFAULT_OPEN_TELEMETRY_ENABLED = "true";
  private String semconvOptIn = "";


  /**
   * Lock used to ensure that only one thread can access the configuration at a time.
   */
  private static final ReentrantLock observabilityConfigurationLock = new ReentrantLock();

  /**
   * Indicates whether traces are enabled
   */
  private boolean enabled = true;

  /**
   * Indicates whether sensitive data is enabled
   */
  private boolean sensitiveDataEnabled;

  /**
   * List of enabled tracers
   */
  private List<String> enabledTracers = new ArrayList<>();

  /**
   * Maps registered tracer's name to its instance.
   */
  Map<String, ObservabilityTracer> registeredTracers = new HashMap<>(2, 1);


  /**
   * Types of configuration. For backward compatibility allows to use OTEL for
   * Oracle JDBC Open Telemetry Provider configuration properties.
   */
  public enum ObservabilityConfigurationType {
    /**
     * Use Oracle JDBC Open Telemetry Provider configuration properties
     */
    OTEL,
    /**
     * USE Oracle JDBC Observability Provider configuration properties
     */
    OBSERVABILITY
  }

  /**
   * Constructor
   */
  public ObservabilityConfiguration() { 
    this(ObservabilityConfigurationType.OBSERVABILITY);
  }

  /**
   * Constructor used by {@link ObservabilityTraceEventListener} to create a configuration.
   * 
   * @param configurationType indicates which system properties to use. When 
   * {@link ObservabilityConfigurationType#OTEL}, the previous
   * verison of system properties are used.
   */
  ObservabilityConfiguration(ObservabilityConfigurationType configurationType) {  
    String enabledTracers = DEFAULT_ENABLED_TRACERS;
    String sensitiveDataEnabled = DEFAULT_SENSITIVE_DATA_ENABLED;
    String optIn = System.getenv(OTEL_SEMCONV_STABILITY_OPT_IN);

    if (ObservabilityConfigurationType.OBSERVABILITY.equals(configurationType)) {
      enabledTracers = System.getProperty(ENABLED_TRACERS, DEFAULT_ENABLED_TRACERS);
      sensitiveDataEnabled = System.getProperty(SENSITIVE_DATA_ENABLED, DEFAULT_SENSITIVE_DATA_ENABLED);
    } else {
      String otelEnabled = System.getProperty(OPEN_TELEMETRY_TRACE_EVENT_LISTENER_ENABLED, DEFAULT_OPEN_TELEMETRY_ENABLED);
      if (otelEnabled != null) {
        enabledTracers = "OTEL";
        this.enabled = Boolean.parseBoolean(otelEnabled);
      }
      String otelSensitiveDataEnabled = System.getProperty(OPEN_TELEMETRY_TRACE_EVENT_LISTENER_SENSITIVE_ENABLED, DEFAULT_SENSITIVE_DATA_ENABLED);
      if(otelSensitiveDataEnabled != null) {
        sensitiveDataEnabled = otelSensitiveDataEnabled;
      }
    }

    setEnabledTracers(enabledTracers);
    setSensitiveDataEnabled(Boolean.parseBoolean(sensitiveDataEnabled));
    setSemconvOptIn(optIn);

  }

  /**
   * Returns true if the provider is enabled, otherwise false.
   * 
   * @return true if the provider is enabled, otherwise false.
   */
  @Override
  public boolean getEnabled() {
    observabilityConfigurationLock.lock();
    try {
      return  enabled;
    } finally {
      observabilityConfigurationLock.unlock();
    }
  }

  /**
   * Enables/disables the provider.
   * 
   * @param enabled true to enable the provider, otherwise false.
   */
  @Override
  public void setEnabled(boolean enabled) {
    observabilityConfigurationLock.lock();
    try {
      this.enabled = enabled;
    } finally {
      observabilityConfigurationLock.unlock();
    }
  }

  /**
   * Returns a comma separated list of enabled tracers. Not {@code null}.
   */
  @Override
  public String getEnabledTracers() {
    observabilityConfigurationLock.lock();
    try {
      return enabledTracers == null ? 
          "" : 
          enabledTracers.stream().collect(Collectors.joining(","));
    } finally {
      observabilityConfigurationLock.unlock();
    }
  }

  /**
   * Enables the tracers. 
   * <p>
   * This extension implements two tracers:
   * </p>
   * <ul>
   * <li>OTEL: which exports traces to Open Telemetry {@link OTelTracer}</li>
   * <li>JFR: which exports traces to Java Flight recorder {@link JFRTracer}</li>
   * </ul>
   * <p>
   * Other tracer can be registered using the {@link ObservabilityConfiguration#registeredTracers}
   * method.
   * </p>
   * @param tracers comma separated list of enabled tracers.
   */
  @Override
  public void setEnabledTracers(String tracers){
    observabilityConfigurationLock.lock();
    try {
      String[] items = tracers.replaceAll("\\s", "").split(",");
      enabledTracers = Arrays.asList(items);
    } finally {
      observabilityConfigurationLock.unlock();
    }

  }

  /**
   * Returns true if sensitive data is enabled, otherwise false.
   * @return true if sensitive data is enabled, otherwise false.
   */
  @Override
  public boolean getSensitiveDataEnabled() {
    observabilityConfigurationLock.lock();
    try {
      return sensitiveDataEnabled;
    } finally {
      observabilityConfigurationLock.unlock();
    }
  }

  /**
   * Enables/disables sensitive data.
   * 
   * @param sensitiveDataEnabled true to enable sensitive data, otherwise false.
   */
  @Override
  public void setSensitiveDataEnabled(boolean sensitiveDataEnabled) {
    observabilityConfigurationLock.lock();
    try {
      this.sensitiveDataEnabled = sensitiveDataEnabled;
    } finally {
      observabilityConfigurationLock.unlock();
    }
  }

  /**
   * Returns the OpenTelemetry semantic convention stability opt-in configuration.
   *
   * @return the current semantic convention mode, never {@code null}
   */
  @Override
  public String getSemconvOptIn() {
    observabilityConfigurationLock.lock();
    try {
      return semconvOptIn == null ? "" : semconvOptIn;
    } finally {
      observabilityConfigurationLock.unlock();
    }
  }

  /**
   * Sets the OpenTelemetry semantic convention stability opt-in configuration.
   *
   * @param optIn the semantic convention mode to set; {@code null} is
   * treated as empty string
   */
  @Override
  public void setSemconvOptIn(String optIn) {
    observabilityConfigurationLock.lock();
    try {
      this.semconvOptIn = optIn == null ? "" : optIn;
    } finally {
      observabilityConfigurationLock.unlock();
    }
  }


  /**
   * Returns a list of enabled tracers.
   * @return then list of enabled tracers.
   */
  public List<String> getEnabledTracersAsList() {
    observabilityConfigurationLock.lock();
    try {
      return enabledTracers;
    } finally {
      observabilityConfigurationLock.unlock();
    }
  }

  /**
   * Returns the tracer registered with that name.
   * @param tracerName the name of the tracer.
   * @return returns the registered tracer that was registered using that name.
   */
  public ObservabilityTracer getTracer(String tracerName) {
    return registeredTracers.get(tracerName);
  }

  /**
   * Registeres a tracer.
   * 
   * @param tracer the tracer to register
   */
  public void registerTracer(ObservabilityTracer tracer) {
    observabilityConfigurationLock.lock();
    try {
      registeredTracers.put(tracer.getName(), tracer);
    } finally {
      observabilityConfigurationLock.unlock();
    }
  }


}
