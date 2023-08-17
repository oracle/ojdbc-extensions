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

package oracle.jdbc.provider.oci.vault;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.secrets.SecretsClient;
import com.oracle.bmc.secrets.model.SecretBundle;
import com.oracle.bmc.secrets.requests.GetSecretBundleRequest;
import oracle.jdbc.provider.cache.CachedResourceFactory;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.oci.OciResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.factory.Resource;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Factory for requesting secrets from the Vault service. Secrets are
 * represented as {@link Secret} objects. Oracle JDBC can use the content of
 * a secret as a database password, or any other security sensitive value.
 * </p>
 */
public final class SecretFactory extends OciResourceFactory<Secret> {

  /** OCID of a secret bundle */
  public static final Parameter<String> OCID = Parameter.create();

  private static final ResourceFactory<Secret> INSTANCE =
    CachedResourceFactory.create(new SecretFactory());

  private SecretFactory() { }

  /**
   * Returns a singleton of {@code SecretFactory}.
   * @return a singleton of {@code SecretFactory}
   */
  public static ResourceFactory<Secret> getInstance() {
    return INSTANCE;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Requests the content of a secret bundle from the Vault service. The
   * {@code parameterSet} is required to include an {@link #OCID}.
   * </p>
   */
  @Override
  protected Resource<Secret> request(
      AbstractAuthenticationDetailsProvider authenticationDetails,
      ParameterSet parameterSet) {

    String ocid = parameterSet.getRequired(OCID);
    parseRegion(ocid);

    SecretBundle secretBundle = requestSecret(authenticationDetails, ocid);

    Secret secret = Secret.fromSecretBundle(secretBundle);
    Date expireTimeDate = secretBundle.getTimeOfExpiry();

    if (expireTimeDate == null) {
      return Resource.createPermanentResource(secret, true);
    }
    else {
      OffsetDateTime expireTime =
          expireTimeDate.toInstant().atOffset(ZoneOffset.UTC);

      return Resource.createExpiringResource(secret, expireTime, true);
    }
  }

  /** Requests a secret from the OCI Vault service. */
  private SecretBundle requestSecret(
      AbstractAuthenticationDetailsProvider authenticationDetails,
      String ocid) {

    try (SecretsClient client =
           SecretsClient.builder()
             .region(parseRegion(ocid))
             .build(authenticationDetails)) {

      GetSecretBundleRequest request =
        GetSecretBundleRequest.builder()
          .secretId(ocid)
          .stage(GetSecretBundleRequest.Stage.Current)
          .build();

      return client.getSecretBundle(request)
        .getSecretBundle();
    }
  }

  /**
   * Returns a {@link Region} which is parsed from the provided {@code ocid}.
   * The format of Oracle Cloud ID (OCID) is documented as follows:
   * <pre>
   *   ocid1.<RESOURCE TYPE>.<REALM>.[REGION][.FUTURE USE].<UNIQUE ID>
   * </pre>
   * @see <a href="https://docs.oracle.com/en-us/iaas/Content/General/Concepts/identifiers.htm">Resource Identifiers</a>
   * @param ocid OCID of the regional resource
   * @return an {@code Region} which is extracted from the {@code ocid}
   */
  private Region parseRegion(String ocid) {
    String regex = "ocid1\\.[^.]+\\.[^.]+\\.([^.]+)\\..+";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(ocid);
    if (matcher.matches()) {
      return Region.fromRegionCode(matcher.group(1));
    }
    throw new IllegalStateException(
      "Fail to parse region from the Secret OCID: " + ocid);
  }

}
