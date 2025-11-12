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
package oracle.jdbc.provider.observability.tracers;

import java.util.EnumMap;
import java.util.Map;

import oracle.jdbc.TraceEventListener.JdbcExecutionEvent;
import oracle.jdbc.TraceEventListener.Sequence;
import oracle.jdbc.TraceEventListener.TraceContext;
import oracle.jdbc.provider.observability.ObservabilityTraceEventListener;

/**
 * This interface must be implemented by all Observability tracers.
 */
public interface ObservabilityTracer {

    /**
   * Map containing the number of parameters expected for each execution event
   */
  Map<JdbcExecutionEvent, Integer> EXECUTION_EVENTS_PARAMETERS 
      = new EnumMap<JdbcExecutionEvent, Integer>(JdbcExecutionEvent.class) {
    {
      put(JdbcExecutionEvent.AC_REPLAY_STARTED, 3);
      put(JdbcExecutionEvent.AC_REPLAY_SUCCESSFUL, 3);
      put(JdbcExecutionEvent.VIP_RETRY, 8);
    }
  };

  /**
   * Returns the unique name of the tracer.
   * @return the unique name of the tracer.
   */
  String getName();


  /**
   * Called by {@link ObservabilityTraceEventListener} when a round trip event
   * is received.
   * 
   * @param sequence BEFORE if before the round trip, AFTER if after the round 
   * trip
   * @param traceContext Information about the round trip. Valid only during the
   * call
   * @param userContext Result of previous call on this Connection or null if no
   * previous call or if observability was disabled since the previous call.
   * @return a user context object that is passed to the next call on this 
   * Connection. May be null.
   */
  Object traceRoundTrip(Sequence sequence, TraceContext traceContext, Object userContext);

  /**
   * Called by {@link ObservabilityTraceEventListener} when an execution event
   * is received.
   * 
   * @param event the event.
   * @param userContext the result of the previous call or null if no previous 
   * call has been made.
   * @param params event specific parameters.
   * @return a user context object that is passed to the next call for the same 
   * type of event. May be null.
   */
  Object traceExecutionEvent(JdbcExecutionEvent event, Object userContext, Object... params);

}
