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

package oracle.jdbc.provider.observability.tracers.otel;

/**
 * OpenTelemetry semantic convention attribute keys for Oracle Database instrumentation.
 */
final class OtelSemanticConventions {

  /**
   * Private constructor to prevent instantiation.
   */
  private OtelSemanticConventions() {
    throw new AssertionError("OtelSemanticConventions should not be instantiated");
  }

  // ========================================================================
  // Stable OpenTelemetry Database Semantic Convention Attributes
  // ========================================================================

  /**
   * Attribute key for the database system identifier.
   */
  static final String DB_SYSTEM_ATTRIBUTE = "db.system.name";

  /**
   * Value for {@link #DB_SYSTEM_ATTRIBUTE} representing Oracle Database.
   */
  static final String DB_SYSTEM_VALUE_ORACLE = "oracle.db";

  /**
   * Attribute key for the database namespace.
   */
  static final String DB_NAMESPACE_ATTRIBUTE = "db.namespace";

  /**
   * Attribute key for the database operation name.
   */
  static final String DB_OPERATION_NAME_ATTRIBUTE = "db.operation.name";

  /**
   * Attribute key for low cardinality query summary.
   */
  static final String DB_QUERY_SUMMARY_ATTRIBUTE = "db.query.summary";

  /**
   * Attribute key for the actual database query text.
   */
  static final String DB_QUERY_TEXT_ATTRIBUTE = "db.query.text";

  /**
   * Attribute key for the database user name.
   */
  static final String DB_USER_ATTRIBUTE = "db.user";

  /**
   * Attribute key for Oracle Database error number.
   */
  static final String DB_RESPONSE_STATUS_CODE_ATTRIBUTE = "db.response.status_code";

  /**
   * Attribute key for the number of rows returned by the operation.
   */
  static final String DB_RESPONSE_RETURNED_ROWS_ATTRIBUTE = "db.response.returned_rows";

  /**
   * Attribute key for the number of queries in a batch operation.
   */
  static final String DB_OPERATION_BATCH_SIZE_ATTRIBUTE = "db.operation.batch.size";

  /**
   * Attribute key for the database host name.
   */
  static final String SERVER_ADDRESS_ATTRIBUTE = "server.address";

  /**
   * Attribute key for the database server port number.
   */
  static final String SERVER_PORT_ATTRIBUTE = "server.port";

  /**
   * Attribute key for the error type describing the class of error.
   */
  static final String ERROR_TYPE_ATTRIBUTE = "error.type";

  /**
   * Attribute key for the current thread ID.
   */
  static final String THREAD_ID_ATTRIBUTE = "thread.id";

  /**
   * Attribute key for the current thread name.
   */
  static final String THREAD_NAME_ATTRIBUTE = "thread.name";

  // ========================================================================
  // Custom Attributes
  // ========================================================================

  /**
   * Attribute key for Oracle SQL statement identifier (SQL_ID).
   */
  static final String ORACLE_SQL_ID_ATTRIBUTE = "oracle.db.query.sql.id";

  /**
   * Attribute key for Oracle database session ID.
   */
  static final String ORACLE_SESSION_ID_ATTRIBUTE = "oracle.db.session.id";

  /**
   * Attribute key for Oracle database server process ID.
   */
  static final String ORACLE_SERVER_PID_ATTRIBUTE = "oracle.db.server.pid";

  /**
   * Attribute key for Oracle database instance identifier.
   */
  static final String ORACLE_INSTANCE_ID_ATTRIBUTE = "oracle.db.instance.id";

  /**
   * Attribute key for Oracle PDB name.
   */
  static final String ORACLE_PDB_ATTRIBUTE = "oracle.db.pdb";

  /**
   * Attribute key for Oracle shard name.
   */
  static final String ORACLE_SHARD_NAME_ATTRIBUTE = "oracle.db.shard.name";

  /**
   * Attribute key for the connection protocol during VIP retry.
   */
  static final String ORACLE_VIP_PROTOCOL_ATTRIBUTE = "oracle.db.vip.protocol";

  /**
   * Attribute key for the failed host during VIP retry.
   */
  static final String ORACLE_VIP_FAILED_HOST_ATTRIBUTE = "oracle.db.vip.failed_host";

  /**
   * Attribute key for the service name during VIP retry.
   */
  static final String ORACLE_VIP_SERVICE_NAME_ATTRIBUTE = "oracle.db.vip.service_name";

  /**
   * Attribute key for the Oracle SID during VIP retry.
   */
  static final String ORACLE_VIP_SID_ATTRIBUTE = "oracle.db.vip.sid";

  /**
   * Attribute key for the connection descriptor during VIP retry.
   */
  static final String ORACLE_VIP_CONNECTION_DESCRIPTOR_ATTRIBUTE = "oracle.db.vip.connection_descriptor";

  // ========================================================================
  // Legacy Attributes (Experimental Semantic Conventions - Backward Compatibility)
  // ========================================================================

  /**
   * Legacy attribute key for connection ID.
   */
  static final String LEGACY_CONNECTION_ID_ATTRIBUTE = "Connection ID";

  /**
   * Legacy attribute key for database operation.
   */
  static final String LEGACY_DATABASE_OPERATION_ATTRIBUTE = "Database Operation";

  /**
   * Legacy attribute key for database user.
   */
  static final String LEGACY_DATABASE_USER_ATTRIBUTE = "Database User";

  /**
   * Legacy attribute key for SQL ID.
   */
  static final String LEGACY_SQL_ID_ATTRIBUTE = "SQL ID";

  /**
   * Legacy attribute key for database tenant.
   */
  static final String LEGACY_DATABASE_TENANT_ATTRIBUTE = "Database Tenant";

  /**
   * Legacy attribute key for original SQL text.
   */
  static final String LEGACY_ORIGINAL_SQL_TEXT_ATTRIBUTE = "Original SQL Text";

  /**
   * Legacy attribute key for actual SQL text.
   */
  static final String LEGACY_ACTUAL_SQL_TEXT_ATTRIBUTE = "Actual SQL Text";

  /**
   * Legacy attribute key for error message (lowercase 'm').
   */
  static final String LEGACY_ERROR_MESSAGE_ATTRIBUTE = "Error message";

  /**
   * Legacy attribute key for error message (uppercase 'M').
   */
  static final String LEGACY_ERROR_MESSAGE_CAPITAL_ATTRIBUTE = "Error Message";

  /**
   * Legacy attribute key for error code.
   */
  static final String LEGACY_ERROR_CODE_ATTRIBUTE = "Error code";

  /**
   * Legacy attribute key for SQL state.
   */
  static final String LEGACY_SQL_STATE_ATTRIBUTE = "SQL state";

  /**
   * Legacy attribute key for VIP address.
   */
  static final String LEGACY_VIP_ADDRESS_ATTRIBUTE = "VIP Address";

  /**
   * Legacy attribute key for protocol.
   */
  static final String LEGACY_PROTOCOL_ATTRIBUTE = "Protocol";

  /**
   * Legacy attribute key for host.
   */
  static final String LEGACY_HOST_ATTRIBUTE = "Host";

  /**
   * Legacy attribute key for port.
   */
  static final String LEGACY_PORT_ATTRIBUTE = "Port";

  /**
   * Legacy attribute key for service name.
   */
  static final String LEGACY_SERVICE_NAME_ATTRIBUTE = "Service name";

  /**
   * Legacy attribute key for SID.
   */
  static final String LEGACY_SID_ATTRIBUTE = "SID";

  /**
   * Legacy attribute key for connection data.
   */
  static final String LEGACY_CONNECTION_DATA_ATTRIBUTE = "Connection data";

  /**
   * Legacy attribute key for replay retry count.
   */
  static final String LEGACY_REPLAY_RETRY_COUNT_ATTRIBUTE = "Current replay retry count";
}