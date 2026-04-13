package oracle.ucp.provider.observability.stress;

import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import oracle.jdbc.OracleConnection;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Exhausting stress test for the OTel UCP provider.
 *
 * Phases:
 *   1.  Warm-up          — slow ramp to initial pool size
 *   2.  Full saturation  — borrow all MAX slots, hold, force waiters to queue
 *   3.  Burst release    — return all at once (cliff drop on charts)
 *   4.  Cascade fill     — borrow one-by-one every 200ms (visible staircase up)
 *   5.  Cascade drain    — return one-by-one every 200ms (visible staircase down)
 *   6.  Hammer           — 30 threads vs 10 slots, high contention, random holds
 *   7.  Sawtooth x10     — rapid flood+drain cycles, 10 rounds
 *   8.  Spike burst x5   — instantaneous borrow-all then hold 3s then release-all
 *   9.  Maintenance      — purge, recycle, refresh (triggers churn metrics)
 *   10. Cool-down        — pool returns to min idle
 *
 * NOT pushed to repo — local verification only.
 */
public class OtelUCPTest {

  private static final int INITIAL_POOL_SIZE = 5;
  private static final int MIN_POOL_SIZE     = 3;
  private static final int MAX_POOL_SIZE     = 25;

  // Phase 6 — Hammer
  private static final int HAMMER_THREADS = 40;
  private static final int HAMMER_CYCLES  = 20;
  private static final int HOLD_MIN_MS    = 50;
  private static final int HOLD_MAX_MS    = 4000;

  // Phase 7 — Sawtooth
  private static final int SAWTOOTH_ROUNDS    = 10;
  private static final int SAWTOOTH_HOLD_MS   = 1500;
  private static final int SAWTOOTH_PAUSE_MS  = 800;

  // Phase 8 — Spike bursts
  private static final int SPIKE_ROUNDS   = 5;
  private static final int SPIKE_HOLD_MS  = 3000;

  private static final int SCRAPE_PAUSE_MS = 4000;

  private static final Random RANDOM = new Random();

  public static void main(String[] args) throws Exception {

    // =========================================================================
    // Init OTel SDK BEFORE pool creation
    // =========================================================================
    PrometheusHttpServer prometheusServer = PrometheusHttpServer.builder()
        .setPort(9464)
        .build();

    SdkMeterProvider meterProvider = SdkMeterProvider.builder()
        .registerMetricReader(prometheusServer)
        .build();

    OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
        .setMeterProvider(meterProvider)
        .buildAndRegisterGlobal();

    log("OTel SDK initialized. Metrics at http://localhost:9464/metrics");
    log("Grafana:    http://localhost:3000");
    log("Prometheus: http://localhost:9090\n");

    // =========================================================================
    // Configure pool — small max so saturation is easy to reach
    // =========================================================================
    PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
    pds.setInitialPoolSize(INITIAL_POOL_SIZE);
    pds.setMinPoolSize(MIN_POOL_SIZE);
    pds.setMaxPoolSize(MAX_POOL_SIZE);
    pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
    pds.setURL("jdbc:oracle:thin:@adb_medium?TNS_ADMIN=/Users/abdessamadelaaissaoui/Desktop/Wallet_adb/");
    pds.setUser("ADMIN");
    pds.setPassword("Madara@@@1234");
    pds.setConnectionPoolName("OtelTestPool");
    pds.setUCPEventListenerProvider("otel-ucp-listener");

    // =========================================================================
    // PHASE 1 — Warm-up: gradual ramp
    // =========================================================================
    phase("1 — Warm-up: slow ramp to initial pool size");
    List<OracleConnection> warmup = new ArrayList<>();
    for (int i = 1; i <= INITIAL_POOL_SIZE; i++) {
      warmup.add(borrow(pds, "warmup-" + i));
      sleep(600);
    }
    sleep(SCRAPE_PAUSE_MS);
    for (OracleConnection c : warmup) returnConn(c, "warmup");
    warmup.clear();
    log("  Warm-up released.\n");
    sleep(SCRAPE_PAUSE_MS);

    // =========================================================================
    // PHASE 2 — Full saturation + waiters
    // =========================================================================
    phase("2 — Full saturation: borrow all " + MAX_POOL_SIZE + " connections + 5 waiters");
    List<OracleConnection> saturated = new ArrayList<>();
    for (int i = 1; i <= MAX_POOL_SIZE; i++) {
      saturated.add(borrow(pds, "sat-" + i));
      log("  Slot " + i + "/" + MAX_POOL_SIZE + " taken");
    }
    log("  Pool fully saturated. Holding for " + (SCRAPE_PAUSE_MS * 2) + "ms...");
    sleep(SCRAPE_PAUSE_MS * 2);

    // 5 waiters — more than before, longer hold → bigger wait_time spike
    log("  Launching 5 waiters...");
    ExecutorService waiters = Executors.newFixedThreadPool(5);
    for (int i = 0; i < 5; i++) {
      waiters.submit(() -> {
        try {
          OracleConnection c = (OracleConnection) pds.getConnection();
          log("  Waiter got connection");
          sleep(1000);
          c.close();
        } catch (Exception e) {
          log("  Waiter timed out: " + e.getMessage());
        }
      });
    }
    sleep(3000);

    // =========================================================================
    // PHASE 3 — Burst release: return all at once (cliff drop)
    // =========================================================================
    phase("3 — Burst release: return all connections at once");
    for (OracleConnection c : saturated) returnConn(c, "sat");
    saturated.clear();
    waiters.shutdown();
    waiters.awaitTermination(15, TimeUnit.SECONDS);
    log("  Burst release done.\n");
    sleep(SCRAPE_PAUSE_MS);

    // =========================================================================
    // PHASE 4 — Cascade fill: borrow one-by-one (staircase up)
    // =========================================================================
    phase("4 — Cascade fill: borrow one-by-one every 200ms");
    List<OracleConnection> cascade = new ArrayList<>();
    for (int i = 1; i <= MAX_POOL_SIZE; i++) {
      cascade.add(borrow(pds, "cascade-" + i));
      log("  Filled " + i + "/" + MAX_POOL_SIZE);
      sleep(200);
    }
    sleep(SCRAPE_PAUSE_MS);

    // =========================================================================
    // PHASE 5 — Cascade drain: return one-by-one (staircase down)
    // =========================================================================
    phase("5 — Cascade drain: return one-by-one every 200ms");
    for (int i = 0; i < cascade.size(); i++) {
      returnConn(cascade.get(i), "cascade-" + (i + 1));
      log("  Drained " + (i + 1) + "/" + cascade.size());
      sleep(200);
    }
    cascade.clear();
    log("  Cascade drain done.\n");
    sleep(SCRAPE_PAUSE_MS);

    // =========================================================================
    // PHASE 6 — Hammer: 30 threads vs 10 slots
    // =========================================================================
    phase("6 — Hammer: " + HAMMER_THREADS + " threads vs " + MAX_POOL_SIZE
        + " slots (" + HAMMER_CYCLES + " cycles, hold " + HOLD_MIN_MS + "-" + HOLD_MAX_MS + "ms)");
    ExecutorService hammer = Executors.newFixedThreadPool(HAMMER_THREADS);
    CountDownLatch hammerDone = new CountDownLatch(HAMMER_THREADS);
    AtomicInteger totalBorrows = new AtomicInteger(0);

    for (int t = 0; t < HAMMER_THREADS; t++) {
      final int tid = t + 1;
      hammer.submit(() -> {
        for (int cycle = 0; cycle < HAMMER_CYCLES; cycle++) {
          try {
            OracleConnection conn = (OracleConnection) pds.getConnection();
            int hold = HOLD_MIN_MS + RANDOM.nextInt(HOLD_MAX_MS - HOLD_MIN_MS);
            try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM DUAL");
                 ResultSet rs = ps.executeQuery()) {
              rs.next();
            }
            sleep(hold);
            conn.close();
            int total = totalBorrows.incrementAndGet();
            if (total % 30 == 0) log("  Hammer: " + total + " borrows completed");
          } catch (Exception e) {
            log("  Thread-" + tid + " error: " + e.getMessage());
          }
        }
        hammerDone.countDown();
      });
    }

    hammerDone.await(15, TimeUnit.MINUTES);
    hammer.shutdown();
    log("  Hammer done. Total borrows: " + totalBorrows.get() + "\n");
    sleep(SCRAPE_PAUSE_MS);

    // =========================================================================
    // PHASE 7 — Sawtooth: 10 rapid flood+drain cycles
    // =========================================================================
    phase("7 — Sawtooth x" + SAWTOOTH_ROUNDS + ": rapid flood+drain");
    for (int round = 1; round <= SAWTOOTH_ROUNDS; round++) {
      log("  Round " + round + "/" + SAWTOOTH_ROUNDS + " — flooding...");
      List<OracleConnection> flood = new ArrayList<>();
      for (int i = 0; i < MAX_POOL_SIZE; i++) {
        flood.add(borrow(pds, "saw-r" + round + "-" + i));
      }
      log("  Round " + round + " — holding " + SAWTOOTH_HOLD_MS + "ms...");
      sleep(SAWTOOTH_HOLD_MS);
      for (OracleConnection c : flood) returnConn(c, "saw-r" + round);
      flood.clear();
      log("  Round " + round + " — drained.");
      sleep(SAWTOOTH_PAUSE_MS);
    }
    log("  Sawtooth done.\n");
    sleep(SCRAPE_PAUSE_MS);

    // =========================================================================
    // PHASE 8 — Spike bursts: instantaneous borrow-all, hold 3s, release-all
    // =========================================================================
    phase("8 — Spike bursts x" + SPIKE_ROUNDS + ": instantaneous fill, hold " + SPIKE_HOLD_MS + "ms, release");
    for (int round = 1; round <= SPIKE_ROUNDS; round++) {
      log("  Spike " + round + "/" + SPIKE_ROUNDS + " — borrowing all...");
      // Use a synchronized list — multiple threads add connections concurrently
      List<OracleConnection> spike = Collections.synchronizedList(new ArrayList<>());
      ExecutorService spiker = Executors.newFixedThreadPool(MAX_POOL_SIZE);
      CountDownLatch spikeDone = new CountDownLatch(MAX_POOL_SIZE);
      for (int i = 0; i < MAX_POOL_SIZE; i++) {
        spiker.submit(() -> {
          try {
            OracleConnection c = (OracleConnection) pds.getConnection();
            spike.add(c);
          } catch (Exception e) {
            log("  Spike borrow failed: " + e.getMessage());
          } finally {
            spikeDone.countDown();
          }
        });
      }
      spikeDone.await(15, TimeUnit.SECONDS);
      spiker.shutdown();
      log("  Spike " + round + " — " + spike.size() + " connections held for " + SPIKE_HOLD_MS + "ms...");
      sleep(SPIKE_HOLD_MS);
      // Snapshot the list before iterating to avoid ConcurrentModificationException
      List<OracleConnection> toReturn = new ArrayList<>(spike);
      for (OracleConnection c : toReturn) returnConn(c, "spike-" + round);
      spike.clear();
      log("  Spike " + round + " — released. Pause 1s...");
      sleep(1000);
    }
    log("  Spike bursts done.\n");
    sleep(SCRAPE_PAUSE_MS);

    // =========================================================================
    // PHASE 9 — Maintenance: purge, recycle, refresh
    // =========================================================================
    phase("9 — Maintenance: purge, recycle, refresh");
    try {
      UniversalConnectionPoolManager mgr =
          UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager();
      log("  Purging pool...");
      mgr.purgeConnectionPool("OtelTestPool");
      sleep(1500);
      log("  Recycling pool...");
      mgr.recycleConnectionPool("OtelTestPool");
      sleep(1500);
      log("  Refreshing pool...");
      mgr.refreshConnectionPool("OtelTestPool");
      sleep(1500);
      log("  Maintenance done.\n");
    } catch (Exception e) {
      log("  Maintenance skipped: " + e.getMessage() + "\n");
    }
    sleep(SCRAPE_PAUSE_MS);

    // =========================================================================
    // PHASE 10 — Cool-down
    // =========================================================================
    phase("10 — Cool-down: pool returning to minimum idle state");
    sleep(SCRAPE_PAUSE_MS * 2);
    log("  All phases complete!\n");

    log("Prometheus endpoint running at http://localhost:9464/metrics");
    log("Metrics exported:");
    log("  db_client_connection_count{state=\"used\"}  — used connections       (Gauge)");
    log("  db_client_connection_count{state=\"idle\"}  — idle connections       (Gauge)");
    log("  db_client_connection_max                  — configured max          (Gauge)");
    log("  db_client_connection_idle_min             — configured min idle     (Gauge)");
    log("  db_client_connection_wait_time_seconds    — borrow wait time        (Histogram)");
    log("  db_client_connection_established          — cumulative opened       (Gauge)");
    log("  db_client_connection_closed               — cumulative closed       (Gauge)");
    log("\nPress Ctrl+C to stop.");

    Thread.currentThread().join();
    openTelemetry.close();
  }

  private static OracleConnection borrow(PoolDataSource pds, String label) throws Exception {
    OracleConnection conn = (OracleConnection) pds.getConnection();
    log("  [borrow] " + label);
    return conn;
  }

  private static void returnConn(OracleConnection conn, String label) {
    try {
      conn.close();
      log("  [return] " + label);
    } catch (Exception e) {
      log("  [return error] " + label + ": " + e.getMessage());
    }
  }

  private static void phase(String name) {
    log("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    log("  PHASE " + name);
    log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
  }

  private static void sleep(long ms) {
    try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
  }

  private static void log(String msg) {
    System.out.println(msg);
  }
}