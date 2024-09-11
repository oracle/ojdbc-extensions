package oracle.jdbc.provider.oson.deser;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import oracle.jdbc.jackson.oson.OsonParser;

import java.io.IOException;
import java.time.OffsetDateTime;

public class OsonOffsetDateTimeDeserializer extends StdScalarDeserializer<OffsetDateTime> {
	public static final OsonOffsetDateTimeDeserializer INSTANCE = new OsonOffsetDateTimeDeserializer();

	protected OsonOffsetDateTimeDeserializer() {
		super(OffsetDateTime.class);
	}
	protected OsonOffsetDateTimeDeserializer(Class<?> vc) {
		super(vc);
	}

	protected OsonOffsetDateTimeDeserializer(JavaType valueType) {
		super(valueType);
	}

	protected OsonOffsetDateTimeDeserializer(StdScalarDeserializer<?> src) {
		super(src);
	}

	@Override
	public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
		final OsonParser _parser = (OsonParser)p;

		return _parser.readOffsetDateTime();
	}
}