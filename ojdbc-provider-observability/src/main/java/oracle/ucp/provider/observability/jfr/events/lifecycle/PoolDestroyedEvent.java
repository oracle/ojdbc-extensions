package oracle.ucp.provider.observability.jfr.events.lifecycle;

import oracle.ucp.provider.observability.jfr.core.UCPBaseEvent;
import jdk.jfr.Category;
import jdk.jfr.Label;
import jdk.jfr.Name;
import oracle.ucp.events.core.UCPEventContext;

@Name("ucp.PoolDestroyed")
@Label("Pool Destroyed")
@Category({"UCP Events","Pool Lifecycle Events"})
public class PoolDestroyedEvent extends UCPBaseEvent {
  public PoolDestroyedEvent(UCPEventContext ctx) { initCommonFields(ctx); }
}
