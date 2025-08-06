package oracle.ucp.provider.observability.jfr.events.maintenance;

import jdk.jfr.Category;
import jdk.jfr.Label;
import jdk.jfr.Name;
import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.provider.observability.jfr.core.UCPBaseEvent;

// Pool Restart
@Name("ucp.PoolRestarting") @Label("Pool Restarting")
@Category({"UCP Events","Pool Lifecycle Events"})
public class PoolRestartingEvent extends UCPBaseEvent {
  public PoolRestartingEvent(UCPEventContext ctx) {
        initCommonFields(ctx);
    }
}
