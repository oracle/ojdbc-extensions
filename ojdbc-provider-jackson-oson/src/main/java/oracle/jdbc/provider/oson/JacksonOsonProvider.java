package oracle.jdbc.provider.oson;

import oracle.jdbc.spi.JsonProvider;
import oracle.jdbc.spi.OsonConverter;

import java.io.IOException;
import java.util.Map;

public class JacksonOsonProvider implements JsonProvider{

	public static final String PROVIDER_NAME = "jackson-json-provider";
	
	public JacksonOsonProvider () throws IOException {
	}

	@Override
	public String getName() {
		return PROVIDER_NAME;
	}


	@Override
	public OsonConverter getOsonConverter(Map<Parameter, CharSequence> parameterValues) {
		try {
			return new JacksonOsonConverter();
		} 
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
