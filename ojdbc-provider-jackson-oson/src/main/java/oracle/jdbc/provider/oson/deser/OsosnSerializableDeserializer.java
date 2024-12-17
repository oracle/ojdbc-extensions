package oracle.jdbc.provider.oson.deser;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

public class OsosnSerializableDeserializer extends JsonDeserializer<Object> {
    public static final OsosnSerializableDeserializer INSTANCE = new OsosnSerializableDeserializer();

    OsosnSerializableDeserializer() {}

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        byte[] data = p.getBinaryValue();
        if (data == null) {
            return null;
        }
        try (ObjectInputStream objectStream = new ObjectInputStream(new ByteArrayInputStream(data))) {
            return (Serializable) objectStream.readObject(); // Deserialize the object
        } catch (ClassNotFoundException e) {
            throw new IOException("Class not found during deserialization", e);
        }
    }
}
