package oson.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import oracle.jdbc.jackson.oson.OsonGenerator;

import java.io.IOException;
import java.math.BigInteger;

public class OsonBigIntegerSerializer extends StdSerializer<BigInteger> {
	public static final OsonBigIntegerSerializer INSTANCE = new OsonBigIntegerSerializer();
	public OsonBigIntegerSerializer() {
		super(BigInteger.class);
	}

	@Override
	public void serialize(BigInteger value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		final OsonGenerator _gen = (OsonGenerator)gen;

		_gen.writeNumber(value);
	}
}