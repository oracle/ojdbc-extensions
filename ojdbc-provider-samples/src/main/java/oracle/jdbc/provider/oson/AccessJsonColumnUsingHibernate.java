/*
 ** Copyright (c) 2024 Oracle and/or its affiliates.
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
package oracle.jdbc.provider.oson;

import oracle.jdbc.OracleConnection;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.query.Query;

import oracle.jdbc.provider.oson.model.Emp;
import oracle.jdbc.provider.oson.model.JsonOsonSample;

import java.util.List;

/**
 *
 */

public class AccessJsonColumnUsingHibernate {


  public static void main(String[] args) {
    try {
      Session session = getSession();

      Emp emp = JacksonOsonSampleUtil.createEmp();
      JsonOsonSample jsonOsonSample = new JsonOsonSample();
      jsonOsonSample.setId(1);
      jsonOsonSample.setEmp(emp);
      session.persist(jsonOsonSample);
      session.getTransaction().commit();

      // Getting the data
      String hql = "select b.emp FROM JsonOsonSample b";
      Query query = session.createQuery(hql);
      List<Emp> results = query.list();

      for (Emp empResult : results) {
        System.out.println(empResult);
      }

      // closing the session
      session.close();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }


  private static SessionFactory sessionFactory;

  static {
    // set the system property to the class implementing the "oracle.jdbc.spi.JsonProvider interface"
    System.setProperty(OracleConnection.CONNECTION_PROPERTY_PROVIDER_JSON, JacksonOsonProvider.PROVIDER_NAME);

    Configuration configuration = new Configuration();
    configuration.setProperty(Environment.JAKARTA_JDBC_DRIVER, "oracle.jdbc.OracleDriver");
    configuration.setProperty(Environment.JAKARTA_JDBC_URL, JacksonOsonSampleUtil.URL);
    configuration.setProperty(Environment.JAKARTA_JDBC_USER, JacksonOsonSampleUtil.USER);
    configuration.setProperty(Environment.JAKARTA_JDBC_PASSWORD, JacksonOsonSampleUtil.PASSWORD);
    configuration.addAnnotatedClass(JsonOsonSample.class);

    StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties());
    sessionFactory = configuration.buildSessionFactory(builder.build());
  }

  private static Session getSession() {
    Session session = sessionFactory.openSession();
    System.out.println("session connected");
    session.beginTransaction();
    return session;
  }

}
