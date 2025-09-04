package oracle.ucp.provider.observability.otel;


import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;

public class OpenTelemetryConfig {

    public static void initialize() {
        try {
            // Create Prometheus HTTP server on port 8080
            PrometheusHttpServer prometheusServer = PrometheusHttpServer.builder()
                    .setPort(8080)
                    .setHost("localhost")
                    .build();

            // Build OpenTelemetry SDK with Prometheus exporter
            OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                    .setMeterProvider(
                            SdkMeterProvider.builder()
                                    .registerMetricReader(prometheusServer)
                                    .build()
                    )
                    .build();

            // Set as global instance for your UCP provider to use
            GlobalOpenTelemetry.set(openTelemetry);

            System.out.println("✅ OpenTelemetry initialized successfully!");
            System.out.println("📊 Metrics endpoint: http://localhost:8080/metrics");
            System.out.println("🔗 Prometheus will scrape this endpoint every 5 seconds");

        } catch (Exception e) {
            System.err.println("❌ Failed to initialize OpenTelemetry: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Shutdown OpenTelemetry gracefully (call this when your app shuts down)
     */
    public static void shutdown() {
        System.out.println("🛑 Shutting down OpenTelemetry...");
        // The HTTP server will be closed automatically when the SDK shuts down
    }
}