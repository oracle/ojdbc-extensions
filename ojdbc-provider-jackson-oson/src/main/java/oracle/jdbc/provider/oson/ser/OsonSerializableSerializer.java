package oracle.jdbc.provider.oson.ser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import oracle.jdbc.provider.oson.OsonGenerator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class OsonSerializableSerializer extends JsonSerializer<Object> {

    public final static OsonSerializableSerializer INSTANCE = new OsonSerializableSerializer();

    public OsonSerializableSerializer() {}


    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        }
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {
            objectStream.writeObject(value); // Serialize object to byte array
        }

        byte[] bytes = byteStream.toByteArray();
        gen.writeBinary(bytes);
    }
}
