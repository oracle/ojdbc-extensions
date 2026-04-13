package oracle.ucp.provider.observability.stress;

import oracle.ucp.UniversalConnectionPoolAdapter;
import oracle.ucp.UniversalPooledConnection;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

/**
 * Standalone JFR sample that exercises the UCP listener without going through test execution.
 */
public final class JfrSampleTestUCP {

  private static final String POOL_NAME = "Abdessamad's Pool";

  private JfrSampleTestUCP() {}

  public static void main(String[] args) throws Exception {
    PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
    pds.setInitialPoolSize(2);
    pds.setMinPoolSize(0);
    pds.setMaxPoolSize(50);
    pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
    pds.setURL("jdbc:oracle:thin:@adb_medium?TNS_ADMIN=/Users/abdessamadelaaissaoui/Desktop/Wallet_adb/");
    pds.setUser("ADMIN");
    pds.setPassword("Madara@@@1234");
    pds.setConnectionPoolName(POOL_NAME);
    pds.setUCPEventListenerProvider("jfr-ucp-listener");

    UniversalConnectionPoolManager mgr = UniversalConnectionPoolManagerImpl
      .getUniversalConnectionPoolManager();

    mgr.createConnectionPool((UniversalConnectionPoolAdapter) pds);
    mgr.startConnectionPool(POOL_NAME);

    UniversalPooledConnection[] borrowedConnections = new UniversalPooledConnection[9];
    for (int i = 0; i < borrowedConnections.length; i++) {
      borrowedConnections[i] = mgr.getConnectionPool(POOL_NAME)
        .borrowConnection(mgr.getConnectionPool(POOL_NAME).getConnectionRetrievalInfo());
    }

    for (UniversalPooledConnection borrowedConnection : borrowedConnections) {
      mgr.getConnectionPool(POOL_NAME).closeConnection(borrowedConnection);
    }

    for (UniversalPooledConnection borrowedConnection : borrowedConnections) {
      mgr.getConnectionPool(POOL_NAME).returnConnection(borrowedConnection);
    }

    mgr.purgeConnectionPool(POOL_NAME);
    mgr.recycleConnectionPool(POOL_NAME);
    mgr.refreshConnectionPool(POOL_NAME);

    mgr.stopConnectionPool(POOL_NAME);
    mgr.destroyConnectionPool(POOL_NAME);
  }
}
