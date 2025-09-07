package oracle.ucp.provider.observability.jfr.core;

import jdk.jfr.*;
import oracle.ucp.events.core.UCPEventContext;

import java.util.Objects;

/**
 * Abstract base class for UCP JFR events providing common fields and initialization.
 * All UCP events extend this class to inherit standard pool metrics and metadata.
 */
@Category("UCP Events")
@Description("Base UCP Event")
public abstract class UCPBaseEvent extends Event {

  /** Name of the connection pool */
  @Label("Pool Name")
  protected String poolName;

  /** Event timestamp in milliseconds since epoch */
  @Label("Timestamp")
  protected long timestamp;

  /** Maximum configured pool size */
  @Label("Max Pool Size")
  protected int maxPoolSize;

  /** Minimum configured pool size */
  @Label("Min Pool Size")
  protected int minPoolSize;

  /** Current count of borrowed connections */
  @Label("Borrowed Connections")
  protected int borrowedConnections;

  /** Current count of available connections */
  @Label("Available Connections")
  protected int availableConnections;

  /** Total active connections (borrowed + available) */
  @Label("Total Connections")
  protected int totalConnections;

  /** Lifetime count of closed connections */
  @Label("Closed Connections")
  protected int closedConnections;

  /** Lifetime count of created connections */
  @Label("Created Connections")
  protected int createdConnections;

  /** Average connection wait time in milliseconds */
  @Label("Average Wait Time (ms)")
  @Timespan(Timespan.MILLISECONDS)
  protected long avgWaitTime;

  /**
   * Initializes common fields from UCP event context.
   *
   * @param ctx event context containing pool metrics
   * @throws NullPointerException if ctx is null
   */
  protected void initCommonFields(UCPEventContext ctx) {
    Objects.requireNonNull(ctx, "UCPEventContext cannot be null");

    this.poolName = ctx.poolName();
    this.timestamp = ctx.timestamp();
    this.maxPoolSize = ctx.maxPoolSize();
    this.minPoolSize = ctx.minPoolSize();
    this.borrowedConnections = ctx.borrowedConnectionsCount();
    this.availableConnections = ctx.availableConnectionsCount();
    this.totalConnections = ctx.totalConnections();
    this.closedConnections = ctx.closedConnections();
    this.createdConnections = ctx.createdConnections();
    this.avgWaitTime = ctx.getAverageConnectionWaitTime();
  }
}