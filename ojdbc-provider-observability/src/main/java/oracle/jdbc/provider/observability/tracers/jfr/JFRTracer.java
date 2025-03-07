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
package oracle.jdbc.provider.observability.tracers.jfr;

import jdk.jfr.Event;
import oracle.jdbc.TraceEventListener.JdbcExecutionEvent;
import oracle.jdbc.TraceEventListener.Sequence;
import oracle.jdbc.TraceEventListener.TraceContext;
import oracle.jdbc.provider.observability.ObservabilityConfiguration;
import oracle.jdbc.provider.observability.tracers.ObservabilityTracer;
import oracle.jdbc.provider.observability.tracers.jfr.JFREventFactory.RoundTripEvent;

/**
 * {@link ObservabilityTracer} for tracing Java Flight Recorder events.
 */
public class JFRTracer implements ObservabilityTracer{

  /**
   * Configuraiton
   */
  private final ObservabilityConfiguration configuration;

  /**
   * Creates a new instance.
   * 
   * @param configuration the configuraiton.
   */
  public JFRTracer(ObservabilityConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public String getName() {
    return "JFR";
  }

  @Override
  public Object traceRoundTrip(Sequence sequence, TraceContext traceContext, Object userContext) {
    if (sequence.equals(Sequence.BEFORE)) {
      // Create the event and start measuring event duration
      RoundTripEvent event = JFREventFactory.createJFRRoundTripEvent(traceContext, configuration);
      event.begin();
      return event;
    } else {
      if (userContext != null) {
        RoundTripEvent event = (RoundTripEvent) userContext;
        // set event attributes
        event.setValues(traceContext, configuration);
        // stop the measuring event durating and commit
        event.commit();
      }
    }
    return null;
  }

  @Override
  public Object traceExecutionEvent(JdbcExecutionEvent event, Object userContext, Object... params) {
    // Create event and commit
    Event executionEvent = JFREventFactory.createExecutionEvent(event, params);
    executionEvent.begin();
    executionEvent.commit();
    //Return previous user context
    return userContext;
  }

}
