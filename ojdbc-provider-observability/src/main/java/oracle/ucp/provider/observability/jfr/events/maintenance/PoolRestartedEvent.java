package oracle.ucp.provider.observability.jfr.events.maintenance;

import jdk.jfr.Category;
import jdk.jfr.Label;
import jdk.jfr.Name;
import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.provider.observability.jfr.core.UCPBaseEvent;

@Name("ucp.PoolRestarted") @Label("Pool Restarted")
@Category({"UCP Events","Pool Lifecycle Events"})
public class PoolRestartedEvent extends UCPBaseEvent {
  public PoolRestartedEvent(UCPEventContext ctx) { initCommonFields(ctx); }
}