/*
 ** Copyright (c) 2026 Oracle and/or its affiliates.
 **
 ** The Universal Permissive License (UPL), Version 1.0
 **
 ** Subject to the condition set forth below, permission is hereby granted to any
 ** person obtaining a copy of this software, associated documentation and/or data
 ** (collectively the "Software"), free of charge and under any and all copyright
 ** rights in the Software, and any and all patent rights owned or freely
 ** licensable by each licensor hereunder covering either (i) the unmodified
 ** Software as contributed to or provided by such licensor, or (ii) the Larger
 ** Works (as defined below), to deal in both
 **
 ** (a) the Software, and
 ** (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 ** one is included with the Software (each a "Larger Work" to which the Software
 ** is contributed by such licensors),
 **
 ** without restriction, including without limitation the rights to copy, create
 ** derivative works of, display, perform, and distribute the Software and make,
 ** use, sell, offer for sale, import, export, have made, and have sold the
 ** Software and the Larger Work(s), and to sublicense the foregoing rights on
 ** either these or other terms.
 **
 ** This license is subject to the following condition:
 ** The above copyright notice and either this complete permission notice or at
 ** a minimum a reference to the UPL must be included in all copies or
 ** substantial portions of the Software.
 **
 ** THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 ** IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 ** FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 ** AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 ** LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 ** OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 ** SOFTWARE.
 */

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
                    Assertions.assertEquals(organisation, deserOrg);
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