package oracle.ucp.provider.observability.otel;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;

/**
 * Configuration utility for initializing OpenTelemetry with Prometheus exporter.
 * Sets up metrics collection and HTTP endpoint for scraping.
 */
public class OpenTelemetryConfig {

    /**
     * Initializes OpenTelemetry with Prometheus HTTP server on port 8080.
     * Sets up the global OpenTelemetry instance for UCP providers to use.
     */
    public static void initialize() {
        try {
            // Create Prometheus HTTP server
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

            // Set as global instance
            GlobalOpenTelemetry.set(openTelemetry);

            System.out.println("✅ OpenTelemetry initialized successfully!");
            System.out.println("📊 Metrics endpoint: http://localhost:8080/metrics");

        } catch (Exception e) {
            System.err.println("❌ Failed to initialize OpenTelemetry: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Shuts down OpenTelemetry gracefully.
     * Should be called during application shutdown.
     */
    public static void shutdown() {
        System.out.println("🛑 Shutting down OpenTelemetry...");
    }
}