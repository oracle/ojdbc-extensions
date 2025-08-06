package oracle.ucp.provider.observability.jfr.events.connection;

import jdk.jfr.Category;
import jdk.jfr.Label;
import jdk.jfr.Name;
import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.provider.observability.jfr.core.UCPBaseEvent;

@Name("ucp.ConnectionBorrowed")
@Label("Connection Borrowed")
@Category({"UCP Events","Connection Lifecycle Events"})
public class ConnectionBorrowedEvent extends UCPBaseEvent {
  public ConnectionBorrowedEvent(UCPEventContext ctx) {
        initCommonFields(ctx);
    }
}