/*
 *  Copyright (c) 2026 Oracle and/or its affiliates.
 *
 *  The Universal Permissive License (UPL), Version 1.0
 *
 *  Subject to the condition set forth below, permission is hereby granted to any
 *  person obtaining a copy of this software, associated documentation and/or data
 *  (collectively the "Software"), free of charge and under any and all copyright
 *  rights in the Software, and any and all patent rights owned or freely
 *  licensable by each licensor hereunder covering either (i) the unmodified
 *  Software as contributed to or provided by such licensor, or (ii) the Larger
 *  Works (as defined below), to deal in both
 *
 *  (a) the Software, and
 *  (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 *  one is included with the Software (each a "Larger Work" to which the Software
 *  is contributed by such licensors),
 *
 *  without restriction, including without limitation the rights to copy, create
 *  derivative works of, display, perform, and distribute the Software and make,
 *  use, sell, offer for sale, import, export, have made, and have sold the
 *  Software and the Larger Work(s), and to sublicense the foregoing rights on
 *  either these or other terms.
 *
 *  This license is subject to the following condition:
 *  The above copyright notice and either this complete permission notice or at
 *  a minimum a reference to the UPL must be included in all copies or
 *  substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package oracle.jdbc.provider.spring.oauth;

import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.parameter.ParameterSetBuilder;
import oracle.jdbc.provider.spring.context.ApplicationContextHolder;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.util.Collection;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;
import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.SENSITIVE;

/**
 * Parameters used by {@link OAuth2ResourceFactory} to request resources as an
 * OAuth 2.0 client.
 */
public final class OAuth2ResourceParameters {
  private OAuth2ResourceParameters(){}

  /**
   * <p>
   * The ID of an {@link ApplicationContext}.
   * </p><p><i>
   * This parameter should always be set in any {@link ParameterSet} that
   * contains values read from the {@link Environment} of a
   * {@code ApplicationContext}
   * </i></p><p>
   * It is possible that two different ApplicationContext have two different
   * OAuth 2.0 client registrations with the same registration ID. This means
   * {@link #REGISTRATION_ID} alone is not enough to identify an identical
   * {@link ParameterSet} when using a
   * {@link oracle.jdbc.provider.cache.CachedResourceFactory}.
   * </p>
   */
  public static final Parameter<String> APPLICATION_CONTEXT_ID =
    Parameter.create();

  /**
   * <p>
   * The ID of an OAuth 2.0 client registration or provider. This ID can appear
   * within a Spring property name, such as:
   * </p><pre>
   * spring.security.oauth2.client.registration.${registration-id}.client-id
   * </pre>
   */
  public static final Parameter<String> REGISTRATION_ID =
    Parameter.create();

  /**
   * spring.security.oauth2.client.provider.${provider-id}.token-uri
   */
  public static final ProviderParameter<String> TOKEN_URI;

  /**
   * spring.security.oauth2.client.registration.${registration-id}.provider
   */
  public static final RegistrationParameter<String> PROVIDER;

  /**
   * spring.security.oauth2.client.registration.${registration-id}.client-id
   */
  public static final RegistrationParameter<AuthorizationGrantType> GRANT_TYPE;

  /**
   * spring.security.oauth2.client.registration.${registration-id}.client-id
   */
  public static final RegistrationParameter<String> CLIENT_ID;

  /**
   * spring.security.oauth2.client.registration.${registration-id}.client-secret
   */
  public static final RegistrationParameter<String> CLIENT_SECRET;

  /**
   * spring.security.oauth2.client.registration.${registration-id}.scope
   */
  public static final RegistrationParameter<String> SCOPE;

  private static final ProviderParameter<?>[] PROVIDER_PARAMETERS = {

    TOKEN_URI = new ProviderParameter<>(
      Parameter.create(REQUIRED),
      "token-uri",
      String.class),
  };

  private static final RegistrationParameter<?>[] REGISTRATION_PARAMETERS = {

    PROVIDER = new RegistrationParameter<>(
      Parameter.create(REQUIRED),
      "provider",
      String.class),

    GRANT_TYPE = new RegistrationParameter<>(
      Parameter.create(REQUIRED),
      "authorization-grant-type",
      AuthorizationGrantType.class),

    CLIENT_ID =
      new RegistrationParameter<>(
        Parameter.create(REQUIRED),
        "client-id",
        String.class),

    CLIENT_SECRET =
      new RegistrationParameter<>(
        Parameter.create(SENSITIVE),
        "client-secret",
        String.class),

    SCOPE =
      new RegistrationParameter<>(
        Parameter.create(),
        "scope",
        String.class),
  };


  /**
   * <p>
   * Returns a copy of a ParameterSet that is configured with OAuth 2.0
   * parameters read from Spring properties. The input ParameterSet must
   * contain a {@link #REGISTRATION_ID} indicating which OAuth 2.0 client
   * registration to read properties from.
   * </p><p>
   * The ParameterSet returned by this
   * method is suitable for use by {@link OAuth2ResourceFactory} and its
   * subclasses. This method will not overwrite any parameter that is already
   * configured in the original ParameterSet.
   *</p>
   *
   * @param parameterSet Existing parameters. Not null.
   *
   * @return A copy of the original ParameterSet, with the addition of Spring
   * properties. Not null.
   */
  public static ParameterSet addSpringProperties(ParameterSet parameterSet) {
    ParameterSetBuilder builder = parameterSet.copyBuilder();
    ApplicationContext applicationContext = ApplicationContextHolder.get();

    if (applicationContext == null) {
      return builder.build();
    }

    builder.add(
      "spring-application-context-id",
      APPLICATION_CONTEXT_ID,
      applicationContext.getId());

    Environment environment = applicationContext.getEnvironment();
    String registrationId = parameterSet.getRequired(REGISTRATION_ID);

    for (RegistrationParameter<?> parameter : REGISTRATION_PARAMETERS) {
      if (! parameterSet.contains(parameter)) {
        set(environment, registrationId, parameter, builder);
      }
    }

    String providerId =
      resolveProviderId(parameterSet, environment, registrationId);

    for (OAuth2ResourceParameter<?> parameter : PROVIDER_PARAMETERS) {
      if (! parameterSet.contains(parameter)) {
        set(environment, providerId, parameter, builder);
      }
    }

    return builder.build();
  }

  /**
   * Returns the provider ID that should be used to look up Spring properties.
   * If a provider name is configured for a client registration, it should
   * be used. Otherwise, the client registration ID is used as a provider ID.
   */
  private static String resolveProviderId(
    ParameterSet parameterSet,
    Environment environment,
    String registrationId) {

    String providerId = parameterSet.getOptional(PROVIDER);

    if (providerId != null) {
      return providerId;
    }

    providerId = environment.getProperty(
      PROVIDER.springPropertyName(registrationId),
      String.class);

    if (providerId != null) {
      return providerId;
    }

    return registrationId;
  }

  /**
   * Sets a Spring property as a parameter, or does nothing if no value is
   * configured for the Spring property.
   *
   * @param environment Environment to read the property from. Not null.
   * @param registrationId OAuth 2.0 registration ID that appears in the
   * property name. Not null.
   * @param parameter Parameter to set. Not null.
   * @param builder Builder to set with the parameter. Not null.
   * @param <T> Class to parse the parameter as.
   */
  private static <T> void set(
    Environment environment,
    String registrationId,
    OAuth2ResourceParameter<T> parameter,
    ParameterSetBuilder builder) {

    String propertyName = parameter.springPropertyName(registrationId);

    T value = environment.getProperty(propertyName, parameter.valueClass());

    if (value == null) {
      return;
    }

    builder.add(propertyName, parameter, value);
  }

  /**
   * A parameter for OAuth 2.0 resources accessed using Spring. The basic
   * Parameter interface is extended to read values from Spring properties.
   * @param <T> Class of the parameter value.
   */
  static abstract class OAuth2ResourceParameter<T> implements Parameter<T> {
    /**
     * Delegate object that implement the basic Parameter interface methods.
     */
    private final Parameter<T> delegate;

    /**
     * Unqualified name of a Spring property that provides a value for this
     * parameter, such as the unqualified "client-id" name that appears
     * within the fully qualified
     * "spring.security.oauth2.client.registration.${registration-id}.client-id"
     * name.
     */
    private final String unqualifiedName;

    /**
     * Class of the Parameter value, such as {@code String.class} for a
     * {@code Parameter<String>}.
     */
    private final Class<T> valueClass;

    public OAuth2ResourceParameter(
      Parameter<T> delegate,
      String unqualifiedName,
      Class<T> valueClass) {
      this.delegate = delegate;
      this.unqualifiedName = unqualifiedName;
      this.valueClass = valueClass;
    }

    @Override
    public Collection<? extends Attribute> getAttributes() {
      return delegate.getAttributes();
    }

    @Override
    public boolean isSensitive() {
      return delegate.isSensitive();
    }

    @Override
    public boolean isRequired() {
      return delegate.isRequired();
    }

    /**
     * Returns the unqualified name of a Spring property that provides a value
     * for this parameter, such as the unqualified "client-id" name that appears
     * within the fully qualified
     * "spring.security.oauth2.client.registration.${registration-id}.client-id"
     * name. Not null.
     *
     * @return The unqualified name. Not null.
     */
    public String unqualifiedName() {
      return unqualifiedName;
    }

    /**
     * Returns the class to parse this parameter as.
     * @return the class to parse this parameter as. Not null.
     */
    public Class<T> valueClass() {
      return valueClass;
    }

    /**
     * Returns a fully qualified Spring property name that may provide a value
     * for this parameter, such as
     * "spring.security.oauth2.client.registration.${registration-id}.client-id"
     * for {@link #CLIENT_ID}.
     *
     * @param registrationId Registration (or provider) ID that may appear in
     * the fully qualified name. Not null.
     * @return The fully qualified Spring property name.
     */
    public abstract String springPropertyName(String registrationId);
  }

  /**
   * An OAuth 2.0 client registration parameter, such as
   * spring.security.oauth2.client.registration.{registrationId}.client-id
   *
   * @param <T> Class to parse the parameter as
   */
  public static final class RegistrationParameter<T>
    extends OAuth2ResourceParameter<T> {

    /**
     * Constructs an OAuth 2.0 client registration parameter that can be read
     * from a Spring property.
     *
     * @param delegate Parameter to use for when delegating methods of the
     * basic {@link Parameter} interface, such as
     * {@link Parameter#isSensitive()}. Not null.
     *
     * @param unqualifiedName Unqualified name of a Spring property that
     * provides a value for this parameter, such as the unqualified
     * "client-id" name that appears within the fully qualified
     * "spring.security.oauth2.client.provider.${registration-id}.client-id"
     * name.
     *
     * @param valueClass Class to parse the parameter as. Not null.
     */
    public RegistrationParameter(
      Parameter<T> delegate, String unqualifiedName, Class<T> valueClass) {
      super(delegate, unqualifiedName, valueClass);
    }

    @Override
    public String springPropertyName(String registrationId) {
      return "spring.security.oauth2.client.registration."
        + registrationId
        + '.' + unqualifiedName();
    }
  }

  /**
   * An OAuth 2.0 client provider parameter, such as
   * spring.security.oauth2.client.provider.{registration-id}.token-uri
   *
   * @param <T> Class to parse the parameter as
   */
  public static final class ProviderParameter<T>
    extends OAuth2ResourceParameter<T> {

    /**
     * Constructs an OAuth 2.0 client provider parameter that can be read from
     * a Spring property.
     *
     * @param delegate Parameter to use for when delegating methods of the
     * basic {@link Parameter} interface, such as
     * {@link Parameter#isSensitive()}. Not null.
     *
     * @param unqualifiedName Unqualified name of a Spring property that
     * provides a value for this parameter, such as the unqualified
     * "token-uri" name that appears within the fully qualified
     * "spring.security.oauth2.client.provider.${registration-id}.token-uri"
     * name.
     *
     * @param valueClass Class to parse the parameter as. Not null.
     */
    public ProviderParameter(
      Parameter<T> delegate, String unqualifiedName, Class<T> valueClass) {
      super(delegate, unqualifiedName, valueClass);
    }

    @Override
    public String springPropertyName(String registrationId) {
      return "spring.security.oauth2.client.provider."
        + registrationId
        + '.' + unqualifiedName();
    }
  }
}
