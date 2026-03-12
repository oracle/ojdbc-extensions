package oracle.ucp.provider.observability.jfr.core;

import jdk.jfr.Event;
import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.events.core.UCPEventListener;
import oracle.ucp.provider.observability.jfr.events.connection.*;
import oracle.ucp.provider.observability.jfr.events.lifecycle.*;
import oracle.ucp.provider.observability.jfr.events.maintenance.*;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Factory for creating and recording JFR events from UCP operations.
 * Maps UCP event types to specific JFR event classes and handles
 * recording.
 */
public class UCPEventFactory {

  private static final Logger LOGGER =
    Logger.getLogger(UCPEventFactory.class.getName());

  /**
   * Creates a JFR event instance for the specified UCP event type.
   *
   * @param type UCP event type
   * @param ctx  event context with pool metrics
   * @return configured JFR event ready for recording
   * @throws NullPointerException if parameters are null
   */
  static Event createEvent(
    UCPEventListener.EventType type, UCPEventContext ctx) {
    Objects.requireNonNull(type, "EventType cannot be null");
    Objects.requireNonNull(ctx, "UCPEventContext cannot be null");

    switch (type) {
      // Pool Lifecycle Events
      case POOL_CREATED:    return new PoolCreatedEvent(ctx);
      case POOL_STARTING:   return new PoolStartingEvent(ctx);
      case POOL_STARTED:    return new PoolStartedEvent(ctx);
      case POOL_STOPPED:    return new PoolStoppedEvent(ctx);
      case POOL_DESTROYED:  return new PoolDestroyedEvent(ctx);

      // Connection Lifecycle Events
      case CONNECTION_CREATED:   return new ConnectionCreatedEvent(ctx);
      case CONNECTION_BORROWED:  return new ConnectionBorrowedEvent(ctx);
      case CONNECTION_RETURNED:  return new ConnectionReturnedEvent(ctx);
      case CONNECTION_CLOSED:    return new ConnectionClosedEvent(ctx);

      // Maintenance Operations
      case POOL_REFRESHED: return new PoolRefreshedEvent(ctx);
      case POOL_RECYCLED:  return new PoolRecycledEvent(ctx);
      case POOL_PURGED:    return new PoolPurgedEvent(ctx);

      default:
        LOGGER.fine(() ->
          "Unrecognized UCP EventType ignored by JFR provider: " + type);
        return null;
    }
  }

  /**
   * Creates and records a JFR event for the given UCP operation,
   * only if JFR recording is currently active.
   *
   * @param type UCP event type to record
   * @param ctx  event context with pool metrics
   * @throws NullPointerException if parameters are null
   */
  public static void recordEvent(
    UCPEventListener.EventType type, UCPEventContext ctx) {
    Objects.requireNonNull(type, "EventType cannot be null");
    Objects.requireNonNull(ctx, "UCPEventContext cannot be null");

    Event event = createEvent(type, ctx);

    if (event == null) {
      return;
    }

    if (event.shouldCommit()) {
      event.commit();
    }
  }
}