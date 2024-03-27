package oracle.jdbc.provider.oci;

import oracle.jdbc.provider.oci.databasetools.DatabaseToolsConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OciResourceFactoryTest {
    private static final String PROPERTY_NAME = "oci.javasdk.apache.idle.connection.monitor.thread.enabled";

    @AfterEach
    void tearDown() {
        System.clearProperty(PROPERTY_NAME);
    }

    @Test
    public void testSystemPropertyAsFalse() {
        assertTrue(System.getProperty(PROPERTY_NAME) == null);

        OciResourceFactory factory = (OciResourceFactory) DatabaseToolsConnectionFactory.getInstance();
        assertTrue(System.getProperty(PROPERTY_NAME).equals("false"));
    }

    @Test
    public void testSystemPropertyAsTrue() {
        assertTrue(System.getProperty(PROPERTY_NAME) == null);

        System.setProperty(PROPERTY_NAME, "true");
        assertTrue(System.getProperty(PROPERTY_NAME).equals("true"));

        OciResourceFactory factory = (OciResourceFactory) DatabaseToolsConnectionFactory.getInstance();
        assertTrue(System.getProperty(PROPERTY_NAME).equals("true"));
    }
}