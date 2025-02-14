package oracle.jdbc.provider.observability.tracers;

import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;

import java.sql.SQLException;

import jdk.jfr.Category;
import oracle.jdbc.TraceEventListener.JdbcExecutionEvent;
import oracle.jdbc.TraceEventListener.TraceContext;
import oracle.jdbc.provider.observability.configuration.ObservabilityConfiguration;
import oracle.jdbc.provider.observability.tracers.JFREventFactory.ExecutionEvent;

/**
 * Factory class for creating JFR events depending on the database function.
 */
public class JFREventFactory {

  /**
   * This class only has a static method, no public constructor needed.
   */
  private JFREventFactory() { }

  /**
   * Creates an instance of {@link RoundTripEvent} for the given trace context. 
   * The type of round trip event depends on the database function.
   * 
   * @param traceContext the trace context received by a TraceEventListener.
   * @return the {@link RoundTripEvent} for the database function.
   */
  public static RoundTripEvent createJFREvent(TraceContext traceContext) {
    switch (traceContext.databaseFunction()) {
      case ADVANCED_QUEUING_12C_EMON_DEQUEUE:
        return new AdvancedQueuing12cEminDequeueEvent(traceContext);
      case ADVANCED_QUEUING_ARRAY_ENQUEUE_DEQUEUE:
        return new AdvancedQueuingArrayEnqueueDequeue(traceContext);
      case ADVANCED_QUEUING_DEQUEUE_V8:
        return new AdvancedQueuingDequeueV8(traceContext);
      case ADVANCED_QUEUING_ENQUEUE:
        return new AdvancedQueuingEnqueue(traceContext);
      case ADVANCED_QUEUING_GET_PROPAGATION_STATUS:
        return new AdvancedQueuingGetPropagationStatus(traceContext);
      case ADVANCED_QUEUING_LISTEN:
        return new AdvancedQueuingListen(traceContext);
      case ADVANCED_QUEUING_SESSION_GET_RPC_1:
        return new AdvancedQueuingSessionGetRPC1(traceContext);
      case ADVANCED_QUEUING_SESSION_GET_RPC_2:
        return new AdvancedQueuingSessionGetRPC2(traceContext);
      case ADVANCED_QUEUING_SHARED_DEQUEUE:
        return new AdvancedQueuingSharedDequeue(traceContext);
      case ADVANCED_QUEUING_SHARED_ENQUEUE:
        return new AdvancedQueuingSharedEnqueue(traceContext);
      case APP_REPLAY:
        return new AppReplay(traceContext);
      case AUTH_CALL:
        return new AuthCall(traceContext);
      case AUTO_COMMIT_OFF:
        return new AutoCommitOff(traceContext);
      case AUTO_COMMIT_ON:
        return new AutoCommitOn(traceContext);
      case CANCEL_ALL:
        return new CancelAll(traceContext);
      case CANCEL_OPERATION:
        return new CancelOperation(traceContext);
      case CHUNCK_INFO:
        return new ChunkInfo(traceContext);
      case CLIENT_FEATURES:
        return new ClientFeatures(traceContext);
      case CLIENT_QUERY_CACHE_IDS:
        return new ClientQueryCacheIds(traceContext);
      case CLIENT_QUERY_CACHE_STATS_UPDATE:
        return new ClientQueryCacheStatsUpdate(traceContext);
      case CLOSE_ALL_CURSOR:
        return new CloseAllCursor(traceContext);
      case CLOSE_CURSOR:
        return new CloseCursor(traceContext);
      case COMMIT:
        return new Commit(traceContext);
      case DB12C_NOTIFICATION_RCV:
        return new DB12cNotificationRCV(traceContext);
      case DBNS_SAGAS:
        return new DBNSSagas(traceContext);
      case DESCRIBE_ANY_V8:
        return new DescribeAnyV8(traceContext);
      case DESCRIBE_ARRAY:
        return new DescribeArray(traceContext);
      case DESCRIBE_QUERY_CALL:
        return new DescribeQueryCall(traceContext);
      case DIRECT_PATH_LOAD_STREAM:
        return new DirectPathLoadStream(traceContext);
      case DIRECT_PATH_MISC_OP:
        return new DirectPathMISCOp(traceContext);
      case DIRECT_PATH_PREPARE:
        return new DirectPathPrepare(traceContext);
      case DISTRIBUTED_TRANS_MGR_RPC:
        return new DistributedTransMGRRPC(traceContext);
      case EXECUTE_QUERY:
        return new ExecuteQuery(traceContext);
      case EXTENSIBLE_SECURITY_SESSION_CREATE:
        return new ExtensibleSecuritySessionCreate(traceContext);
      case EXTENSIBLE_SECURITY_SESSION_PIGGYBACK:
        return new ExtensibleSecuritySessionPiggyback(traceContext);
      case EXTENSIBLE_SECURITY_SESSION_ROUNDTRIP:
        return new ExtensibleSecuritySessionRoundtrip(traceContext);
      case FAST_UPI_CALLS:
        return new FastUPICalls(traceContext);
      case FETCH_ROW:
        return new FetchRow(traceContext);
      case GET_VERSION:
        return new GetVersion(traceContext);
      case KERNEL_PROGRAMMATIC_NOTIFICATION:
        return new KernelProgrammaticNotification(traceContext);
      case KEY_VALUE:
        return new KeyValue(traceContext);
      case LOB_FILE_CALL:
        return new LOBFileCall(traceContext);
      case LOGOFF:
        return new LogOff(traceContext);
      case LOGON_CHALLENGE_RESPONSE_1:
        return new LogonChallengeResponse1(traceContext);
      case LOGON_CHALLENGE_RESPONSE_2:
        return new LogonChallengeResponse2(traceContext);
      case OEXFEN:
        return new OEXFEN(traceContext);
      case OPEN_CURSOR:
        return new OpenCursor(traceContext);
      case OSQL7:
        return new OSQL7(traceContext);
      case OSTART:
        return new OStart(traceContext);
      case OSTOP:
        return new OStop(traceContext);
      case PARAMETER_PUT_SPFILE:
        return new ParameterPutSPFile(traceContext);
      case PING:
        return new Ping(traceContext);
      case PIPELINE_END:
        return new PipelineEnd(traceContext);
      case PIPELINE_PIGGYBACK_BEGIN:
        return new PipelinePiggybackBegin(traceContext);
      case PIPELINE_PIGGYBACK_OP:
        return new PipelinePiggybackOp(traceContext);
      case ROLLBACK:
        return new Rollback(traceContext);
      case SESSION_KEY:
        return new SessionKey(traceContext);
      case SESSION_STATE_OPS:
        return new SessionStateOps(traceContext);
      case SESSION_STATE_TEMPLATE:
        return new SessionStateTemplate(traceContext);
      case SESSION_SWITCH_V8:
        return new SessionSwitchV8(traceContext);
      case TRACING_MESSAGE:
        return new TracingMessage(traceContext);
      case TRANSACTION_COMMIT:
        return new TransactionCommit(traceContext);
      case TRANSACTION_START:
        return new TransactionStart(traceContext);
      case TTC_DTY_ROUNDTRIP:
        return new TTCDTYRoundtrip(traceContext);
      case TTC_PRO_ROUNDTRIP:
        return new TTCPRORoundtrip(traceContext);
      case XS_ATTACH_SESSION:
        return new XSAttachSession(traceContext);
      case XS_CREATE_SESSION:
        return new XSCreateSession(traceContext);
      case XS_DESTROY_SESSION:
        return new XSDestroySession(traceContext);
      case XS_DETACH_SESSION:
        return new XSDetachSession(traceContext);
      case XS_NAMESPACE_OP:
        return new XSNamespaceOp(traceContext);
      case XS_NAMESPACE_OPS:
        return new XSNamespaceOps(traceContext);
      case XS_SET_SESSION_PARAMETER:
        return new XSSetSessionParameter(traceContext);
      case XS_STATE_SYNC_OP:
        return new XSStateSyncOp(traceContext);
      default:
        return new RoundTripEvent(traceContext);
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
        return new ExecutionEvent(event, params);
    }
  }

  // Round-trip events

  @Name("oracle.jdbc.provider.observability.RoundTrip.ADVANCED_QUEUING_12C_EMON_DEQUEUE")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class AdvancedQueuing12cEminDequeueEvent extends RoundTripEvent{
    public AdvancedQueuing12cEminDequeueEvent(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.ADVANCED_QUEUING_ARRAY_ENQUEUE_DEQUEUE")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class AdvancedQueuingArrayEnqueueDequeue extends RoundTripEvent{
    public AdvancedQueuingArrayEnqueueDequeue(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.ADVANCED_QUEUING_DEQUEUE_V8")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class AdvancedQueuingDequeueV8 extends RoundTripEvent{
    public AdvancedQueuingDequeueV8(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.ADVANCED_QUEUING_ENQUEUE")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class AdvancedQueuingEnqueue extends RoundTripEvent{
    public AdvancedQueuingEnqueue(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.ADVANCED_QUEUING_GET_PROPAGATION_STATUS")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class AdvancedQueuingGetPropagationStatus extends RoundTripEvent{
    public AdvancedQueuingGetPropagationStatus(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.ADVANCED_QUEUING_LISTEN")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class AdvancedQueuingListen extends RoundTripEvent{
    public AdvancedQueuingListen(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.ADVANCED_QUEUING_SESSION_GET_RPC_1")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class AdvancedQueuingSessionGetRPC1 extends RoundTripEvent{
    public AdvancedQueuingSessionGetRPC1(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.ADVANCED_QUEUING_SESSION_GET_RPC_2")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class AdvancedQueuingSessionGetRPC2 extends RoundTripEvent{
    public AdvancedQueuingSessionGetRPC2(TraceContext traceContext) {
      super(traceContext);
    }
  }
  
  @Name("oracle.jdbc.provider.observability.RoundTrip.ADVANCED_QUEUING_SHARED_DEQUEUE")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class AdvancedQueuingSharedDequeue extends RoundTripEvent{
    public AdvancedQueuingSharedDequeue(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.ADVANCED_QUEUING_SHARED_ENQUEUE")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class AdvancedQueuingSharedEnqueue extends RoundTripEvent{
    public AdvancedQueuingSharedEnqueue(TraceContext traceContext) {
      super(traceContext);
    }
  }
  
  @Name("oracle.jdbc.provider.observability.RoundTrip.APP_REPLAY")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class AppReplay extends RoundTripEvent{
    public AppReplay(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.AUTH_CALL")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class AuthCall extends RoundTripEvent{
    public AuthCall(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.AUTO_COMMIT_OFF")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class AutoCommitOff extends RoundTripEvent{
    public AutoCommitOff(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.AUTO_COMMIT_ON")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class AutoCommitOn extends RoundTripEvent{
    public AutoCommitOn(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.CANCEL_ALL")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class CancelAll extends RoundTripEvent{
    public CancelAll(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.CANCEL_OPERATION")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class CancelOperation extends RoundTripEvent{
    public CancelOperation(TraceContext traceContext) {
      super(traceContext);
    }
  }
  
  @Name("oracle.jdbc.provider.observability.RoundTrip.CHUNCK_INFO")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class ChunkInfo extends RoundTripEvent{
    public ChunkInfo(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.CLIENT_FEATURES")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class ClientFeatures extends RoundTripEvent{
    public ClientFeatures(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.CLIENT_QUERY_CACHE_IDS")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class ClientQueryCacheIds extends RoundTripEvent{
    public ClientQueryCacheIds(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.CLIENT_QUERY_CACHE_STATS_UPDATE")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class ClientQueryCacheStatsUpdate extends RoundTripEvent{
    public ClientQueryCacheStatsUpdate(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.CLOSE_ALL_CURSOR")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class CloseAllCursor extends RoundTripEvent{
    public CloseAllCursor(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.CLOSE_CURSOR")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class CloseCursor extends RoundTripEvent{
    public CloseCursor(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.COMMIT")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class Commit extends RoundTripEvent{
    public Commit(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.DB12C_NOTIFICATION_RCV")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class DB12cNotificationRCV extends RoundTripEvent{
    public DB12cNotificationRCV(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.DBNS_SAGAS")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class DBNSSagas extends RoundTripEvent{
    public DBNSSagas(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.DESCRIBE_ANY_V8")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class DescribeAnyV8 extends RoundTripEvent{
    public DescribeAnyV8(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.DESCRIBE_ARRAY")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class DescribeArray extends RoundTripEvent{
    public DescribeArray(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.DESCRIBE_QUERY_CALL")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class DescribeQueryCall extends RoundTripEvent{
    public DescribeQueryCall(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.DIRECT_PATH_LOAD_STREAM")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class DirectPathLoadStream extends RoundTripEvent{
    public DirectPathLoadStream(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.DIRECT_PATH_MISC_OP")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class DirectPathMISCOp extends RoundTripEvent{
    public DirectPathMISCOp(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.DIRECT_PATH_PREPARE")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class DirectPathPrepare extends RoundTripEvent{
    public DirectPathPrepare(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.DISTRIBUTED_TRANS_MGR_RPC")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class DistributedTransMGRRPC extends RoundTripEvent{
    public DistributedTransMGRRPC(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.EXECUTE_QUERY")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class ExecuteQuery extends RoundTripEvent{
    public ExecuteQuery(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.EXTENSIBLE_SECURITY_SESSION_CREATE")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class ExtensibleSecuritySessionCreate extends RoundTripEvent{
    public ExtensibleSecuritySessionCreate(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.EXTENSIBLE_SECURITY_SESSION_PIGGYBACK")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class ExtensibleSecuritySessionPiggyback extends RoundTripEvent{
    public ExtensibleSecuritySessionPiggyback(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.EXTENSIBLE_SECURITY_SESSION_ROUNDTRIP")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class ExtensibleSecuritySessionRoundtrip extends RoundTripEvent{
    public ExtensibleSecuritySessionRoundtrip(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.FAST_UPI_CALLS")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class FastUPICalls extends RoundTripEvent{
    public FastUPICalls(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.FETCH_ROW")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class FetchRow extends RoundTripEvent{
    public FetchRow(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.GET_VERSION")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class GetVersion extends RoundTripEvent{
    public GetVersion(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.KERNEL_PROGRAMMATIC_NOTIFICATION")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class KernelProgrammaticNotification extends RoundTripEvent{
    public KernelProgrammaticNotification(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.KEY_VALUE")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class KeyValue extends RoundTripEvent{
    public KeyValue(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.LOB_FILE_CALL")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class LOBFileCall extends RoundTripEvent{
    public LOBFileCall(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.LOGOFF")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class LogOff extends RoundTripEvent{
    public LogOff(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.LOGON_CHALLENGE_RESPONSE_1")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class LogonChallengeResponse1 extends RoundTripEvent{
    public LogonChallengeResponse1(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.LOGON_CHALLENGE_RESPONSE_2")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class LogonChallengeResponse2 extends RoundTripEvent{
    public LogonChallengeResponse2(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.OEXFEN")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class OEXFEN extends RoundTripEvent{
    public OEXFEN(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.OPEN_CURSOR")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class OpenCursor extends RoundTripEvent{
    public OpenCursor(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.OSQL7")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class OSQL7 extends RoundTripEvent{
    public OSQL7(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.OSTART")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class OStart extends RoundTripEvent{
    public OStart(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.OSTOP")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class OStop extends RoundTripEvent{
    public OStop(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.PARAMETER_PUT_SPFILE")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class ParameterPutSPFile extends RoundTripEvent{
    public ParameterPutSPFile(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.PING")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class Ping extends RoundTripEvent{
    public Ping(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.PIPELINE_END")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class PipelineEnd extends RoundTripEvent{
    public PipelineEnd(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.PIPELINE_PIGGYBACK_BEGIN")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class PipelinePiggybackBegin extends RoundTripEvent{
    public PipelinePiggybackBegin(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.PIPELINE_PIGGYBACK_OP")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class PipelinePiggybackOp extends RoundTripEvent{
    public PipelinePiggybackOp(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.ROLLBACK")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class Rollback extends RoundTripEvent{
    public Rollback(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.SESSION_KEY")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class SessionKey extends RoundTripEvent{
    public SessionKey(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.SESSION_STATE_OPS")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class SessionStateOps extends RoundTripEvent{
    public SessionStateOps(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.SESSION_STATE_TEMPLATE")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class SessionStateTemplate extends RoundTripEvent{
    public SessionStateTemplate(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.SESSION_SWITCH_V8")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class SessionSwitchV8 extends RoundTripEvent{
    public SessionSwitchV8(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.TRACING_MESSAGE")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class TracingMessage extends RoundTripEvent{
    public TracingMessage(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.TRANSACTION_COMMIT")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class TransactionCommit extends RoundTripEvent{
    public TransactionCommit(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.TRANSACTION_START")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class TransactionStart extends RoundTripEvent{
    public TransactionStart(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.TTC_DTY_ROUNDTRIP")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class TTCDTYRoundtrip extends RoundTripEvent{
    public TTCDTYRoundtrip(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.TTC_PRO_ROUNDTRIP")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class TTCPRORoundtrip extends RoundTripEvent{
    public TTCPRORoundtrip(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.XS_ATTACH_SESSION")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class XSAttachSession extends RoundTripEvent{
    public XSAttachSession(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.XS_CREATE_SESSION")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class XSCreateSession extends RoundTripEvent{
    public XSCreateSession(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.XS_DESTROY_SESSION")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class XSDestroySession extends RoundTripEvent{
    public XSDestroySession(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.XS_DETACH_SESSION")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class XSDetachSession extends RoundTripEvent{
    public XSDetachSession(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.XS_NAMESPACE_OP")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class XSNamespaceOp extends RoundTripEvent{
    public XSNamespaceOp(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.XS_NAMESPACE_OPS")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class XSNamespaceOps extends RoundTripEvent{
    public XSNamespaceOps(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.XS_SET_SESSION_PARAMETER")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class XSSetSessionParameter extends RoundTripEvent{
    public XSSetSessionParameter(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip.XS_STATE_SYNC_OP")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class XSStateSyncOp extends RoundTripEvent{
    public XSStateSyncOp(TraceContext traceContext) {
      super(traceContext);
    }
  }

  @Name("oracle.jdbc.provider.observability.RoundTrip")
  @Label("Round trip")
  @Category({"Oracle JDBC", "Round trips"})
  static class RoundTripEvent extends Event {

    public RoundTripEvent(TraceContext traceContext) {
      setValues(traceContext);
    }

    public void setValues(TraceContext traceContext) {
      this.connectionID = traceContext.getConnectionId();
      this.databaseOperation = traceContext.databaseOperation();
      this.tenant = traceContext.tenant();
      this.sqlID = traceContext.getSqlId();
      if (ObservabilityConfiguration.getInstance().getSensitiveDataEnabled()) {
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
          sid = params[5].toString();
          connectionData = params[6].toString();
          vipAddress = params[7].toString();
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
