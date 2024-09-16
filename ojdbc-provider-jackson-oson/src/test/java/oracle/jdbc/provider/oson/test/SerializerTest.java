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

package oracle.jdbc.provider.oson.test;

import oracle.jdbc.provider.oson.JacksonOsonConverter;
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