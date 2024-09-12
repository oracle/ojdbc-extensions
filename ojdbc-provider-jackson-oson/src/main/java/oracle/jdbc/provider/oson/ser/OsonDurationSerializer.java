package oracle.jdbc.provider.oson.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import oracle.jdbc.provider.oson.OsonGenerator;

import java.io.IOException;
import java.time.Duration;

public class OsonDurationSerializer extends StdSerializer<Duration> {
	public static final OsonDurationSerializer INSTANCE = new OsonDurationSerializer();
	public OsonDurationSerializer() {
		super(Duration.class);
	}

	@Override
	public void serialize(Duration value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		final OsonGenerator _gen = (OsonGenerator)gen;

		_gen.writeDuration(value);
	}
}