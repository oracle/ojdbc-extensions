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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import oracle.jdbc.provider.oson.OsonFactory;
import oracle.jdbc.provider.oson.OsonModule;
import oracle.jdbc.spi.OsonConverter;
import oracle.sql.json.OracleJsonGenerator;
import oracle.sql.json.OracleJsonParser;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class JacksonOsonConverter implements OsonConverter{

  private static final OsonFactory osonFactory = new OsonFactory();
  private static final ObjectMapper om = new ObjectMapper(osonFactory);
  private final Lock lock = new ReentrantLock();
  
  static {
    om.findAndRegisterModules();
    om.registerModule(new OsonModule());
  }
  
  public JacksonOsonConverter(){}

  @Override
  public void serialize(OracleJsonGenerator oGen, Object object) throws IllegalStateException {
    try {
      lock.lock();
      om.writeValue(osonFactory.createGenerator(oGen), object);
    } 
    catch (IOException e) {
      throw new IllegalStateException("Oson conversion failed", e);
    }
    finally {
      lock.unlock();
    }
  }

  @Override
  public Object deserialize(OracleJsonParser oParser, Class<?> type) throws IllegalStateException {
    if(!oParser.hasNext()) return null;
    try {
      lock.lock();
      return om.readValue(osonFactory.createParser(oParser), type);
    } 
    catch (IOException e) {
      throw new IllegalArgumentException("Object parsing from oson failed", e);
    } finally {
      lock.unlock();
    }
  }
  
  public static Object convertValue(Object fromValue, JavaType javaType) {
    return om.convertValue(fromValue, javaType);
  }


}
