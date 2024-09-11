package oracle.jdbc.provider.oson.deser;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import oracle.jdbc.jackson.oson.OsonParser;

import java.io.IOException;
import java.math.BigInteger;

public class OsonBigIntegerDeserializer extends StdScalarDeserializer<BigInteger> {
	public static final OsonBigIntegerDeserializer INSTANCE = new OsonBigIntegerDeserializer();

	protected OsonBigIntegerDeserializer() {
		super(BigInteger.class);
	}
	protected OsonBigIntegerDeserializer(Class<?> vc) {
		super(vc);
	}

	protected OsonBigIntegerDeserializer(JavaType valueType) {
		super(valueType);
	}

	protected OsonBigIntegerDeserializer(StdScalarDeserializer<?> src) {
		super(src);
	}

	@Override
	public BigInteger deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
		final OsonParser _parser = (OsonParser)p;

		return _parser.getBigIntegerValue();
	}
}