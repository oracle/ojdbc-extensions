package oracle.jdbc.provider.oson.deser;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import oracle.jdbc.provider.oson.OsonParser;

import java.io.IOException;
import java.time.Period;

public class OsonPeriodDeserializer extends StdScalarDeserializer<Period> {
	public static final OsonPeriodDeserializer INSTANCE = new OsonPeriodDeserializer();

	protected OsonPeriodDeserializer() {
		super(Period.class);
	}
	protected OsonPeriodDeserializer(Class<?> vc) {
		super(vc);
	}

	protected OsonPeriodDeserializer(JavaType valueType) {
		super(valueType);
	}

	protected OsonPeriodDeserializer(StdScalarDeserializer<?> src) {
		super(src);
	}

	@Override
	public Period deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
		final OsonParser _parser = (OsonParser)p;

		return _parser.readPeriod();
	}
}