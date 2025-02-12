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

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.json.JsonWriteContext;
import oracle.jdbc.driver.json.tree.OracleJsonDateImpl;
import oracle.jdbc.driver.json.tree.OracleJsonTimestampImpl;
import oracle.sql.DATE;
import oracle.sql.TIMESTAMP;
import oracle.sql.json.OracleJsonDate;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonGenerator;
import oracle.sql.json.OracleJsonTimestamp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The OsonGenerator class extends the GeneratorBase class to provide a custom JSON generator
 * implementation that works with Oracle's JSON generator {@link OracleJsonGenerator}. This class
 * supports writing JSON data with various data types.
 */
public class OsonGenerator extends GeneratorBase {

  private static final boolean DEBUG = false;
  private Logger logger = Logger.getLogger(OsonGenerator.class.getName());

  private OutputStream out = null;
  private OracleJsonGenerator gen = null;

  private boolean closed;

  /**
   * Constructs an OsonGenerator with the specified features, codec, OracleJsonGenerator,
   * and output stream.
   *
   * @param features The features for the generator.
   * @param codec The codec for object serialization.
   * @param gen The OracleJsonGenerator instance.
   * @param out The output stream to write JSON content to.
   */
  protected OsonGenerator(int features, ObjectCodec codec, OracleJsonGenerator gen, OutputStream out) {
    super(features, codec);
    this.out = out;
    this.gen = gen;
  }

  /**
   * Constructs an OsonGenerator with the specified features, codec, and OracleJsonGenerator.
   *
   * @param features The features for the generator.
   * @param codec The codec for object serialization.
   * @param gen The OracleJsonGenerator instance.
   */
  protected OsonGenerator(int features, ObjectCodec codec, OracleJsonGenerator gen) {
    super(features, codec);
    this.gen = gen;

  }

  /**
   * Used to Enable Fields such as UUID to be written as byte[].
   * @return boolean to support writing Binary Natively
   */
  @Override
  public boolean canWriteBinaryNatively() {
    return true;
  }

  /**
   * Flushes the generator and writes any buffered output to the underlying stream.
   *
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void flush() throws IOException {
    logger.log(Level.FINEST, "flush");
    gen.flush();
  }

  /**
   * Releases any buffers associated with this generator.
   */
  @Override
  protected void _releaseBuffers() {
    logger.log(Level.FINEST, "_releaseBuffers");
    if(out instanceof ByteArrayOutputStream)
      ((ByteArrayOutputStream) out).reset();
  }

  /**
   * Verifies whether it is valid to write a value, and throws an error if not.
   *
   * @param typeMsg The type of value being written.
   * @throws IOException If the value write is invalid.
   */
  @Override
  protected void _verifyValueWrite(String typeMsg) throws IOException {
    int status = _writeContext.writeValue();
    if(status == _writeContext.STATUS_EXPECT_NAME) {
      _reportError("error: expecting value. Got name: " + typeMsg);
    }
  }

  /**
   * Writes the start of a JSON array.
   *
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void writeStartArray() throws IOException {
    _verifyValueWrite("writeStartArray");
    logger.log(Level.FINEST, "writeStartArray");
    _writeContext = _writeContext.createChildArrayContext();
    gen.writeStartArray();
  }

  /**
   * Writes the end of a JSON array.
   *
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void writeEndArray() throws IOException {
    int status = _writeContext.writeValue();
    if(status != _writeContext.STATUS_OK_AFTER_COMMA) {
      _reportError("error: expecting end array");
    }
    if (!_writeContext.inArray()) {
      _reportError("Current context not an ARRAY but " + _writeContext.getTypeDesc());
    }
    logger.log(Level.FINEST, "writeEndArray");
    gen.writeEnd();
    _writeContext = _writeContext.getParent();
  }

  /**
   * Writes the start of a JSON object.
   *
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void writeStartObject() throws IOException {
    _verifyValueWrite("write start object");
    logger.log(Level.FINEST, "writeStartObject");
    _writeContext = _writeContext.createChildObjectContext();
    gen.writeStartObject();
  }

  /**
   * Writes the end of a JSON object.
   *
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void writeEndObject() throws IOException {
    int status = _writeContext.writeValue();
    if (status != JsonWriteContext.STATUS_EXPECT_NAME) {
      _reportError("error: expecting end object");
    }
    if (!_writeContext.inObject()) {
      _reportError("Current context not an OBJECT but " + _writeContext.getTypeDesc());
    }
    logger.log(Level.FINEST, "writeEndObject");
    gen.writeEnd();
    _writeContext = _writeContext.getParent();
  }

  /**
   * Writes a field name within a JSON object.
   *
   * @param name The field name to write.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void writeFieldName(String name) throws IOException {
    int status = _writeContext.writeFieldName(name);
    if (status == JsonWriteContext.STATUS_EXPECT_VALUE) {
      _reportError("Can not write a field name, expecting a value");
    }
    logger.log(Level.FINEST, "writeFieldName: " + name);
    gen.writeKey(name);
  }

  /**
   * Writes a string value.
   *
   * @param text The string to write.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void writeString(String text) throws IOException {
    _verifyValueWrite("write string");
    logger.log(Level.FINEST, "writeString: " + text);
    gen.write(text);
  }

  /**
   * Writes a string value from a character array.
   *
   * @param buffer The character array containing the string.
   * @param offset The starting offset in the array.
   * @param len The number of characters to write.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void writeString(char[] buffer, int offset, int len) throws IOException {
    _verifyValueWrite("write string");
    logger.log(Level.FINEST, "writeString ");
    gen.write(new String(buffer, offset, len));
  }

  /**
   * Writes a string from a byte array using UTF-8 encoding.
   *
   * @param buffer The byte array containing the string.
   * @param offset The starting offset in the array.
   * @param len The number of bytes to write.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void writeRawUTF8String(byte[] buffer, int offset, int len) throws IOException {
    _verifyValueWrite("writeRawUTF8String");
    logger.log(Level.FINEST, "writeRawUTF8String ");
    gen.write(new String(buffer, offset, len, StandardCharsets.UTF_8));
  }

  /**
   * Writes a string from a byte array using UTF-8 encoding.
   *
   * @param buffer The byte array containing the string.
   * @param offset The starting offset in the array.
   * @param len The number of bytes to write.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void writeUTF8String(byte[] buffer, int offset, int len) throws IOException {
    _verifyValueWrite("writeUTF8String");
   logger.log(Level.FINEST, "writeUTF8String ");
    gen.write(new String(buffer, offset, len, StandardCharsets.UTF_8));
  }

  /**
   * Writes raw text.
   *
   * @param text The raw text to write.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void writeRaw(String text) throws IOException {
    _verifyValueWrite("writeRaw");
    logger.log(Level.FINEST, "writeRaw: " + text);
    gen.write(text);
  }

  /**
   * Writes raw text from a substring.
   *
   * @param text The text containing the substring.
   * @param offset The starting offset of the substring.
   * @param len The number of characters in the substring.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void writeRaw(String text, int offset, int len) throws IOException {
    _verifyValueWrite("writeRaw");
    logger.log(Level.FINEST, "writeRaw: " + text);
    gen.write(text.substring(offset, offset+len));
  }

  /**
   * Writes raw (unescaped) characters from an array.
   *
   * @param text The character array.
   * @param offset The starting offset in the array.
   * @param len The number of characters to write.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void writeRaw(char[] text, int offset, int len) throws IOException {
    _verifyValueWrite("writeRaw");
    logger.log(Level.FINEST, "writeRaw");
    gen.write(new String(text, offset, offset+len));
  }

  /**
   * Writes a single raw (unescaped) character.
   *
   * @param c The character to write.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void writeRaw(char c) throws IOException {
    _verifyValueWrite("writeRaw");
    logger.log(Level.FINEST, "writeRaw: " + c);
    gen.write(c);
  }

  /**
   * Writes a binary value encoded as a Base64 string.
   *
   * @param bv The Base64 variant.
   * @param data The byte array containing the binary data.
   * @param offset The starting offset in the array.
   * @param len The number of bytes to write.
   * @throws IOException If an I/O error occurs.
   */
  // discuss
  @Override
  public void writeBinary(Base64Variant bv, byte[] data, int offset, int len) throws IOException {
    _verifyValueWrite("writeBinary");
//    gen.write(bv.encode(Arrays.copyOfRange(data, offset, offset+len)));
    logger.log(Level.FINEST, "writeBinary");
    gen.write(Arrays.copyOfRange(data, offset, offset+len));
  }

  /**
   * Writes an integer value.
   *
   * @param v The integer to write.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void writeNumber(int v) throws IOException {
    _verifyValueWrite("writeNumber");
    logger.log(Level.FINEST, "writeNumber: " + v);
    gen.write(v);
  }

  /**
   * Writes a long value.
   *
   * @param v The long value to write.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void writeNumber(long v) throws IOException {
    _verifyValueWrite("writeNumber");
    logger.log(Level.FINEST, "writeNumber: " + v);
    gen.write(v);
  }

  /**
   * Writes a BigInteger value.
   *
   * @param v The BigInteger value to write.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void writeNumber(BigInteger v) throws IOException {
    _verifyValueWrite("writeNumber");
    logger.log(Level.FINEST, "writeNumber: " + v);
    gen.write(v);
  }

  /**
   * Writes a double value.
   *
   * @param v The double value to write.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void writeNumber(double v) throws IOException {
    _verifyValueWrite("writeNumber");
    logger.log(Level.FINEST, "writeNumber: " + v);
    gen.write(v);
  }

  /**
   * Writes a float value.
   *
   * @param v The float value to write.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void writeNumber(float v) throws IOException {
    _verifyValueWrite("writeNumber");
    logger.log(Level.FINEST, "writeNumber: " + v);
    gen.write(v);
  }

  /**
   * Writes a BigDecimal value.
   *
   * @param v The BigDecimal value to write.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void writeNumber(BigDecimal v) throws IOException {
    _verifyValueWrite("writeNumber");
    logger.log(Level.FINEST, "writeNumber ");
    gen.write(v);
  }

  /**
   * Writes a number from a string.
   *
   * @param encodedValue The string representation of the number.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void writeNumber(String encodedValue) throws IOException {
    _verifyValueWrite("writeNumber");
    logger.log(Level.FINEST, "writeNumber: " + encodedValue);
    gen.write(new BigDecimal(encodedValue));
  }

  /**
   * Writes a boolean value.
   *
   * @param state The boolean value to write.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void writeBoolean(boolean state) throws IOException {
    _verifyValueWrite("writeBoolean");
    logger.log(Level.FINEST, "writeBoolean: " + state);
    gen.write(state);
  }

  /**
   * Writes a LocalDateTime value.
   *
   * @param v The LocalDateTime to write.
   * @throws IOException If an I/O error occurs.
   */
  public void writeLocalDateTime(LocalDateTime v) throws IOException {
    _verifyValueWrite("writeLocalDateTime");
    logger.log(Level.FINEST, "writeLocalDateTime:");
    gen.write(v);
  }

  /**
   * Writes an OffsetDateTime value.
   *
   * @param v The OffsetDateTime to write.
   * @throws IOException If an I/O error occurs.
   */
  public void writeOffsetDateTime(OffsetDateTime v) throws IOException {
    _verifyValueWrite("writeOffsetDateTime");
    logger.log(Level.FINEST, "writeOffsetDateTime:");
    gen.write(v);
  }

  /**
   * Writes a Duration value.
   *
   * @param v The Duration to write.
   * @throws IOException If an I/O error occurs.
   */
  public void writeDuration(Duration v) throws IOException {
    _verifyValueWrite("writeDuration");
    logger.log(Level.FINEST, "writeDuration:");
    gen.write(v);
  }

  /**
   * Writes a Period value.
   *
   * @param v The Period to write.
   * @throws IOException If an I/O error occurs.
   */
  public void writePeriod(Period v) throws IOException {
    _verifyValueWrite("writePeriod");
    logger.log(Level.FINEST, "writePeriod:");
    gen.write(v);
  }

  /**
   * Writes a null value.
   *
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void writeNull() throws IOException {
    _verifyValueWrite("writeNull");
    logger.log(Level.FINEST, "writeNull");
    gen.writeNull();
  }

  /**
   * Checks if the generator is closed.
   * @return true if the generator is closed, otherwise false.
   */
  @Override
  public boolean isClosed() {
    logger.log(Level.FINEST, "isClosed");
    return closed;
  }

  /**
   * Closes the generator and releases any associated resources.
   *
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void close() throws IOException {
    logger.log(Level.FINEST, "close");
    gen.close();
    closed = true;
  }

  /**
   * Writes a `java.util.Date` object as an Oracle JSON `DATE`.
   * If the input is a `java.sql.Date`, it is first converted to a `DATE` object.
   *
   * @param value the `Date` to write
   * @throws IOException if an I/O error occurs during writing
   */
  public void writeDate(Date value) throws IOException {
    _verifyValueWrite("write date");

    if(value instanceof java.sql.Date) {
      logger.log(Level.FINEST, "writeDate: java.sql.Date");
      DATE dd = new DATE((java.sql.Date)value);
      OracleJsonDate jsonDate = new OracleJsonDateImpl(dd.shareBytes());
      gen.write(jsonDate);
    }else {
      // java.util.Date
      logger.log(Level.FINEST, "writeDate: java.util.Date");
      Timestamp ts = new Timestamp(value.getTime());
      TIMESTAMP timestamp = new TIMESTAMP(ts);
      OracleJsonTimestamp writeTimeStamp = new OracleJsonTimestampImpl(timestamp.shareBytes());
      gen.write(writeTimeStamp);
    }
  }

  /**
   * Writes a `LocalDate` object as an Oracle JSON `DATE`.
   * Converts the `LocalDate` to a `DATE` object and serializes it.
   *
   * @param value the `LocalDate` to write
   * @throws IOException if an I/O error occurs during writing
   */
  public void writeLocalDate(LocalDate value) throws IOException {
    _verifyValueWrite("write LocalDate");
    logger.log(Level.FINEST, "writeLocalDate");
    try {
      DATE dd = new DATE(value);
      OracleJsonDate jsonDate = new OracleJsonDateImpl(dd.shareBytes());
      gen.write(jsonDate);
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
  }

  /**
   * Writes a `Timestamp` object as an Oracle JSON `TIMESTAMP`.
   * Converts the `Timestamp` to a `TIMESTAMP` object and serializes it.
   *
   * @param value the `Timestamp` to write
   * @throws IOException if an I/O error occurs during writing
   */
  public void writeTimeStamp(Timestamp value) throws IOException {
    _verifyValueWrite("write TimeStamp");
    logger.log(Level.FINEST, "writeTimeStamp");
    TIMESTAMP timestamp = new TIMESTAMP(value);
    OracleJsonTimestamp writeTimeStamp = new OracleJsonTimestampImpl(timestamp.shareBytes());
    gen.write(writeTimeStamp);
  }
}
