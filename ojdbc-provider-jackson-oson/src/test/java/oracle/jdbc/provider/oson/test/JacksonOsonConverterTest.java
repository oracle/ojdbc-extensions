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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import oracle.jdbc.provider.oson.JacksonOsonConverter;
import oracle.jdbc.provider.oson.OsonFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests that converter instances do not share mapper or factory state.
 */
class JacksonOsonConverterTest {

  /**
   * Tests that two converters use separate mappers and naming settings.
   */
  @Test
  void converterMappersAreIndependent() {
    JacksonOsonConverter firstConverter = new JacksonOsonConverter();
    JacksonOsonConverter secondConverter = new JacksonOsonConverter();

    ObjectMapper firstMapper = firstConverter.createObjectMapper();
    ObjectMapper secondMapper = secondConverter.createObjectMapper();

    assertNotSame(firstMapper, secondMapper);

    firstMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    assertEquals(PropertyNamingStrategies.SNAKE_CASE,
        firstMapper.getSerializationConfig().getPropertyNamingStrategy());
    assertNull(secondMapper.getSerializationConfig().getPropertyNamingStrategy());
  }

  /**
   * Tests that the deprecated mapper method returns a new mapper each time.
   */
  @Test
  @SuppressWarnings("deprecation")
  void legacyMapperAccessorReturnsIndependentMappers() {
    ObjectMapper firstMapper = JacksonOsonConverter.getObjectMapper();
    ObjectMapper secondMapper = JacksonOsonConverter.getObjectMapper();

    assertNotSame(firstMapper, secondMapper);
    firstMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    assertNull(secondMapper.getSerializationConfig().getPropertyNamingStrategy());
  }

  /**
   * Tests that the deprecated factory method returns a new factory each time.
   */
  @Test
  @SuppressWarnings("deprecation")
  void legacyFactoryAccessorReturnsIndependentFactories() {
    OsonFactory firstFactory = JacksonOsonConverter.getOsonFactory();
    OsonFactory secondFactory = JacksonOsonConverter.getOsonFactory();

    assertNotSame(firstFactory, secondFactory);
    assertNotSame(firstFactory.getOracleJsonFactory(),
        secondFactory.getOracleJsonFactory());
  }

  /**
   * Tests that the constructor rejects a null factory.
   */
  @Test
  void converterRejectsNullFactory() {
    assertThrows(NullPointerException.class,
        () -> new JacksonOsonConverter(null));
  }
}
