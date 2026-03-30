/*
 ** Copyright (c) 2025 Oracle and/or its affiliates.
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

package oracle.ucp.provider.observability.jfr.core;

import jdk.jfr.*;
import oracle.ucp.events.core.UCPEventContext;

import java.util.Objects;

/**
 * Abstract base class for UCP JFR events providing common fields
 * and initialization. All UCP events extend this class to inherit
 * standard pool metrics and metadata.
 */
@StackTrace(false)
@Description("Base UCP Event")
public abstract class UCPBaseEvent extends Event {

  /** Name of the connection pool */
  @Label("Pool Name")
  private String poolName;

  /**
   *
   * <p>Note: This is the timestamp captured by UCP at event creation time,
   * preserved for correlation purposes. JFR also records its own startTime
   * automatically — this field complements it rather than replacing it.
   */
  @Label("UCP Timestamp (ms)")
  private long ucpTimestamp;

  /** Maximum configured pool size */
  @Label("Max Pool Size")
  private int maxPoolSize;

  /** Minimum configured pool size */
  @Label("Min Pool Size")
  private int minPoolSize;

  /** Current count of borrowed connections */
  @Label("Borrowed Connections")
  private int borrowedConnections;

  /** Current count of available connections */
  @Label("Available Connections")
  private int availableConnections;

  /** Total active connections (borrowed + available) */
  @Label("Total Connections")
  private int totalConnections;

  /** Lifetime count of closed connections */
  @Label("Closed Connections")
  private int closedConnections;

  /** Lifetime count of created connections */
  @Label("Created Connections")
  private int createdConnections;

  /** Average connection wait time in milliseconds */
  @Label("Average Wait Time (ms)")
  @Timespan(Timespan.MILLISECONDS)
  private long avgWaitTime;

  /**
   * Initializes common fields from UCP event context.
   *
   * @param ctx event context containing pool metrics
   * @throws NullPointerException if ctx is null
   */
  protected void initCommonFields(UCPEventContext ctx) {
    Objects.requireNonNull(ctx, "UCPEventContext cannot be null");

    String name             = ctx.poolName();
    this.poolName           = name != null ? name : "";
    this.ucpTimestamp       = ctx.timestamp();
    this.maxPoolSize        = ctx.maxPoolSize();
    this.minPoolSize        = ctx.minPoolSize();
    this.borrowedConnections = ctx.borrowedConnectionsCount();
    this.availableConnections = ctx.availableConnectionsCount();
    this.totalConnections   = ctx.totalConnections();
    this.closedConnections  = ctx.closedConnections();
    this.createdConnections = ctx.createdConnections();
    this.avgWaitTime        = ctx.getAverageConnectionWaitTime();
  }
}