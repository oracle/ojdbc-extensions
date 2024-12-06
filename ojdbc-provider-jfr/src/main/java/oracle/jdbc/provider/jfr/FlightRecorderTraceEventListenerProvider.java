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

package oracle.jdbc.provider.jfr;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import oracle.jdbc.TraceEventListener;
import oracle.jdbc.spi.TraceEventListenerProvider;

/**
 * Implements a TraceEventListenerProvider for FlightRecorderTraceEventListener. 
 * By default sensitive attributes like SQL statements and database used will 
 * be added to the event, the configureation property "enableSensitiveData" can
 * be used to enable sensitive attributes to be added to the event.
 * To enable sensitive data set the connection property 
 * oracle.jdbc.provider.traceEventListener.enableSensitiveData to true.
*/
public class FlightRecorderTraceEventListenerProvider implements TraceEventListenerProvider {

  /**
   * Set this value to the connection property {@link oracle.jdbc.OracleConnection#CONNECTION_PROPERTY_PROVIDER_TRACE_EVENT_LISTENER}
   * to  indicate that this provider should be used.
   */
  public static final String TRACE_EVENT_LISTENER_NAME = "java-flight-recorder-trace-event-listener-provider";

  private static final Parameter ENABLE_SENSITIVE_DATA = new Parameter() {
    @Override
    public boolean isSensitive() {
      return false;
    }
    @Override
    public String name() {
      return "enableSensitiveData";
    }
  };

  @Override
  public String getName() {
    return TRACE_EVENT_LISTENER_NAME;
  }

  @Override
  public Collection<? extends Parameter> getParameters() {

    List<Parameter> parameters = Collections.singletonList(ENABLE_SENSITIVE_DATA);
    return parameters;
  }

  @Override
  public TraceEventListener getTraceEventListener(Map<Parameter, CharSequence> parametersMap) {
    boolean enableSensitiveData = getBooleanParameterValue(ENABLE_SENSITIVE_DATA, parametersMap);
    return new FlightRecorderTraceEventListener(enableSensitiveData);
  }

  private boolean getBooleanParameterValue(Parameter parameter, Map<Parameter, CharSequence> parametersMap) {
    CharSequence value = parametersMap.get(parameter);
    if (value != null) {
      return Boolean.valueOf(value.toString());
    }
    return false;

  }
  
}
