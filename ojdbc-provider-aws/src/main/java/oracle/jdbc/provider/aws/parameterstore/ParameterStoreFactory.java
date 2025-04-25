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

package oracle.jdbc.provider.aws.parameterstore;

import oracle.jdbc.provider.aws.AwsResourceFactory;
import oracle.jdbc.provider.cache.CachedResourceFactory;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import static oracle.jdbc.provider.aws.configuration.AwsConfigurationParameters.REGION;
import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;

public final class ParameterStoreFactory
        extends AwsResourceFactory<String> {

  /**
   * The name of the parameter to retrieve from AWS Systems Manager Parameter Store.
   * This is a required parameter.
   */
  public static final Parameter<String> PARAMETER_NAME =
          Parameter.create(REQUIRED);

  /**
   * The single instance of {@code CachedResourceFactory} for requesting
   * parameter values from AWS Systems Manager Parameter Store.
   */
  private static final ResourceFactory<String> INSTANCE =
          CachedResourceFactory.create(new ParameterStoreFactory());

  private ParameterStoreFactory() { }

  /**
   * Returns a singleton instance of {@code ParameterStoreFactory}.
   *
   * @return the singleton factory instance
   */
  public static ResourceFactory<String> getInstance() {
    return INSTANCE;
  }

  @Override
  public Resource<String> request(AwsCredentials awsCredentials, ParameterSet parameterSet) {
    String name = parameterSet.getRequired(PARAMETER_NAME);
    String region = parameterSet.getOptional(REGION);

    SsmClientBuilder builder = SsmClient
            .builder()
            .credentialsProvider(() -> awsCredentials);

    if (region != null) {
      builder.region(Region.of(region));
    }

    try (SsmClient client = builder.build()) {
      GetParameterRequest req = GetParameterRequest.builder()
              .name(name)
              .withDecryption(true)
              .build();
      GetParameterResponse resp = client.getParameter(req);
      String value = resp.parameter().value();
      return Resource.createPermanentResource(value, true);
    }
  }
}