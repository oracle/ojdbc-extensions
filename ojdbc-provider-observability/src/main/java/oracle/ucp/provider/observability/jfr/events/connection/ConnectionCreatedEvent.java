package oracle.ucp.provider.observability.jfr.events.connection;

import jdk.jfr.Category;
import jdk.jfr.Label;
import jdk.jfr.Name;
import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.provider.observability.jfr.core.UCPBaseEvent;

@Name("ucp.ConnectionCreated")
@Label("Connection Created")
@Category({"UCP Events","Connection Lifecycle Events"})

public class ConnectionCreatedEvent extends UCPBaseEvent {
  public ConnectionCreatedEvent(UCPEventContext ctx) {
    initCommonFields(ctx);
  }
}
