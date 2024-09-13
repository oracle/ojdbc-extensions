package oracle.jdbc.provider.oson.deser;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import oracle.jdbc.provider.oson.OsonParser;

import java.io.IOException;
import java.time.Year;

public class OsonYearDeserializer extends StdScalarDeserializer<Year> {
	public static final OsonYearDeserializer INSTANCE = new OsonYearDeserializer();

	protected OsonYearDeserializer() {
		super(Year.class);
	}
	protected OsonYearDeserializer(Class<?> vc) {
		super(vc);
	}

	protected OsonYearDeserializer(JavaType valueType) {
		super(valueType);
	}

	protected OsonYearDeserializer(StdScalarDeserializer<?> src) {
		super(src);
	}

	@Override
	public Year deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
		final OsonParser _parser = (OsonParser)p;

		return Year.of(_parser.getIntValue());
	}
}