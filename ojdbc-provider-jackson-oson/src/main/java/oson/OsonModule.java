package oson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.VersionUtil;
import com.fasterxml.jackson.databind.module.SimpleModule;
import oracle.jdbc.jackson.oson.deser.*;
import oracle.jdbc.jackson.oson.ser.*;

import java.math.BigInteger;
import java.time.*;

public class OsonModule extends SimpleModule {
	public final static Version VERSION = VersionUtil.parseVersion(
			"1.0.0-b", "com.oracle.database.jdbc", "jackson-oson"
	);

	public OsonModule() {
		super(VERSION);

		addDeserializer(LocalDateTime.class, OsonLocalDateTimeDeserializer.INSTANCE);
		addSerializer(LocalDateTime.class, OsonLocalDateTimeSerializer.INSTANCE);

		addDeserializer(OffsetDateTime.class, OsonOffsetDateTimeDeserializer.INSTANCE);
		addSerializer(OffsetDateTime.class, OsonOffsetDateTimeSerializer.INSTANCE);
		
		addDeserializer(Period.class, OsonPeriodDeserializer.INSTANCE);
		addSerializer(Period.class, OsonPeriodSerializer.INSTANCE);
		
		addDeserializer(Duration.class, OsonDurationDeserializer.INSTANCE);
		addSerializer(Duration.class, OsonDurationSerializer.INSTANCE);
		
		addDeserializer(BigInteger.class, OsonBigIntegerDeserializer.INSTANCE);
		addSerializer(BigInteger.class, OsonBigIntegerSerializer.INSTANCE);
		
		addDeserializer(Year.class, OsonYearDeserializer.INSTANCE);
		addSerializer(Year.class, OsonYearSerializer.INSTANCE);
		
		addDeserializer(byte[].class, OsonByteDeserializer.INSTANCE);
		addSerializer(byte[].class, OsonByteSerializer.INSTANCE);
		
	}
}
