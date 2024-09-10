package oson.test;

import oracle.jdbc.jackson.oson.provider.JacksonOsonConverter;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonGenerator;
import oracle.sql.json.OracleJsonParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SerializerTest {
  public static void main(String[] args) throws  IOException {
   JacksonOsonConverter conv = new JacksonOsonConverter();

   OracleJsonFactory factory = new OracleJsonFactory();
   ByteArrayOutputStream out = new ByteArrayOutputStream();
   
   byte[] bytes = new byte[] { 1 };
   Employee emp = new Employee( "salah", "eng" ,bytes);
   
   OracleJsonGenerator oGen = factory.createJsonBinaryGenerator(out);
   conv.serialize(oGen, emp );
   oGen.close();
   
   OracleJsonParser oParser = factory.createJsonBinaryParser(new ByteArrayInputStream(out.toByteArray()));
   Employee j = (Employee) conv.deserialize(oParser, Employee.class);

   System.out.println(j.name);

  }
}


class Employee {

  String name;

  String job;

  byte[] bytes;

  public Employee() {
  }

  public Employee(String name, String job,byte[] b) {
   this.name = name;
   this.job = job;

   this.bytes= b;

  }

  public void setName(String name) {
   this.name = name;
  }

  public String getName() {
   return name;
  }


  public String getJob() {
   return job;
  }

  public void setJob(String job) {
   this.job = job;
  }
  public byte[] getBytes() {
   return bytes;
  }

  public void setBytes(byte[] b) {
   this.bytes = b;
  }

}