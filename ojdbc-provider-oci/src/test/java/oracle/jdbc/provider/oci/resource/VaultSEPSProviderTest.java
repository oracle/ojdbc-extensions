package oracle.jdbc.provider.oci.resource;

import oracle.jdbc.provider.TestProperties;
import oracle.jdbc.provider.oci.OciTestProperty;
import oracle.jdbc.spi.OracleResourceProvider;
import oracle.jdbc.spi.PasswordProvider;
import oracle.jdbc.spi.UsernameProvider;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.createParameterValues;
import static oracle.jdbc.provider.resource.ResourceProviderTestUtil.findProvider;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Verifies {@link VaultSEPSProvider} */
public class VaultSEPSProviderTest {
    private static final PasswordProvider PASSWORD_PROVIDER =
            findProvider(PasswordProvider.class, "ojdbc-provider-oci-vault-seps");

    private static final UsernameProvider USERNAME_PROVIDER =
            findProvider(UsernameProvider.class, "ojdbc-provider-oci-vault-seps");

    @Test
    public void testPassword() {
        Map<String, CharSequence> testParameters = new HashMap<>();
        testParameters.put("authenticationMethod", "config-file");
        testParameters.put("configFile", TestProperties.getOrAbort(OciTestProperty.OCI_CONFIG_FILE));
        testParameters.put("profile", TestProperties.getOrAbort(OciTestProperty.OCI_CONFIG_PROFILE));
        testParameters.put("ocid", TestProperties.getOrAbort(OciTestProperty.OCI_SEPS_WALLET_OCID));
        Optional.ofNullable(TestProperties.getOptional(OciTestProperty.OCI_SEPS_WALLET_PASSWORD))
                .ifPresent(password -> testParameters.put("walletPassword", password));

        Map<OracleResourceProvider.Parameter, CharSequence> parameterValues = createParameterValues(PASSWORD_PROVIDER, testParameters);

        char[] password = PASSWORD_PROVIDER.getPassword(parameterValues);
        assertNotNull(password);
    }

    @Test
    public void testUsername() {
        Map<String, CharSequence> testParameters = new HashMap<>();
        testParameters.put("authenticationMethod", "config-file");
        testParameters.put("configFile", TestProperties.getOrAbort(OciTestProperty.OCI_CONFIG_FILE));
        testParameters.put("profile", TestProperties.getOrAbort(OciTestProperty.OCI_CONFIG_PROFILE));
        testParameters.put("ocid", TestProperties.getOrAbort(OciTestProperty.OCI_SEPS_WALLET_OCID));
        Optional.ofNullable(TestProperties.getOptional(OciTestProperty.OCI_SEPS_WALLET_PASSWORD))
                .ifPresent(password -> testParameters.put("walletPassword", password));

        Map<OracleResourceProvider.Parameter, CharSequence> parameterValues = createParameterValues(USERNAME_PROVIDER, testParameters);

        String username = USERNAME_PROVIDER.getUsername(parameterValues);
        assertNotNull(username);

    }
}