package oracle.ucp.provider.observability;

import oracle.ucp.UniversalConnectionPool;
import oracle.ucp.UniversalConnectionPoolAdapter;
import oracle.ucp.UniversalPooledConnection;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import oracle.ucp.provider.observability.otel.OpenTelemetryConfig;

/**
 * Comprehensive stress test for OpenTelemetry UCP event listener.
 * Generates realistic usage patterns to validate metric collection including
 * pool lifecycle, connection operations, and maintenance events.
 */
public class OtelStressTestUCP {

    private static final String POOL_NAME = "test-pool";
    private static final String DB_URL = "jdbc:oracle:thin:@adb_medium?TNS_ADMIN=/Users/abdessamadelaaissaoui/Desktop/Wallet_adb/";
    private static final String DB_USER = "ADMIN";
    private static final String DB_PASSWORD = "Madara@@1234";

    // Test configuration
    private static final int WAVE_COUNT = 12;
    private static final int CONNECTIONS_PER_WAVE = 15;
    private static final int STRESS_CYCLES = 5;

    /**
     * Executes comprehensive OpenTelemetry stress test.
     *
     * @param args command line arguments
     * @throws Exception if test execution fails
     */
    public static void main(String[] args) throws Exception {
        // Initialize OpenTelemetry
        System.out.println("=== Initializing OpenTelemetry ===");
        OpenTelemetryConfig.initialize();
        System.out.println("OpenTelemetry ready! Metrics: http://localhost:8080/metrics\n");

        setupPoolWithOpenTelemetry();
        UniversalConnectionPoolManager mgr = UniversalConnectionPoolManagerImpl
                .getUniversalConnectionPoolManager();

        // Phase 1: Pool lifecycle
        System.out.println("=== [PHASE 1] POOL LIFECYCLE EVENTS ===");
        PoolDataSource pds = createPoolDataSource();
        mgr.createConnectionPool((UniversalConnectionPoolAdapter) pds);
        Thread.sleep(1000);
        mgr.startConnectionPool(POOL_NAME);
        Thread.sleep(1000);

        // Phase 2: Connection stress
        System.out.println("\n=== [PHASE 2] CONNECTION STRESS TESTING ===");
        for (int cycle = 1; cycle <= STRESS_CYCLES; cycle++) {
            System.out.println("--- Stress Cycle " + cycle + "/" + STRESS_CYCLES + " ---");
            executeConnectionWaves(mgr, POOL_NAME);
            Thread.sleep(2000);
        }

        // Phase 3: Maintenance operations
        System.out.println("\n=== [PHASE 3] MAINTENANCE OPERATIONS ===");
        executeMaintenanceOperations(mgr, POOL_NAME);

        // Phase 4: Pool restart
        System.out.println("\n=== [PHASE 4] POOL RESTART EVENTS ===");
        mgr.stopConnectionPool(POOL_NAME);
        Thread.sleep(1000);

        // Phase 5: Final burst
        System.out.println("\n=== [PHASE 5] FINAL CONNECTION BURST ===");
        executeFinalBurst(mgr, POOL_NAME);

        // Phase 6: Cleanup
        System.out.println("\n=== [PHASE 6] POOL DESTRUCTION ===");
        mgr.stopConnectionPool(POOL_NAME);
        Thread.sleep(1000);
        mgr.destroyConnectionPool(POOL_NAME);

        System.out.println("\n=== OpenTelemetry UCP Stress Test Completed ===");
        printExpectedMetrics();
    }

    /**
     * Configures OpenTelemetry-specific settings.
     */
    private static void setupPoolWithOpenTelemetry() {
        System.setProperty("oracle.ucp.wls.jta", "false");
        System.setProperty("UCPEventListenerProvider", "opentelemetry-ucp-listener");
        System.out.println("OpenTelemetry provider configured");
    }

    /**
     * Creates and configures pool data source for OpenTelemetry testing.
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
        pds.setMinPoolSize(2);
        pds.setMaxPoolSize(30);
        pds.setInitialPoolSize(3);
        pds.setConnectionWaitTimeout(1);

        System.out.println("Pool configured: Min=2, Max=30, Initial=3");
        return pds;
    }

    /**
     * Executes connection waves to generate borrowing and returning events.
     *
     * @param mgr connection pool manager
     * @param poolName name of the pool
     * @throws Exception if wave execution fails
     */
    private static void executeConnectionWaves(UniversalConnectionPoolManager mgr,
                                               String poolName) throws Exception {
        UniversalConnectionPool pool = mgr.getConnectionPool(poolName);

        for (int wave = 1; wave <= WAVE_COUNT; wave++) {
            System.out.println("  Wave " + wave + "/" + WAVE_COUNT +
                    ": Borrowing " + CONNECTIONS_PER_WAVE + " connections");

            // Borrow phase
            UniversalPooledConnection[] connections = new UniversalPooledConnection[CONNECTIONS_PER_WAVE];
            for (int i = 0; i < CONNECTIONS_PER_WAVE; i++) {
                try {
                    connections[i] = pool.borrowConnection(pool.getConnectionRetrievalInfo());
                    Thread.sleep(50);
                } catch (Exception e) {
                    System.out.println("    Connection " + (i + 1) + " failed: " + e.getMessage());
                    connections[i] = null;
                }

                if ((i + 1) % 4 == 0) {
                    printCurrentMetrics(pool, "    Borrowed " + (i + 1));
                }
            }

            Thread.sleep(800);

            // Release phase
            for (int i = 0; i < CONNECTIONS_PER_WAVE; i++) {
                if (connections[i] != null) {
                    if (wave % 3 == 0) {
                        pool.closeConnection(connections[i]);
                    } else {
                        pool.returnConnection(connections[i]);
                    }
                    Thread.sleep(30);
                }
            }

            printCurrentMetrics(pool, "  Wave " + wave + " completed");
            Thread.sleep(500);
        }
    }

    /**
     * Executes maintenance operations to generate maintenance events.
     *
     * @param mgr connection pool manager
     * @param poolName name of the pool
     * @throws Exception if maintenance fails
     */
    private static void executeMaintenanceOperations(UniversalConnectionPoolManager mgr,
                                                     String poolName) throws Exception {
        UniversalConnectionPool pool = mgr.getConnectionPool(poolName);

        // Borrow test connections
        UniversalPooledConnection[] testConnections = new UniversalPooledConnection[5];
        for (int i = 0; i < 5; i++) {
            try {
                testConnections[i] = pool.borrowConnection(pool.getConnectionRetrievalInfo());
                System.out.println("  Borrowed test connection " + (i + 1) + "/5");
            } catch (Exception e) {
                System.out.println("  Could not borrow test connection " + (i + 1));
                testConnections[i] = null;
                break;
            }
        }

        printCurrentMetrics(pool, "Pre-maintenance baseline");

        // Execute maintenance operations
        System.out.println("  Executing PURGE operation");
        pool.purge();
        Thread.sleep(1000);
        printCurrentMetrics(pool, "Post-purge");

        System.out.println("  Executing RECYCLE operation");
        pool.recycle();
        Thread.sleep(1000);
        printCurrentMetrics(pool, "Post-recycle");

        System.out.println("  Executing REFRESH operation");
        pool.refresh();
        Thread.sleep(1000);
        printCurrentMetrics(pool, "Post-refresh");

        // Return test connections
        for (UniversalPooledConnection conn : testConnections) {
            if (conn != null) {
                try {
                    pool.returnConnection(conn);
                } catch (Exception e) {
                    System.out.println("  Could not return connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Executes final connection burst to stress test pool limits.
     *
     * @param mgr connection pool manager
     * @param poolName name of the pool
     * @throws Exception if burst execution fails
     */
    private static void executeFinalBurst(UniversalConnectionPoolManager mgr,
                                          String poolName) throws Exception {
        UniversalConnectionPool pool = mgr.getConnectionPool(poolName);
        System.out.println("  Executing final connection burst");

        UniversalPooledConnection[] burstConnections = new UniversalPooledConnection[30];

        for (int i = 0; i < 30; i++) {
            try {
                burstConnections[i] = pool.borrowConnection(pool.getConnectionRetrievalInfo());
                if (i % 5 == 0) {
                    printCurrentMetrics(pool, "    Burst progress " + (i + 1) + "/30");
                }
            } catch (Exception e) {
                System.out.println("    Burst connection " + (i + 1) + " failed (expected)");
                burstConnections[i] = null;
            }
        }

        Thread.sleep(1000);

        // Rapid release
        for (int i = 0; i < 30; i++) {
            if (burstConnections[i] != null) {
                pool.returnConnection(burstConnections[i]);
            }
        }

        printCurrentMetrics(pool, "Final burst completed");
    }

    /**
     * Prints current pool metrics for correlation with OpenTelemetry data.
     *
     * @param pool connection pool to analyze
     * @param phase description of current phase
     */
    private static void printCurrentMetrics(UniversalConnectionPool pool, String phase) {
        try {
            System.out.printf("    [%s] Borrowed: %d, Available: %d, Total: %d, Created: %d, Closed: %d%n",
                    phase,
                    pool.getStatistics().getBorrowedConnectionsCount(),
                    pool.getStatistics().getAvailableConnectionsCount(),
                    pool.getStatistics().getTotalConnectionsCount(),
                    pool.getStatistics().getConnectionsCreatedCount(),
                    pool.getStatistics().getConnectionsClosedCount());
        } catch (Exception e) {
            System.out.println("    [" + phase + "] Metrics temporarily unavailable");
        }
    }

    /**
     * Prints expected metrics information for validation.
     */
    private static void printExpectedMetrics() {
        System.out.println("Expected metrics at http://localhost:8080/metrics:");
        System.out.println("- Event Counters: ucp_pool_created_total, ucp_connection_borrowed_total, etc.");
        System.out.println("- State Gauges: ucp_borrowed_connections, ucp_available_connections, etc.");
        System.out.println("- Performance: ucp_connection_wait_time_ms histogram");
        System.out.println("- Labels: pool_name=\"" + POOL_NAME + "\"");
    }
}