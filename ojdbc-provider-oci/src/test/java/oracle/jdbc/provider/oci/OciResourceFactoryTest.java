package oracle.jdbc.provider.oci;

import oracle.jdbc.provider.oci.objectstorage.ObjectFactory;
import oracle.jdbc.spi.OracleConfigurationProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.PropertyPermission;

import static org.junit.jupiter.api.Assertions.*;

class OciResourceFactoryTest {
  private static final String PROPERTY_NAME =
          "oci.javasdk.apache.idle.connection.monitor.thread.enabled";
  private static final OracleConfigurationProvider PROVIDER =
          OracleConfigurationProvider.find("ociobject");
  String originalPropertyValue;

  @BeforeEach
  void setUp() {
    originalPropertyValue = System.getProperty(PROPERTY_NAME);
  }

  @AfterEach
  void tearDown() {
    if (originalPropertyValue == null) {
      System.clearProperty(PROPERTY_NAME);
    } else {
      System.setProperty(PROPERTY_NAME, originalPropertyValue);
    }
  }

  @Test
  public void testSystemPropertyToFalse() throws Exception {
    System.clearProperty(PROPERTY_NAME);
    testEnsurePropertySetToFalseUsingReflection();
    assertTrue(System.getProperty(PROPERTY_NAME).equals("false"));
  }

  @Test
  public void testSystemPropertyAsTrue() throws Exception {
    System.setProperty(PROPERTY_NAME, "true");
    testEnsurePropertySetToFalseUsingReflection();
    assertTrue(System.getProperty(PROPERTY_NAME).equals("true"));
  }

  @Test
  public void testSystemPropertyAsFalse() throws Exception {
    System.setProperty(PROPERTY_NAME, "false");
    testEnsurePropertySetToFalseUsingReflection();
    assertTrue(System.getProperty(PROPERTY_NAME).equals("false"));
  }

  @Nested
  class ExceptionTest {
    private SecurityManager originalSecurityManager;

    @BeforeEach
    public void setUp() {
      originalSecurityManager = System.getSecurityManager();
    }

    @AfterEach
    public void tearDown() {
      System.setSecurityManager(originalSecurityManager);
    }

    @Test
    public void testSystemPropertyThrows() throws Exception{
      System.clearProperty(PROPERTY_NAME);
      System.setSecurityManager(new CustomSecurityManager());
      testEnsurePropertySetToFalseUsingReflection();
    }

    private class CustomSecurityManager extends SecurityManager {
      @Override
      public void checkPermission(Permission permission) {
        if (permission instanceof PropertyPermission) {
          PropertyPermission propertyPermission = (PropertyPermission) permission;
          if (propertyPermission.getName().equals(PROPERTY_NAME) && propertyPermission.getActions().contains("write")) {
            throw new AccessControlException("access denied (\"java.util.PropertyPermission\" \"" + PROPERTY_NAME + "\" \"write\")");
          }
        }
      }
    }
  }

  public void testEnsurePropertySetToFalseUsingReflection() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    OciResourceFactory factory = (OciResourceFactory) ObjectFactory.getInstance();
    Method method = OciResourceFactory.class.getDeclaredMethod("ensurePropertySetToFalse");
    method.setAccessible(true);
    method.invoke(factory);
  }
}