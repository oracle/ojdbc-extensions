/*
 ** Copyright (c) 2024 Oracle and/or its affiliates.
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
package oracle.jdbc.provider.gcp.secrets;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.google.cloud.secretmanager.v1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1.Secret;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretName;
import com.google.cloud.secretmanager.v1.SecretPayload;
import com.google.cloud.secretmanager.v1.SecretVersionName;

import oracle.jdbc.provider.cache.CachedResourceFactory;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;

/**
 * <p>
 * Factory for requesting secrets from the Vault service. Secrets are
 * represented as {@link SecretPayload} objects. Oracle JDBC can use the content
 * of a secret as a database password, or any other security sensitive value.
 * </p>
 */
public class GcpSecretManagerFactory implements ResourceFactory<SecretPayload> {

  /** The secret version name for the secret */
  public static final Parameter<String> SECRET_VERSION_NAME = Parameter.create(REQUIRED);

  private static final ResourceFactory<SecretPayload> INSTANCE = CachedResourceFactory
      .create(new GcpSecretManagerFactory());

  private GcpSecretManagerFactory() {
  }

  /**
   * Returns a singleton of {@code GcpVaultSecretFactory}.
   * 
   * @return a singleton of {@code GcpVaultSecretFactory}
   */
  public static ResourceFactory<SecretPayload> getInstance() {
    return INSTANCE;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Requests the content of a secret bundle from the Vault service. The
   * {@code parameterSet} is required to include a {@link #SECRET_VERSION_NAME}.
   * </p>
   */
  @Override
  public Resource<SecretPayload> request(ParameterSet parameterSet)
      throws IllegalStateException, IllegalArgumentException {
    String paramerter = parameterSet.getRequired(SECRET_VERSION_NAME);

    try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
      SecretVersionName secretVersionName = SecretVersionName.parse(paramerter);
      SecretName secretName = SecretName.of(secretVersionName.getProject(), secretVersionName.getSecret());
      Secret secret = client.getSecret(secretName);
      AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);
      if (secret.hasExpireTime()) {
        OffsetDateTime expireTime = OffsetDateTime
            .ofInstant(Instant.ofEpochMilli(secret.getExpireTime().getNanos() * 1000), ZoneId.systemDefault());
        return Resource.createExpiringResource(response.getPayload(), expireTime, true);
      } else {
        return Resource.createPermanentResource(response.getPayload(), true);
      }
    } catch (IOException io) {
      throw new IllegalStateException(io);
    }
  }

}
