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

package oracle.jdbc.provider.oci.resource;

import com.oracle.bmc.Region;
import oracle.jdbc.provider.oci.authentication.AuthenticationDetailsFactory;
import oracle.jdbc.provider.oci.authentication.AuthenticationMethod;
import oracle.jdbc.provider.oci.database.WalletFactory;
import oracle.jdbc.provider.oci.vault.Secret;
import oracle.jdbc.provider.oci.vault.SecretFactory;
import oracle.jdbc.provider.resource.AbstractResourceProvider;
import oracle.jdbc.provider.resource.ResourceParameter;
import oracle.jdbc.provider.util.Wallet;

import java.util.Map;
import java.util.stream.Stream;

import static oracle.jdbc.provider.oci.authentication.AuthenticationMethod.*;

/**
 * Super class of all {@link oracle.jdbc.spi.OracleResourceProvider}
 * implementations that request a resource from OCI. This super class defines
 * parameters for authentication with OCI.
 */
public abstract class OciResourceProvider
  extends AbstractResourceProvider {

  private static final ResourceParameter[] PARAMETERS =
    new ResourceParameter[] {
      new ResourceParameter(
        "authenticationMethod",
        AuthenticationDetailsFactory.AUTHENTICATION_METHOD,
        "auto-detect",
        OciResourceProvider::parseAuthenticationMethod),
      new ResourceParameter(
        "configFile",
        AuthenticationDetailsFactory.CONFIG_FILE_PATH),
      new ResourceParameter(
        "profile",
        AuthenticationDetailsFactory.CONFIG_PROFILE),
      new ResourceParameter(
        "tenantId",
        AuthenticationDetailsFactory.TENANT_ID),
      new ResourceParameter(
        "username",
        AuthenticationDetailsFactory.USERNAME),
      new ResourceParameter(
        "region",
        AuthenticationDetailsFactory.REGION,
        null, // no default
        Region::fromRegionCodeOrId)
    };

  /**
   * <p>
   * Constructs a provider that is identified by the name:
   * </p><pre>{@code
   *   ojdbc-provider-oci-{valueType}
   * }</pre><p>
   * This constructor defines all parameter for authentication with OCI.
   * Subclasses must call this constructor with any additional parameters for
   * the specific resource they provide.
   * </p>
   *
   * @param valueType The value type identifier that appears the name of the
   *   provider. Not null.
   * @param parameters parameters that are specific to the subclass provider.
   *   Not null.
   */
  protected OciResourceProvider(
      String valueType, ResourceParameter... parameters) {

    super("oci", valueType,
      Stream.concat(
          Stream.of(PARAMETERS),
          Stream.of(parameters))
        .toArray(ResourceParameter[]::new));
  }

  /**
   * Parses the "authentication-method" URI parameter as an
   * {@link AuthenticationMethod}
   * recognized by the {@link AuthenticationDetailsFactory}.
   */
  private static AuthenticationMethod parseAuthenticationMethod(
      String authenticationMethod) {
    switch (authenticationMethod) {
      case "config-file": return CONFIG_FILE;
      case "instance-principal": return INSTANCE_PRINCIPAL;
      case "resource-principal": return RESOURCE_PRINCIPAL;
      case "cloud-shell": return CLOUD_SHELL;
      case "interactive": return INTERACTIVE;
      case "auto-detect": return AUTO_DETECT;
      default:
        throw new IllegalArgumentException(authenticationMethod);
    }
  }

  /**
   * <p>
   * Retrieves a secret from OCI Vault identified by a set of parameters
   * provided in {@code parameterValues}. This method is intended to centralize
   * secret retrieval logic and can be called by subclasses implementing
   * {@link oracle.jdbc.spi.OracleResourceProvider} SPI.
   * </p><p>
   * This method uses the {@code getResource} method to parse parameters
   * from {@code parameterValues} and retrieve the secret from OCI Vault
   * through the {@link SecretFactory} instance.
   * </p>
   *
   * @param parameterValues The map of parameter names and their corresponding
   * text values required for secret retrieval. Must not be null.
   * @return The {@link Secret} object containing the retrieved secret data.
   * Not null.
   */
  protected Secret getVaultSecret(
          Map<Parameter, CharSequence> parameterValues) {
    return getResource(SecretFactory.getInstance(),parameterValues);
  }

  /**
   * <p>
   * Retrieves a wallet from the Autonomous Database (ADB) service using
   * a set of parameters provided in {@code parameterValues}. This method
   * centralizes wallet retrieval logic for use by subclasses implementing the
   * {@link oracle.jdbc.spi.OracleResourceProvider} SPI.
   * </p><p>
   * This method uses the {@code getResource} method to parse parameters
   * from {@code parameterValues} and retrieve the wallet from the ADB
   * service through the {@link WalletFactory} instance. Wallets contain
   * connection strings and TLS key and trust material for establishing secure
   * connections with the database.
   * </p>
   *
   * @param parameterValues The map of parameter names and their corresponding
   * text values required for wallet retrieval. Must not be null.
   * @return The {@link Wallet} object containing connection strings and
   * TLS material. Not null.
   */
  protected Wallet getAutonomousDatabaseWallet(
          Map<Parameter, CharSequence> parameterValues) {
    return getResource(WalletFactory.getInstance(), parameterValues);
  }

}
