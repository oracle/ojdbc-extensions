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
package oracle.jdbc.provider.observability.tracers.jfr;

import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;

import java.sql.SQLException;
import java.util.logging.Logger;

import jdk.jfr.Category;
import oracle.jdbc.TraceEventListener.JdbcExecutionEvent;
import oracle.jdbc.TraceEventListener.TraceContext;
import oracle.jdbc.provider.observability.ObservabilityConfiguration;
import oracle.jdbc.provider.observability.ObservabilityTraceEventListener;
import oracle.jdbc.provider.observability.tracers.ObservabilityTracer;

/**
 * Factory class for creating JFR events depending on the database function.
 */
public class JFREventFactory {

  /**
   * Logger
   */
  private static final Logger logger = Logger.getLogger(
      ObservabilityTraceEventListener.class.getPackageName());

  /**
   * This class only has a static method, no public constructor needed.
   */
  private JFREventFactory() { }

  /**
   * Creates an instance of {@link RoundTripEvent} for the given trace context. 
   * The type of round trip event depends on the database function.
   * 
   * @param traceContext the trace context received by a TraceEventListener.
   * @param configuration the configuration
   * @return the {@link RoundTripEvent} for the database function.
   */
  public static RoundTripEvent createJFRRoundTripEvent(TraceContext traceContext, ObservabilityConfiguration configuration) {
    switch (traceContext.databaseFunction()) {
      case ADVANCED_QUEUING_12C_EMON_DEQUEUE:
        return new AdvancedQueuing12cEminDequeueEvent(traceContext, configuration);
      case ADVANCED_QUEUING_ARRAY_ENQUEUE_DEQUEUE:
        return new AdvancedQueuingArrayEnqueueDequeue(traceContext, configuration);
      case ADVANCED_QUEUING_DEQUEUE_V8:
        return new AdvancedQueuingDequeueV8(traceContext, configuration);
      case ADVANCED_QUEUING_ENQUEUE:
        return new AdvancedQueuingEnqueue(traceContext, configuration);
      case ADVANCED_QUEUING_GET_PROPAGATION_STATUS:
        return new AdvancedQueuingGetPropagationStatus(traceContext, configuration);
      case ADVANCED_QUEUING_LISTEN:
        return new AdvancedQueuingListen(traceContext, configuration);
      case ADVANCED_QUEUING_SESSION_GET_RPC_1:
        return new AdvancedQueuingSessionGetRPC1(traceContext, configuration);
      case ADVANCED_QUEUING_SESSION_GET_RPC_2:
        return new AdvancedQueuingSessionGetRPC2(traceContext, configuration);
      case ADVANCED_QUEUING_SHARED_DEQUEUE:
        return new AdvancedQueuingSharedDequeue(traceContext, configuration);
      case ADVANCED_QUEUING_SHARED_ENQUEUE:
        return new AdvancedQueuingSharedEnqueue(traceContext, configuration);
      case APP_REPLAY:
        return new AppReplay(traceContext, configuration);
      case AUTH_CALL:
        return new AuthCall(traceContext, configuration);
      case AUTO_COMMIT_OFF:
        return new AutoCommitOff(traceContext, configuration);
      case AUTO_COMMIT_ON:
        return new AutoCommitOn(traceContext, configuration);
      case CANCEL_ALL:
        return new CancelAll(traceContext, configuration);
      case CANCEL_OPERATION:
        return new CancelOperation(traceContext, configuration);
      case CHUNCK_INFO:
        return new ChunkInfo(traceContext, configuration);
      case CLIENT_FEATURES:
        return new ClientFeatures(traceContext, configuration);
      case CLIENT_QUERY_CACHE_IDS:
        return new ClientQueryCacheIds(traceContext, configuration);
      case CLIENT_QUERY_CACHE_STATS_UPDATE:
        return new ClientQueryCacheStatsUpdate(traceContext, configuration);
      case CLOSE_ALL_CURSOR:
        return new CloseAllCursor(traceContext, configuration);
      case CLOSE_CURSOR:
        return new CloseCursor(traceContext, configuration);
      case COMMIT:
        return new Commit(traceContext, configuration);
      case DB12C_NOTIFICATION_RCV:
        return new DB12cNotificationRCV(traceContext, configuration);
      case DBNS_SAGAS:
        return new DBNSSagas(traceContext, configuration);
      case DESCRIBE_ANY_V8:
        return new DescribeAnyV8(traceContext, configuration);
      case DESCRIBE_ARRAY:
        return new DescribeArray(traceContext, configuration);
      case DESCRIBE_QUERY_CALL:
        return new DescribeQueryCall(traceContext, configuration);
      case DIRECT_PATH_LOAD_STREAM:
        return new DirectPathLoadStream(traceContext, configuration);
      case DIRECT_PATH_MISC_OP:
        return new DirectPathMISCOp(traceContext, configuration);
      case DIRECT_PATH_PREPARE:
        return new DirectPathPrepare(traceContext, configuration);
      case DISTRIBUTED_TRANS_MGR_RPC:
        return new DistributedTransMGRRPC(traceContext, configuration);
      case EXECUTE_QUERY:
        return new ExecuteQuery(traceContext, configuration);
      case EXTENSIBLE_SECURITY_SESSION_CREATE:
        return new ExtensibleSecuritySessionCreate(traceContext, configuration);
      case EXTENSIBLE_SECURITY_SESSION_PIGGYBACK:
        return new ExtensibleSecuritySessionPiggyback(traceContext, configuration);
      case EXTENSIBLE_SECURITY_SESSION_ROUNDTRIP:
        return new ExtensibleSecuritySessionRoundtrip(traceContext, configuration);
      case FAST_UPI_CALLS:
        return new FastUPICalls(traceContext, configuration);
      case FETCH_ROW:
        return new FetchRow(traceContext, configuration);
      case GET_VERSION:
        return new GetVersion(traceContext, configuration);
      case KERNEL_PROGRAMMATIC_NOTIFICATION:
        return new KernelProgrammaticNotification(traceContext, configuration);
      case KEY_VALUE:
        return new KeyValue(traceContext, configuration);
      case LOB_FILE_CALL:
        return new LOBFileCall(traceContext, configuration);
      case LOGOFF:
        return new LogOff(traceContext, configuration);
      case LOGON_CHALLENGE_RESPONSE_1:
        return new LogonChallengeResponse1(traceContext, configuration);
      case LOGON_CHALLENGE_RESPONSE_2:
        return new LogonChallengeResponse2(traceContext, configuration);
      case OEXFEN:
        return new OEXFEN(traceContext, configuration);
      case OPEN_CURSOR:
        return new OpenCursor(traceContext, configuration);
      case OSQL7:
        return new OSQL7(traceContext, configuration);
      case OSTART:
        return new OStart(traceContext, configuration);
      case OSTOP:
        return new OStop(traceContext, configuration);
      case PARAMETER_PUT_SPFILE:
        return new ParameterPutSPFile(traceContext, configuration);
      case PING:
        return new Ping(traceContext, configuration);
      case PIPELINE_END:
        return new PipelineEnd(traceContext, configuration);
      case PIPELINE_PIGGYBACK_BEGIN:
        return new PipelinePiggybackBegin(traceContext, configuration);
      case PIPELINE_PIGGYBACK_OP:
        return new PipelinePiggybackOp(traceContext, configuration);
      case ROLLBACK:
        return new Rollback(traceContext, configuration);
      case SESSION_KEY:
        return new SessionKey(traceContext, configuration);
      case SESSION_STATE_OPS:
        return new SessionStateOps(traceContext, configuration);
      case SESSION_STATE_TEMPLATE:
        return new SessionStateTemplate(traceContext, configuration);
      case SESSION_SWITCH_V8:
        return new SessionSwitchV8(traceContext, configuration);
      case TRACING_MESSAGE:
        return new TracingMessage(traceContext, configuration);
      case TRANSACTION_COMMIT:
        return new TransactionCommit(traceContext, configuration);
      case TRANSACTION_START:
        return new TransactionStart(traceContext, configuration);
      case TTC_DTY_ROUNDTRIP:
        return new TTCDTYRoundtrip(traceContext, configuration);
      case TTC_PRO_ROUNDTRIP:
        return new TTCPRORoundtrip(traceContext, configuration);
      case XS_ATTACH_SESSION:
        return new XSAttachSession(traceContext, configuration);
      case XS_CREATE_SESSION:
        return new XSCreateSession(traceContext, configuration);
      case XS_DESTROY_SESSION:
        return new XSDestroySession(traceContext, configuration);
      case XS_DETACH_SESSION:
        return new XSDetachSession(traceContext, configuration);
      case XS_NAMESPACE_OP:
        return new XSNamespaceOp(traceContext, configuration);
      case XS_NAMESPACE_OPS:
        return new XSNamespaceOps(traceContext, configuration);
      case XS_SET_SESSION_PARAMETER:
        return new XSSetSessionParameter(traceContext, configuration);
      case XS_STATE_SYNC_OP:
        return new XSStateSyncOp(traceContext, configuration);
      default:
        logger.warning("Unknown round trip received: " + traceContext.databaseFunction());
        return new RoundTripEvent(traceContext, configuration);
    }
  }

  /**
   * Creates an instance {@link ExecutionEvent} for the given {@link 
   * JdbcExecutionEvent}.
   * 
   * @param event the event.
   * @param params the parameters to populate the event properties.
   * @return the execution event.
   */
  public static Event createExecutionEvent(JdbcExecutionEvent event, Object... params) {
    switch (event) {
      case AC_REPLAY_STARTED:
        return new ACReplayStarted(event, params);
      case AC_REPLAY_SUCCESSFUL:
        return new ACReplaySuccessful(event, params);
      case VIP_RETRY:
        return new VIPRetry(event, params); 
      default:
        logger.warning("Unknow event received: " + event);
        return new ExecutionEvent(event, params);
    }
  }

  // Round-trip events

  @Name("oracle.jdbc.provider.observability.RoundTrip.ADVANCED_QUEUING_12C_EMON_DEQUEUE")
  @Label("AQ 12c emon dequeue")
  @Category({"Oracle JDBC", "Round trips"})
  static class AdvancedQueuing12cEminDequeueEvent extends RoundTripEvent{
    public AdvancedQueuing12cEminDequeueEvent(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.ADVANCED_QUEUING_ARRAY_ENQUEUE_DEQUEUE")
  @Label("AQ Array Enqueue/Dequeue")
  @Category({"Oracle JDBC", "Round trips"})
  static class AdvancedQueuingArrayEnqueueDequeue extends RoundTripEvent{
    public AdvancedQueuingArrayEnqueueDequeue(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.ADVANCED_QUEUING_DEQUEUE_V8")
  @Label("AQ Dequeue before 8.1")
  @Category({"Oracle JDBC", "Round trips"})
  static class AdvancedQueuingDequeueV8 extends RoundTripEvent{
    public AdvancedQueuingDequeueV8(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.ADVANCED_QUEUING_ENQUEUE")
  @Label("AQ EnQueue")
  @Category({"Oracle JDBC", "Round trips"})
  static class AdvancedQueuingEnqueue extends RoundTripEvent{
    public AdvancedQueuingEnqueue(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.ADVANCED_QUEUING_GET_PROPAGATION_STATUS")
  @Label("AQ get propagation status entries")
  @Category({"Oracle JDBC", "Round trips"})
  static class AdvancedQueuingGetPropagationStatus extends RoundTripEvent{
    public AdvancedQueuingGetPropagationStatus(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.ADVANCED_QUEUING_LISTEN")
  @Label("AQ Listen")
  @Category({"Oracle JDBC", "Round trips"})
  static class AdvancedQueuingListen extends RoundTripEvent{
    public AdvancedQueuingListen(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.ADVANCED_QUEUING_SESSION_GET_RPC_1")
  @Label("Session get RPC in server pool scenario")
  @Category({"Oracle JDBC", "Round trips"})
  static class AdvancedQueuingSessionGetRPC1 extends RoundTripEvent{
    public AdvancedQueuingSessionGetRPC1(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.ADVANCED_QUEUING_SESSION_GET_RPC_2")
  @Label("Session get RPC in server pool scenario")
  @Category({"Oracle JDBC", "Round trips"})
  static class AdvancedQueuingSessionGetRPC2 extends RoundTripEvent{
    public AdvancedQueuingSessionGetRPC2(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }
  
  @Name("oracle.jdbc.provider.observability.RoundTrip.ADVANCED_QUEUING_SHARED_DEQUEUE")
  @Label("AQ Sharded dequeue")
  @Category({"Oracle JDBC", "Round trips"})
  static class AdvancedQueuingSharedDequeue extends RoundTripEvent{
    public AdvancedQueuingSharedDequeue(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.ADVANCED_QUEUING_SHARED_ENQUEUE")
  @Label("AQ Sharded enqueue")
  @Category({"Oracle JDBC", "Round trips"})
  static class AdvancedQueuingSharedEnqueue extends RoundTripEvent{
    public AdvancedQueuingSharedEnqueue(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }
  
  @Name("oracle.jdbc.provider.observability.RoundTrip.APP_REPLAY")
  @Label("Application continuity REPLAY")
  @Category({"Oracle JDBC", "Round trips"})
  static class AppReplay extends RoundTripEvent{
    public AppReplay(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.AUTH_CALL")
  @Label("Generic authentication call")
  @Category({"Oracle JDBC", "Round trips"})
  static class AuthCall extends RoundTripEvent{
    public AuthCall(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.AUTO_COMMIT_OFF")
  @Label("Auto commit off")
  @Category({"Oracle JDBC", "Round trips"})
  static class AutoCommitOff extends RoundTripEvent{
    public AutoCommitOff(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.AUTO_COMMIT_ON")
  @Label("Auto commit on")
  @Category({"Oracle JDBC", "Round trips"})
  static class AutoCommitOn extends RoundTripEvent{
    public AutoCommitOn(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.CANCEL_ALL")
  @Label("Cancel All")
  @Category({"Oracle JDBC", "Round trips"})
  static class CancelAll extends RoundTripEvent{
    public CancelAll(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.CANCEL_OPERATION")
  @Label("Cancel the current operation")
  @Category({"Oracle JDBC", "Round trips"})
  static class CancelOperation extends RoundTripEvent{
    public CancelOperation(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }
  
  @Name("oracle.jdbc.provider.observability.RoundTrip.CHUNCK_INFO")
  @Label("Chunk info RPC")
  @Category({"Oracle JDBC", "Round trips"})
  static class ChunkInfo extends RoundTripEvent{
    public ChunkInfo(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.CLIENT_FEATURES")
  @Label("Client features")
  @Category({"Oracle JDBC", "Round trips"})
  static class ClientFeatures extends RoundTripEvent{
    public ClientFeatures(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.CLIENT_QUERY_CACHE_IDS")
  @Label("Client query cache IDs")
  @Category({"Oracle JDBC", "Round trips"})
  static class ClientQueryCacheIds extends RoundTripEvent{
    public ClientQueryCacheIds(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.CLIENT_QUERY_CACHE_STATS_UPDATE")
  @Label("Client query cache statistics update")
  @Category({"Oracle JDBC", "Round trips"})
  static class ClientQueryCacheStatsUpdate extends RoundTripEvent{
    public ClientQueryCacheStatsUpdate(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.CLOSE_ALL_CURSOR")
  @Label("Cursor close all")
  @Category({"Oracle JDBC", "Round trips"})
  static class CloseAllCursor extends RoundTripEvent{
    public CloseAllCursor(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.CLOSE_CURSOR")
  @Label("Close a cursor")
  @Category({"Oracle JDBC", "Round trips"})
  static class CloseCursor extends RoundTripEvent{
    public CloseCursor(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.COMMIT")
  @Label("Commit")
  @Category({"Oracle JDBC", "Round trips"})
  static class Commit extends RoundTripEvent{
    public Commit(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.DB12C_NOTIFICATION_RCV")
  @Label("12c notification receive")
  @Category({"Oracle JDBC", "Round trips"})
  static class DB12cNotificationRCV extends RoundTripEvent{
    public DB12cNotificationRCV(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.DBNS_SAGAS")
  @Label("DBMS Sagas")
  @Category({"Oracle JDBC", "Round trips"})
  static class DBNSSagas extends RoundTripEvent{
    public DBNSSagas(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.DESCRIBE_ANY_V8")
  @Label("V8 Describe Any")
  @Category({"Oracle JDBC", "Round trips"})
  static class DescribeAnyV8 extends RoundTripEvent{
    public DescribeAnyV8(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.DESCRIBE_ARRAY")
  @Label("Array describe")
  @Category({"Oracle JDBC", "Round trips"})
  static class DescribeArray extends RoundTripEvent{
    public DescribeArray(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.DESCRIBE_QUERY_CALL")
  @Label("New describe query call")
  @Category({"Oracle JDBC", "Round trips"})
  static class DescribeQueryCall extends RoundTripEvent{
    public DescribeQueryCall(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.DIRECT_PATH_LOAD_STREAM")
  @Label("Direct Path Load Stream")
  @Category({"Oracle JDBC", "Round trips"})
  static class DirectPathLoadStream extends RoundTripEvent{
    public DirectPathLoadStream(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.DIRECT_PATH_MISC_OP")
  @Label("Direct Path Misc Operations")
  @Category({"Oracle JDBC", "Round trips"})
  static class DirectPathMISCOp extends RoundTripEvent{
    public DirectPathMISCOp(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.DIRECT_PATH_PREPARE")
  @Label("Direct Path Prepare")
  @Category({"Oracle JDBC", "Round trips"})
  static class DirectPathPrepare extends RoundTripEvent{
    public DirectPathPrepare(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.DISTRIBUTED_TRANS_MGR_RPC")
  @Label("Distributed transaction manager RPC")
  @Category({"Oracle JDBC", "Round trips"})
  static class DistributedTransMGRRPC extends RoundTripEvent{
    public DistributedTransMGRRPC(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.EXECUTE_QUERY")
  @Label("Execute query")
  @Category({"Oracle JDBC", "Round trips"})
  static class ExecuteQuery extends RoundTripEvent{
    public ExecuteQuery(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.EXTENSIBLE_SECURITY_SESSION_CREATE")
  @Label("eXtensible Security Sessions Create Session")
  @Category({"Oracle JDBC", "Round trips"})
  static class ExtensibleSecuritySessionCreate extends RoundTripEvent{
    public ExtensibleSecuritySessionCreate(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.EXTENSIBLE_SECURITY_SESSION_PIGGYBACK")
  @Label("eXtensible Security Sessions Piggyback")
  @Category({"Oracle JDBC", "Round trips"})
  static class ExtensibleSecuritySessionPiggyback extends RoundTripEvent{
    public ExtensibleSecuritySessionPiggyback(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.EXTENSIBLE_SECURITY_SESSION_ROUNDTRIP")
  @Label("eXtensible Security  Session Roundtrip")
  @Category({"Oracle JDBC", "Round trips"})
  static class ExtensibleSecuritySessionRoundtrip extends RoundTripEvent{
    public ExtensibleSecuritySessionRoundtrip(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.FAST_UPI_CALLS")
  @Label("Fast UPI calls to opial7")
  @Category({"Oracle JDBC", "Round trips"})
  static class FastUPICalls extends RoundTripEvent{
    public FastUPICalls(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.FETCH_ROW")
  @Label("Fetch a row")
  @Category({"Oracle JDBC", "Round trips"})
  static class FetchRow extends RoundTripEvent{
    public FetchRow(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.GET_VERSION")
  @Label("Get Oracle version-date string in new format")
  @Category({"Oracle JDBC", "Round trips"})
  static class GetVersion extends RoundTripEvent{
    public GetVersion(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.KERNEL_PROGRAMMATIC_NOTIFICATION")
  @Label("Kernel Programmatic Notification")
  @Category({"Oracle JDBC", "Round trips"})
  static class KernelProgrammaticNotification extends RoundTripEvent{
    public KernelProgrammaticNotification(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.KEY_VALUE")
  @Label("Client app context, configurationspace, attribute, values")
  @Category({"Oracle JDBC", "Round trips"})
  static class KeyValue extends RoundTripEvent{
    public KeyValue(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.LOB_FILE_CALL")
  @Label("LOB and FILE related calls")
  @Category({"Oracle JDBC", "Round trips"})
  static class LOBFileCall extends RoundTripEvent{
    public LOBFileCall(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.LOGOFF")
  @Label("Logoff of Oracle")
  @Category({"Oracle JDBC", "Round trips"})
  static class LogOff extends RoundTripEvent{
    public LogOff(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.LOGON_CHALLENGE_RESPONSE_1")
  @Label("First half of challenge-response logon")
  @Category({"Oracle JDBC", "Round trips"})
  static class LogonChallengeResponse1 extends RoundTripEvent{
    public LogonChallengeResponse1(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.LOGON_CHALLENGE_RESPONSE_2")
  @Label("Second half of challenge-response logon")
  @Category({"Oracle JDBC", "Round trips"})
  static class LogonChallengeResponse2 extends RoundTripEvent{
    public LogonChallengeResponse2(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.OEXFEN")
  @Label("OEXFEN")
  @Category({"Oracle JDBC", "Round trips"})
  static class OEXFEN extends RoundTripEvent{
    public OEXFEN(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.OPEN_CURSOR")
  @Label("Open a cursor")
  @Category({"Oracle JDBC", "Round trips"})
  static class OpenCursor extends RoundTripEvent{
    public OpenCursor(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.OSQL7")
  @Label("OSQL7")
  @Category({"Oracle JDBC", "Round trips"})
  static class OSQL7 extends RoundTripEvent{
    public OSQL7(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.OSTART")
  @Label("Starts Oracle")
  @Category({"Oracle JDBC", "Round trips"})
  static class OStart extends RoundTripEvent{
    public OStart(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.OSTOP")
  @Label("Stops Oracle")
  @Category({"Oracle JDBC", "Round trips"})
  static class OStop extends RoundTripEvent{
    public OStop(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.PARAMETER_PUT_SPFILE")
  @Label("Put parameter using spfile (for startup)")
  @Category({"Oracle JDBC", "Round trips"})
  static class ParameterPutSPFile extends RoundTripEvent{
    public ParameterPutSPFile(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.PING")
  @Label("Ping")
  @Category({"Oracle JDBC", "Round trips"})
  static class Ping extends RoundTripEvent{
    public Ping(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.PIPELINE_END")
  @Label("Pipeline End")
  @Category({"Oracle JDBC", "Round trips"})
  static class PipelineEnd extends RoundTripEvent{
    public PipelineEnd(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.PIPELINE_PIGGYBACK_BEGIN")
  @Label("Pipeline Begin Piggyback")
  @Category({"Oracle JDBC", "Round trips"})
  static class PipelinePiggybackBegin extends RoundTripEvent{
    public PipelinePiggybackBegin(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.PIPELINE_PIGGYBACK_OP")
  @Label("Pipeline Operation Piggyback")
  @Category({"Oracle JDBC", "Round trips"})
  static class PipelinePiggybackOp extends RoundTripEvent{
    public PipelinePiggybackOp(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.ROLLBACK")
  @Label("Rollback")
  @Category({"Oracle JDBC", "Round trips"})
  static class Rollback extends RoundTripEvent{
    public Rollback(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.SESSION_KEY")
  @Label("Get the session key")
  @Category({"Oracle JDBC", "Round trips"})
  static class SessionKey extends RoundTripEvent{
    public SessionKey(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.SESSION_STATE_OPS")
  @Label("Session state ops")
  @Category({"Oracle JDBC", "Round trips"})
  static class SessionStateOps extends RoundTripEvent{
    public SessionStateOps(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.SESSION_STATE_TEMPLATE")
  @Label("Session state template")
  @Category({"Oracle JDBC", "Round trips"})
  static class SessionStateTemplate extends RoundTripEvent{
    public SessionStateTemplate(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.SESSION_SWITCH_V8")
  @Label("V8 session switching piggyback")
  @Category({"Oracle JDBC", "Round trips"})
  static class SessionSwitchV8 extends RoundTripEvent{
    public SessionSwitchV8(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.TRACING_MESSAGE")
  @Label("End to end tracing message")
  @Category({"Oracle JDBC", "Round trips"})
  static class TracingMessage extends RoundTripEvent{
    public TracingMessage(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.TRANSACTION_COMMIT")
  @Label("Transaction commit, rollback, recover")
  @Category({"Oracle JDBC", "Round trips"})
  static class TransactionCommit extends RoundTripEvent{
    public TransactionCommit(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.TRANSACTION_START")
  @Label("Transaction start, attach, detach")
  @Category({"Oracle JDBC", "Round trips"})
  static class TransactionStart extends RoundTripEvent{
    public TransactionStart(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.TTC_DTY_ROUNDTRIP")
  @Label("Data type message exchange")
  @Category({"Oracle JDBC", "Round trips"})
  static class TTCDTYRoundtrip extends RoundTripEvent{
    public TTCDTYRoundtrip(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.TTC_PRO_ROUNDTRIP")
  @Label("Protocol negotiation message exchange")
  @Category({"Oracle JDBC", "Round trips"})
  static class TTCPRORoundtrip extends RoundTripEvent{
    public TTCPRORoundtrip(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.XS_ATTACH_SESSION")
  @Label("XS Attach Session")
  @Category({"Oracle JDBC", "Round trips"})
  static class XSAttachSession extends RoundTripEvent{
    public XSAttachSession(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.XS_CREATE_SESSION")
  @Label("XS Create Session")
  @Category({"Oracle JDBC", "Round trips"})
  static class XSCreateSession extends RoundTripEvent{
    public XSCreateSession(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.XS_DESTROY_SESSION")
  @Label("XS Destroy Session")
  @Category({"Oracle JDBC", "Round trips"})
  static class XSDestroySession extends RoundTripEvent{
    public XSDestroySession(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.XS_DETACH_SESSION")
  @Label("XS Detach Session")
  @Category({"Oracle JDBC", "Round trips"})
  static class XSDetachSession extends RoundTripEvent{
    public XSDetachSession(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.XS_NAMESPACE_OP")
  @Label("XS Namespace OP")
  @Category({"Oracle JDBC", "Round trips"})
  static class XSNamespaceOp extends RoundTripEvent{
    public XSNamespaceOp(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.XS_NAMESPACE_OPS")
  @Label("XS namespace OPs")
  @Category({"Oracle JDBC", "Round trips"})
  static class XSNamespaceOps extends RoundTripEvent{
    public XSNamespaceOps(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.XS_SET_SESSION_PARAMETER")
  @Label("XS Set Session Parameter")
  @Category({"Oracle JDBC", "Round trips"})
  static class XSSetSessionParameter extends RoundTripEvent{
    public XSSetSessionParameter(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.XS_STATE_SYNC_OP")
  @Label("XS State Sync OP")
  @Category({"Oracle JDBC", "Round trips"})
  static class XSStateSyncOp extends RoundTripEvent{
    public XSStateSyncOp(TraceContext traceContext, ObservabilityConfiguration configuration) {
      super(traceContext, configuration);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class RoundTripEvent extends Event {

    public RoundTripEvent(TraceContext traceContext, ObservabilityConfiguration configuration) {
      setValues(traceContext, configuration);
    }

    public void setValues(TraceContext traceContext, ObservabilityConfiguration configuration) {
      this.connectionID = traceContext.getConnectionId();
      this.databaseOperation = traceContext.databaseOperation();
      this.tenant = traceContext.tenant();
      this.sqlID = traceContext.getSqlId();
      if (configuration.getSensitiveDataEnabled()) {
        this.originalSQLText = traceContext.originalSqlText();
        this.actualSQLText = traceContext.actualSqlText();
        this.databaseUser = traceContext.user();
      }
    }

    @Label("Connection ID")
    String connectionID;

    @Label("Database operation")
    String databaseOperation;

    @Label("Database tenant")
    String tenant;

    @Label("SQL ID")
    String sqlID;

    @Label("Original SQL text")
    String originalSQLText;

    @Label("Actual SQL text")
    String actualSQLText;

    @Label("Database user")
    String databaseUser;

  }

  // Execution Events
  @Name("oracle.jdbc.provider.observability.ExecutionEvent.AC_REPLAY_STARTED")
  @Label("AC replay started")
  @Category({"Oracle JDBC", "Execution events"})
  static class ACReplayStarted extends ACReplay {
    public ACReplayStarted(JdbcExecutionEvent event, Object... params) {
      super(event, params);
    }
  }

  @Name("oracle.jdbc.provider.observability.ExecutionEvent.AC_REPLAY_SUCCESSFUL")
  @Label("AC replay successful")
  @Category({"Oracle JDBC", "Execution events"})
  static class ACReplaySuccessful extends ACReplay {
    public ACReplaySuccessful(JdbcExecutionEvent event, Object... params) {
      super(event, params);
    }
  }


  @Name("oracle.jdbc.provider.observability.ExecutionEvent.AC_REPLAY")
  @Label("AC replay")
  @Category({"Oracle JDBC", "Execution events"})
  static class ACReplay extends ExecutionEvent {
    public ACReplay(JdbcExecutionEvent event, Object... params) {
      super(event, params);
      if (ObservabilityTracer.EXECUTION_EVENTS_PARAMETERS.get(event) == params.length) {
        this.errorCode = ((SQLException) params[1]).getErrorCode();
        this.sqlState = ((SQLException) params[1]).getSQLState();
        this.currentReplayRetryCount = params[2].toString();
      }
    }
    @Label("Error code")
    public int errorCode;

    @Label("SQL state")
    public String sqlState;

    @Label("Current replay retry count")
    public String currentReplayRetryCount;
  }

  @Name("oracle.jdbc.provider.observability.ExecutionEvent.VIP_RETRY")
  @Label("VIP retry")
  @Category({"Oracle JDBC", "Round trips"})
  static class VIPRetry extends ExecutionEvent {
    public VIPRetry(JdbcExecutionEvent event, Object... params) {
        super(event, params);
        if (ObservabilityTracer.EXECUTION_EVENTS_PARAMETERS.get(event) == params.length) {
          protocol = params[1].toString();
          host = params[2].toString();
          port = params[3].toString();
          serviceName = params[4].toString();
          sid =  params[5] != null ? params[5].toString() : "" ;
          connectionData =  params[6] != null ? params[6].toString() : "";
          vipAddress =  params[7] != null ? params[7].toString() : "";
        }
    }

    @Label("The protocol")
    public String protocol;
    
    @Label("The host")
    public String host;
    
    @Label("The port")
    public String port;
    
    @Label("The service name")
    public String serviceName;
    
    @Label("The SID")
    public String sid;
    
    @Label("The connection data")
    public String connectionData;
    
    @Label("The VIP address")
    public String vipAddress;
  }

  @Name("oracle.jdbc.provider.observability.ExecutionEvent")
  @Label("Execution event")
  @Category({"Oracle JDBC", "Execution events"})
  static class ExecutionEvent extends Event {
    public ExecutionEvent(JdbcExecutionEvent event, Object... params) {
      if (ObservabilityTracer.EXECUTION_EVENTS_PARAMETERS.get(event) == params.length) {
        if (params != null && params.length > 0) {
          this.errorMessage = params[0].toString();
        }
      }
    }

    @Label("Error message")
    String errorMessage;
  }

}
