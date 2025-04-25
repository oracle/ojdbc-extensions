package oracle.jdbc.provider.observability.tracers.otel;

import java.lang.ProcessHandle.Info;
import java.lang.management.ManagementFactory;
import java.lang.module.Configuration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import oracle.jdbc.provider.observability.ObservabilityConfiguration;
import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManagerMBeanImpl;

public class OTelMetrics {
  private DoubleHistogram duration;
  private LongHistogram returnedRows;
  private LongUpDownCounter statusCounter;
  //private LongUpDownCounter idleMaxCounter;
  private LongUpDownCounter idleMinCounter;
  private LongUpDownCounter maxCounter;
  private LongUpDownCounter pendingRequestsCounter;
  private LongCounter timeoutCounter;
  private DoubleHistogram createTime;
  private DoubleHistogram waitTime;
  private DoubleHistogram useTime;

  ObservabilityConfiguration configuration;

  /**
   * Logger.
   */
  private static Logger logger = Logger.getLogger(OTelTracer.class.getPackageName());


  public OTelMetrics(ObservabilityConfiguration configuration) {
    Meter meter = GlobalOpenTelemetry.getMeter(OTelMetrics.class.getName());
    duration = meter.histogramBuilder("db.client.operation.duration").setDescription("Operation duration").setUnit("s").build();
    returnedRows = meter.histogramBuilder("db.client.response.returned_rows").ofLongs().build();
    statusCounter = meter.upDownCounterBuilder("db.client.operation.count").build();
    //idleMaxCounter = meter.upDownCounterBuilder("db.client.operation.idle.max").build();
    idleMinCounter = meter.upDownCounterBuilder("db.client.operation.idle.min").build();
    maxCounter = meter.upDownCounterBuilder("db.client.operation.max").build();
    pendingRequestsCounter = meter.upDownCounterBuilder("db.client.operation.pending_requests").build();
    timeoutCounter = meter.counterBuilder("db.client.connection.timeouts").build();
    createTime = meter.histogramBuilder("db.client.operation.create_time").build();
    waitTime = meter.histogramBuilder("db.client.operation.wait_time").build();
    useTime = meter.histogramBuilder("db.client.operation.use_time").build();
    this.configuration = configuration;
  }

  public void recordDuration(double value, Attributes attributes) {
    logger.log(Level.INFO, "Logging duration: " + value);
    duration.record(value, attributes);
  }

  public void recordReturnedRows(long value, Attributes attributes) {
    logger.log(Level.INFO, "Logging returned rows: " + value);
    returnedRows.record(value, attributes);
  }

  public void addStatusCounter(long value, Attributes attributes) {
    logger.log(Level.INFO, "Logging status counter: " + value);
    statusCounter.add(value, attributes);
  }

  // public void addIdleMaxCounter(long value, Attributes attributes) {
  //   logger.log(Level.INFO, "Logging idle max counter: " + value);
  //   idleMaxCounter.add(value, attributes);
  // }

  public void addIdleMinCounter(long value, Attributes attributes) {
    logger.log(Level.INFO, "Logging idle min counter: " + value);
    idleMinCounter.add(value, attributes);
  }

  public void addMaxCounter(long value, Attributes attributes) {
    logger.log(Level.INFO, "Logging max counter: " + value);
    maxCounter.add(value, attributes);
  }

  public void addPendingRequestsCounter(long value, Attributes attributes) {
    logger.log(Level.INFO, "Logging pending requests counter: " + value);
    pendingRequestsCounter.add(value, attributes);
  }

  public void addTimeoutCounter(long value, Attributes attributes) {
    logger.log(Level.INFO, "Logging timeout counter: " + value);
    timeoutCounter.add(value, attributes);
  }

  public void recordCreateTime(long value, Attributes attributes) {
    logger.log(Level.INFO, "Logging create time: " + value);
    createTime.record(value, attributes);
  }

  public void recordWaitTime(long value, Attributes attributes) {
    logger.log(Level.INFO, "Logging wait time: " + value);
    waitTime.record(value, attributes);
  }

  public void recordUseTime(double value, Attributes attributes) {
    logger.log(Level.INFO, "Logging use time: " + value);
    useTime.record(value, attributes);
  }

  public void ucpProbe() {
    try {
      String poolName = System.getProperty(ObservabilityConfiguration.UCP_POOL_NAME);
      if ((poolName != null) && !poolName.isEmpty()) {
        OTelMetrics metrics = configuration.getOTelMetrics();
        String beanName = "oracle.ucp.admin.UniversalConnectionPoolMBean:name=" +
            UniversalConnectionPoolManagerMBeanImpl.getUniversalConnectionPoolManagerMBean().getMBeanNameForConnectionPool(poolName);
        logger.log(Level.INFO, beanName);
        ObjectName objectName = new ObjectName(beanName);
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        try {
          MBeanInfo info = server.getMBeanInfo(objectName);
          for (MBeanAttributeInfo attribute : info.getAttributes()) {
            logger.log(Level.INFO, "Attribute: name: " + attribute.getName() + " type: " + attribute.getType() + " description: " + attribute.getDescription());
          }
          for (MBeanOperationInfo operation : info.getOperations()) {
            logger.log(Level.INFO, "Operation: name: " + operation.getName() + " type: " + operation.getReturnType() + " description: " + operation.getDescription());
          } 
        } catch (IntrospectionException | InstanceNotFoundException | ReflectionException e1) {
          e1.printStackTrace();
        } 

        Attributes poolAttributes = Attributes.builder().put("db.client.connection.pool.name", poolName).build();
        Attributes idlePoolAttributes = Attributes.builder()
            .put("db.client.connection.pool.name", poolName)
            .put("db.client.connection.state", "idle").build();
        Attributes activePoolAttributes = Attributes.builder()
            .put("db.client.connection.pool.name", poolName)
            .put("db.client.connection.state", "used").build();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

          @Override
          public void run() {
            try {
              int maxPoolSize = Integer.parseInt(server.getAttribute(objectName, "maxPoolSize").toString());
              int borrowedConnectionsCount = Integer.parseInt(server.getAttribute(objectName, "borrowedConnectionsCount").toString());
              int availableConnectionsCount = Integer.parseInt(server.getAttribute(objectName, "availableConnectionsCount").toString());
              int averageConnectionWaitTime = Integer.parseInt(server.getAttribute(objectName, "averageConnectionWaitTime").toString());
              int idleMinCount = Integer.parseInt(server.getAttribute(objectName, "minIdle").toString());
              int pendingRequests = Integer.parseInt(server.getAttribute(objectName, "pendingRequestsCount").toString());
              int timeouts = Integer.parseInt(server.getAttribute(objectName, "cumulativeFailedConnectionWaitCount").toString());
              metrics.addStatusCounter(availableConnectionsCount, idlePoolAttributes);
              metrics.addStatusCounter(borrowedConnectionsCount, activePoolAttributes);
              metrics.recordWaitTime(averageConnectionWaitTime, poolAttributes);
              metrics.addIdleMinCounter(idleMinCount, poolAttributes);
              metrics.addMaxCounter(maxPoolSize, poolAttributes);
              metrics.addPendingRequestsCounter(pendingRequests, poolAttributes);
              metrics.addTimeoutCounter(timeouts, poolAttributes);

            } catch (NumberFormatException | InstanceNotFoundException | AttributeNotFoundException | ReflectionException
                | MBeanException e) {
              logger.log(Level.WARNING, "Could not retrieve pool attribute.", e);
            }
          }
          
        }, 5000, 5000 );

        timer.schedule(new TimerTask() {

          @Override
          public void run() {
            try {
              server.invoke(objectName, "resetCumulativePoolStatistics", null, null);
            } catch (InstanceNotFoundException | MBeanException | ReflectionException e) {
              logger.log(Level.WARNING, "Could not reset pool statistics.", e);
            }
          }
        }, 2000, 3000);

      }
    } catch (UniversalConnectionPoolException ucpException) {
      logger.log(Level.WARNING, "Could not retrieve connection pool MBean name.", ucpException);
    } catch (MalformedObjectNameException malformedObjectNameException) {
      logger.log(Level.WARNING, "Could not retrieve connection pool MBean name.", malformedObjectNameException);
    } 
  }
  
}
