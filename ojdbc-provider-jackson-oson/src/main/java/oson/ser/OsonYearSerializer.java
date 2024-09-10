package oson.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import oracle.jdbc.jackson.oson.OsonGenerator;

import java.io.IOException;
import java.time.Year;

public class OsonYearSerializer extends StdSerializer<Year> {

	public static final OsonYearSerializer INSTANCE = new OsonYearSerializer();
	public OsonYearSerializer() {
		super(Year.class);
	}

	@Override
	public void serialize(Year value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		final OsonGenerator _gen = (OsonGenerator)gen;

		_gen.writeNumber(value.getValue());
	}
}
