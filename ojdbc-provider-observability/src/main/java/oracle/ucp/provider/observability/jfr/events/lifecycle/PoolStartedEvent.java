package oracle.ucp.provider.observability.jfr.events.lifecycle;

import oracle.ucp.provider.observability.jfr.core.UCPBaseEvent;
import jdk.jfr.Category;
import jdk.jfr.Label;
import jdk.jfr.Name;
import oracle.ucp.events.core.UCPEventContext;

@Name("ucp.PoolStarted")
@Label("Pool Started")
@Category({"UCP Events","Pool Lifecycle Events"})
public class PoolStartedEvent extends UCPBaseEvent {
  public PoolStartedEvent(UCPEventContext ctx) {
        initCommonFields(ctx);
    }
}