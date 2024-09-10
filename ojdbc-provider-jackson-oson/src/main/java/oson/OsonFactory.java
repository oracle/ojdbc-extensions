package oson;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonGenerator;
import oracle.sql.json.OracleJsonParser;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class OsonFactory extends JsonFactory{
	
	private final OracleJsonFactory factory = getFactoryFromCache();

	final static Map<Long, OracleJsonFactory> factoryCache = new HashMap<>();
	
	public static OracleJsonFactory getFactoryFromCache() {
		OracleJsonFactory result;
		if ((result = factoryCache.get(Thread.currentThread().getId())) == null) {
			result = new OracleJsonFactory();
			factoryCache.put(Thread.currentThread().getId(), result);
		}

		return result;
	}

	@Override
	public OsonGenerator createGenerator(OutputStream out) throws IOException {
		return createGenerator(out, JsonEncoding.UTF8);
	}
	
	@Override
	public OsonGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException {
		IOContext ctxt = _createContext(out, true);
		ctxt.setEncoding(enc);
		if (enc == JsonEncoding.UTF8 && _outputDecorator != null) {
    	out = _outputDecorator.decorate(ctxt, out);
		}
		OsonGenerator g = new OsonGenerator(_generatorFeatures, null,
				factory.createJsonBinaryGenerator(out), out);
		ObjectCodec codec = getCodec();
		if (codec != null) {
    	g.setCodec(codec);
		}
		if (_characterEscapes != null) {
    	g.setCharacterEscapes(_characterEscapes);
		}
		return g;
	}
	
	@Override
	public OsonGenerator createGenerator(Writer out) throws IOException {
		IOContext ctxt = _createContext(out, true);
		OsonGenerator g = new OsonGenerator(_generatorFeatures, null, 
				factory.createJsonTextGenerator(out));
		ObjectCodec codec = getCodec();
		if (codec != null) {
    	g.setCodec(codec);
		}
		if (_characterEscapes != null) {
    	g.setCharacterEscapes(_characterEscapes);
		}
		return g;
	}
	
	@Override
	public OsonGenerator createGenerator(DataOutput out) throws IOException {
		return createGenerator((OutputStream) out, JsonEncoding.UTF8);
	}
	
	@Override
  public OsonGenerator createGenerator(DataOutput out, JsonEncoding enc) throws IOException {
		return createGenerator((OutputStream) out, enc);
  }
	
	@Override
	public OsonGenerator createGenerator(File f, JsonEncoding enc) throws IOException {
		OutputStream out = new FileOutputStream(f);
		IOContext ctxt = _createContext(out, true);
		ctxt.setEncoding(enc);
		if (enc == JsonEncoding.UTF8 && _outputDecorator != null) {
    	out = _outputDecorator.decorate(ctxt, out);
		}
		return createGenerator(out, enc);
	}
	
	public OsonGenerator createGenerator(OracleJsonGenerator oGen) {
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
	
	@Override
	public OsonParser createParser(byte[] data) {
		IOContext ctxt = _createContext(data, true);
		return _createParser(data, 0, data.length, ctxt);
	}
	
	@Override
	public OsonParser createParser(char[] content) {
		IOContext ctxt = _createContext(content, true);
		return _createParser(content, 0, content.length, ctxt, false);
	}
	
	@SuppressWarnings({ "deprecation", "resource" })
	@Override
	public OsonParser createParser(File f) throws IOException {
		IOContext ctxt = _createContext(f, true);
		InputStream in = new FileInputStream(f);
		if (_inputDecorator != null) {
    	in = _inputDecorator.decorate(ctxt, in);
		}
		return _createParser(in, ctxt);
	}
	
	@Override
	public OsonParser createParser(Reader r) throws IOException {
		IOContext ctxt = _createContext(r, true);
		return _createParser(r, ctxt);
	}
	
	@Override
	public OsonParser createParser(InputStream in) throws IOException {
		IOContext ctxt = _createContext(in, true);
		return _createParser(in, ctxt);
	}
	
	@Override
	public OsonParser createParser(String content) throws IOException {
		IOContext ctxt = _createContext(content, true);
		return _createParser(content.toCharArray(), 0, content.length(), ctxt, false);
	}
	
	@Override
	public OsonParser createParser(URL url) throws IOException {
		IOContext ctxt = _createContext(url, true);
		InputStream in = _optimizedStreamFromURL(url);
		if (_inputDecorator != null) {
      in = _inputDecorator.decorate(ctxt, in);
		}
		return _createParser(in, ctxt);
	}
	
	@Override
	public OsonParser createParser(byte[] data, int offset, int len) throws IOException {
		IOContext ctxt = _createContext(data, true);
		if (_inputDecorator != null) {
  		InputStream in = _inputDecorator.decorate(ctxt, data, offset, len);
  		if (in != null) {
      	return _createParser(in, ctxt);
  		}
		}
		return _createParser(data, offset, len, ctxt);
	}
	
	@Override
	public OsonParser createParser(char[] content, int offset, int len) throws IOException {
		IOContext ctxt = _createContext(content, true);
		Reader r = new CharArrayReader(content, offset, len);
		if (_inputDecorator != null) {
	  	r = _inputDecorator.decorate(ctxt, r);
		}
		return _createParser(r, ctxt);
	}
	
	@Override
	public OsonParser _createParser(InputStream in, IOContext ctxt) {
		return new OsonParser(ctxt, _factoryFeatures, factory.createJsonBinaryParser(in));
	}
	
	@Override
	public OsonParser _createParser(Reader r, IOContext ctxt) {
		
		return new OsonParser(ctxt, _factoryFeatures, factory.createJsonTextParser(r));
	}

	@Override
	public OsonParser _createParser(char[] data, int offset, int len, IOContext ctxt, boolean recyclable) {
		return new OsonParser(ctxt, _factoryFeatures, 
				factory.createJsonTextParser(new CharArrayReader(data, offset, len)));
	}
	
	@Override
	public OsonParser _createParser(byte[] data, int offset, int len, IOContext ctxt) {
		return new OsonParser(ctxt, _factoryFeatures, 
				factory.createJsonBinaryParser(ByteBuffer.wrap(data, offset, len)));
	}
	
	public OsonParser createParser(OracleJsonParser oParser) {
		return new OsonParser(this._createContext(null, false), _factoryFeatures, oParser);
	}
	
	@Override
	protected OsonParser _createParser(DataInput input, IOContext ctxt) throws IOException {	
		throw new UnsupportedOperationException("Cannot create parser for Data input values");
  	}
	
}
