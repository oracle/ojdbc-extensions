package oracle.ucp.provider.observability;

import oracle.ucp.UniversalConnectionPool;
import oracle.ucp.UniversalConnectionPoolAdapter;
import oracle.ucp.UniversalPooledConnection;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

public class SampleTestUCP {


    private static final String POOL_NAME = "Abdessamad's Pool";
    private static final String DB_URL = "jdbc:oracle:thin:@adb_medium?TNS_ADMIN=/Users/abdessamadelaaissaoui/Desktop/Wallet_adb-3";

    public static void main(String[] args) throws Exception {
        // Setup tracing
        System.setProperty("oracle.jdbc.Trace", "true");
        System.setProperty("java.util.logging.config.file", "./src_test/test/standalone/ucplogging.properties");
        System.setProperty("oracle.ucp.wls.jta", "false");

        // Pool configuration
        PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
        pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
        pds.setURL(DB_URL);
        pds.setUser("ADMIN");
        pds.setPassword("Madara@@1234");
        pds.setConnectionPoolName(POOL_NAME);
        pds.setMinPoolSize(5);
        pds.setMaxPoolSize(30); // Increased for wave patterns
        pds.setInitialPoolSize(5);
        pds.setUCPEventListenerProvider("jfr-ucp-listener");
        pds.setConnectionWaitTimeout(3); // Makes wait times visible

        UniversalConnectionPoolManager mgr = UniversalConnectionPoolManagerImpl
                .getUniversalConnectionPoolManager();

        // 1. POOL INITIALIZATION
        System.out.println("=== [1] POOL INITIALIZATION ===");
        mgr.createConnectionPool((UniversalConnectionPoolAdapter)pds);
        mgr.startConnectionPool(POOL_NAME);

        // 2. CONNECTION WAVES PATTERN
        System.out.println("=== [2] CONNECTION WAVES ===");
        createConnectionWaves(mgr, POOL_NAME, 5, 15); // 5 waves, 15 connections per wave

        // 3. MAINTENANCE PHASE
        System.out.println("=== [3] MAINTENANCE OPERATIONS ===");
        performMaintenanceWithMetrics(mgr, POOL_NAME);

        // 4. FINAL SHUTDOWN
        System.out.println("=== [4] POOL SHUTDOWN ===");
        mgr.stopConnectionPool(POOL_NAME);
        mgr.destroyConnectionPool(POOL_NAME);
    }

    private static void createConnectionWaves(UniversalConnectionPoolManager mgr,
                                              String poolName, int waveCount, int connectionsPerWave) throws Exception {

        UniversalConnectionPool pool = mgr.getConnectionPool(poolName);

        for (int wave = 1; wave <= waveCount; wave++) {
            System.out.println("\n--- Starting Wave " + wave + " ---");

            // Borrow phase (ramp up)
            UniversalPooledConnection[] connections = new UniversalPooledConnection[connectionsPerWave];
            for (int i = 0; i < connectionsPerWave; i++) {
                connections[i] = pool.borrowConnection(pool.getConnectionRetrievalInfo());
                Thread.sleep(100); // Gradual ramp up
                System.out.printf("Borrowed %d/%d (Active: %d, Available: %d)%n",
                        i+1, connectionsPerWave,
                        pool.getStatistics().getBorrowedConnectionsCount(),
                        pool.getStatistics().getAvailableConnectionsCount());
            }

            // Hold phase (create contention)
            Thread.sleep(1000);

            // Release phase (ramp down)
            for (int i = 0; i < connectionsPerWave; i++) {
                if (wave % 2 == 0) {
                    pool.returnConnection(connections[i]); // Even waves return
                } else {
                    pool.closeConnection(connections[i]); // Odd waves close
                }
                Thread.sleep(150); // Gradual ramp down
                System.out.printf("Released %d/%d (Active: %d, Available: %d)%n",
                        i+1, connectionsPerWave,
                        pool.getStatistics().getBorrowedConnectionsCount(),
                        pool.getStatistics().getAvailableConnectionsCount());
            }


            Thread.sleep(2000); // Clear separation between waves
        }
    }

    private static void performMaintenanceWithMetrics(UniversalConnectionPoolManager mgr,
                                                      String poolName) throws Exception {

        UniversalConnectionPool pool = mgr.getConnectionPool(poolName);
        // Baseline metrics
        printPoolMetrics("Pre-Purge", pool);
        pool.purge();
        printPoolMetrics("Post-Purge", pool);
        Thread.sleep(1500);

        printPoolMetrics("Pre-Recycle", pool);
        pool.recycle();
        printPoolMetrics("Post-Recycle", pool);
        Thread.sleep(1500);

        printPoolMetrics("Pre-Refresh", pool);
        pool.refresh();
        printPoolMetrics("Post-Refresh", pool);
        Thread.sleep(1500);
    }

    private static void printPoolMetrics(String phase, UniversalConnectionPool pool) {
    }


    /*public static void main(String args[]) throws Exception {

        System.setProperty("oracle.jdbc.Trace", "true");
        System.setProperty("java.util.logging.config.file", "./src_test/test/standalone/ucplogging.properties");
        System.setProperty("oracle.ucp.wls.jta", "false");
        // Configure the connection pool
        PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();

        pds.setInitialPoolSize(0);
        pds.setMinPoolSize(0);
        pds.setMaxPoolSize(50);
        pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
        pds.setURL("jdbc:oracle:thin:@adb_medium?TNS_ADMIN=/Users/abdessamadelaaissaoui/Desktop/Wallet_adb/");
        pds.setUser("ADMIN");
        pds.setPassword("Madara@@1234");
        pds.setConnectionPoolName("Abdessamad's Pool");

        //pds.setUCPEventListenerProvider("logging-ucp-listener");
        System.setProperty("UCPEventListenerProvider","logging-ucp-listener");

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
    }*/
}

