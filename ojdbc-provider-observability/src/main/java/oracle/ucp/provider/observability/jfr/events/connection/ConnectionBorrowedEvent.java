package oracle.ucp.provider.observability.jfr.events.connection;

import oracle.ucp.provider.observability.jfr.core.UCPBaseEvent;
import jdk.jfr.Category;
import jdk.jfr.Label;
import jdk.jfr.Name;
import oracle.ucp.events.core.UCPEventContext;

@Name("ucp.ConnectionBorrowed")
@Label("Connection Borrowed")
@Category({"UCP Events","Connection Lifecycle Events"})
public class ConnectionBorrowedEvent extends UCPBaseEvent {
  public ConnectionBorrowedEvent(UCPEventContext ctx) {
        initCommonFields(ctx);
    }
}