package oracle.ucp.provider.observability.jfr.events.maintenance;


import jdk.jfr.Category;
import jdk.jfr.Label;
import jdk.jfr.Name;
import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.provider.observability.jfr.core.UCPBaseEvent;

@Name("ucp.PoolRefreshed")
@Label("Pool Refreshed")
@Category({"UCP Events","Maintenance Operations Events"})

public class PoolRefreshedEvent extends UCPBaseEvent {
  public PoolRefreshedEvent(UCPEventContext ctx) {
        initCommonFields(ctx);
    }
}