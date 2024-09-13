package oracle.jdbc.provider.oson;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.json.JsonWriteContext;
import oracle.sql.json.OracleJsonGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OsonGenerator extends GeneratorBase {

	private static final boolean DEBUG = false;
	private Logger logger = Logger.getLogger("OsonLogger");

	private OutputStream out = null;
	private OracleJsonGenerator gen = null;

	int hierarchyLevel;

	boolean closed;
  
	protected OsonGenerator(int features, ObjectCodec codec, OracleJsonGenerator gen, OutputStream out) {
		super(features, codec);
		this.out = out;
		this.gen = gen;
	}
	
	protected OsonGenerator(int features, ObjectCodec codec, OracleJsonGenerator gen) {
		super(features, codec);
		this.gen = gen;
	}

	@Override
	public void flush() throws IOException {
		if(DEBUG) logger.log(Level.FINEST, "flush");
		gen.flush();
	}

	@Override
	protected void _releaseBuffers() {
		if(DEBUG) logger.log(Level.FINEST, "_releaseBuffers");
		if(out instanceof ByteArrayOutputStream)
			((ByteArrayOutputStream) out).reset();
	}

	@Override
	protected void _verifyValueWrite(String typeMsg) throws IOException {
		int status = _writeContext.writeValue();
		if(status == _writeContext.STATUS_EXPECT_NAME) {
			_reportError("error: expecting value. Got name: " + typeMsg);
		}
	}

	@Override
	public void writeStartArray() throws IOException {
		_verifyValueWrite("write start array");
		_writeContext = _writeContext.createChildArrayContext();
		gen.writeStartArray();
	}

	@Override
	public void writeEndArray() throws IOException {
		int status = _writeContext.writeValue();
		if(status != _writeContext.STATUS_OK_AFTER_COMMA) {
			_reportError("error: expecting end array");
		}
		if (!_writeContext.inArray()) {
      _reportError("Current context not an ARRAY but " + _writeContext.getTypeDesc());
	  }
	  gen.writeEnd();
	  _writeContext = _writeContext.getParent();
	}

	@Override
	public void writeStartObject() throws IOException {
		_verifyValueWrite("write start object");
		_writeContext = _writeContext.createChildObjectContext();
		gen.writeStartObject();
	}

	@Override
	public void writeEndObject() throws IOException {
		int status = _writeContext.writeValue();
		if (status != JsonWriteContext.STATUS_EXPECT_NAME) {
    	_reportError("error: expecting end object");
		}
		if (!_writeContext.inObject()) {
      _reportError("Current context not an OBJECT but " + _writeContext.getTypeDesc());
  	}
  	gen.writeEnd();
  	_writeContext = _writeContext.getParent();
	}

	@Override
	public void writeFieldName(String name) throws IOException {
		int status = _writeContext.writeFieldName(name);
		if (status == JsonWriteContext.STATUS_EXPECT_VALUE) {
    	_reportError("Can not write a field name, expecting a value");
		}
		gen.writeKey(name);
	}

	@Override
	public void writeString(String text) throws IOException {
		_verifyValueWrite("write string");
		gen.write(text);
	}

	@Override
	public void writeString(char[] buffer, int offset, int len) throws IOException {
		_verifyValueWrite("write string");
		gen.write(new String(buffer, offset, len));
	}

	@Override
	public void writeRawUTF8String(byte[] buffer, int offset, int len) throws IOException {
		_verifyValueWrite("writeRawUTF8String");
		gen.write(new String(buffer, offset, len));
	}

	@Override
	public void writeUTF8String(byte[] buffer, int offset, int len) throws IOException {
		_verifyValueWrite("writeUTF8String");
		gen.write(new String(buffer, offset, len));
	}

	@Override
	public void writeRaw(String text) throws IOException {
		_verifyValueWrite("writeRaw");
		gen.write(text);
	}

	@Override
	public void writeRaw(String text, int offset, int len) throws IOException {
		_verifyValueWrite("writeRaw");
		gen.write(text.substring(offset, offset+len));
	}

	@Override
	public void writeRaw(char[] text, int offset, int len) throws IOException {
		_verifyValueWrite("writeRaw");
		gen.write(new String(text, offset, offset+len));
	}

	@Override
	public void writeRaw(char c) throws IOException {
		_verifyValueWrite("writeRaw");
		gen.write(c);
	}

	// discuss
	@Override
	public void writeBinary(Base64Variant bv, byte[] data, int offset, int len) throws IOException {
		_verifyValueWrite("writeBinary");
		//gen.write(Base64.getEncoder().encode(Arrays.copyOfRange(data, offset, offset+len)));
		gen.write(Arrays.copyOfRange(data, offset, offset+len));
	}

	@Override
	public void writeNumber(int v) throws IOException {
		_verifyValueWrite("writeNumber");
		gen.write(v);
	}

	@Override
	public void writeNumber(long v) throws IOException {
		_verifyValueWrite("writeNumber");
		gen.write(v);
	}

	@Override
	public void writeNumber(BigInteger v) throws IOException {
		_verifyValueWrite("writeNumber");
		gen.write(v);
	}

	@Override
	public void writeNumber(double v) throws IOException {
		_verifyValueWrite("writeNumber");
		gen.write(v);
	}

	@Override
	public void writeNumber(float v) throws IOException {
		_verifyValueWrite("writeNumber");
		gen.write(v);
	}

	@Override
	public void writeNumber(BigDecimal v) throws IOException {
		_verifyValueWrite("writeNumber");
		gen.write(v);
	}

	@Override
	public void writeNumber(String encodedValue) throws IOException {
		_verifyValueWrite("writeNumber");
		gen.write(new BigDecimal(encodedValue));
	}

	@Override
	public void writeBoolean(boolean state) throws IOException {
		_verifyValueWrite("writeBoolean");
		gen.write(state);
	}
	
	public void writeLocalDateTime(LocalDateTime v) throws IOException {
		_verifyValueWrite("writeLocalDateTime");
		gen.write(v);
	}
	
	public void writeOffsetDateTime(OffsetDateTime v) throws IOException {
		_verifyValueWrite("writeOffsetDateTime");
		gen.write(v);
	}
	
	public void writeDuration(Duration v) throws IOException {
		_verifyValueWrite("writeDuration");
		gen.write(v);
	}

	public void writePeriod(Period v) throws IOException {
		_verifyValueWrite("writePeriod");
		gen.write(v);
	}

	@Override
	public void writeNull() throws IOException {
		_verifyValueWrite("writeNull");
		gen.writeNull();
	}
	
	@Override
	public boolean isClosed() {
		return closed;
	}
	
	@Override
	public void close() throws IOException {
		if(DEBUG) logger.log(Level.FINEST, "close");
		gen.close();
		closed = true;
	}
	
}
