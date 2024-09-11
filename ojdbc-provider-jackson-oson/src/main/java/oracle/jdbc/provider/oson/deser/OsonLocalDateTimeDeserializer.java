package oracle.jdbc.provider.oson.deser;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import oracle.jdbc.jackson.oson.OsonParser;

import java.io.IOException;
import java.time.LocalDateTime;

public class OsonLocalDateTimeDeserializer extends StdScalarDeserializer<LocalDateTime> {
	public static final OsonLocalDateTimeDeserializer INSTANCE = new OsonLocalDateTimeDeserializer();

	protected OsonLocalDateTimeDeserializer() {
		super(LocalDateTime.class);
	}
	protected OsonLocalDateTimeDeserializer(Class<?> vc) {
		super(vc);
	}

	protected OsonLocalDateTimeDeserializer(JavaType valueType) {
		super(valueType);
	}

	protected OsonLocalDateTimeDeserializer(StdScalarDeserializer<?> src) {
		super(src);
	}

	@Override
	public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
		final OsonParser _parser = (OsonParser)p;

		return _parser.readLocalDateTime();
	}
}