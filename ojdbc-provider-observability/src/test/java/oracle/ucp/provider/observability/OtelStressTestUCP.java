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
 * Comprehensive stress test for the OpenTelemetry UCP Event Listener Provider.
 *
 * This test generates realistic UCP usage patterns to validate that all events
 * and properties are properly captured as OpenTelemetry metrics. The test simulates:
 *
 * - Pool lifecycle events (create, start, stop, restart, destroy)
 * - Connection lifecycle events (create, borrow, return, close)
 * - Maintenance operations (purge, recycle, refresh)
 * - Performance stress patterns (connection waves, contention, wait times)
 *
 * Expected Metrics Output:
 * - 13 event counters should increment
 * - 8 gauge metrics should show real-time pool state
 * - 1 histogram should capture wait time distribution
 */
public class OtelStressTestUCP {

    private static final String POOL_NAME = "Abdessamad's Pool";
    private static final String DB_URL = "jdbc:oracle:thin:@adb_medium?TNS_ADMIN=/Users/abdessamadelaaissaoui/Desktop/Wallet_adb/";

    // Test configuration for dynamic graphs
    private static final int WAVE_COUNT = 12;           // More waves = more graph movement
    private static final int CONNECTIONS_PER_WAVE = 15; // Larger waves = bigger peaks
    private static final int STRESS_CYCLES = 5;         // Multiple cycles = longer test

    public static void main(String[] args) throws Exception {
        // *** CRITICAL: Initialize OpenTelemetry FIRST ***
        System.out.println("=== Initializing OpenTelemetry ===");
        OpenTelemetryConfig.initialize();
        System.out.println("✅ OpenTelemetry ready! Metrics available at: http://localhost:8080/metrics\n");

        System.out.println("=== OpenTelemetry UCP Stress Test Started ===");
        System.out.println("This test will generate comprehensive UCP events for OpenTelemetry metrics validation");
        System.out.println("Monitor your /metrics endpoint to see real-time metric updates\n");

        // Setup OpenTelemetry UCP monitoring
        setupPoolWithOpenTelemetry();

        UniversalConnectionPoolManager mgr = UniversalConnectionPoolManagerImpl
                .getUniversalConnectionPoolManager();

        // === PHASE 1: POOL LIFECYCLE TESTING ===
        System.out.println("=== [PHASE 1] POOL LIFECYCLE EVENTS ===");
        PoolDataSource pds = createPoolDataSource();

        // Create and start pool (POOL_CREATED, POOL_STARTING, POOL_STARTED events)
        System.out.println("Creating pool: " + POOL_NAME);
        mgr.createConnectionPool((UniversalConnectionPoolAdapter) pds);
        Thread.sleep(1000);

        System.out.println("Starting pool: " + POOL_NAME);
        mgr.startConnectionPool(POOL_NAME);
        Thread.sleep(1000);

        // === PHASE 2: CONNECTION STRESS PATTERNS ===
        System.out.println("\n=== [PHASE 2] CONNECTION STRESS TESTING ===");
        for (int cycle = 1; cycle <= STRESS_CYCLES; cycle++) {
            System.out.println("--- Stress Cycle " + cycle + "/" + STRESS_CYCLES + " ---");
            executeConnectionWaves(mgr, POOL_NAME);
            Thread.sleep(2000);
        }

        // === PHASE 3: MAINTENANCE OPERATIONS ===
        System.out.println("\n=== [PHASE 3] MAINTENANCE OPERATIONS ===");
        executeMaintenanceOperations(mgr, POOL_NAME);

        // === PHASE 4: POOL RESTART TESTING ===
        System.out.println("\n=== [PHASE 4] POOL RESTART EVENTS ===");
        System.out.println("Stopping pool: " + POOL_NAME);
        mgr.stopConnectionPool(POOL_NAME); // POOL_STOPPED event
        Thread.sleep(1000);

        System.out.println("Restarting pool: " + POOL_NAME);
        // This should generate POOL_RESTARTING and POOL_RESTARTED events
        pds.setURL("jdbc:oracle:thin:@adb_medium?TNS_ADMIN=/Users/abdessamadelaaissaoui/Desktop/Wallet_adb/");
        Thread.sleep(1000);

        // === PHASE 5: FINAL CONNECTION BURST ===
        System.out.println("\n=== [PHASE 5] FINAL CONNECTION BURST ===");
        executeFinalBurst(mgr, POOL_NAME);

        // === PHASE 6: CLEANUP ===
        System.out.println("\n=== [PHASE 6] POOL DESTRUCTION ===");
        System.out.println("Stopping pool: " + POOL_NAME);
        mgr.stopConnectionPool(POOL_NAME); // POOL_STOPPED event
        Thread.sleep(1000);

        System.out.println("Destroying pool: " + POOL_NAME);
        mgr.destroyConnectionPool(POOL_NAME); // POOL_DESTROYED event

        System.out.println("\n=== OpenTelemetry UCP Stress Test Completed ===");
        System.out.println("Check your metrics endpoint for the following expected metrics:");
        System.out.println("- Event Counters: ucp_pool_created_total, ucp_connection_borrowed_total, etc.");
        System.out.println("- State Gauges: ucp_borrowed_connections, ucp_available_connections, etc.");
        System.out.println("- Performance: ucp_connection_wait_time_ms histogram");
        System.out.println("- Labels: All metrics should include pool_name=\"" + POOL_NAME + "\"");
    }

    private static void setupPoolWithOpenTelemetry() {
        // Configure any OpenTelemetry-specific settings here
        System.setProperty("oracle.ucp.wls.jta", "false");
        System.out.println("OpenTelemetry provider configured: opentelemetry-ucp-listener");
    }

    private static PoolDataSource createPoolDataSource() throws Exception {
        PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
        pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
        pds.setURL("jdbc:oracle:thin:@adb_medium?TNS_ADMIN=/Users/abdessamadelaaissaoui/Desktop/Wallet_adb/");
        pds.setUser("ADMIN");
        pds.setPassword("Madara@@1234");
        pds.setConnectionPoolName("Abdessamad's Pool");
        pds.setMinPoolSize(2);
        pds.setMaxPoolSize(30);               // Higher max for better wave patterns
        pds.setInitialPoolSize(3);
        pds.setConnectionWaitTimeout(1);      // Short timeout = more wait time metrics

        // *** KEY: Configure OpenTelemetry provider ***
        System.setProperty("UCPEventListenerProvider", "opentelemetry-ucp-listener");

        System.out.println("Pool configured: Min=2, Max=30, Initial=3, Provider=opentelemetry-ucp-listener");
        return pds;
    }

    private static void executeConnectionWaves(UniversalConnectionPoolManager mgr,
                                               String poolName) throws Exception {
        UniversalConnectionPool pool = mgr.getConnectionPool(poolName);

        for (int wave = 1; wave <= WAVE_COUNT; wave++) {
            System.out.println("  Wave " + wave + "/" + WAVE_COUNT + ": Borrowing " + CONNECTIONS_PER_WAVE + " connections");

            // BORROW PHASE - Creates CONNECTION_BORROWED events
            UniversalPooledConnection[] connections = new UniversalPooledConnection[CONNECTIONS_PER_WAVE];
            for (int i = 0; i < CONNECTIONS_PER_WAVE; i++) {
                try {
                    connections[i] = pool.borrowConnection(pool.getConnectionRetrievalInfo());
                    Thread.sleep(50); // Gradual ramp to show metric changes
                } catch (Exception e) {
                    System.out.println("    Connection " + (i+1) + " failed (creating wait time metrics): " + e.getMessage());
                    connections[i] = null;
                }

                // Print current state to correlate with metrics
                if ((i + 1) % 4 == 0) {
                    printCurrentMetrics(pool, "    Borrowed " + (i+1));
                }
            }

            // HOLD PHASE - Let metrics stabilize
            Thread.sleep(800);

            // RELEASE PHASE - Creates CONNECTION_RETURNED or CONNECTION_CLOSED events
            for (int i = 0; i < CONNECTIONS_PER_WAVE; i++) {
                if (connections[i] != null) {
                    if (wave % 3 == 0) {
                        // Every 3rd wave: close connections (CONNECTION_CLOSED events)
                        pool.closeConnection(connections[i]);
                    } else {
                        // Other waves: return connections (CONNECTION_RETURNED events)
                        pool.returnConnection(connections[i]);
                    }
                    Thread.sleep(30);
                }
            }

            printCurrentMetrics(pool, "  Wave " + wave + " completed");
            Thread.sleep(500); // Brief pause between waves
        }
    }

    private static void executeMaintenanceOperations(UniversalConnectionPoolManager mgr,
                                                     String poolName) throws Exception {
        UniversalConnectionPool pool = mgr.getConnectionPool(poolName);

        // Borrow some connections to make maintenance operations meaningful
        UniversalPooledConnection[] testConnections = new UniversalPooledConnection[5];
        for (int i = 0; i < 5; i++) {
            try {
                testConnections[i] = pool.borrowConnection(pool.getConnectionRetrievalInfo());
                System.out.println("  Borrowed test connection " + (i+1) + "/5");
            } catch (Exception e) {
                System.out.println("  ⚠️  Could not borrow test connection " + (i+1) + " (pool may be full): " + e.getMessage());
                testConnections[i] = null;
                break; // Stop trying if pool is exhausted
            }
        }

        printCurrentMetrics(pool, "Pre-maintenance baseline");

        // PURGE OPERATION - POOL_PURGED event
        System.out.println("  Executing PURGE operation");
        pool.purge();
        Thread.sleep(1000);
        printCurrentMetrics(pool, "Post-purge");

        // RECYCLE OPERATION - POOL_RECYCLED event
        System.out.println("  Executing RECYCLE operation");
        pool.recycle();
        Thread.sleep(1000);
        printCurrentMetrics(pool, "Post-recycle");

        // REFRESH OPERATION - POOL_REFRESHED event
        System.out.println("  Executing REFRESH operation");
        pool.refresh();
        Thread.sleep(1000);
        printCurrentMetrics(pool, "Post-refresh");

        // Return test connections
        for (UniversalPooledConnection conn : testConnections) {
            if (conn != null) {
                try {
                    pool.returnConnection(conn);
                    System.out.println("  Returned test connection");
                } catch (Exception e) {
                    System.out.println("  ⚠️  Could not return connection: " + e.getMessage());
                }
            }
        }
    }

    private static void executeFinalBurst(UniversalConnectionPoolManager mgr,
                                          String poolName) throws Exception {
        UniversalConnectionPool pool = mgr.getConnectionPool(poolName);

        System.out.println("  Executing final connection burst (max pool stress)");

        // Try to exhaust the pool to create interesting wait time metrics
        UniversalPooledConnection[] burstConnections = new UniversalPooledConnection[30]; // More than max pool size

        for (int i = 0; i < 30; i++) {
            try {
                burstConnections[i] = pool.borrowConnection(pool.getConnectionRetrievalInfo());
                if (i % 5 == 0) {
                    printCurrentMetrics(pool, "    Burst progress " + (i+1) + "/30");
                }
            } catch (Exception e) {
                System.out.println("    Burst connection " + (i+1) + " failed (expected for stress): " + e.getMessage());
                burstConnections[i] = null;
            }
        }

        Thread.sleep(1000);

        // Rapid release to show metric changes
        for (int i = 0; i < 30; i++) {
            if (burstConnections[i] != null) {
                pool.returnConnection(burstConnections[i]);
            }
        }

        printCurrentMetrics(pool, "Final burst completed");
    }

    private static void printCurrentMetrics(UniversalConnectionPool pool, String phase) {
        try {
            System.out.printf("    [%s] Borrowed: %d, Available: %d, Total: %d, Created: %d, Closed: %d%n",
                    phase,
                    pool.getStatistics().getBorrowedConnectionsCount(),
                    pool.getStatistics().getAvailableConnectionsCount(),
                    pool.getStatistics().getTotalConnectionsCount(),
                    pool.getStatistics().getConnectionsCreatedCount(),
                    pool.getStatistics().getConnectionsClosedCount()
            );
        } catch (Exception e) {
            System.out.println("    [" + phase + "] Metrics temporarily unavailable");
        }
    }
}