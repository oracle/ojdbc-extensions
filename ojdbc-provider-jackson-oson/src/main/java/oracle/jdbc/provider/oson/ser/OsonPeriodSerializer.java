package oracle.jdbc.provider.oson.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import oracle.jdbc.provider.oson.OsonGenerator;

import java.io.IOException;
import java.time.Period;

public class OsonPeriodSerializer extends StdSerializer<Period> {
	public static final OsonPeriodSerializer INSTANCE = new OsonPeriodSerializer();
	public OsonPeriodSerializer() {
		super(Period.class);
	}

	@Override
	public void serialize(Period value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		final OsonGenerator _gen = (OsonGenerator)gen;

		_gen.writePeriod(value);
	}
}