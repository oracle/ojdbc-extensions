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

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.IOContext;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonGenerator;
import oracle.sql.json.OracleJsonParser;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * The OsonFactory class extends the JsonFactory to provide custom JSON generation and parsing
 * capabilities using {@link OsonGenerator} and {@link OsonParser}. This class overrides methods from
 * JsonFactory to create instances of JsonGenerator and JsonParser.
 */
public class OsonFactory extends JsonFactory {

  /**
   * A ThreadLocal object to ensure thread-safe creation of OracleJsonFactory instances.
   */
  private final ThreadLocal<OracleJsonFactory> factory = ThreadLocal.withInitial(OracleJsonFactory::new);

  /**
   * Creates a JSON generator that writes to the given output stream, using UTF-8 encoding.
   *
   * @param out The output stream to write JSON content to.
   * @return A custom OsonGenerator instance.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public JsonGenerator createGenerator(OutputStream out) throws IOException {
    return createGenerator(out, JsonEncoding.UTF8);
  }

  /**
   * Creates a JSON generator that writes to the given output stream with the specified encoding.
   *
   * @param out The output stream to write JSON content to.
   * @param enc The encoding to use for the generator.
   * @return A custom OsonGenerator instance.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public JsonGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException {
    IOContext ctxt = _createContext(out, true);
    ctxt.setEncoding(enc);
    if (_outputDecorator != null) {
      out = _outputDecorator.decorate(ctxt, out);
    }

    // todo:check for other encodings
    OsonGenerator g = new OsonGenerator(_generatorFeatures, null,
        factory.get().createJsonBinaryGenerator(out), out);
    ObjectCodec codec = getCodec();
    if (codec != null) {
      g.setCodec(codec);
    }
    if (_characterEscapes != null) {
      g.setCharacterEscapes(_characterEscapes);
    }
    return g;
  }

  /**
   * Creates a JSON generator that writes to the given writer.
   *
   * @param out The writer to write JSON content to.
   * @return A custom OsonGenerator instance.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public JsonGenerator createGenerator(Writer out) throws IOException {
    OsonGenerator g = new OsonGenerator(_generatorFeatures, null, 
        factory.get().createJsonTextGenerator(out));
    ObjectCodec codec = getCodec();
    if (codec != null) {
      g.setCodec(codec);
    }
    if (_characterEscapes != null) {
      g.setCharacterEscapes(_characterEscapes);
    }
    return g;
  }

  /**
   * Creates a JSON generator that writes to the given DataOutput stream.
   *
   * @param out The DataOutput stream to write JSON content to.
   * @return A custom OsonGenerator instance.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public JsonGenerator createGenerator(DataOutput out) throws IOException {
    return createGenerator((OutputStream) out, JsonEncoding.UTF8);
  }

  /**
   * Creates a JSON generator that writes to the given DataOutput stream and Encoding.
   *
   * @param out The DataOutput stream to write JSON content to.
   * @return A custom OsonGenerator instance.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public JsonGenerator createGenerator(DataOutput out, JsonEncoding enc) throws IOException {
    return createGenerator((OutputStream) out, enc);
  }

  /**
   * Creates a JSON generator that writes to the specified file with the given encoding.
   *
   * @param f The file to write JSON content to.
   * @param enc The encoding to use.
   * @return A custom OsonGenerator instance.
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public JsonGenerator createGenerator(File f, JsonEncoding enc) throws IOException {
    OutputStream out = new FileOutputStream(f);
    IOContext ctxt = _createContext(out, true);
    ctxt.setEncoding(enc);
    if (enc == JsonEncoding.UTF8 && _outputDecorator != null) {
      out = _outputDecorator.decorate(ctxt, out);
    }
    return createGenerator(out, enc);
  }

  /**
   * Creates a JSON generator using the provided OracleJsonGenerator instance.
   *
   * @param oGen The OracleJsonGenerator to use for generating JSON content.
   * @return A custom OsonGenerator instance.
   */
  public JsonGenerator createGenerator(OracleJsonGenerator oGen) {
    OsonGenerator g = new OsonGenerator(_generatorFeatures, null, oGen);
    ObjectCodec codec = getCodec();
    if (codec != null) {
      g.setCodec(codec);
    }
    if (_characterEscapes != null) {
      g.setCharacterEscapes(_characterEscapes);
    }
    return g;
  }

  /**
   * Creates a JSON parser from a byte array.
   *
   * @param data The byte array containing JSON content.
   * @return A custom OsonParser instance.
   */
  @Override
  public JsonParser createParser(byte[] data) {
    IOContext ctxt = _createContext(data, true);
    return _createParser(data, 0, data.length, ctxt);
  }

  /**
   * Creates a JSON parser from a char array.
   *
   * @param content The char array containing JSON content.
   * @return A custom OsonParser instance.
   */
  @Override
  public JsonParser createParser(char[] content) {
    IOContext ctxt = _createContext(content, true);
    return _createParser(content, 0, content.length, ctxt, false);
  }

  /**
   * Creates a JSON parser from a File.
   *
   * @param f The file containing JSON content.
   * @return A custom OsonParser instance.
   */
  @SuppressWarnings({ "deprecation", "resource" })
  @Override
  public JsonParser createParser(File f) throws IOException {
    IOContext ctxt = _createContext(f, true);
    InputStream in = new FileInputStream(f);
    if (_inputDecorator != null) {
      in = _inputDecorator.decorate(ctxt, in);
    }
    return _createParser(in, ctxt);
  }

  /**
   * Creates a JSON parser from a Reader.
   *
   * @param r The reader containing JSON content.
   * @return A custom OsonParser instance.
   */
  @Override
  public JsonParser createParser(Reader r) throws IOException {
    IOContext ctxt = _createContext(r, true);
    return _createParser(r, ctxt);
  }

  /**
   * Creates a JSON parser from a Input Stream.
   *
   * @param in The inputStream containing JSON content.
   * @return A custom OsonParser instance.
   */
  @Override
  public JsonParser createParser(InputStream in) throws IOException {
    IOContext ctxt = _createContext(in, true);
    return _createParser(in, ctxt);
  }

  /**
   * Creates a JSON parser from a Json String.
   *
   * @param content The String containing JSON content.
   * @return A custom OsonParser instance.
   */
  @Override
  public JsonParser createParser(String content) throws IOException {
    IOContext ctxt = _createContext(content, true);
    return _createParser(content.toCharArray(), 0, content.length(), ctxt, false);
  }

  /**
   * Method for constructing JSON parser instance to parse contents of resource reference by given URL.
   *
   * @param url URL pointing to resource that contains JSON content to parse
   *
   * @return
   * @throws IOException
   */
  @Override
  public JsonParser createParser(URL url) throws IOException {
    IOContext ctxt = _createContext(url, true);
    InputStream in = _optimizedStreamFromURL(url);
    if (_inputDecorator != null) {
      in = _inputDecorator.decorate(ctxt, in);
    }
    return _createParser(in, ctxt);
  }

  /**
   * Creates a JSON parser from a byte[]
   *
   * @param data Buffer that contains data to parse
   * @param offset Offset of the first data byte within buffer
   * @param len Length of contents to parse within buffer
   *
   * @return
   * @throws IOException
   */
  @Override
  public JsonParser createParser(byte[] data, int offset, int len) throws IOException {
    IOContext ctxt = _createContext(data, true);
    if (_inputDecorator != null) {
      InputStream in = _inputDecorator.decorate(ctxt, data, offset, len);
      if (in != null) {
        return _createParser(in, ctxt);
      }
    }
    return _createParser(data, offset, len, ctxt);
  }

  /**
   * Creates a JSON parser from a char[].
   *
   * @param content char[] containing the JSON data
   * @param offset offset of the first json data in char[]
   * @param len length of the data
   * @return
   * @throws IOException
   */
  @Override
  public JsonParser createParser(char[] content, int offset, int len) throws IOException {
    IOContext ctxt = _createContext(content, true);
    Reader r = new CharArrayReader(content, offset, len);
    if (_inputDecorator != null) {
      r = _inputDecorator.decorate(ctxt, r);
    }
    return _createParser(r, ctxt);
  }


  /**
   * Creates a OSON parser from a InputStream
   *
   * @param in InputStream to use for reading content to parse
   * @param ctxt I/O context to use for parsing
   *
   * @return
   */
  @Override
  public JsonParser _createParser(InputStream in, IOContext ctxt) {
    return new OsonParser(ctxt, _factoryFeatures, factory.get().createJsonBinaryParser(in));
  }

  /**
   *  Creates a OSON parser from a Reader
   * @param r Reader to use for reading content to parse
   * @param ctxt I/O context to use for parsing
   *
   * @return
   */
  @Override
  public JsonParser _createParser(Reader r, IOContext ctxt) {
    
    return new OsonParser(ctxt, _factoryFeatures, factory.get().createJsonTextParser(r));
  }

  /**
   *  Creates a OSON parser from a char[].
   *
   * @param data Buffer that contains content to parse
   * @param offset Offset to the first character of data to parse
   * @param len Number of characters within buffer to parse
   * @param ctxt I/O context to use for parsing
   * @param recyclable Whether input buffer is recycled by the factory
   *
   * @return
   */
  @Override
  public JsonParser _createParser(char[] data, int offset, int len, IOContext ctxt, boolean recyclable) {
    return new OsonParser(ctxt, _factoryFeatures, 
        factory.get().createJsonTextParser(new CharArrayReader(data, offset, len)));
  }

  /**
   *  Creates a JSON parser from a byte[]
   * @param data Buffer that contains content to parse
   * @param offset Offset to the first character of data to parse
   * @param len Number of characters within buffer to parse
   * @param ctxt I/O context to use for parsing
   *
   * @return
   */
  @Override
  public JsonParser _createParser(byte[] data, int offset, int len, IOContext ctxt) {
    return new OsonParser(ctxt, _factoryFeatures, 
        factory.get().createJsonBinaryParser(ByteBuffer.wrap(data, offset, len)));
  }

  /**
   * Creates a JSON parser from a OracleJsonParser.
   * @param oParser
   * @return
   */
  public JsonParser createParser(OracleJsonParser oParser) {
    return new OsonParser(this._createContext(null, false), _factoryFeatures, oParser);
  }

  /**
   * Creating OSON parser is not supported for DataInput.
   * @param input DataInput to use for reading content to parse
   * @param ctxt I/O context to use for parsing
   *
   * @return
   * @throws IOException
   */
  @Override
  protected JsonParser _createParser(DataInput input, IOContext ctxt) throws IOException {
    throw new UnsupportedOperationException("Cannot create parser for Data input values");
    }
  
}
