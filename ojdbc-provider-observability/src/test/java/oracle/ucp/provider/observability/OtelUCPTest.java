package oracle.ucp.provider.observability;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.events.core.UCPEventListener;
import oracle.ucp.provider.observability.otel.OtelUCPEventListenerProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static oracle.ucp.events.core.UCPEventListener.EventType;
import static org.junit.Assert.*;

public class OtelUCPTest {

  private InMemoryMetricReader metricReader;
  private OtelUCPEventListenerProvider provider;
  private UCPEventListener listener;

  @Before
  public void setup() {
    GlobalOpenTelemetry.resetForTest();

    metricReader = InMemoryMetricReader.create();
    SdkMeterProvider meterProvider = SdkMeterProvider.builder()
      .registerMetricReader(metricReader)
      .build();

    OpenTelemetrySdk.builder()
      .setMeterProvider(meterProvider)
      .buildAndRegisterGlobal();

    provider = new OtelUCPEventListenerProvider();
    listener = provider.getListener(null);
  }

  @After
  public void cleanup() {
    GlobalOpenTelemetry.resetForTest();
  }

  @Test
  public void testProviderName() {
    assertEquals("otel-ucp-listener", provider.getName());
  }

  @Test
  public void testProviderReturnsListener() {
    assertNotNull("Provider should return a listener", listener);
  }

  @Test
  public void testProviderReturnsSameListenerInstance() {
    UCPEventListener listener1 = provider.getListener(null);
    UCPEventListener listener2 = provider.getListener(new HashMap<>());
    assertSame("Provider should return same listener instance", listener1,
      listener2);
  }

  @Test
  public void testListenerAcceptsEvents() {
    UCPEventContext ctx = createTestContext("pool1", 1, 1, 10, 2);

    listener.onUCPEvent(EventType.POOL_CREATED, ctx);
    listener.onUCPEvent(EventType.CONNECTION_BORROWED, ctx);
    listener.onUCPEvent(EventType.CONNECTION_RETURNED, ctx);
  }

  @Test
  public void testAllEventTypesAccepted() {
    EventType[] allEvents = {
      EventType.POOL_CREATED, EventType.POOL_STARTING,
      EventType.POOL_STARTED, EventType.POOL_STOPPED,
      EventType.POOL_RESTARTING, EventType.POOL_RESTARTED,
      EventType.POOL_DESTROYED, EventType.CONNECTION_CREATED,
      EventType.CONNECTION_BORROWED, EventType.CONNECTION_RETURNED,
      EventType.CONNECTION_CLOSED, EventType.POOL_REFRESHED,
      EventType.POOL_RECYCLED, EventType.POOL_PURGED
    };

    UCPEventContext ctx = createTestContext("test-pool", 1, 1, 10, 2);

    for (EventType event : allEvents) {
      listener.onUCPEvent(event, ctx);
    }
  }

  @Test
  public void testNullContextIgnored() {
    listener.onUCPEvent(EventType.CONNECTION_BORROWED, null);
  }

  @Test
  public void testNullEventTypeIgnored() {
    UCPEventContext ctx = createTestContext("pool1", 1, 1, 10, 2);
    listener.onUCPEvent(null, ctx);
  }

  @Test
  public void testEmptyPoolNameAccepted() {
    UCPEventContext ctx = createTestContext("", 1, 1, 10, 2);
    listener.onUCPEvent(EventType.CONNECTION_BORROWED, ctx);
  }

  @Test
  public void testVeryLongPoolNameAccepted() {
    String longName = new String(new char[1000]).replace('\0', 'a');
    UCPEventContext ctx = createTestContext(longName, 1, 1, 10, 2);
    listener.onUCPEvent(EventType.POOL_CREATED, ctx);
  }

  @Test
  public void testZeroValuesAccepted() {
    UCPEventContext ctx = createTestContext("pool1", 0, 0, 0, 0);
    listener.onUCPEvent(EventType.POOL_CREATED, ctx);
  }

  @Test
  public void testLargeValuesAccepted() {
    UCPEventContext ctx = createTestContext("pool1",
      Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 0);
    listener.onUCPEvent(EventType.CONNECTION_BORROWED, ctx);
  }

  @Test
  public void testMultiplePoolsAccepted() {
    listener.onUCPEvent(EventType.POOL_CREATED,
      createTestContext("pool1", 1, 1, 10, 2));
    listener.onUCPEvent(EventType.POOL_CREATED,
      createTestContext("pool2", 2, 2, 20, 4));
    listener.onUCPEvent(EventType.POOL_CREATED,
      createTestContext("pool3", 3, 3, 30, 6));
  }

  @Test
  public void testConcurrentAccess() throws InterruptedException {
    Thread[] threads = new Thread[5];
    for (int i = 0; i < 5; i++) {
      final int threadId = i;
      threads[i] = new Thread(() -> {
        for (int j = 0; j < 10; j++) {
          UCPEventContext ctx = createTestContext("pool" + threadId, j, j,
            10, 2);
          listener.onUCPEvent(EventType.CONNECTION_BORROWED, ctx);
        }
      });
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }
  }

  private UCPEventContext createTestContext(String poolName, int borrowed,
    int available, int max, int min) {
    return new UCPEventContext() {
      @Override
      public String poolName() {
        return poolName;
      }

      @Override
      public long timestamp() {
        return System.currentTimeMillis();
      }

      @Override
      public int borrowedConnectionsCount() {
        return borrowed;
      }

      @Override
      public int availableConnectionsCount() {
        return available;
      }

      @Override
      public int totalConnections() {
        return borrowed + available;
      }

      @Override
      public int maxPoolSize() {
        return max;
      }

      @Override
      public int minPoolSize() {
        return min;
      }

      @Override
      public long getAverageConnectionWaitTime() {
        return 0;
      }

      @Override
      public int createdConnections() {
        return borrowed + available;
      }

      @Override
      public int closedConnections() {
        return 0;
      }

      @Override
      public String formattedTimestamp() {
        return new java.text.SimpleDateFormat(
          "MMMM dd, yyyy HH:mm:ss.SSS z")
          .format(new java.util.Date(timestamp()));
      }
    };
  }
}