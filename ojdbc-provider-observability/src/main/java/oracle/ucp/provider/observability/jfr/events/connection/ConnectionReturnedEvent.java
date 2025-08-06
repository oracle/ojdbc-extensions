package oracle.ucp.provider.observability.jfr.events.connection;

import jdk.jfr.Category;
import jdk.jfr.Label;
import jdk.jfr.Name;
import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.provider.observability.jfr.core.UCPBaseEvent;

@Name("ucp.ConnectionReturned")
@Label("Connection Returned")
@Category({"UCP Events","Connection Lifecycle Events"})

public class ConnectionReturnedEvent extends UCPBaseEvent {
  public ConnectionReturnedEvent(UCPEventContext ctx) {
        initCommonFields(ctx);
    }
}