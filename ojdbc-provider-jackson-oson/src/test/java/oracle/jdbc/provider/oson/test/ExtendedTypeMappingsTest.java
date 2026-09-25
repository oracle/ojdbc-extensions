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

import com.fasterxml.jackson.databind.ObjectMapper;
import oracle.jdbc.provider.oson.JacksonOsonConverter;
import oracle.jdbc.provider.oson.OsonFactory;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;
import oracle.sql.json.OracleJsonValue;
import oracle.sql.json.OracleJsonValue.OracleJsonType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the extended Java-to-OSON mappings provided by
 * {@link JacksonOsonConverter}.
 *
 * <p>The tests serialize values to binary OSON, read them back, and verify
 * their physical {@link OracleJsonType}. They cover individual values, POJO
 * properties, and automatic Jackson module discovery.</p>
 */
class ExtendedTypeMappingsTest {

  /**
   * The production-configured mapper used by normal OSON serialization.
   */
  private static final ObjectMapper MAPPER = JacksonOsonConverter.getObjectMapper();

  /**
   * Verifies one Java-to-OSON mapping for each case from
   * {@link #mappingCases()}.
   *
   * <p>JUnit invokes this method once for every {@link Arguments} returned by
   * the method source. The value is wrapped in an object so its OSON type can
   * be inspected.</p>
   *
   * @param name readable name of the mapping, also used as the assertion
   * failure message
   * @param value Java value to serialize
   * @param expectedType Oracle OSON type that should be encoded
   * @throws IOException if Jackson cannot serialize the value
   */
  @ParameterizedTest(name = "{0} maps to {2}")
  @MethodSource("mappingCases")
  void serializesJavaTypeAsExpectedOracleJsonType(
      String name, Object value, OracleJsonType expectedType) throws IOException {
    byte[] oson = MAPPER.writeValueAsBytes(Collections.singletonMap("value", value));

    OracleJsonValue serializedValue = new OracleJsonFactory()
        .createJsonBinaryValue(ByteBuffer.wrap(oson))
        .asJsonObject()
        .get("value");

    assertEquals(expectedType, serializedValue.getOracleJsonType(), name);
  }

  /**
   * Supplies the Java values and expected OSON types for the parameterized
   * test.
   *
   * <p>The cases cover the documented mappings, including
   * {@code OffsetDateTime -> TIMESTAMPTZ}, {@code LocalDate -> DATE},
   * {@code Duration -> INTERVALDS}, and {@code byte[] -> BINARY}.
   * </p>
   *
   * @return one {@link Arguments} instance for each documented mapping
   */
  private static Stream<Arguments> mappingCases() {
    return Stream.of(
        Arguments.of("LocalDateTime", LocalDateTime.of(2026, 9, 10, 3, 4, 5, 123000000),
            OracleJsonType.TIMESTAMP),
        Arguments.of("OffsetDateTime",
            OffsetDateTime.of(2026, 9, 10, 3, 4, 5, 123000000, ZoneOffset.ofHours(2)),
            OracleJsonType.TIMESTAMPTZ),
        Arguments.of("Period", Period.of(2, 3, 0), OracleJsonType.INTERVALYM),
        Arguments.of("Duration", Duration.ofSeconds(5, 123000000), OracleJsonType.INTERVALDS),
        Arguments.of("BigInteger", new BigInteger("12345678901234567890"), OracleJsonType.DECIMAL),
        Arguments.of("Year", Year.of(2026), OracleJsonType.DECIMAL),
        Arguments.of("byte[]", new byte[] {1, 2, 3}, OracleJsonType.BINARY),
        Arguments.of("java.util.Date", Date.from(Instant.parse("2026-09-10T03:04:05.123Z")),
            OracleJsonType.TIMESTAMP),
        Arguments.of("java.sql.Date", java.sql.Date.valueOf("2026-09-10"), OracleJsonType.DATE),
        Arguments.of("Timestamp", Timestamp.valueOf("2026-09-10 03:04:05.123"),
            OracleJsonType.TIMESTAMP),
        Arguments.of("LocalDate", LocalDate.of(2026, 9, 10), OracleJsonType.DATE),
        Arguments.of("Boolean", Boolean.TRUE, OracleJsonType.TRUE),
        Arguments.of("UUID", UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
            OracleJsonType.BINARY));
  }

  /**
   * Verifies the mappings when values are serialized as Java bean properties.
   *
   * <p>This covers Jackson's property and contextual-serializer paths.</p>
   *
   * @throws IOException if Jackson cannot serialize the bean
   */
  @Test
  void serializesPojoPropertiesAsExpectedOracleJsonTypes() throws IOException {
    ExtendedTypes value = new ExtendedTypes();
    OracleJsonObject serialized = new OracleJsonFactory()
        .createJsonBinaryValue(ByteBuffer.wrap(MAPPER.writeValueAsBytes(value)))
        .asJsonObject();

    assertEquals(OracleJsonType.TIMESTAMP, serialized.get("localDateTime").getOracleJsonType());
    assertEquals(OracleJsonType.TIMESTAMPTZ, serialized.get("offsetDateTime").getOracleJsonType());
    assertEquals(OracleJsonType.INTERVALYM, serialized.get("period").getOracleJsonType());
    assertEquals(OracleJsonType.INTERVALDS, serialized.get("duration").getOracleJsonType());
    assertEquals(OracleJsonType.DECIMAL, serialized.get("bigInteger").getOracleJsonType());
    assertEquals(OracleJsonType.DECIMAL, serialized.get("year").getOracleJsonType());
    assertEquals(OracleJsonType.BINARY, serialized.get("bytes").getOracleJsonType());
    assertEquals(OracleJsonType.TIMESTAMP, serialized.get("utilDate").getOracleJsonType());
    assertEquals(OracleJsonType.DATE, serialized.get("sqlDate").getOracleJsonType());
    assertEquals(OracleJsonType.TIMESTAMP, serialized.get("timestamp").getOracleJsonType());
    assertEquals(OracleJsonType.DATE, serialized.get("localDate").getOracleJsonType());
    assertEquals(OracleJsonType.TRUE, serialized.get("booleanValue").getOracleJsonType());
    assertEquals(OracleJsonType.BINARY, serialized.get("uuid").getOracleJsonType());
  }

  /**
   * Verifies that automatic module discovery preserves OSON serializer
   * precedence.
   *
   * <p>It simulates an application using
   * {@code findAndRegisterModules()} and verifies that
   * {@code JavaTimeModule} is registered before {@code OsonModule}.</p>
   *
   * @throws IOException if Jackson cannot serialize the value
   */
  @Test
  void autoDiscoveredModulesKeepOsonSerializerPrecedence() throws IOException {
    ObjectMapper mapper = new ObjectMapper(new OsonFactory());
    mapper.findAndRegisterModules();

    byte[] oson = mapper.writeValueAsBytes(Collections.singletonMap(
        "value", OffsetDateTime.of(2026, 9, 10, 3, 4, 5, 123000000, ZoneOffset.ofHours(2))));
    OracleJsonType actualType = new OracleJsonFactory()
        .createJsonBinaryValue(ByteBuffer.wrap(oson))
        .asJsonObject()
        .get("value")
        .getOracleJsonType();

    assertEquals(OracleJsonType.TIMESTAMPTZ, actualType);
  }

  /**
   * Bean containing one value for every extended mapping tested above.
   */
  private static class ExtendedTypes {
    public LocalDateTime localDateTime = LocalDateTime.of(2026, 9, 10, 3, 4, 5, 123000000);
    public OffsetDateTime offsetDateTime =
        OffsetDateTime.of(2026, 9, 10, 3, 4, 5, 123000000, ZoneOffset.ofHours(2));
    public Period period = Period.of(2, 3, 0);
    public Duration duration = Duration.ofSeconds(5, 123000000);
    public BigInteger bigInteger = new BigInteger("12345678901234567890");
    public Year year = Year.of(2026);
    public byte[] bytes = new byte[] {1, 2, 3};
    public Date utilDate = Date.from(Instant.parse("2026-09-10T03:04:05.123Z"));
    public java.sql.Date sqlDate = java.sql.Date.valueOf("2026-09-10");
    public Timestamp timestamp = Timestamp.valueOf("2026-09-10 03:04:05.123");
    public LocalDate localDate = LocalDate.of(2026, 9, 10);
    public Boolean booleanValue = Boolean.TRUE;
    public UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
  }
}
