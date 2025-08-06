package oracle.ucp.provider.observability.jfr.core;

import jdk.jfr.*;
import oracle.ucp.events.core.UCPEventContext;

import java.util.Objects;

/**
 * The abstract base class for all UCP (Universal Connection Pool) JFR (Java
 * Flight Recorder) events. Provides common fields and initialization for
 * concrete UCP event types.
 *
 * <p><b>Metadata Annotations:</b></p>
 * <ul>
 * <li>{@code @Category("UCP Events")} - Groups all UCP events together in
 * JFR recordings</li>
 * <li>{@code @Description} - Provides human-readable descriptions for
 * events</li>
 * <li>{@code @Label} - Defines display names for fields in JFR tools</li>
 * <li>{@code @Timespan} - Specifies unit formatting for time fields</li>
 * </ul>
 *
 * <p><b>Common Fields:</b></p>
 * <table>
 * <tr><th>Field</th><th>Description</th><th>Source</th></tr>
 * <tr><td>poolName</td><td>Name of the connection pool</td><td>{@link
 * UCPEventContext#poolName()}</td></tr>
 * <tr><td>timestamp</td><td>Event time in milliseconds</td><td>{@link
 * UCPEventContext#timestamp()}</td></tr>
 * <tr><td>maxPoolSize</td><td>Configured maximum pool size</td><td>{@link
 * UCPEventContext#maxPoolSize()}</td></tr>
 * <tr><td>minPoolSize</td><td>Configured minimum pool size</td><td>{@link
 * UCPEventContext#minPoolSize()}</td></tr>
 * <tr><td>borrowedConnections</td><td>Currently borrowed connections</td>
 * <td>{@link UCPEventContext#borrowedConnectionsCount()}</td></tr>
 * <tr><td>availableConnections</td><td>Currently available connections</td>
 * <td>{@link UCPEventContext#availableConnectionsCount()}</td></tr>
 * <tr><td>totalConnections</td><td>Total active connections</td><td>{@link
 * UCPEventContext#totalConnections()}</td></tr>
 * <tr><td>closedConnections</td><td>Lifetime closed connections</td><td>
 * {@link UCPEventContext#closedConnections()}</td></tr>
 * <tr><td>createdConnections</td><td>Lifetime created connections</td><td>
 * {@link UCPEventContext#createdConnections()}</td></tr>
 * <tr><td>avgWaitTime</td><td>Average connection wait time (ms)</td><td>
 * {@link UCPEventContext#getAverageConnectionWaitTime()}</td></tr>
 * </table>
 *
 * @see Event
 */
@Category("UCP Events")
@Description("Base UCP Event")
public abstract class UCPBaseEvent extends Event {

  /**
   * The name of the connection pool that generated this event.
   * Appears as "Pool Name" in JFR tools.
   */
  @Label("Pool Name")
  protected String poolName;

  /**
   * The timestamp when the event occurred (milliseconds since epoch).
   * Appears as "Timestamp" in JFR tools.
   */
  @Label("Timestamp")
  protected long timestamp;

  /**
   * The configured maximum size of the connection pool.
   * Appears as "Max Pool Size" in JFR tools.
   */
  @Label("Max Pool Size")
  protected int maxPoolSize;

  /**
   * The configured minimum size of the connection pool.
   * Appears as "Min Pool Size" in JFR tools.
   */
  @Label("Min Pool Size")
  protected int minPoolSize;

  /**
   * Current count of borrowed connections.
   * Appears as "Borrowed Connections" in JFR tools.
   */
  @Label("Borrowed Connections")
  protected int borrowedConnections;

  /**
   * Current count of available (idle) connections.
   * Appears as "Available Connections" in JFR tools.
   */
  @Label("Available Connections")
  protected int availableConnections;

  /**
   * Total count of active connections (borrowed + available).
   * Appears as "Total Connections" in JFR tools.
   */
  @Label("Total Connections")
  protected int totalConnections;

  /**
   * Lifetime count of closed connections.
   * Appears as "Closed Connections" in JFR tools.
   */
  @Label("Closed Connections")
  protected int closedConnections;

  /**
   * Lifetime count of created connections.
   * Appears as "Created Connections" in JFR tools.
   */
  @Label("Created Connections")
  protected int createdConnections;

  /**
   * Average wait time for connection requests in milliseconds.
   * Formatted as a timespan in JFR tools.
   */
  @Label("Average Wait Time (ms)")
  @Timespan(Timespan.MILLISECONDS)
  protected long avgWaitTime;

  /**
   * Initializes common fields from a UCP event context.
   *
   * @param ctx The event context containing pool metrics (must not be null)
   * @throws NullPointerException if the context is null
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