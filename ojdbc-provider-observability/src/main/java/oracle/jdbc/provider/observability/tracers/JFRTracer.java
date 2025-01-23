package oracle.jdbc.provider.observability.tracers;

import java.util.ArrayList;
import java.util.List;

import oracle.jdbc.DatabaseFunction;
import oracle.jdbc.TraceEventListener.JdbcExecutionEvent;
import oracle.jdbc.TraceEventListener.Sequence;
import oracle.jdbc.TraceEventListener.TraceContext;
import oracle.jdbc.provider.observability.configuration.ObservabilityConfiguration;
import jdk.jfr.AnnotationElement;
import jdk.jfr.Event;
import jdk.jfr.EventFactory;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Category;
import jdk.jfr.ValueDescriptor;

public class JFRTracer implements ObservabilityTracer{

  public JFRTracer() {}

  @Override
  public Object traceRoudtrip(Sequence sequence, TraceContext traceContext, Object userContext) {
    if (sequence.equals(Sequence.BEFORE)) {
      Event event = createEvent(
        traceContext.databaseFunction());
      event.begin();
      return event;
    } else {
      if (userContext != null) {
        Event event = (Event) userContext;
        event.set(0, traceContext.getConnectionId());
        event.set(1, traceContext.databaseOperation());
        event.set(2, traceContext.tenant());
        event.set(3, traceContext.getSqlId());
        if (ObservabilityConfiguration.getInstance().getSensitiveDataEnabled()) {
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

  @Override
  public Object traceExecutionEvent(JdbcExecutionEvent event, Object userContext, Object... params) {
    return null;
  }

    /**
   * Creates an event for a given database operation.
   * @param databaseFunction The database function originating the round trip.
   * @return returns a Java Flight Recorder Event containing the following 
   * fields:
   * <ul>
   *  <li>ConnectionID</li>
   *  <li>DatabaseOperation</li>
   *  <li>OriginalSqlText</li>
   *  <li>ActualSqlText</li>
   *  <li>User</li>
   * </ul>
   */
  private static Event createEvent(DatabaseFunction databaseFunction) {
    String eventName = "oracle.jdbc.provider.jfr.roundtrip." + databaseFunction.toString();
    List<AnnotationElement> eventAnnotations = new ArrayList<AnnotationElement>();
    eventAnnotations
        .add(new AnnotationElement(Name.class, eventName));
    eventAnnotations.add(new AnnotationElement(Label.class, databaseFunction.getDescription()));
    eventAnnotations.add(new AnnotationElement(Category.class,  new String[] { "Oracle JDBC", "Round trips" }));

    List<ValueDescriptor> fields = new ArrayList<ValueDescriptor>();
    fields.add(new ValueDescriptor(String.class, "Connection_ID"));
    fields.add(new ValueDescriptor(String.class, "Database_operation"));
    fields.add(new ValueDescriptor(String.class, "Database_tenant"));
    fields.add(new ValueDescriptor(String.class, "SQL_ID"));
    fields.add(new ValueDescriptor(String.class, "Original_SQL_text"));
    fields.add(new ValueDescriptor(String.class, "Actual_SQL_text"));
    fields.add(new ValueDescriptor(String.class, "Database_user"));

    EventFactory f = EventFactory.create(eventAnnotations, fields);
    return f.newEvent();
  }


}
