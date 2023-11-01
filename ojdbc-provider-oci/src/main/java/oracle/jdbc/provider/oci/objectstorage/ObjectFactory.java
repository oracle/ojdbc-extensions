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
package oracle.jdbc.provider.oci.objectstorage;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.oci.OciResourceFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.REQUIRED;

/**
 * <p>
 * Factory for requesting Object from the Object Storage service. Objects are
 * represented as {@link InputStream}.
 * </p>
 */
public class ObjectFactory extends OciResourceFactory<InputStream> {
  /** URL Path of an Object **/
  public static final Parameter<String> OBJECT_URL = Parameter.create(REQUIRED);

  private static final ResourceFactory<InputStream> INSTANCE = new ObjectFactory();

  private ObjectFactory() {}

  /**
   * @return a singleton of {@code ObjectFactory}
   */
  public static ResourceFactory<InputStream> getInstance() {
    return INSTANCE;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Requests the content of an Object in {@code InputStream} type from the
   * Object Storage service. A copy of {@code InputStream} is returned.
   * Otherwise, the {@code InputStream} becomes unavailable after the server is
   * closed.
   * </p><p>
   * The {@code parameterSet} is required to include an {@link #OBJECT_URL}.
   * </p>
   * @param authenticationDetails Authentication details configured by the
   * {@code parameterSet}. Not null.
   * @param parameterSet Set of parameters that configure the request. Not null.
   * @return InputStream which represents the content of an Object
   * @throws IllegalArgumentException If the parser cannot find matching strings
   * in the URL Path.
   * @throws IllegalStateException If the {@code InputStream} returned from the
   * Object Storage service cannot be read, or {@code ObjectStorageClient}
   * cannot be acquired.
   */
  @Override
  protected Resource<InputStream> request(
    AbstractAuthenticationDetailsProvider authenticationDetails,
    ParameterSet parameterSet) {
    String urlString = parameterSet.getRequired(OBJECT_URL);

    ObjectUrl objectUrl = new ObjectUrl(urlString);

    try (ObjectStorageClient client =
           ObjectStorageClient.builder().build(authenticationDetails)) {
      client.setRegion(objectUrl.region);

      GetObjectResponse getResponse = client
        .getObject(
          GetObjectRequest
            .builder()
            .namespaceName(objectUrl.namespaceName)
            .bucketName(objectUrl.bucketName)
            .objectName(objectUrl.objectName)
            .build());

      return Resource.createPermanentResource(
        cloneInputStream(getResponse.getInputStream()), false);
    } catch (IOException ioException) {
      throw new IllegalStateException(
        "Failed to read data stream from Object Storage", ioException);
    } catch (Exception e) {
      throw new IllegalStateException(
        "Error occurs while acquiring Object Storage client", e);
    }
  }

  private static class ObjectUrl {
    // The OCI resource name may contain only letters, numbers, dashes and underscores
    private static final String NAME = "([-_\\w]*)";
    private static final Pattern URL_PATTERN = Pattern
      .compile("https://objectstorage\\." + NAME + "\\.oraclecloud\\.com"
          + "\\/n\\/" + NAME
          + "\\/b\\/" + NAME
          + "\\/o\\/(.*)", // parameters to the provider
        Pattern.CASE_INSENSITIVE);
    private static final Pattern NEW_URL_PATTERN = Pattern
      .compile("https://" + NAME +
          "\\.objectstorage\\." + NAME + "\\.oci\\.customer-oci\\.com"
          + "\\/n\\/" + "\\1" // namespace should be the same as 1st NAME
          + "\\/b\\/" + NAME
          + "\\/o\\/(.*)", // parameters to the provider (new)
        Pattern.CASE_INSENSITIVE);
    private Region region;
    private String namespaceName;
    private String bucketName;
    private String objectName;

    ObjectUrl(String urlString) {
      Matcher urlMatcher = URL_PATTERN.matcher(urlString);
      Matcher newUrlMatcher = NEW_URL_PATTERN.matcher(urlString);

      if (urlMatcher.matches()) {
        region = Region.fromRegionId(urlMatcher.group(1));
        namespaceName = urlMatcher.group(2);
        bucketName = urlMatcher.group(3);
        objectName = urlMatcher.group(4);
      } else if (newUrlMatcher.matches()) {
        namespaceName = newUrlMatcher.group(1);
        region = Region.fromRegionId(newUrlMatcher.group(2));
        bucketName = newUrlMatcher.group(3);
        objectName = newUrlMatcher.group(4);
      } else {
        throw new IllegalArgumentException(
          "Fail to parse Object URL: " + urlString);
      }
    }
  }

  /**
   * Returns a cloned {@code InputStream}.
   * @throws IOException if fails to read {@code source}
   */
  private InputStream cloneInputStream(InputStream source) throws IOException {
    ByteArrayOutputStream clonedSource = new ByteArrayOutputStream();
    byte[] buffer = new byte[512];
    int len;
    while ((len = source.read(buffer)) > -1) {
      clonedSource.write(buffer, 0, len);
    }
    clonedSource.flush();

    return new ByteArrayInputStream(clonedSource.toByteArray());
  }
}
