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
package oracle.jdbc.provider.spring;

import java.util.Map;

/**
 * <p>
 * Names of properties that configure Spring Security tests. Descriptions and
 * examples of each property can be found in the "example-test.properties" file
 * within the root directory of the ojdbc-provider-spring module.
 * </p><p>
 * Many test properties can be mapped to a Spring property for configuring an
 * OAuth 2.0 client registration or provider. A list of such Spring properties
 * <a href="https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html#oauth2login-boot-property-mappings">
 * may be found here
 * </a>. This enum class implements a {@link #toSpringProperty(String)} method
 * so that tests may create client registration and provider configurations
 * at runtime. See the getSpringPropertyParameters method in
 * {@link oracle.jdbc.provider.spring.resource.SpringSecurityContextProviderTest}
 * for an example of that. The SpringSecurityContextProvider is able to
 * read this configuration via its
 * {@link oracle.jdbc.provider.spring.context.ApplicationContextHolder} class.
 * This is an alternative way to configure the provider, as opposed to
 * passing a parameters Map to
 * {@link oracle.jdbc.spi.EndUserSecurityContextProvider#getEndUserSecurityContext(Map)}.
 * It is though that most users will opt for Spring's configuration over
 * JDBC connection properties, so it is important to test this scenario.
 * </p>
 */
public enum SpringTestProperty {
  AZURE_TOKEN_URI(SpringPropertyType.PROVIDER, "token-uri"),
  AZURE_CLIENT_ID(SpringPropertyType.REGISTRATION, "client-id"),
  AZURE_CLIENT_SECRET(SpringPropertyType.REGISTRATION, "client-secret"),
  AZURE_SCOPE(SpringPropertyType.REGISTRATION, "scope"),
  OCI_TOKEN_URI(SpringPropertyType.PROVIDER, "token-uri"),
  OCI_CLIENT_ID(SpringPropertyType.REGISTRATION, "client-id"),
  OCI_CLIENT_SECRET(SpringPropertyType.REGISTRATION, "client-secret"),
  OCI_SCOPE(SpringPropertyType.REGISTRATION, "scope"),
  OCI_USER_OCID(SpringPropertyType.NONE, null);

  /**
   * Prefix of the Spring property that this test property maps to, such as
   * "spring.security.oauth2.client.registration." for both
   * {@link #AZURE_CLIENT_ID} and {@link #OCI_CLIENT_ID}.
   * This field is null if there is no spring property associated to this test
   * property.
   */
  final SpringPropertyType springPropertyType;

  /**
   * Name of Spring property that this test property maps to, such as
   * "client-id" for both {@link #AZURE_CLIENT_ID} and {@link #OCI_CLIENT_ID}.
   * This field is null if there is no spring property associated to this test
   * property.
   */
  final String springName;

  SpringTestProperty(SpringPropertyType springPropertyType, String springName) {
    this.springPropertyType = springPropertyType;
    this.springName = springName;
  }

  public String springName() {
    return springName;
  }

  /**
   * Composes a fully-qualified Spring property name from a given ID string,
   * such as "spring.security.oauth2.client.registration.example.client-id" if
   * the identifier is "example" and this is {@link #AZURE_CLIENT_ID} or
   * {@link #OCI_CLIENT_ID}.
   *
   * @param identifier The identifier of an OAUTH 2.0 client registration or
   * provider. Not null.
   *
   * @return The fully-qualified Spring property name, or null if this test
   * property cannot be configured as a Spring property.
   */
  public String toSpringProperty(String identifier) {
    if (springPropertyType == SpringPropertyType.NONE) {
      return null;
    }

    return springPropertyType.prefix + identifier + "." + springName;
  }

  public enum SpringPropertyType {
    NONE(null),
    REGISTRATION("spring.security.oauth2.client.registration."),
    PROVIDER("spring.security.oauth2.client.provider.");

    /**
     * Prefix for this type of Spring property, identifying it as a client
     * registration or provider.
     */
    private final String prefix;

    SpringPropertyType(String value) {
      this.prefix = value;
    }

    public String prefix() {
      return prefix;
    }
  }
}
