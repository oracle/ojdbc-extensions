/*
 ** Copyright (c) 2026 Oracle and/or its affiliates.
 **
 ** The Universal Permissive License (UPL), Version 1.0
 **
 ** Subject to the condition set forth below, permission is hereby granted to any
 ** person obtaining a copy of this software, associated documentation and/or data
 ** (collectively the "Software"), free of charge and under any and all copyright
 ** rights in the Software, and any and all patent rights owned or freely
 ** licensable by each licensor hereunder covering either (i) the unmodified
 ** Software as contributed to or provided by such licensor, or (ii) the Larger
 ** Works (as defined below), to deal in both
 **
 ** (a) the Software, and
 ** (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 ** one is included with the Software (each a "Larger Work" to which the Software
 ** is contributed by such licensors),
 **
 ** without restriction, including without limitation the rights to copy, create
 ** derivative works of, display, perform, and distribute the Software and make,
 ** use, sell, offer for sale, import, export, have made, and have sold the
 ** Software and the Larger Work(s), and to sublicense the foregoing rights on
 ** either these or other terms.
 **
 ** This license is subject to the following condition:
 ** The above copyright notice and either this complete permission notice or at
 ** a minimum a reference to the UPL must be included in all copies or
 ** substantial portions of the Software.
 **
 ** THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 ** IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 ** FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 ** AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 ** LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 ** OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 ** SOFTWARE.
 */

package oracle.jdbc.provider.ucp.observability.jfr.core;

import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.events.core.UCPEventListener;
import oracle.jdbc.provider.ucp.observability.jfr.events.connection.*;
import oracle.jdbc.provider.ucp.observability.jfr.events.lifecycle.*;
import oracle.jdbc.provider.ucp.observability.jfr.events.maintenance.*;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Factory for creating and recording JFR events from UCP operations.
 * Maps UCP event types to specific JFR event classes and handles
 * recording.
 */
public final class UCPEventFactory {

  private UCPEventFactory() {}

  private static final Logger LOGGER =
      Logger.getLogger(UCPEventFactory.class.getName());

  /**
   * Creates a JFR event instance for the specified UCP event type.
   *
   * @param type UCP event type
   * @return empty JFR event, or {@code null} for unrecognized types
   * @throws NullPointerException if parameters are null
   */
  static UCPBaseEvent createEvent(UCPEventListener.EventType type) {
    Objects.requireNonNull(type, "EventType cannot be null");

    switch (type) {
      // Pool Lifecycle Events
      case POOL_CREATED:    return new PoolCreatedEvent();
      case POOL_STARTING:   return new PoolStartingEvent();
      case POOL_STARTED:    return new PoolStartedEvent();
      case POOL_STOPPED:    return new PoolStoppedEvent();
      case POOL_DESTROYED:  return new PoolDestroyedEvent();

      // Connection Lifecycle Events
      case CONNECTION_CREATED:   return new ConnectionCreatedEvent();
      case CONNECTION_BORROWED:  return new ConnectionBorrowedEvent();
      case CONNECTION_RETURNED:  return new ConnectionReturnedEvent();
      case CONNECTION_CLOSED:    return new ConnectionClosedEvent();

      // Maintenance Operations
      case POOL_REFRESHED: return new PoolRefreshedEvent();
      case POOL_RECYCLED:  return new PoolRecycledEvent();
      case POOL_PURGED:    return new PoolPurgedEvent();

      default:
        LOGGER.fine(() ->
            "Unrecognized UCP EventType ignored by JFR provider: " + type);
        return null;
    }
  }

  /**
   * Creates and records a JFR event for the given UCP operation.
   *
   * <p>When no active recording enables this event type, this method returns
   * before reading UCP context fields.
   *
   * @param type UCP event type to record
   * @param ctx  event context with pool metrics
   * @throws NullPointerException if parameters are null
   */
  public static void recordEvent(
      UCPEventListener.EventType type, UCPEventContext ctx) {
    Objects.requireNonNull(type, "EventType cannot be null");
    Objects.requireNonNull(ctx, "UCPEventContext cannot be null");

    UCPBaseEvent event = createEvent(type);

    if (event == null || !event.isEnabled()) {
      return;
    }

    event.initCommonFields(ctx);
    event.commit();
  }
}
