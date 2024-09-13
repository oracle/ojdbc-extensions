package oracle.jdbc.provider.oson;

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
