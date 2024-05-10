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

package oracle.jdbc.provider.azure.authentication;

import com.azure.core.credential.TokenCredential;
import com.azure.core.util.Configuration;
import com.azure.identity.*;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;

import java.util.Locale;

import static com.azure.core.util.Configuration.*;
import static java.lang.String.format;
import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;
import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.SENSITIVE;

/**
 * <p>
 * A factory for creating {@link TokenCredential} objects from the Azure SDK
 * for Java.
 * </p><p>
 * This class is implemented using the azure-identity module of the Azure SDK
 * for Java. The Azure SDK defines environment variables that configure its
 * behavior. This class will attempt use these environment variables whenever
 * a {@link ParameterSet} has not configured a value otherwise. This is done to
 * accommodate programmers who may already be using the environment variables,
 * and do not wish to re-apply their configuration as Oracle JDBC connection
 * properties and URL parameters.
 * </p>
 */
public final class TokenCredentialFactory
    implements ResourceFactory<TokenCredential> {

  /** Method of authentication supported by the Azure SDK */
  public static final Parameter<AzureAuthenticationMethod>
    AUTHENTICATION_METHOD = Parameter.create(REQUIRED);

  /** ID of an Azure tenant to authenticate with */
  public static final Parameter<String> TENANT_ID = Parameter.create();

  /** ID of a client registered with Azure Active Directory */
  public static final Parameter<String> CLIENT_ID = Parameter.create();

  /** Secret of a client registered with Azure Active Directory */
  public static final Parameter<String> CLIENT_SECRET =
    Parameter.create(SENSITIVE);

  /**
   * File system path for the certificate of a client registered with Azure
   * Active Directory
   */
  public static final Parameter<String> CLIENT_CERTIFICATE_PATH =
    Parameter.create();

  /**
   * Password for the certificate of a client registered with Azure Active
   * Directory. This parameter is only required if a
   * {@link #CLIENT_CERTIFICATE_PATH} is configured for a certificate with
   * password protection (ie: PFX).
   */
  public static final Parameter<String> CLIENT_CERTIFICATE_PASSWORD =
    Parameter.create(SENSITIVE);

  /** Username of an Azure account */
  public static final Parameter<String> USERNAME = Parameter.create();

  /** Password of an Azure account */
  public static final Parameter<String> PASSWORD = Parameter.create(SENSITIVE);

  /**
   * Redirect URL registered with Azure Active Directory. This parameter is
   * optional for {@link AzureAuthenticationMethod#INTERACTIVE} authentication
   * in a web browser.
   */
  public static final Parameter<String> REDIRECT_URL = Parameter.create();

  private static final TokenCredentialFactory INSTANCE
      = new TokenCredentialFactory();

  private TokenCredentialFactory() { }

  /**
   * Returns a singleton of {@code TokenCredentialFactory}.
   * @return a singleton of {@code TokenCredentialFactory}
   */
  public static TokenCredentialFactory getInstance() {
    return INSTANCE;
  }

  @Override
  public Resource<TokenCredential> request(ParameterSet parameterSet) {
    TokenCredential tokenCredential = getCredential(parameterSet);
    // TODO: Access tokens expire. Does a TokenCredential internally cache one?
    //   If so, then return an expiring resource.
    return Resource.createPermanentResource(tokenCredential, true);
  }

  /**
   * Returns credentials for requesting an access token. The type of credentials
   * used are configured by the parameters of the given {@code parameters}.
   * Supported parameters are defined by the class variables in
   * {@link TokenCredentialFactory}.
   * @param parameters parameters that configure credentials. Not null.
   * @return Credentials configured by parameters
   */
  private static TokenCredential getCredential(ParameterSet parameterSet) {

    AzureAuthenticationMethod authenticationMethod =
      parameterSet.getRequired(AUTHENTICATION_METHOD);

    switch (authenticationMethod) {
      case DEFAULT:
        return defaultCredentials(parameterSet);
      case AUTO_DETECT:
        return autoDetectCredentials(parameterSet);
      case SERVICE_PRINCIPLE:
        return servicePrincipalCredentials(parameterSet);
      case MANAGED_IDENTITY:
        return managedIdentityCredentials(parameterSet);
      case PASSWORD:
        return passwordCredentials(parameterSet);
      case DEVICE_CODE:
        return deviceCodeCredentials(parameterSet);
      case INTERACTIVE:
        return interactiveCredentials(parameterSet);
        // TODO: Understand what's going on with client id here.
        //  If not specified, then the SDK uses a default id of
        //    04b07795-8ddb-461a-bbee-02f9e1bf7b46, which is defined in
        //    com.azure.identity.implementation.util.IdentityConstants. What is
        //    this?
        //  Unlike the default credentials class, the interactive credentials
        //    class does not read the AZURE_CLIENT_ID environment variable. But
        //    reading the envvar seems like what would be expected to happen,
        //    and so that's what this implementation will do.
        //  In testing, it is noted that the builder's configure(Configuration)
        //    method seems to have no effect. The client ID and tenant ID must
        //    be configured with their individual builder methods
        //  Unsettling feeling about all this. Why did MS devs think their
        //    implementation is the correct one? Investigate further to
        //    understand what information is missing. This code may not be doing
        //    "the right thing".
      default :
        throw new IllegalArgumentException(
          "Unrecognized authentication method: " + authenticationMethod);
    }
  }

  /**
   * Returns credentials resolved by {@link DefaultAzureCredential}, and
   * {@link #TENANT_ID} or {@link #CLIENT_ID} as optional parameters.
   * @param parameterSet
   * @return
   */
  private static TokenCredential defaultCredentials(ParameterSet parameterSet) {
    return new DefaultAzureCredentialBuilder()
      .tenantId(
        optionalParameter(parameterSet, TENANT_ID, PROPERTY_AZURE_TENANT_ID))
      .managedIdentityClientId(
        optionalParameter(parameterSet, CLIENT_ID, PROPERTY_AZURE_CLIENT_ID))
      .build();
  }

  /**
   * Returns credentials for interactive authentication in a web browser.
   * @param parameterSet Configures the request with a client ID (optional), and
   * redirect URL (required).
   * @return Credentials for interactive authentication
   * //throws //TODO: What does this throw?
   */
  private static InteractiveBrowserCredential interactiveCredentials(
    ParameterSet parameterSet) {
    return new InteractiveBrowserCredentialBuilder()
      .clientId(parameterSet.getOptional(CLIENT_ID))
      .redirectUrl(parameterSet.getOptional(REDIRECT_URL))
      .build();
  }

  /**
   * <p>
   * Returns credentials for authentication as a service principal. This
   * authentication method requires a client ID along with either a secret or
   * certificate. These values may be provided as URI parameters, or as
   * environment variables read by the Azure SDK.
   * </p><p>
   * All parameter values have precedence over SDK environment variables.
   * Secrets have precedence over certificates, and certificates have precedence
   * over passwords. This order of precedence is consistent with the behavior of
   * the Azure SDK's
   * {@link com.azure.identity.EnvironmentCredential} class.
   * </p>
   * @param parameterSet Optional parameters. Not null.
   * @return Credentials for a service principal. Not null.
   * @throws IllegalStateException If a required parameter is not configured.
   */
  private static TokenCredential servicePrincipalCredentials(
    ParameterSet parameters) {

    String secret = parameters.getOptional(CLIENT_SECRET);
    if (secret != null)
      return clientSecretCredentials(parameters, secret);

    String certificate = parameters.getOptional(CLIENT_CERTIFICATE_PATH);
    if (certificate != null)
      return clientCertificateCredentials(parameters, certificate);

    String sdkSecret =
      Configuration.getGlobalConfiguration()
        .get(PROPERTY_AZURE_CLIENT_SECRET);
    if (sdkSecret != null)
      return clientSecretCredentials(parameters, sdkSecret);

    String sdkCertificate =
      Configuration.getGlobalConfiguration()
        .get(PROPERTY_AZURE_CLIENT_CERTIFICATE_PATH);
    if (sdkCertificate != null)
      return clientCertificateCredentials(parameters, sdkCertificate);

    throw new IllegalStateException(
      "Unable to authenticate as a service principal:" +
        " Neither a secret nor a certificate is configured");
  }

  /**
   * Returns credentials for a client secret.
   * @param parameters optional parameters. Not null.
   * @param secret Client secret. Not null.
   * @return Credentials for a client secret. Not null.
   */
  private static TokenCredential clientSecretCredentials(
    ParameterSet parameters, String secret) {
    return configureBuilder(parameters, new ClientSecretCredentialBuilder())
      .clientSecret(secret)
      .build();
  }

  /**
   * <p>
   * Returns credentials for a client certificate. This method examines the
   * extension of the file name to detect if the certificate is PFX format, and
   * requires the "certificate-password" URI parameter if so. If the extension
   * is not ".pfx", this method assumes it is a PEM file.
   * </p><p>
   * Note that a password is optional for PFX certificates. The SDK does not
   * require one, and it is possible to create PFX certificates that don't have
   * a password.
   * </p>
   * @param parameters optional parameters. Not null.
   * @param certificatePath Path of the certificate file. Not null.
   * @return Credentials for a certificate. Not null.
   */
  private static TokenCredential clientCertificateCredentials(
    ParameterSet parameters, String certificatePath) {

    ClientCertificateCredentialBuilder builder =
      configureBuilder(parameters, new ClientCertificateCredentialBuilder());

    if (certificatePath.toLowerCase(Locale.getDefault()).endsWith(".pfx")) {
      builder.pfxCertificate(
        certificatePath,
        parameters.getOptional(CLIENT_CERTIFICATE_PASSWORD));
    }
    else{
      builder.pemCertificate(certificatePath);
    }

    return builder.build();
  }

  /**
   * Returns the given {@code builder} configured with standard parameters:
   * client ID, and tenant ID.
   * @param parameters optional parameters. Not null.
   * @param builder Builder to configure. Not null.
   * @return The configured builder. Not null.
   * @param <T> The generic type of the builder.
   * @throws IllegalStateException If a required parameter is not specified.
   */
  private static <T extends AadCredentialBuilderBase<T>> T configureBuilder(
    ParameterSet parameters, T  builder) {
    return builder
      .clientId(requireParameter(
        parameters, CLIENT_ID, PROPERTY_AZURE_CLIENT_ID))
      .tenantId(requireParameter(
        parameters, TENANT_ID, PROPERTY_AZURE_TENANT_ID));
  }

  /**
   * Returns credentials for a managed identity.
   * @param parameters optional parameters. Not null.
   * @return Credentials for a managed identity. Not null.
   */
  private static TokenCredential managedIdentityCredentials(
    ParameterSet parameters) {
    return new ManagedIdentityCredentialBuilder()
      .clientId(parameters.getOptional(CLIENT_ID))
      .build();
  }

  /**
   * Returns credentials for a username and password.
   * @param parameters optional parameters. Not null.
   * @return Credentials for a username and password. Not null.
   */
  private static TokenCredential passwordCredentials(ParameterSet parameters) {
    return new UsernamePasswordCredentialBuilder()
      .tenantId(optionalParameter(
        parameters, TENANT_ID, PROPERTY_AZURE_TENANT_ID))
      .clientId(requireParameter(
        parameters, CLIENT_ID, PROPERTY_AZURE_CLIENT_ID))
      .username(requireParameter(
        parameters, USERNAME, PROPERTY_AZURE_USERNAME))
      .password(requireParameter(
        parameters, PASSWORD, PROPERTY_AZURE_PASSWORD))
      .build();
  }

  /**
   * Returns credentials for a device code
   * @param parameters optional parameters. Not null.
   * @return Credentials for a username and password. Not null.
   */
  private static TokenCredential deviceCodeCredentials(ParameterSet parameters) {
    return new DeviceCodeCredentialBuilder()
      .tenantId(optionalParameter(
        parameters, TENANT_ID, PROPERTY_AZURE_TENANT_ID))
      .clientId(optionalParameter(
        parameters, CLIENT_ID, PROPERTY_AZURE_CLIENT_ID))
      .challengeConsumer(challenge -> {
        // Prints a URL for the user to visit and complete the challenge.
        System.out.println(challenge.getMessage());
      }).build();
  }

  /**
   * Returns credentials that are most appropriate for the current environment.
   * This method is intended to behave consistently with
   * {@link DefaultAzureCredential}, except that it also checks for
   * configuration in the {@link ParameterSet} passed to this method.
   * Credentials configured by a {@link ParameterSet} take precedence over
   * those configured by SDK environment variables. For instance, a username and
   * password configured as parameters will take precedence over a client id and
   * secret configured as environment variables.
   *
   * The presence of certain parameters will have certain authentication methods
   * attempted before the default credential
   *
   *
   * @param parameters optional parameters. Not null.
   * @return Credentials for the current environment. Not null.
   */
  private static TokenCredential autoDetectCredentials(ParameterSet parameters) {
    ChainedTokenCredentialBuilder builder = new ChainedTokenCredentialBuilder();

    // Check for parameters that configure authentication as a service
    // principal. If any are configured, then attempt this authentication method
    // before attempting authentication with a username and password configured
    // as parameters
    if (parameters.contains(CLIENT_ID)
      || parameters.contains(CLIENT_SECRET)
      || parameters.contains(CLIENT_CERTIFICATE_PATH)
      || parameters.contains(CLIENT_CERTIFICATE_PASSWORD))
      builder.addLast(servicePrincipalCredentials(parameters));

    // Check for parameters that configure authentication as an Azure user. If
    // any are configured, then attempt this authentication method before
    // attempting authentication with SDK environment variables.
    if (parameters.contains(USERNAME) || parameters.contains(PASSWORD))
      builder.addLast(passwordCredentials(parameters));

    // After attempting authentication configured by parameters, attempt
    // authentication using SDK environment variables.
    builder.addLast(defaultCredentials(parameters));

    return builder.build();
  }

  /**
   * Returns the value of a required parameter that may be configured by a
   * provider or an SDK environment variable.
   * @param parameters Parameters provided by the provider. Not null
   * @param parameter Parameter that may be configured by the provider. Not null
   * @param sdkName Name of the SDK environment variable, or null if there is
   * none.
   * @return The configured value of the parameter. Not null.
   * @throws IllegalStateException If the parameter is not configured by the
   * provider or the SDK.
   */
  private static String requireParameter(
    ParameterSet parameters, Parameter<String> parameter, String sdkName) {
    try {
      return parameters.getRequired(parameter);
    }
    catch (IllegalStateException parameterNotConfigured) {
      String sdkValue = Configuration.getGlobalConfiguration().get(sdkName);
      if (sdkValue != null)
        return sdkValue;

      throw new IllegalArgumentException(format(
        "No value is configured for parameter \"%s\"," +
          " or SDK variable \"%s\"",
        parameters.getName(parameter), sdkName), parameterNotConfigured);
    }
  }

  /**
   * Returns the value of an optional parameter which may be configured by a
   * provider or an SDK environment variable.
   * @param parameters Parameters provided by the provider. Not null
   * @param parameter Parameter that may be configured by the provider. Not null
   * @param sdkName Name of the SDK environment variable, or null if there is
   * none.
   * @return The configured value of the parameter, or null if not configured.
   */
  private static String optionalParameter(
    ParameterSet parameters, Parameter<String> parameter, String sdkName) {
    String value = parameters.getOptional(parameter);

    if (value != null)
      return value;

    if (sdkName != null)
      return Configuration.getGlobalConfiguration().get(sdkName);

    return null;
  }

}
