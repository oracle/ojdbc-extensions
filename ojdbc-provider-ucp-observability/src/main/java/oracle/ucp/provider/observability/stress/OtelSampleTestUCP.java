package oracle.ucp.provider.observability.stress;

import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import oracle.ucp.UniversalConnectionPoolAdapter;
import oracle.ucp.UniversalPooledConnection;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

public class OtelSampleTestUCP {

  public static void main(String[] args) throws Exception {

    // =========================================================================
    // Init OTel SDK + Prometheus BEFORE pool creation
    // =========================================================================
    PrometheusHttpServer prometheusServer = PrometheusHttpServer.builder()
        .setPort(9464)
        .build();

    SdkMeterProvider meterProvider = SdkMeterProvider.builder()
        .registerMetricReader(prometheusServer)
        .build();

    OpenTelemetrySdk.builder()
        .setMeterProvider(meterProvider)
        .buildAndRegisterGlobal();

    System.out.println("OTel SDK initialized. Metrics at http://localhost:9464/metrics");

    // =========================================================================
    // Configure pool — same as SampleTestUCP
    // =========================================================================
    PoolDataSource pds = PoolDataSourceFactory.getPoolDataSource();
    pds.setInitialPoolSize(2);
    pds.setMinPoolSize(0);
    pds.setMaxPoolSize(50);
    pds.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
    pds.setURL("jdbc:oracle:thin:@adb_medium?TNS_ADMIN=/Users/abdessamadelaaissaoui/Desktop/Wallet_adb/");
    pds.setUser("ADMIN");
    pds.setPassword("Madara@@@1234");
    pds.setConnectionPoolName("Abdessamad's Pool");
    pds.setUCPEventListenerProvider("otel-ucp-listener");

    UniversalConnectionPoolManager mgr = UniversalConnectionPoolManagerImpl
        .getUniversalConnectionPoolManager();

    // POOL_CREATED + POOL_STARTING + POOL_STARTED
    mgr.createConnectionPool((UniversalConnectionPoolAdapter) pds);
    mgr.startConnectionPool("Abdessamad's Pool");

    // CONNECTION_BORROWED x9
    UniversalPooledConnection conn1 = mgr.getConnectionPool("Abdessamad's Pool").borrowConnection(mgr.getConnectionPool("Abdessamad's Pool").getConnectionRetrievalInfo());
    UniversalPooledConnection conn2 = mgr.getConnectionPool("Abdessamad's Pool").borrowConnection(mgr.getConnectionPool("Abdessamad's Pool").getConnectionRetrievalInfo());
    UniversalPooledConnection conn3 = mgr.getConnectionPool("Abdessamad's Pool").borrowConnection(mgr.getConnectionPool("Abdessamad's Pool").getConnectionRetrievalInfo());
    UniversalPooledConnection conn4 = mgr.getConnectionPool("Abdessamad's Pool").borrowConnection(mgr.getConnectionPool("Abdessamad's Pool").getConnectionRetrievalInfo());
    UniversalPooledConnection conn5 = mgr.getConnectionPool("Abdessamad's Pool").borrowConnection(mgr.getConnectionPool("Abdessamad's Pool").getConnectionRetrievalInfo());
    UniversalPooledConnection conn6 = mgr.getConnectionPool("Abdessamad's Pool").borrowConnection(mgr.getConnectionPool("Abdessamad's Pool").getConnectionRetrievalInfo());
    UniversalPooledConnection conn7 = mgr.getConnectionPool("Abdessamad's Pool").borrowConnection(mgr.getConnectionPool("Abdessamad's Pool").getConnectionRetrievalInfo());
    UniversalPooledConnection conn8 = mgr.getConnectionPool("Abdessamad's Pool").borrowConnection(mgr.getConnectionPool("Abdessamad's Pool").getConnectionRetrievalInfo());
    UniversalPooledConnection conn9 = mgr.getConnectionPool("Abdessamad's Pool").borrowConnection(mgr.getConnectionPool("Abdessamad's Pool").getConnectionRetrievalInfo());

    // CONNECTION_CLOSED x9
    mgr.getConnectionPool("Abdessamad's Pool").closeConnection(conn1);
    mgr.getConnectionPool("Abdessamad's Pool").closeConnection(conn2);
    mgr.getConnectionPool("Abdessamad's Pool").closeConnection(conn3);
    mgr.getConnectionPool("Abdessamad's Pool").closeConnection(conn4);
    mgr.getConnectionPool("Abdessamad's Pool").closeConnection(conn5);
    mgr.getConnectionPool("Abdessamad's Pool").closeConnection(conn6);
    mgr.getConnectionPool("Abdessamad's Pool").closeConnection(conn7);
    mgr.getConnectionPool("Abdessamad's Pool").closeConnection(conn8);
    mgr.getConnectionPool("Abdessamad's Pool").closeConnection(conn9);

    // CONNECTION_RETURNED x9
    mgr.getConnectionPool("Abdessamad's Pool").returnConnection(conn1);
    mgr.getConnectionPool("Abdessamad's Pool").returnConnection(conn2);
    mgr.getConnectionPool("Abdessamad's Pool").returnConnection(conn3);
    mgr.getConnectionPool("Abdessamad's Pool").returnConnection(conn4);
    mgr.getConnectionPool("Abdessamad's Pool").returnConnection(conn5);
    mgr.getConnectionPool("Abdessamad's Pool").returnConnection(conn6);
    mgr.getConnectionPool("Abdessamad's Pool").returnConnection(conn7);
    mgr.getConnectionPool("Abdessamad's Pool").returnConnection(conn8);
    mgr.getConnectionPool("Abdessamad's Pool").returnConnection(conn9);


    // POOL_PURGED + POOL_RECYCLED + POOL_REFRESHED
    mgr.purgeConnectionPool("Abdessamad's Pool");
    mgr.recycleConnectionPool("Abdessamad's Pool");
    mgr.refreshConnectionPool("Abdessamad's Pool");

    // POOL_STOPPED + POOL_DESTROYED
    mgr.stopConnectionPool("Abdessamad's Pool");
    mgr.destroyConnectionPool("Abdessamad's Pool");

    // Give Prometheus a moment to scrape before we print
    Thread.sleep(5000);

    System.out.println("\nAll events fired. Metrics at http://localhost:9464/metrics");
    System.out.println("Press Ctrl+C to stop.");

    Thread.currentThread().join();
  }
}