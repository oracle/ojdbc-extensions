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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

import javax.management.MBeanServer;

import oracle.jdbc.TraceEventListener;
import oracle.jdbc.provider.observability.ObservabilityConfiguration.ObservabilityConfigurationType;
import oracle.jdbc.spi.TraceEventListenerProvider;

/**
 * Implementation of Oracle JDBC {@link TraceEventListenerProvider} for 
 * {@link ObservabilityTraceEventListener}.
 */
public class ObservabilityTraceEventListenerProvider implements TraceEventListenerProvider {

  /**
   * Provider name
   */
  private static final String PROVIDER_NAME = "observability-trace-event-listener-provider";

  /**
   * Name Parameter name, identifies the listener
   */
  private static final String NAME_PARAMETER_NAME = "NAME";


  /**
   * Name Parameter, identifies the listener
   */
  protected static final Parameter nameParameter = new Parameter() {

    @Override
    public boolean isSensitive() {
      return false;
    }

    @Override
    public String name() {
      return NAME_PARAMETER_NAME;
    }
    
  };


  /**
   * Constructs a new instance of ObservabilityTraceEventListenerProvider. This
   * constructor will be called by the driver's service provider to create a new
   * instance.
   */
  public ObservabilityTraceEventListenerProvider() { 


  }

  @Override
  public TraceEventListener getTraceEventListener(Map<Parameter, CharSequence> map) {
    String name = map.get(nameParameter).toString();
    return ObservabilityTraceEventListener.getOrCreateInstance(name, ObservabilityConfigurationType.OBSERVABILITY);
  }

  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  @Override
  public Collection<? extends Parameter> getParameters() {
    return Collections.singletonList(nameParameter);
  }

}
