package oracle.ucp.provider.observability;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import oracle.ucp.events.core.UCPEventContext;
import oracle.ucp.events.core.UCPEventListener;
import oracle.ucp.provider.observability.jfr.core.JFRUCPEventListenerProvider;
import oracle.ucp.provider.observability.jfr.core.UCPEventFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static oracle.ucp.events.core.UCPEventListener.EventType;
import static org.junit.Assert.*;

public class JFRUCPEventListenerProviderTest {

  private JFRUCPEventListenerProvider provider;
  private UCPEventListener listener;
  private Recording recording;

  @Before
  public void setup() {
    provider = new JFRUCPEventListenerProvider();
    listener = provider.getListener(null);

    recording = new Recording();
    recording.enable("ucp.*");
    recording.start();
  }

  @After
  public void cleanup() {
    if (recording != null) {
      try {
        if (recording.getState() == RecordingState.RUNNING) {
          recording.stop();
        }
      } catch (IllegalStateException e) {
        // Already stopped, ignore
      } finally {
        recording.close();
      }
    }
  }

  @Test
  public void testProviderName() {
    assertEquals("jfr-ucp-listener", provider.getName());
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
  public void testProviderReturnsSingletonListener() {
    assertSame("Listener should be singleton TRACE_EVENT_LISTENER",
      JFRUCPEventListenerProvider.TRACE_EVENT_LISTENER, listener);
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
  public void testEventFactoryCreatesEvents() {
    UCPEventContext ctx = createTestContext("pool1", 1, 1, 10, 2);

    Event event = UCPEventFactory.createEvent(EventType.POOL_CREATED, ctx);
    assertNotNull("Factory should create event", event);
    assertTrue("Event should be a JFR Event", event instanceof Event);
  }

  @Test
  public void testEventFactoryCreatesAllEventTypes() {
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

    for (EventType eventType : allEvents) {
      Event event = UCPEventFactory.createEvent(eventType, ctx);
      assertNotNull("Factory should create event for " + eventType, event);
    }
  }

  @Test(expected = NullPointerException.class)
  public void testEventFactoryRejectsNullContext() {
    UCPEventFactory.createEvent(EventType.POOL_CREATED, null);
  }

  @Test(expected = NullPointerException.class)
  public void testEventFactoryRejectsNullEventType() {
    UCPEventContext ctx = createTestContext("pool1", 1, 1, 10, 2);
    UCPEventFactory.createEvent(null, ctx);
  }

  @Test
  public void testRecordEventCommitsEvent() throws IOException {
    UCPEventContext ctx = createTestContext("record-test-pool", 5, 3, 10,
      2);

    UCPEventFactory.recordEvent(EventType.CONNECTION_BORROWED, ctx);

    if (recording.getState() == RecordingState.RUNNING) {
      recording.stop();
    }
    Path recordingFile = Files.createTempFile("ucp-test", ".jfr");
    recording.dump(recordingFile);

    List<RecordedEvent> events = RecordingFile.readAllEvents(recordingFile);
    List<RecordedEvent> ucpEvents = events.stream()
      .filter(e -> e.getEventType().getName().startsWith("ucp."))
      .collect(Collectors.toList());

    assertTrue("Should have recorded at least one UCP event",
      ucpEvents.size() > 0);

    Files.deleteIfExists(recordingFile);
  }

  @Test
  public void testRecordedEventContainsPoolName() throws IOException {
    UCPEventContext ctx = createTestContext("test-pool-name", 1, 1, 10, 2);

    UCPEventFactory.recordEvent(EventType.POOL_CREATED, ctx);

    if (recording.getState() == RecordingState.RUNNING) {
      recording.stop();
    }
    Path recordingFile = Files.createTempFile("ucp-test", ".jfr");
    recording.dump(recordingFile);

    List<RecordedEvent> events = RecordingFile.readAllEvents(recordingFile);
    RecordedEvent ucpEvent = events.stream()
      .filter(e -> e.getEventType().getName().equals("ucp.PoolCreated"))
      .findFirst()
      .orElse(null);

    assertNotNull("Should find PoolCreated event", ucpEvent);
    assertEquals("test-pool-name", ucpEvent.getString("poolName"));

    Files.deleteIfExists(recordingFile);
  }

  @Test
  public void testRecordedEventContainsMetrics() throws IOException {
    UCPEventContext ctx = createTestContext("metrics-pool", 5, 3, 10, 2);

    UCPEventFactory.recordEvent(EventType.CONNECTION_BORROWED, ctx);

    if (recording.getState() == RecordingState.RUNNING) {
      recording.stop();
    }
    Path recordingFile = Files.createTempFile("ucp-test", ".jfr");
    recording.dump(recordingFile);

    List<RecordedEvent> events = RecordingFile.readAllEvents(recordingFile);
    RecordedEvent ucpEvent = events.stream()
      .filter(e ->
        e.getEventType().getName().equals("ucp.ConnectionBorrowed"))
      .findFirst()
      .orElse(null);

    assertNotNull("Should find ConnectionBorrowed event", ucpEvent);
    assertEquals("metrics-pool", ucpEvent.getString("poolName"));
    assertEquals(5, ucpEvent.getInt("borrowedConnections"));
    assertEquals(3, ucpEvent.getInt("availableConnections"));
    assertEquals(8, ucpEvent.getInt("totalConnections"));
    assertEquals(10, ucpEvent.getInt("maxPoolSize"));
    assertEquals(2, ucpEvent.getInt("minPoolSize"));

    Files.deleteIfExists(recordingFile);
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

  @Test
  public void testRapidFireEvents() {
    UCPEventContext ctx = createTestContext("rapid-pool", 1, 1, 10, 2);

    for (int i = 0; i < 1000; i++) {
      listener.onUCPEvent(EventType.CONNECTION_BORROWED, ctx);
    }
  }

  @Test
  public void testAllLifecycleEventsInSequence() {
    UCPEventContext ctx = createTestContext("lifecycle-pool", 1, 1, 10, 2);

    listener.onUCPEvent(EventType.POOL_CREATED, ctx);
    listener.onUCPEvent(EventType.POOL_STARTING, ctx);
    listener.onUCPEvent(EventType.POOL_STARTED, ctx);
    listener.onUCPEvent(EventType.CONNECTION_CREATED, ctx);
    listener.onUCPEvent(EventType.CONNECTION_BORROWED, ctx);
    listener.onUCPEvent(EventType.CONNECTION_RETURNED, ctx);
    listener.onUCPEvent(EventType.POOL_REFRESHED, ctx);
    listener.onUCPEvent(EventType.POOL_RECYCLED, ctx);
    listener.onUCPEvent(EventType.POOL_PURGED, ctx);
    listener.onUCPEvent(EventType.CONNECTION_CLOSED, ctx);
    listener.onUCPEvent(EventType.POOL_RESTARTING, ctx);
    listener.onUCPEvent(EventType.POOL_RESTARTED, ctx);
    listener.onUCPEvent(EventType.POOL_STOPPED, ctx);
    listener.onUCPEvent(EventType.POOL_DESTROYED, ctx);
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