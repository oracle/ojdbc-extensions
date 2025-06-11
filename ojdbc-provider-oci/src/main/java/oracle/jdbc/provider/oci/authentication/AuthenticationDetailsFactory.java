/*
 ** Copyright (c) 2023 Oracle and/or its affiliates.
 **
 ** The Universal Permissive License (UPL), Version 1.0
 **
 ** Subject to the condition set forth below, permission is hereby granted to any
 ** person obtaining a copy of this software, associated documentation and/or data
 ** (collectively the "Software"), free of charge and under any and all copyright
 ** rights in the Software, and any and all patent rights owned or freely
 ** licensable by each licensor hereunder covering either (i) the unmodified
 ** Software as contributed to or provided by such licensor, or (ii) the Larger
 ** Works (as defined below), to deal in both
 **
 ** (a) the Software, and
 ** (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 ** one is included with the Software (each a "Larger Work" to which the Software
 ** is contributed by such licensors),
 **
 ** without restriction, including without limitation the rights to copy, create
 ** derivative works of, display, perform, and distribute the Software and make,
 ** use, sell, offer for sale, import, export, have made, and have sold the
 ** Software and the Larger Work(s), and to sublicense the foregoing rights on
 ** either these or other terms.
 **
 ** This license is subject to the following condition:
 ** The above copyright notice and either this complete permission notice or at
 ** a minimum a reference to the UPL must be included in all copies or
 ** substantial portions of the Software.
 **
 ** THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 ** IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 ** FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 ** AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 ** LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 ** OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 ** SOFTWARE.
 */

package oracle.jdbc.provider.oci.authentication;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.auth.RegionProvider;
import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimplePrivateKeySupplier;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.oci.objectstorage.ObjectFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.SENSITIVE;

/**
 * Factory for {@link com.oracle.bmc.auth.AuthenticationDetailsProvider}
 * objects. The {@link AuthenticationMethod} enum defines the authentication
 * methods supported by this factory.
 */
public final class AuthenticationDetailsFactory
    implements ResourceFactory<AbstractAuthenticationDetailsProvider> {

  /** A method of authentication to use */
  public static final Parameter<AuthenticationMethod> AUTHENTICATION_METHOD =
    Parameter.create();

  /** File system path of an OCI configuration file */
  public static final Parameter<String> CONFIG_FILE_PATH =
    Parameter.create();

  /** Name of a profile in an OCI configuration file */
  public static final Parameter<String> CONFIG_PROFILE =
    Parameter.create();

  /** OCID of an OCI tenant */
  public static final Parameter<String> TENANT_ID = Parameter.create();

  /** OCID of an OCI user */
  public static final Parameter<String> USER_ID = Parameter.create();

  /** Fingerprint of the public key */
  public static final Parameter<String> FINGERPRINT = Parameter.create();

  /** Private key used for authentication */
  public static final Parameter<String> PRIVATE_KEY = Parameter.create(SENSITIVE);

  /** Passphrase of the private key, if exists */
  public static final Parameter<String> PASS_PHRASE = Parameter.create(SENSITIVE);

  /**
   * Username of an OCI (IAM) account. Currently, this parameter is only
   * used to create a unique cache key for resources from interactive
   * authentication. A cached resource authenticated with USERNAME="A" can not
   * be accessed when USERNAME="B". The cache recognizes that the USERNAME is
   * different.
   */
  public static final Parameter<String> USERNAME = Parameter.create();

  /**
   * <p>
   * An OCI region provided by instances of
   * {@link AbstractAuthenticationDetailsProvider} which implement the
   * {@link RegionProvider} interface. This parameter is optional, and is only
   * applicable to {@link AuthenticationMethod#API_KEY} and
   * {@link AuthenticationMethod#INTERACTIVE}.
   * </p><p>
   * Many "Client*" classes of the OCI SDK seem to be designed with
   * an assumption that the {@code AbstractAuthenticationDetailsProvider}
   * passed to their constructor will implement the {@code RegionProvider}
   * interface, and will return a non-null region from the
   * {@link RegionProvider#getRegion()} method. If this assumption is broken,
   * a {@code NullPointerException} may be thrown when the client requests a
   * resource.
   * </p><p>
   * This parameter is designed for cases where this factory returns an
   * {@code AbstractAuthenticationDetailsProvider} which breaks the assumption
   * described in the previous paragraph. Users may explicitly configure this
   * parameter to identify a region, either with an ID or code (see
   * {@link Region#fromRegionCodeOrId(String)}).
   * </p><p>
   * This parameter is optional. If no region is configured, a novel solution
   * may be employed to determine the region of a resource. For instance, the
   * {@link ObjectFactory} is able to
   * parse a region code from an OCID. Similarly,
   * {@link InteractiveAuthentication} is able to extract a region code from an
   * ID token.
   * </p>
   */
  public static final Parameter<Region> REGION = Parameter.create();

  private static final AuthenticationDetailsFactory INSTANCE =
      new AuthenticationDetailsFactory();

  private AuthenticationDetailsFactory(){}

  /**
   * Returns a singleton of {@code AuthenticationDetailsFactory}.
   * @return a singleton of {@code AuthenticationDetailsFactory}
   */
  public static AuthenticationDetailsFactory getInstance() {
    return INSTANCE;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Returns authentication details configured by {@link Parameter} objects
   * defined in this class. The {@code parameterSet} is required to contain
   * an {@link #AUTHENTICATION_METHOD}. Additional parameters may be required
   * depending on which authentication method is configured.
   * </p>
   */
  @Override
  public Resource<AbstractAuthenticationDetailsProvider> request(
      ParameterSet parameterSet) {

    AbstractAuthenticationDetailsProvider authenticationDetails =
        getAuthenticationDetails(parameterSet);

    // TODO: Check for authentication details that expire, perhaps because a
    //  config file is updated, or because an interactive session token has
    //  expired. Return an expiring resource when appropriate.
    return Resource.createPermanentResource(authenticationDetails, true);
  }

  private static AbstractAuthenticationDetailsProvider getAuthenticationDetails(
    ParameterSet parameterSet) {

    AuthenticationMethod authenticationMethod =
      parameterSet.getRequired(AUTHENTICATION_METHOD);

    switch (authenticationMethod) {
      case CONFIG_FILE:
        return configFileAuthentication(parameterSet);
      case CLOUD_SHELL:
        return cloudShellAuthentication();
      case INSTANCE_PRINCIPAL:
        return instancePrincipalAuthentication();
      case RESOURCE_PRINCIPAL:
        return resourcePrincipalAuthentication();
      case INTERACTIVE:
        return InteractiveAuthentication.getSessionToken(parameterSet);
      case AUTO_DETECT:
        return autoDetectAuthentication(parameterSet);
      case API_KEY:
        return apiKeyBasedAuthentication(parameterSet);
      default:
        throw new IllegalArgumentException(
          "Unrecognized authentication method: " + authenticationMethod);
    }
  }

  /**
   * Returns authentication details for an OCI Configuration File. The path and
   * profile of the config file may be configured by optional parameters of the
   * given {@code configURI}. Otherwise, this method will read the DEFAULT
   * profile from $HOME/.oci/config.
   * @param parameters optional config file parameters. Not null.
   * @return Authentication details from a config file. Not null.
   * @throws IllegalStateException If a config file can not be accessed
   */
  private static ConfigFileAuthenticationDetailsProvider
    configFileAuthentication(ParameterSet parameters) {
    return configFileAuthentication(
      parameters.getOptional(CONFIG_FILE_PATH),
      parameters.getOptional(CONFIG_PROFILE));
  }

  /**
   * Returns authentication details for the provided credentials from the given
   * {@code parameters}.
   * @param parameters optional config file parameters. Not null.
   * @return Authentication details from a config file. Not null.
   */
  private static AuthenticationDetailsProvider
    simpleAuthentication(ParameterSet parameters) {
    return simpleAuthentication(
      parameters.getRequired(TENANT_ID),
      parameters.getRequired(USER_ID),
      parameters.getRequired(FINGERPRINT),
      parameters.getRequired(PRIVATE_KEY),
      parameters.getOptional(PASS_PHRASE),
      parameters.getOptional(REGION));
  }

  /**
   * Returns authentication details using the following API key-based
   * methods:
   * <ol>
   *   <li>Simple authentication: if all required credentials (tenant ID,
   *   user ID, fingerprint, and private key are provided in the given
   *   {@code parameters}</li>
   *   <li>Config File (API key) authentication: otherwise</li>
   * </ol>
   * @return API key-based authentication details
   */
  private static AuthenticationDetailsProvider
    apiKeyBasedAuthentication(ParameterSet parameters) {
    boolean hasAllRequiredKeys =
      parameters.contains(TENANT_ID)
      && parameters.contains(USER_ID)
      && parameters.contains(FINGERPRINT)
      && parameters.contains(PRIVATE_KEY);

    if(hasAllRequiredKeys)
      return simpleAuthentication(parameters);

    return configFileAuthentication(parameters);
  }

  /**
   * Returns authentication details that are most appropriate for the current
   * environment. Appropriateness of each authentication method is ranked as
   * follows:
   * <ol>
   *   <li>Cloud Shell (delegation token)</li>
   *   <li>Instance Principle</li>
   *   <li>Config File (API key)</li>
   * </ol>
   * This method attempts to use each authentication method in the order listed
   * above, and returns the details for the first successful authentication
   * method it finds.
   * @return Auto-detected authentication details
   * @throws IllegalStateException If all authentication methods fail.
   */
  private static BasicAuthenticationDetailsProvider
    autoDetectAuthentication(ParameterSet parameters) {

    // Keep track of failed attempts from each authentication method, and
    // include them as suppressed exceptions if all authentication methods fail.
    IllegalStateException previousFailure;

    try {
      return configFileAuthentication(parameters);
    }
    catch (RuntimeException noConfigFile) {
      previousFailure =
        new IllegalStateException(
          "Failed to authenticate using a configuration file", noConfigFile);
    }

    try {
      return cloudShellAuthentication();
    }
    catch (RuntimeException notCloudShell) {
      previousFailure.addSuppressed(
        new IllegalStateException(
          "Failed to authenticate using a cloud shell delegation token",
          notCloudShell));
    }

    try {
      return resourcePrincipalAuthentication();
    }
    catch (RuntimeException notContainer) {
      previousFailure.addSuppressed(
        new IllegalStateException(
          "Failed to authenticate as a resource principal",
          notContainer));
    }

    try {
      return instancePrincipalAuthentication();
    }
    catch (RuntimeException notComputeInstance) {
      previousFailure.addSuppressed(
        new IllegalStateException(
          "Failed to authenticate as an instance principal",
          notComputeInstance));
    }

    throw previousFailure;
  }

  /**
   * <p>
   * Returns authentication details for an instance principal. This
   * authentication method should only work on compute instances where internal
   * network endpoints are reachable.
   * </p><p>
   * It is thought that authentication as an instance principal should not take
   * more than a few seconds to complete, so this method will throw an
   * {@code IllegalStateException} if a timeout of 5 seconds is exceeded.
   * </p>
   * @return Authentication details for an instance principal. Not null.
   * @throws IllegalStateException If the current environment is not a compute
   * instance.
   */
  private static InstancePrincipalsAuthenticationDetailsProvider
    instancePrincipalAuthentication() {
    try {
      return InstancePrincipalAuthenticationTask.FUTURE
        .get(5, TimeUnit.SECONDS);
    }
    catch (ExecutionException exception) {
      throw new IllegalStateException(
        "Failed to authenticate as an instance principal",
        exception.getCause());
    }
    catch (InterruptedException interruptedException) {
      throw new IllegalStateException(
        "Authentication as an instance principal was interrupted",
        interruptedException);
    }
    catch (TimeoutException timeoutException) {
      throw new IllegalStateException(
        "Authentication as an instance principal did not complete within" +
          " 5 seconds",
        timeoutException);
    }
  }

  /**
   * <p>
   * Returns authentication details for a resource principal. This
   * authentication method should only work on ... TODO: Where does this work?
   * In an Oracle Function compute instance?
   * </p>
   * @return Authentication details for a resource principal. Not null.
   * @throws IllegalStateException If the current environment is not a compute
   * instance. TODO: What does build() throw in this case? Catch it.
   */
  private static ResourcePrincipalAuthenticationDetailsProvider
    resourcePrincipalAuthentication() {
    // TODO: Check if resource principal authentication is possible in the
    //  current env, similar to requireComputeInstance() maybe?
    return ResourcePrincipalAuthenticationDetailsProvider.builder().build();
  }

  /**
   * Returns authentication details for a Cloud Shell session. In a Cloud Shell
   * session, an "{@code OCI_CONFIG_FILE}" environment variable is expected
   * to be set to the path of an alternative configuration file. If this
   * environment variable is not set, an {@code IllegalStateException} is thrown
   * indicating that the current environment is not a Cloud Shell session.
   * @return Authentication details for a cloud shell session.
   * @throws IllegalStateException If a config file can not be accessed
   * @implNote As of August 2022, the following has been observed:
   * A Cloud Shell session sets the {@code OCI_CONFIG_FILE} environment variable
   * to a non-default location, {@code /etc/oci/config}. That config file has
   * a DEFAULT profile with "authentication_type=instance_principal" and
   * "delegation_token_file=/etc/oci/delegation_token".
   */
  private static AuthenticationDetailsProvider cloudShellAuthentication() {
    String envConfigFile =
      System.getenv(ConfigFileReader.OCI_CONFIG_FILE_PATH_ENV_VAR_NAME);

    if (envConfigFile == null) {
      throw new IllegalStateException(
        "Current environment is not a Cloud Shell session: The \"" +
          ConfigFileReader.OCI_CONFIG_FILE_PATH_ENV_VAR_NAME +
          " environment variable is not set.");
    }

    return configFileAuthentication(envConfigFile, "DEFAULT");
  }

  /**
   * Returns authentication details for an OCI Configuration File. The path and
   * profile of the config file may be configured by optional arguments to this
   * method. Otherwise, this method will read the DEFAULT
   * profile from $HOME/.oci/config.
   * @param filePath Path to the config file, or {@code null} to use the default
   * path: "$HOME/.oci/config".
   * @param profile Profile of the config file, or {@code null} to use the
   * default profile: "DEFAULT".
   * @return Authentication details from a config file.
   * @throws IllegalStateException If a config file can not be accessed
   */
  private static ConfigFileAuthenticationDetailsProvider
    configFileAuthentication(String filePath, String profile) {
    try {
      if (filePath == null)
        return new ConfigFileAuthenticationDetailsProvider(profile);
      else
        return new ConfigFileAuthenticationDetailsProvider(filePath, profile);
    }
    catch (IOException ioException) {
      throw configFileReadFailure(ioException);
    }
  }

  /**
   * Returns an exception indicating that an OCI Configuration File could not
   * be read.
   * @param ioException Cause of the read failure
   * @return An exception for configuration file reading
   */
  private static IllegalStateException configFileReadFailure(
    IOException ioException) {
    return new IllegalStateException(
      "Failed to read an OCI configuration file. See cause for details.",
      ioException);
  }

  /**
   * Returns authentication details for the provided credentials.
   * @param tenancy OCID of the tenancy
   * @param user OCID of the user
   * @param fingerprint Fingerprint of the public key
   * @param privateKey Private key
   * @param passPhrase Passphrase that is used to encrypt the private key.
   *                   {@code null} if not used.
   * @return Authentication details from provided credentials.
   */
  private static AuthenticationDetailsProvider simpleAuthentication(
    String tenancy,
    String user,
    String fingerprint,
    String privateKey,
    String passPhrase,
    Region region) {

    return SimpleAuthenticationDetailsProvider.builder()
      .tenantId(tenancy)
      .userId(user)
      .fingerprint(fingerprint)
      .privateKeySupplier(new SimplePrivateKeySupplier(privateKey))
      .passPhrase(passPhrase)
      .region(region)
      .build();
  }

  /**
   * This class lazily initializes a {@link #FUTURE} that completes with the
   * result of authenticating as an instance principal. The OCI SDK may block a
   * thread for several minutes if this authentication fails, and there does not
   * appear to be any way to configure a shorter timeout. This class is designed
   * to enable a shorter timeout by calling {@link Future#get(long, TimeUnit)}.
   */
  private static final class InstancePrincipalAuthenticationTask {

    /**
     * <p>
     * Future that completes with the result of authentication as an instance
     * principal.
     * </p><p>
     * This field is participating in a
     * <a href="https://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom">
     * lazy-loaded singleton design pattern.
     * </a>.
     * </p>
     */
    private static final Future<InstancePrincipalsAuthenticationDetailsProvider>
      FUTURE = CompletableFuture.supplyAsync(
        () -> InstancePrincipalsAuthenticationDetailsProvider.builder().build(),
        runnable -> {
          Thread thread = new Thread(
            runnable,
            "ojdbc-provider-oci: Instance Principal Authentication");
          thread.setDaemon(true);
          thread.start();
        });

  }
}
