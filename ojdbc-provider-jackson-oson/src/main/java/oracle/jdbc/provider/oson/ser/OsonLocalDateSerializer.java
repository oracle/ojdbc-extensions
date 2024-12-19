package oracle.jdbc.provider.oson.ser;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import oracle.jdbc.provider.oson.OsonGenerator;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class OsonLocalDateSerializer extends LocalDateSerializer {

    public static final OsonLocalDateSerializer INSTANCE = new OsonLocalDateSerializer();

    public OsonLocalDateSerializer() {
        super();
    }

    public OsonLocalDateSerializer(OsonLocalDateSerializer base, Boolean useTimestamp,
                                   DateTimeFormatter dtf, JsonFormat.Shape shape) {
        super(base, useTimestamp, dtf, shape);
    }

    public OsonLocalDateSerializer(DateTimeFormatter formatter) {
        super(formatter);
    }

    @Override
    protected OsonLocalDateSerializer withFormat(Boolean useTimestamp, DateTimeFormatter dtf, JsonFormat.Shape shape) {
        return new OsonLocalDateSerializer(this, useTimestamp, dtf, shape);
    }

    @Override
    public void serialize(LocalDate date, JsonGenerator g, SerializerProvider provider) throws IOException {
        if(_formatter != null || _shape != null) {
            super.serialize(date, g, provider);
            return;
        }
        if (g instanceof OsonGenerator) {
            OsonGenerator generator = (OsonGenerator) g;
            generator.writeLocalDate(date);
            return;
        }else {
            super.serialize(date, g, provider);
        }
    }
}
