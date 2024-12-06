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

import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.jdbc.TraceEventListener;
import jdk.jfr.Event;

/**
 * TraceEventListener that creates Java Flight Recorder events for each
 * round trip.
 */
public class FlightRecorderTraceEventListener implements TraceEventListener {

  private final boolean enabled;
  private final boolean enableSensitiveData;

  private static Logger logger = Logger.getLogger(FlightRecorderTraceEventListener.class.getName());

  /**
   * Constructor
   * @param enable if false, no events are created.
   * @param enableSensitiveData if true, events will contain sensitive information like
   * SQL statements and usernames.
   */
  public FlightRecorderTraceEventListener(boolean enable, boolean enableSensitiveData) {
    this.enabled = enable;
    this.enableSensitiveData = enableSensitiveData;
    logger.log(Level.INFO, "FlightRecorderTraceEventListener started enabled " + this.enabled + " sensitive data enabled " + this.enableSensitiveData);
  }

  /**
   * Implements TraceEventListener.roundTrip and creates Java Flight Recorder events.
   * {@inheritDoc}
   */
  @Override
  public Object roundTrip(Sequence sequence, TraceContext traceContext, Object userContext) {
    if (!enabled) {
      logger.log(Level.FINE, "FlightRecorderTraceEventListener is disabled");
      return null;
    }
    if (sequence.equals(Sequence.BEFORE)) {
      logger.log(Level.FINE, "Received before event");
      Event event = OracleEventFactory.createEvent(
        traceContext.databaseOperation());
      event.begin();
      return event;
    } else {
      logger.log(Level.FINE, "Received after event");
      if (userContext != null) {
        logger.log(Level.FINE, "Received after event not empty");
        Event event = (Event) userContext;
        event.set(0, traceContext.getConnectionId());
        event.set(1, traceContext.databaseOperation());
        event.set(2, traceContext.tenant());
        event.set(3, traceContext.getSqlId());
        if (enableSensitiveData) {
          event.set(4, traceContext.originalSqlText());
          event.set(5, traceContext.actualSqlText());
          event.set(6, traceContext.user());
        } 
        event.end();
        event.commit();
      }
    }
    return null;
  }

}

