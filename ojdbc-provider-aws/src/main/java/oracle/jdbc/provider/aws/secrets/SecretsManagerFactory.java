/*
 ** Copyright (c) 2025 Oracle and/or its affiliates.
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
package oracle.jdbc.provider.aws.secrets;

import oracle.jdbc.provider.aws.AwsResourceFactory;
import oracle.jdbc.provider.cache.CachedResourceFactory;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;

public final class SecretsManagerFactory
    extends AwsResourceFactory<String> {

  /** The name of a secret. This is a required parameter. */
  public static final Parameter<String> SECRET_NAME =
      Parameter.create(REQUIRED);

  /** The Region of a secret. This is an optional parameter. */
  public static final Parameter<String> REGION =
      Parameter.create();

  /**
   * The name of the key if the secret contains key-value pairs.
   * This is an optional parameter.
   * */
  public static final Parameter<String> KEY_NAME =
      Parameter.create();

  private static final OracleJsonFactory JSON_FACTORY = new OracleJsonFactory();

  /**
   * The single instance of {@code CachedResourceFactory} for requesting key
   * vault secrets
   */
  private static final ResourceFactory<String> INSTANCE =
      CachedResourceFactory.create(new SecretsManagerFactory());

  private SecretsManagerFactory() { }

  /**
   * Returns a singleton of {@code KeyVaultSecretFactory}.
   * @return a singleton of {@code KeyVaultSecretFactory}
   */
  public static ResourceFactory<String> getInstance() {
    return INSTANCE;
  }

  @Override
  public Resource<String> request(
      AwsCredentials awsCredentials, ParameterSet parameterSet) {

    String secretName = parameterSet.getRequired(SECRET_NAME);
    String region = parameterSet.getOptional(REGION);
    String key = parameterSet.getOptional(KEY_NAME);

    SecretsManagerClientBuilder builder = SecretsManagerClient.builder()
        .credentialsProvider(() -> awsCredentials);
    if (region != null)
      builder.region(Region.of(region));

    SecretsManagerClient client = builder.build();
    GetSecretValueRequest request = GetSecretValueRequest.builder()
        .secretId(secretName).build();
    GetSecretValueResponse response = client.getSecretValue(request);

    String secretString = response.secretString();
    if (key != null) {
      // If key is provided, assume the secret contains key-value pairs
      try {
        try (InputStream secretInputStream =
                 new ByteArrayInputStream(secretString.getBytes(UTF_8))) {

          OracleJsonObject secretJsonObject = JSON_FACTORY
              .createJsonTextValue(secretInputStream)
              .asJsonObject();

          if (secretJsonObject.containsKey(key)) {
            secretString = secretJsonObject.getString(key);
          } else {
            throw new IllegalArgumentException(
                "Failed to find key \"" + key + "\" in " + secretName);
          }
        }
      } catch (IOException ioException) {
        throw new IllegalArgumentException(
            "Failed to read Secret: " + secretName, ioException);
      }
    }

    return Resource.createPermanentResource(secretString, true);
  }
}