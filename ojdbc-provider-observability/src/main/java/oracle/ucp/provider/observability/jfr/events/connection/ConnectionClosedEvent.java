package oracle.ucp.provider.observability.jfr.events.connection;

import jdk.jfr.Category;
import jdk.jfr.Label;
import jdk.jfr.Name;
import oracle.ucp.provider.observability.jfr.core.UCPBaseEvent;
import oracle.ucp.events.core.UCPEventContext;

@Name("ucp.ConnectionClosed")
@Label("Connection Closed")
@Category({"UCP Events","Connection Lifecycle Events"})
public class ConnectionClosedEvent extends UCPBaseEvent {
  public ConnectionClosedEvent(UCPEventContext ctx) {
    initCommonFields(ctx);
  }
}