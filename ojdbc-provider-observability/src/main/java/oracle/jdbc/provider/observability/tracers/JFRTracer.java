package oracle.jdbc.provider.observability.tracers;

import oracle.jdbc.TraceEventListener.JdbcExecutionEvent;
import oracle.jdbc.TraceEventListener.Sequence;
import oracle.jdbc.TraceEventListener.TraceContext;
import oracle.jdbc.provider.observability.tracers.JFREventFactory.RoundTripEvent;

/**
 * {@link ObservabilityTracer} for tracing Java Flight Recorder events.
 */
public class JFRTracer implements ObservabilityTracer{

  /**
   * Creates a new instance.
   */
  public JFRTracer() {}

  @Override
  public Object traceRoundtrip(Sequence sequence, TraceContext traceContext, Object userContext) {
    if (sequence.equals(Sequence.BEFORE)) {
      RoundTripEvent event = JFREventFactory.createJFREvent(traceContext);
      event.begin();
      return event;
    } else {
      if (userContext != null) {
        RoundTripEvent event = (RoundTripEvent) userContext;
        event.setValues(traceContext);
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
  /* 
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
*/


}
