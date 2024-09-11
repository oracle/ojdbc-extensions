package oracle.jdbc.provider.oson.deser;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import oracle.jdbc.jackson.oson.OsonParser;

import java.io.IOException;
import java.time.Year;

public class OsonByteDeserializer extends StdScalarDeserializer<byte[]> {
	public static final OsonByteDeserializer INSTANCE = new OsonByteDeserializer();

	protected OsonByteDeserializer() {
		super(Year.class);
	}
	protected OsonByteDeserializer(Class<?> vc) {
		super(vc);
	}

	protected OsonByteDeserializer(JavaType valueType) {
		super(valueType);
	}

	protected OsonByteDeserializer(StdScalarDeserializer<?> src) {
		super(src);
	}

	@Override
	public byte[] deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
		final OsonParser _parser = (OsonParser)p;

		return _parser.getBinaryValue();
	}
}