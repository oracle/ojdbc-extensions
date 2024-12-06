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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import jdk.jfr.Event;
import oracle.jdbc.DatabaseFunction;
import oracle.jdbc.TraceEventListener.Sequence;
import oracle.jdbc.TraceEventListener.TraceContext;

public class FlightRecorderTraceEventListenerTest {
  private static MockedStatic<OracleEventFactory> eventFactory;
  private static Event event = Mockito.mock(Event.class);
  private static TraceContext traceContext = Mockito.mock(TraceContext.class);

  @BeforeAll
  public static void createStaticMock() {
    eventFactory = Mockito.mockStatic(OracleEventFactory.class);

    Mockito.when(traceContext.actualSqlText()).thenReturn("Actual SQL");
    Mockito.when(traceContext.getConnectionId()).thenReturn("ConnectionId");
    Mockito.when(traceContext.databaseOperation()).thenReturn("Execute statement");
    Mockito.when(traceContext.getSqlId()).thenReturn("SqlID");
    Mockito.when(traceContext.isCompletedExceptionally()).thenReturn(false);
    Mockito.when(traceContext.originalSqlText()).thenReturn("Original SQL");
    Mockito.when(traceContext.tenant()).thenReturn("tenant");
    Mockito.when(traceContext.user()).thenReturn("user");
    Mockito.when(traceContext.databaseFunction()).thenReturn(DatabaseFunction.EXECUTE_QUERY);
  }

  @BeforeEach
  public void resetMocks() {
    eventFactory.reset();
    Mockito.reset(event);
    eventFactory.when(() -> 
      OracleEventFactory.createEvent(DatabaseFunction.EXECUTE_QUERY)
    ).thenReturn(event);    
  }

  @Test
  public void roundTripSensitiveSuccessTest() throws Exception {
    FlightRecorderTraceEventListener traceEventListener = 
    new FlightRecorderTraceEventListener(true);
    traceEventListener.roundTrip(Sequence.BEFORE, traceContext, traceEventListener);
    // check
    eventFactory.verify(
      () -> OracleEventFactory.createEvent(DatabaseFunction.EXECUTE_QUERY), 
      Mockito.times(1));
   
    traceEventListener.roundTrip(Sequence.AFTER, traceContext, event);
    eventFactory.verify(
      () -> OracleEventFactory.createEvent(DatabaseFunction.EXECUTE_QUERY), 
      Mockito.times(1));
      Mockito.verify(event,Mockito.times(1)).set(0, traceContext.getConnectionId());
      Mockito.verify(event,Mockito.times(1)).set(1, traceContext.databaseOperation());
      Mockito.verify(event,Mockito.times(1)).set(2, traceContext.tenant());
      Mockito.verify(event,Mockito.times(1)).set(3, traceContext.getSqlId());
      Mockito.verify(event,Mockito.times(1)).set(4, traceContext.originalSqlText());
      Mockito.verify(event,Mockito.times(1)).set(5, traceContext.actualSqlText());
      Mockito.verify(event,Mockito.times(1)).set(6, traceContext.user());
    }

  @Test
  public void roundTripNotSensitiveSuccessTest() throws Exception {
    FlightRecorderTraceEventListener traceEventListener = 
    new FlightRecorderTraceEventListener(false);
    traceEventListener.roundTrip(Sequence.BEFORE, traceContext, null);
    // check
    eventFactory.verify(
      () -> OracleEventFactory.createEvent(DatabaseFunction.EXECUTE_QUERY), 
      Mockito.times(1));
  
    traceEventListener.roundTrip(Sequence.AFTER, traceContext, event);
    Mockito.verify(event,Mockito.times(1)).set(0, traceContext.getConnectionId());
    Mockito.verify(event,Mockito.times(1)).set(1, traceContext.databaseOperation());
    Mockito.verify(event,Mockito.times(1)).set(2, traceContext.tenant());
    Mockito.verify(event,Mockito.times(1)).set(3, traceContext.getSqlId());
    Mockito.verify(event,Mockito.times(0)).set(4, traceContext.originalSqlText());
    Mockito.verify(event,Mockito.times(0)).set(5, traceContext.actualSqlText());
    Mockito.verify(event,Mockito.times(0)).set(6, traceContext.user());
  }

}
