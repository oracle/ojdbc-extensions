package oracle.ucp.provider.observability;

import oracle.ucp.UniversalConnectionPoolAdapter;
import oracle.ucp.UniversalPooledConnection;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
public class SampleTestUCP {

public static void main(String args[]) throws Exception {

  System.setProperty("oracle.jdbc.Trace", "true");
  System.setProperty("java.util.logging.config.file", "./src_test/test/standalone/ucplogging.properties");
  System.setProperty("oracle.ucp.wls.jta", "false");
  // Configure the connection pool
  PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();

  pds.setInitialPoolSize(2);
  pds.setMinPoolSize(0);
  pds.setMaxPoolSize(50);
  pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
  pds.setURL("jdbc:oracle:thin:@adb_medium?TNS_ADMIN=/Users/abdessamadelaaissaoui/Desktop/Wallet_adb/");
  pds.setUser("ADMIN");
  pds.setPassword("Madara@@1234");
  pds.setConnectionPoolName("Abdessamad's Pool");

  pds.setUCPEventListenerProvider("jfr-ucp-listener");
  //System.setProperty("UCPEventListenerProvider","jfr-ucp-listener");

  UniversalConnectionPoolManager mgr = UniversalConnectionPoolManagerImpl.
      getUniversalConnectionPoolManager();


  mgr.createConnectionPool((UniversalConnectionPoolAdapter) pds);

  mgr.startConnectionPool("Abdessamad's Pool");

  UniversalPooledConnection conn1 = mgr.getConnectionPool("Abdessamad's Pool").borrowConnection(mgr.getConnectionPool("Abdessamad's Pool").getConnectionRetrievalInfo());

  UniversalPooledConnection conn2 = mgr.getConnectionPool("Abdessamad's Pool").borrowConnection(mgr.getConnectionPool("Abdessamad's Pool").getConnectionRetrievalInfo());

  UniversalPooledConnection conn3 = mgr.getConnectionPool("Abdessamad's Pool").borrowConnection(mgr.getConnectionPool("Abdessamad's Pool").getConnectionRetrievalInfo());

  UniversalPooledConnection conn4 = mgr.getConnectionPool("Abdessamad's Pool").borrowConnection(mgr.getConnectionPool("Abdessamad's Pool").getConnectionRetrievalInfo());

  UniversalPooledConnection conn5 = mgr.getConnectionPool("Abdessamad's Pool").borrowConnection(mgr.getConnectionPool("Abdessamad's Pool").getConnectionRetrievalInfo());

  UniversalPooledConnection conn6 = mgr.getConnectionPool("Abdessamad's Pool").borrowConnection(mgr.getConnectionPool("Abdessamad's Pool").getConnectionRetrievalInfo());

  UniversalPooledConnection conn7 = mgr.getConnectionPool("Abdessamad's Pool").borrowConnection(mgr.getConnectionPool("Abdessamad's Pool").getConnectionRetrievalInfo());

  UniversalPooledConnection conn8 = mgr.getConnectionPool("Abdessamad's Pool").borrowConnection(mgr.getConnectionPool("Abdessamad's Pool").getConnectionRetrievalInfo());

  UniversalPooledConnection conn9 = mgr.getConnectionPool("Abdessamad's Pool").borrowConnection(mgr.getConnectionPool("Abdessamad's Pool").getConnectionRetrievalInfo());


  mgr.getConnectionPool("Abdessamad's Pool").closeConnection(conn1);
  mgr.getConnectionPool("Abdessamad's Pool").closeConnection(conn2);
  mgr.getConnectionPool("Abdessamad's Pool").closeConnection(conn3);
  mgr.getConnectionPool("Abdessamad's Pool").closeConnection(conn4);
  mgr.getConnectionPool("Abdessamad's Pool").closeConnection(conn5);
  mgr.getConnectionPool("Abdessamad's Pool").closeConnection(conn6);
  mgr.getConnectionPool("Abdessamad's Pool").closeConnection(conn7);
  mgr.getConnectionPool("Abdessamad's Pool").closeConnection(conn8);
  mgr.getConnectionPool("Abdessamad's Pool").closeConnection(conn9);

  mgr.getConnectionPool("Abdessamad's Pool").returnConnection(conn1);
  mgr.getConnectionPool("Abdessamad's Pool").returnConnection(conn2);
  mgr.getConnectionPool("Abdessamad's Pool").returnConnection(conn3);
  mgr.getConnectionPool("Abdessamad's Pool").returnConnection(conn4);
  mgr.getConnectionPool("Abdessamad's Pool").returnConnection(conn5);
  mgr.getConnectionPool("Abdessamad's Pool").returnConnection(conn6);
  mgr.getConnectionPool("Abdessamad's Pool").returnConnection(conn7);
  mgr.getConnectionPool("Abdessamad's Pool").returnConnection(conn8);
  mgr.getConnectionPool("Abdessamad's Pool").returnConnection(conn9);


  pds.setURL("jdbc:oracle:thin:@adb_medium?TNS_ADMIN=/Users/abdessamadelaaissaoui/Desktop/Wallet_adb/");


  mgr.purgeConnectionPool("Abdessamad's Pool");
  mgr.recycleConnectionPool("Abdessamad's Pool");
  mgr.refreshConnectionPool("Abdessamad's Pool");


  mgr.stopConnectionPool("Abdessamad's Pool");

  mgr.destroyConnectionPool("Abdessamad's Pool");
}}