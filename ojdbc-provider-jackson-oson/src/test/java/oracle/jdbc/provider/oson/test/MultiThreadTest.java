package oracle.jdbc.provider.oson.test;


import oracle.jdbc.provider.oson.JacksonOsonConverter;
import oracle.jdbc.provider.oson.model.Employee;
import oracle.jdbc.provider.oson.model.EmployeeInstances;
import oracle.jdbc.provider.oson.model.Organisation;
import oracle.jdbc.provider.oson.model.OrganisationInstances;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonGenerator;
import oracle.sql.json.OracleJsonParser;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The {@code MultiThreadTest} class performs a multithreaded test that checks the serialization
 * and deserialization of {@link Employee} objects using a custom {@link JacksonOsonConverter} and
 * Oracle JSON.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MultiThreadTest {



  /**
   * Runs a multithreaded test with a thread pool of 10 threads that serializes and deserializes
   * {@link Employee} objects. It ensures that the deserialized object is equal to the original.
   *
   */
  @Test
  @Order(1)
  public void multithreadTest() {
    ExecutorService executorService = Executors.newFixedThreadPool(10);
    long start = System.currentTimeMillis();

    for (int i = 0; i < 1000; i++) {

      executorService.execute(() -> {
        try {
          Employee employee = EmployeeInstances.getEmployee();

          JacksonOsonConverter conv = new JacksonOsonConverter();
          OracleJsonFactory jsonFactory = new OracleJsonFactory();
          try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (OracleJsonGenerator generator = jsonFactory.createJsonBinaryGenerator(out)) {
              conv.serialize(generator, employee);
            }
            try(ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray())) {
              try (OracleJsonParser oParser = jsonFactory.createJsonBinaryParser(in)) {
                Employee deserEmp = (Employee) conv.deserialize(oParser, Employee.class);
                Assertions.assertEquals(employee, deserEmp);
              }
            }

          }

        } catch (Exception e) {
          e.printStackTrace();
        }
      });
    }

    executorService.shutdown();

    while (!executorService.isTerminated()) {
    }

    System.out.println("Total time: "+(System.currentTimeMillis() - start));
  }
  /**
   * Runs a multithreaded test with varying thread count that serializes and deserializes
   * {@link Employee} objects. It ensures that the deserialized object is equal to the original.
   *
   */
  @Test
  @Order(2)
  public void multithreadTest2() {

    int[] threads = new int[]{6,8,10,12,14,16};
    List<Organisation> organisations = OrganisationInstances.getInstances();
    System.out.println("Starting Test: "+organisations.size() +" instances");

    for (int thread : threads) {
      ExecutorService executorService = Executors.newFixedThreadPool(thread);
      long start = System.currentTimeMillis();
      for (int i = 0; i < 100; i++) {
        executorService.execute(() -> {
          try {
            for (Organisation organisation : organisations) {
              JacksonOsonConverter conv = new JacksonOsonConverter();
              OracleJsonFactory jsonFactory = new OracleJsonFactory();
              try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                try (OracleJsonGenerator generator = jsonFactory.createJsonBinaryGenerator(out)) {
                  conv.serialize(generator, organisation);
                }
                try(ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray())) {
                  try (OracleJsonParser oParser = jsonFactory.createJsonBinaryParser(in)) {
                    Organisation deserOrg = (Organisation) conv.deserialize(oParser, Organisation.class);
                    Assertions.assertEquals(deserOrg, organisation);
                  }
                }

              }

            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        });

      }

      executorService.shutdown();

      while (!executorService.isTerminated()) {
      }

      System.out.println("Thread Count: "+thread+" Total time: "+(System.currentTimeMillis() - start));

    }

  }

}