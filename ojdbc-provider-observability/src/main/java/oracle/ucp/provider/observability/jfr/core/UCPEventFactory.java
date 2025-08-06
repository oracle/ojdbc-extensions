package oracle.ucp.provider.observability.jfr.core;

import jdk.jfr.Event;
import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.events.core.UCPEventListener;
import oracle.ucp.provider.observability.jfr.events.connection.ConnectionBorrowedEvent;
import oracle.ucp.provider.observability.jfr.events.connection.ConnectionClosedEvent;
import oracle.ucp.provider.observability.jfr.events.connection.ConnectionCreatedEvent;
import oracle.ucp.provider.observability.jfr.events.connection.ConnectionReturnedEvent;
import oracle.ucp.provider.observability.jfr.events.lifecycle.*;
import oracle.ucp.provider.observability.jfr.events.maintenance.*;

/**
 * A factory class for creating and recording JFR (Java Flight Recorder) events
 * corresponding to UCP (Universal Connection Pool) operations and state
 * changes.
 *
 * <p><b>Key Responsibilities:</b></p>
 * <ul>
 * <li>Maps UCP event types to specific JFR event classes</li>
 * <li>Creates properly configured JFR event instances</li>
 * <li>Handles event recording lifecycle</li>
 * </ul>
 * <p><b>Event Categories:</b></p>
 * <ol>
 * <li><b>Pool Lifecycle Events</b> - Creation, start, stop, restart, and
 * destruction</li>
 * <li><b>Connection Lifecycle Events</b> - Borrow, return, and close
 * operations</li>
 * <li><b>Maintenance Operations</b> - Refresh, recycle, and purge operations
 * </li>
 * </ol>
 * @see Event
 */
public class UCPEventFactory {

  /**
   * Creates a JFR event instance corresponding to the specified UCP event type.
   *
   * <p><b>Event Mapping:</b></p>
   * Each UCP event type is mapped to a specific JFR event class that captures
   * relevant context information in its fields.
   *
   * @param type The UCP event type (must not be null)
   * @param ctx The event context containing pool metrics (must not be null)
   * @return Configured JFR event instance ready for recording
   * @throws IllegalStateException if the event type is not recognized
   * @throws NullPointerException if either parameter is null
   */
  public static Event createEvent(UCPEventListener.EventType type, UCPEventContext ctx) {
    switch (type) {
      // Pool Lifecycle Events
      case POOL_CREATED:
        return new PoolCreatedEvent(ctx);

      case POOL_STARTING:
        return new PoolStartingEvent(ctx);
      case POOL_STARTED:
        return new PoolStartedEvent(ctx);

      case POOL_STOPPED:
        return new PoolStoppedEvent(ctx);

      case POOL_RESTARTING:
        return new PoolRestartingEvent(ctx);
      case POOL_RESTARTED:
        return new PoolRestartedEvent(ctx);

      case POOL_DESTROYED:
        return new PoolDestroyedEvent(ctx);

      // Connection Lifecycle Events
      case CONNECTION_CREATED:
        return new ConnectionCreatedEvent(ctx);
      case CONNECTION_BORROWED:
        return new ConnectionBorrowedEvent(ctx);
      case CONNECTION_RETURNED:
        return new ConnectionReturnedEvent(ctx);
      case CONNECTION_CLOSED:
        return new ConnectionClosedEvent(ctx);

      // Maintenance Operations
      case POOL_REFRESHED:
        return new PoolRefreshedEvent(ctx);

      case POOL_RECYCLED:
        return new PoolRecycledEvent(ctx);

      case POOL_PURGED:
        return new PoolPurgedEvent(ctx);

      default:
        throw new IllegalStateException("Unexpected event type: " + type);
    }
  }

  /**
   * Creates and immediately records a JFR event for the specified UCP operation.
   * <p><b>Lifecycle:</b></p>
   * <ol>
   * <li>Creates the appropriate event type via {@link #createEvent}</li>
   * <li>Populates all event fields from the context</li>
   * li>Commits the event to JFR</li>
   * </ol>
   *
   * @param type The UCP event type to record (must not be null)
   * @param ctx The event context containing pool metrics (must not be null)
   * @throws NullPointerException if either parameter is null
   */
  public static void recordEvent(UCPEventListener.EventType type, UCPEventContext ctx) {
    Event event = createEvent(type, ctx);
    event.commit();
  }
}