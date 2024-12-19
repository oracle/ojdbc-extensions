package oracle.jdbc.provider.oson.deser;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import oracle.jdbc.provider.oson.OsonParser;
import oracle.sql.json.OracleJsonParser;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OsonLocalDateDeserializer extends LocalDateDeserializer {
    public static final OsonLocalDateDeserializer INSTANCE = new OsonLocalDateDeserializer();
    public OsonLocalDateDeserializer() {
        super();
    }
    public OsonLocalDateDeserializer(DateTimeFormatter dateFormat) {
        super(dateFormat);
    }

    public OsonLocalDateDeserializer(OsonLocalDateDeserializer base, DateTimeFormatter dtf) {
        super(base, dtf);
    }

    public OsonLocalDateDeserializer(OsonLocalDateDeserializer base, Boolean leniency) {
        super(base, leniency);
    }

    public OsonLocalDateDeserializer(OsonLocalDateDeserializer base, JsonFormat.Shape shape) {
        super(base, shape);
    }

    @Override
    protected OsonLocalDateDeserializer withDateFormat(DateTimeFormatter dtf) {
        return new OsonLocalDateDeserializer(dtf);
    }

    @Override
    protected OsonLocalDateDeserializer withShape(JsonFormat.Shape shape) {
        return new OsonLocalDateDeserializer(this,shape);
    }

    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if(_shape != null || _formatter!= DateTimeFormatter.ISO_LOCAL_DATE)
            return super.deserialize(parser, context);

        if (parser instanceof OsonParser) {
            OsonParser _parser = (OsonParser) parser;

            if(_parser.currentOsonEvent().equals(OracleJsonParser.Event.VALUE_DATE)) {
                LocalDateTime dateTime = _parser.getLocalDateTime();
                LocalDate localDate = dateTime.toLocalDate();
                return localDate;
            }
        }
        else {
            return super.deserialize(parser, context);
        }
        return super.deserialize(parser, context);
    }
}
