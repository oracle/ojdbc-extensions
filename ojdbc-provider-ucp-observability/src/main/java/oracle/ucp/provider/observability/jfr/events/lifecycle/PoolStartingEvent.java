package oracle.ucp.provider.observability.jfr.events.lifecycle;

import jdk.jfr.Category;
import jdk.jfr.Label;
import jdk.jfr.Name;
import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.provider.observability.jfr.core.UCPBaseEvent;

@Name("ucp.PoolStarting")
@Label("Pool Starting")
@Category({"UCP Events", "Pool Lifecycle Events"})
public class PoolStartingEvent extends UCPBaseEvent {

  public PoolStartingEvent(UCPEventContext ctx) {
    initCommonFields(ctx);
  }
}
