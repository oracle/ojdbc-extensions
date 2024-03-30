package oracle.jdbc.provider.oci;

import oracle.jdbc.provider.oci.databasetools.DatabaseToolsConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class OciResourceFactoryTest {
    private static final String PROPERTY_NAME = "oci.javasdk.apache.idle.connection.monitor.thread.enabled";

    @AfterEach
    void tearDown() {
        System.clearProperty(PROPERTY_NAME);
    }

    @Test
    public void testSystemPropertyToFalse() {
        assertNull(System.getProperty(PROPERTY_NAME), "Property should be null initially");;
        OciResourceFactory factory = (OciResourceFactory) DatabaseToolsConnectionFactory.getInstance();
        assertTrue(System.getProperty(PROPERTY_NAME).equals("false"));
    }

    @Test
    public void testSystemPropertyAsTrue() {
        System.setProperty(PROPERTY_NAME, "true");
        OciResourceFactory factory = (OciResourceFactory) DatabaseToolsConnectionFactory.getInstance();
        assertTrue(System.getProperty(PROPERTY_NAME).equals("true"));
    }

    @Nested
    class ExceptionTest {
        private Path policyPath;
        private String originalPolicyProperty;
        private SecurityManager originalSecurityManager;

        @BeforeEach
        public void setUp() throws IOException {
            originalPolicyProperty = System.getProperty("java.security.policy");
            originalSecurityManager = System.getSecurityManager();

            String content = "grant {\n" +
                    "    permission java.util.PropertyPermission \"oci.javasdk.apache.idle.connection.monitor.thread.enabled\", \"read\";\n" +
                    "    permission java.lang.RuntimePermission \"setSecurityManager\";\n" +
                    "    permission java.lang.reflect.ReflectPermission \"suppressAccessChecks\";\n" +
                    "    permission java.io.FilePermission \"./security.policy\", \"delete\";\n" +
                    "    permission java.util.PropertyPermission \"java.security.policy\", \"write\";\n" +
                    "};";
            policyPath = Files.createTempFile(Paths.get("./"), "security", ".policy");
            Files.write(policyPath, content.getBytes(StandardCharsets.UTF_8));
        }

        @AfterEach
        public void tearDown() throws IOException {
            if (originalPolicyProperty != null) {
                System.setProperty("java.security.policy", originalPolicyProperty);
            } else {
                System.clearProperty("java.security.policy");
            }
            System.setSecurityManager(originalSecurityManager);
            Files.deleteIfExists(policyPath);
        }

        @Test
        public void testSystemPropertyThrows() {
            System.setProperty("java.security.policy", policyPath.toString());
            System.setSecurityManager(new SecurityManager());

            OciResourceFactory factory = (OciResourceFactory) DatabaseToolsConnectionFactory.getInstance();
        }
    }
}