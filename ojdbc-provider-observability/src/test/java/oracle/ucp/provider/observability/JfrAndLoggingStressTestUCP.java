package oracle.ucp.provider.observability;

import oracle.ucp.UniversalConnectionPool;
import oracle.ucp.UniversalConnectionPoolAdapter;
import oracle.ucp.UniversalPooledConnection;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

/**
 * Stress test for JFR and logging UCP event listeners.
 * Generates realistic connection pool usage patterns to validate event recording.
 */
public class JfrAndLoggingStressTestUCP {

    private static final String POOL_NAME = "test-pool";
    private static final String DB_URL = "jdbc:oracle:thin:@adb_medium?TNS_ADMIN=/Users/abdessamadelaaissaoui/Desktop/Wallet_adb/";
    private static final String DB_USER = "ADMIN";
    private static final String DB_PASSWORD = "Madara@@1234";

    /**
     * Executes comprehensive pool stress test with JFR monitoring.
     *
     * @param args command line arguments
     * @throws Exception if test execution fails
     */
    public static void main(String[] args) throws Exception {
        configureTracing();

        PoolDataSource pds = createPoolDataSource();
        UniversalConnectionPoolManager mgr = UniversalConnectionPoolManagerImpl
                .getUniversalConnectionPoolManager();

        // Pool initialization
        System.out.println("=== [1] POOL INITIALIZATION ===");
        mgr.createConnectionPool((UniversalConnectionPoolAdapter) pds);
        mgr.startConnectionPool(POOL_NAME);

        // Connection wave patterns
        System.out.println("=== [2] CONNECTION WAVES ===");
        createConnectionWaves(mgr, POOL_NAME, 5, 15);

        // Maintenance operations
        System.out.println("=== [3] MAINTENANCE OPERATIONS ===");
        performMaintenanceWithMetrics(mgr, POOL_NAME);

        // Shutdown
        System.out.println("=== [4] POOL SHUTDOWN ===");
        mgr.stopConnectionPool(POOL_NAME);
        mgr.destroyConnectionPool(POOL_NAME);
    }

    /**
     * Configures JVM tracing and logging properties.
     */
    private static void configureTracing() {
        System.setProperty("oracle.jdbc.Trace", "true");
        System.setProperty("java.util.logging.config.file", "./logging.properties");
        System.setProperty("oracle.ucp.wls.jta", "false");
    }

    /**
     * Creates and configures the pool data source.
     *
     * @return configured pool data source
     * @throws Exception if configuration fails
     */
    private static PoolDataSource createPoolDataSource() throws Exception {
        PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
        pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
        pds.setURL(DB_URL);
        pds.setUser(DB_USER);
        pds.setPassword(DB_PASSWORD);
        pds.setConnectionPoolName(POOL_NAME);
        pds.setMinPoolSize(5);
        pds.setMaxPoolSize(30);
        pds.setInitialPoolSize(5);
        pds.setUCPEventListenerProvider("logging-ucp-listener");
        pds.setConnectionWaitTimeout(3);
        return pds;
    }

    /**
     * Creates waves of connection borrowing and returning to stress test the pool.
     *
     * @param mgr connection pool manager
     * @param poolName name of the pool to test
     * @param waveCount number of waves to execute
     * @param connectionsPerWave connections to borrow per wave
     * @throws Exception if wave execution fails
     */
    private static void createConnectionWaves(UniversalConnectionPoolManager mgr,
                                              String poolName, int waveCount,
                                              int connectionsPerWave) throws Exception {
        UniversalConnectionPool pool = mgr.getConnectionPool(poolName);

        for (int wave = 1; wave <= waveCount; wave++) {
            System.out.println("\n--- Starting Wave " + wave + " ---");

            // Borrow connections
            UniversalPooledConnection[] connections = new UniversalPooledConnection[connectionsPerWave];
            for (int i = 0; i < connectionsPerWave; i++) {
                connections[i] = pool.borrowConnection(pool.getConnectionRetrievalInfo());
                Thread.sleep(100);
                System.out.printf("Borrowed %d/%d (Active: %d, Available: %d)%n",
                        i + 1, connectionsPerWave,
                        pool.getStatistics().getBorrowedConnectionsCount(),
                        pool.getStatistics().getAvailableConnectionsCount());
            }

            Thread.sleep(1000);

            // Release connections
            for (int i = 0; i < connectionsPerWave; i++) {
                if (wave % 2 == 0) {
                    pool.returnConnection(connections[i]);
                } else {
                    pool.closeConnection(connections[i]);
                }
                Thread.sleep(150);
                System.out.printf("Released %d/%d (Active: %d, Available: %d)%n",
                        i + 1, connectionsPerWave,
                        pool.getStatistics().getBorrowedConnectionsCount(),
                        pool.getStatistics().getAvailableConnectionsCount());
            }

            Thread.sleep(2000);
        }
    }

    /**
     * Performs maintenance operations on the pool and prints metrics.
     *
     * @param mgr connection pool manager
     * @param poolName name of the pool
     * @throws Exception if maintenance operations fail
     */
    private static void performMaintenanceWithMetrics(UniversalConnectionPoolManager mgr,
                                                      String poolName) throws Exception {
        UniversalConnectionPool pool = mgr.getConnectionPool(poolName);

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

    /**
     * Prints current pool metrics for the specified phase.
     *
     * @param phase description of the current phase
     * @param pool connection pool to analyze
     */
    private static void printPoolMetrics(String phase, UniversalConnectionPool pool) {
        try {
            System.out.printf("[%s] Borrowed: %d, Available: %d, Total: %d%n",
                    phase,
                    pool.getStatistics().getBorrowedConnectionsCount(),
                    pool.getStatistics().getAvailableConnectionsCount(),
                    pool.getStatistics().getTotalConnectionsCount());
        } catch (Exception e) {
            System.out.println("[" + phase + "] Metrics unavailable: " + e.getMessage());
        }
    }
}