package oracle.ucp.provider.observability.jfr.events.lifecycle;

import oracle.ucp.provider.observability.jfr.core.UCPBaseEvent;
import jdk.jfr.Category;
import jdk.jfr.Label;
import jdk.jfr.Name;
import oracle.ucp.events.core.UCPEventContext;

@Name("ucp.PoolStopped")
@Label("Pool Stopped")
@Category({"UCP Events","Pool Lifecycle Events"})
public class PoolStoppedEvent extends UCPBaseEvent {
  public PoolStoppedEvent(UCPEventContext ctx) { initCommonFields(ctx); }
}
