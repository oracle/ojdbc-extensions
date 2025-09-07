package oracle.ucp.provider.observability.jfr.events.lifecycle;

import oracle.ucp.provider.observability.jfr.core.UCPBaseEvent;
import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;
import oracle.ucp.events.core.UCPEventContext;

@Name("ucp.PoolCreated")
@Label("Pool Created")
@Category({"UCP Events","Pool Lifecycle Events"})
public class PoolCreatedEvent extends UCPBaseEvent {
  public PoolCreatedEvent(UCPEventContext ctx) {
        initCommonFields(ctx);
    }
}