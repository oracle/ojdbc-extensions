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

package oracle.jdbc.provider.azure.keyvault;

import com.azure.core.credential.TokenCredential;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import oracle.jdbc.provider.azure.AzureResourceFactory;
import oracle.jdbc.provider.cache.CachedResourceFactory;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;

import java.time.OffsetDateTime;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;

/**
 * Factory for requesting secrets from the Key Vault service of Azure.
 */
public final class KeyVaultSecretFactory
    extends AzureResourceFactory<KeyVaultSecret> {

  /** The URL of a key vault. This is a required parameter. */
  public static final Parameter<String> VAULT_URL =
    Parameter.create(REQUIRED);

  /** The name of a secret within a key vault. This is a required parameter. */
  public static final Parameter<String> SECRET_NAME =
    Parameter.create(REQUIRED);

  /**
   * The single instance of {@code CachedResourceFactory} for requesting key
   * vault secrets
   */
  private static final ResourceFactory<KeyVaultSecret> INSTANCE =
    CachedResourceFactory.create(new KeyVaultSecretFactory());

  private KeyVaultSecretFactory() { }

  /**
   * Returns a singleton of {@code KeyVaultSecretFactory}.
   * @return a singleton of {@code KeyVaultSecretFactory}
   */
  public static ResourceFactory<KeyVaultSecret> getInstance() {
    return INSTANCE;
  }

  @Override
  public Resource<KeyVaultSecret> request(
      TokenCredential tokenCredential, ParameterSet parameterSet) {

    String vaultUri = parameterSet.getRequired(VAULT_URL);
    String secretName = parameterSet.getRequired(SECRET_NAME);

    SecretClient secretClient =
      new SecretClientBuilder()
        .credential(tokenCredential)
        .vaultUrl(vaultUri)
        .buildClient();

    KeyVaultSecret keyVaultSecret = secretClient.getSecret(secretName);
    OffsetDateTime expireTime = keyVaultSecret.getProperties().getExpiresOn();

    if (expireTime == null)
      return Resource.createPermanentResource(keyVaultSecret, true);
    else
      return Resource.createExpiringResource(keyVaultSecret, expireTime, true);
  }
}
