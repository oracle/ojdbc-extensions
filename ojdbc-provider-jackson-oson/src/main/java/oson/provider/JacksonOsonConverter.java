package oson.provider;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import oracle.jdbc.jackson.oson.OsonFactory;
import oracle.jdbc.jackson.oson.OsonModule;
import oracle.jdbc.spi.OsonConverter;
import oracle.sql.json.OracleJsonGenerator;
import oracle.sql.json.OracleJsonParser;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class JacksonOsonConverter implements OsonConverter{

	private static final OsonFactory osonFactory = new OsonFactory();
	private static final ObjectMapper om = new ObjectMapper(osonFactory);
	private final Lock lock = new ReentrantLock();
	
	static {
		om.findAndRegisterModules();
		om.registerModule(new OsonModule());
	}
	
	public JacksonOsonConverter() throws IOException {
	}

	@Override
	public void serialize(OracleJsonGenerator oGen, Object object) throws IllegalStateException {
		System.out.println("Printing from OsonConverter in extension");
		try {
			lock.lock();
			om.writeValue(osonFactory.createGenerator(oGen), object);
		} 
		catch (IOException e) {
			throw new IllegalStateException("Oson conversion failed", e);
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public Object deserialize(OracleJsonParser oParser, Class<?> type) throws IllegalStateException {
		if(!oParser.hasNext()) return null;
		try {
			return om.readValue(osonFactory.createParser(oParser), type);
		} 
		catch (IOException e) {
			throw new IllegalArgumentException("Object parsing from oson failed", e);
		}
	}
	
	public static Object convertValue(Object fromValue, JavaType javaType) {
		return om.convertValue(fromValue, javaType);
	}


}
