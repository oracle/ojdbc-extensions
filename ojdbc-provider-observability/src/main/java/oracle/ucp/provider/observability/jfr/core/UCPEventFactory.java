package oracle.ucp.provider.observability.jfr.core;

import jdk.jfr.Event;
import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.events.core.UCPEventListener;
import oracle.ucp.provider.observability.jfr.events.connection.*;
import oracle.ucp.provider.observability.jfr.events.lifecycle.*;
import oracle.ucp.provider.observability.jfr.events.maintenance.*;

/**
 * Factory for creating and recording JFR events from UCP operations.
 * Maps UCP event types to specific JFR event classes and handles recording.
 */
public class UCPEventFactory {

  /**
   * Creates a JFR event instance for the specified UCP event type.
   *
   * @param type UCP event type
   * @param ctx event context with pool metrics
   * @return configured JFR event ready for recording
   * @throws IllegalStateException if event type is unrecognized
   * @throws NullPointerException if parameters are null
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
   * Creates and immediately records a JFR event for the UCP operation.
   *
   * @param type UCP event type to record
   * @param ctx event context with pool metrics
   * @throws NullPointerException if parameters are null
   */
  public static void recordEvent(UCPEventListener.EventType type, UCPEventContext ctx) {
    Event event = createEvent(type, ctx);
    event.commit();
  }
}