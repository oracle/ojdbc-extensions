package oracle.jdbc.provider.oson.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import oracle.jdbc.jackson.oson.OsonGenerator;

import java.io.IOException;

public class OsonByteSerializer extends StdSerializer<byte[]> {

	public static final OsonByteSerializer INSTANCE = new OsonByteSerializer();
	public OsonByteSerializer() {
		super(byte[].class);
	}

	@Override
	public void serialize(byte[] value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		final OsonGenerator _gen = (OsonGenerator)gen;

		_gen.writeBinary(value);
	}
}
