package oracle.ucp.provider.observability.jfr.events.maintenance;

import jdk.jfr.Category;
import jdk.jfr.Label;
import jdk.jfr.Name;
import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.provider.observability.jfr.core.UCPBaseEvent;

@Name("ucp.PoolPurged")
@Label("Pool Purged")
@Category({"UCP Events","Maintenance Operations Events"})

public class PoolPurgedEvent extends UCPBaseEvent {
  public PoolPurgedEvent(UCPEventContext ctx) {
        initCommonFields(ctx);
    }
}