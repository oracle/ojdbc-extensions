package oracle.jdbc.provider.oson.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import oracle.jdbc.jackson.oson.OsonGenerator;

import java.io.IOException;
import java.time.OffsetDateTime;

public class OsonOffsetDateTimeSerializer extends StdSerializer<OffsetDateTime> {
	public static final OsonOffsetDateTimeSerializer INSTANCE = new OsonOffsetDateTimeSerializer();
	public OsonOffsetDateTimeSerializer() {
		super(OffsetDateTime.class);
	}

	@Override
	public void serialize(OffsetDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		final OsonGenerator _gen = (OsonGenerator)gen;

		_gen.writeOffsetDateTime(value);
	}
}
