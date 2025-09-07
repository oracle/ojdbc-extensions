package oracle.ucp.provider.observability.jfr.events.connection;

import oracle.ucp.provider.observability.jfr.core.UCPBaseEvent;
import jdk.jfr.Category;
import jdk.jfr.Label;
import jdk.jfr.Name;
import oracle.ucp.events.core.UCPEventContext;

@Name("ucp.ConnectionCreated")
@Label("Connection Created")
@Category({"UCP Events","Connection Lifecycle Events"})
public class ConnectionCreatedEvent extends UCPBaseEvent {
  public ConnectionCreatedEvent(UCPEventContext ctx) {
    initCommonFields(ctx);
  }
}
