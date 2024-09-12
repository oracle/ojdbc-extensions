package oracle.jdbc.provider.oson.deser;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import oracle.jdbc.provider.oson.OsonParser;

import java.io.IOException;
import java.time.Duration;

public class OsonDurationDeserializer extends StdScalarDeserializer<Duration> {
	public static final OsonDurationDeserializer INSTANCE = new OsonDurationDeserializer();

	protected OsonDurationDeserializer() {
		super(Duration.class);
	}
	protected OsonDurationDeserializer(Class<?> vc) {
		super(vc);
	}

	protected OsonDurationDeserializer(JavaType valueType) {
		super(valueType);
	}

	protected OsonDurationDeserializer(StdScalarDeserializer<?> src) {
		super(src);
	}

	@Override
	public Duration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
		final OsonParser _parser = (OsonParser)p;

		return _parser.readDuration();
	}
}