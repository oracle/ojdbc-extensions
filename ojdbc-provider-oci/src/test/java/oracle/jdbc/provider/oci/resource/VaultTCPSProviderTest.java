package oracle.jdbc.provider.oci.resource;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.oci.OciTestProperty;
import oracle.jdbc.spi.OracleResourceProvider.Parameter;
import oracle.jdbc.spi.TlsConfigurationProvider;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.createParameterValues;
import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.findProvider;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies {@link VaultTCPSProvider}
 */
public class VaultTCPSProviderTest {


  private static final TlsConfigurationProvider PROVIDER =
          findProvider(TlsConfigurationProvider.class,
                  "ojdbc-provider-oci-vault-tls");

  @Test
  public void test() {
    Map<String, CharSequence> testParameters = new HashMap<>();
    testParameters.put("authenticationMethod", "config-file");
    testParameters.put("configFile",
            TestProperties.getOrAbort(OciTestProperty.OCI_CONFIG_FILE));
    testParameters.put("profile",
            TestProperties.getOrAbort(OciTestProperty.OCI_CONFIG_PROFILE));
    testParameters.put("ocid",
            TestProperties.getOrAbort(OciTestProperty.OCI_TLS_WALLET_OCID));
    testParameters.put("type",
            TestProperties.getOrAbort(OciTestProperty.OCI_TLS_FILE_TYPE));
    Optional.ofNullable(TestProperties.getOptional(
            OciTestProperty.OCI_TLS_WALLET_PASSWORD))
            .ifPresent(password -> testParameters.put("password",
                    password));

    Map<Parameter, CharSequence> parameterValues =
            createParameterValues(PROVIDER, testParameters);


    SSLContext sslContext = PROVIDER.getSSLContext(parameterValues);
    assertNotNull(sslContext);

  }

}