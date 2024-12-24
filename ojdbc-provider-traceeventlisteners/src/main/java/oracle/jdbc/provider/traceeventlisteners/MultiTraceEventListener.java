package oracle.jdbc.provider.traceeventlisteners;

import java.util.ArrayList;
import java.util.List;

import oracle.jdbc.TraceEventListener;

public class MultiTraceEventListener implements TraceEventListener {
  List<TraceEventListener> listeners;

  public MultiTraceEventListener(List<TraceEventListener> listeners) {
    this.listeners = listeners;
  }

  @Override
  public Object roundTrip(Sequence sequence, TraceContext traceContext, Object clientContext) {
    List<Object> clientContextList;
    if (clientContext == null) {
      clientContextList = new ArrayList<>(listeners.size());
    } else {
      clientContextList = (List<Object>)clientContext;
    }
    System.out.println(sequence.toString());
    System.out.println("Client context: " + clientContext);
    System.out.println("Client context list: " + clientContextList);
    System.out.println("Trace context: " + traceContext);
    System.out.println("Database operation: " + traceContext.databaseOperation());
    for (int i = 0; i < listeners.size(); i++) {
      if (clientContextList.size() <= i) {
        System.out.println("Adding element at : " + i + " current list size is: " + clientContextList.size());
        clientContextList.add(null);
      }
      Object context = listeners.get(i).roundTrip(sequence, traceContext, clientContextList.get(i));
      clientContextList.set(i, context);
    }
    System.out.println("Client context list size: " + clientContextList.size());
    System.out.println("Client context list: " + clientContextList);
    return clientContextList;
  }

  @Override
  public Object onExecutionEventReceived(JdbcExecutionEvent event, Object userContext, Object... params) {
    if (userContext == null) {
      userContext = new ArrayList(listeners.size());
    }
    for (int i = 0; i < listeners.size(); i++) {
      ((List)userContext).set(i, listeners.get(i).onExecutionEventReceived(event, userContext, params));
    }
    return userContext;
  }

  @Override
  public boolean isDesiredEvent(JdbcExecutionEvent event) {
    return true;
  }
}
