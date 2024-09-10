package oson.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import oracle.jdbc.jackson.oson.OsonGenerator;

import java.io.IOException;
import java.time.LocalDateTime;

public class OsonLocalDateTimeSerializer extends StdSerializer<LocalDateTime> {
	public static final OsonLocalDateTimeSerializer INSTANCE = new OsonLocalDateTimeSerializer();
	public OsonLocalDateTimeSerializer() {
		super(LocalDateTime.class);
	}

	@Override
	public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		final OsonGenerator _gen = (OsonGenerator)gen;

		_gen.writeLocalDateTime(value);
	}
}
