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
package oracle.jdbc.provider.gcp.configuration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import oracle.jdbc.driver.configuration.OracleConfigurationParsableProvider;
import oracle.jdbc.provider.gcp.objectstorage.GcpCloudStorageFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.util.OracleConfigurationCache;

/**
 * A provider for configuration payloads retrieved from GCP Cloud Storage.
 * Payloads are parsed by {@link OracleConfigurationParsableProvider}; JSON is
 * used by default, and other parser types such as Pkl may be selected with the
 * {@code parser} option.
 */
public class GcpCloudStorageConfigurationProvider
    extends OracleConfigurationParsableProvider {

  private static final OracleConfigurationCache CACHE = OracleConfigurationCache.create(100);
  private static final String GS_URI_PREFIX = "gs://";
  private static final String STORAGE_URL_PREFIX = "https://storage.googleapis.com/";

  public static final String PROJECT_PARAMETER = "project";
  public static final String BUCKET_PARAMETER = "bucket";
  public static final String OBJECT_PARAMETER = "object";

  @Override
  public String getType() {
    return "gcpstorage";
  }

  /**
   * {@inheritDoc}
   * <p>
   * Returns the configuration payload stored in GCP Cloud Storage.
   * </p>
   * 
   * @param location semi-colon separated list of key value pairs
   *                 containing the following keys:
   *                 <ul>
   *                 <li>project: the project id (optional)</li>
   *                 <li>bucket: the bucket containing the configuration
   *                 file</li>
   *                 <li>object: the name of the configuration file</li>
   *                 </ul>
   *                 Unknown keys will be ignored.
   *                 Ex: project=myProject;bucket=myBucket;object=myfile.json
   *                 or bucket=myBucket;object=myfile.json
   *                 <p>
   *                 This method also accepts Cloud Storage URIs and public
   *                 object URLs in the following forms:
   *                 </p>
   *                 <ul>
   *                 <li>gs://myBucket/myfile.json</li>
   *                 <li>https://storage.googleapis.com/myBucket/myfile.json</li>
   *                 </ul>
   *                 When using URI-based formats, the provider uses the
   *                 default Google Cloud Storage client configuration resolved
   *                 by the Google Cloud Java SDK.
   * @return configuration payload
   */
  @Override
  public InputStream getInputStream(String location) throws SQLException {
    if (location.startsWith(GS_URI_PREFIX)) {
      return getContent(parseGsUri(location));
    }

    if (location.startsWith(STORAGE_URL_PREFIX)) {
      return getContent(parseStorageUrl(location));
    }

    if (location.startsWith(PROJECT_PARAMETER + "=")
            || location.startsWith(BUCKET_PARAMETER + "=")
            || location.startsWith(OBJECT_PARAMETER + "=")) {
      return getContent(parseNamedValues(location));
    }

    throw new IllegalArgumentException("Unsupported GCP Cloud Storage location: " + location);
  }

  /**
   * Converts parsed location values into a {@link ParameterSet} and requests
   * the corresponding object content from Cloud Storage.
   * <p>
   * This method is the shared bridge between the different supported input
   * formats. Whether the location came from explicit key/value pairs,
   * a {@code gs://...} URI, or a public Cloud Storage URL, once the values have
   * been parsed into a map this method applies the provider parameter parser
   * and delegates the actual fetch to {@link GcpCloudStorageFactory}.
   * </p>
   *
   * @param values Parsed location values. Not null.
   * @return The requested Cloud Storage object content as an input stream.
   */
  private InputStream getContent(Map<String, String> values) {
    ParameterSet parameterSet = GcpConfigurationParameters.getParser().parseNamedValues(values);
    byte[] content = GcpCloudStorageFactory.getInstance().request(parameterSet).getContent();
    return new ByteArrayInputStream(content);
  }

  /**
   * Parses the semi-colon separated location format used by this
   * provider.
   * <p>
   * This format represents the Cloud Storage location as explicit key/value
   * pairs such as:
   * </p>
   * <pre>
   * project=my-project;bucket=my-bucket;object=config.json
   * </pre>
   * <p>
   * The method splits the input into individual segments, validates that each
   * segment has the form {@code key=value}, and returns the parsed values as a
   * map. If any segment is malformed, an {@link IllegalArgumentException} is
   * thrown with a message that points to the invalid segment.
   * </p>
   *
   * @param location The key/value location string. Not null.
   * @return A map containing the parsed location values.
   */
  static Map<String, String> parseNamedValues(String location) {
    Map<String, String> namedValues = new HashMap<>();
    String[] keyValuePairs = location.split(";");
    for (String keyValuePair : keyValuePairs) {
      String[] keyValue = keyValuePair.split("=", 2);
      if (keyValue.length != 2) {
        throw new IllegalArgumentException("Invalid GCP Cloud Storage location segment: " + keyValuePair);
      }
      namedValues.putIfAbsent(keyValue[0], keyValue[1]);
    }
    return namedValues;
  }

  /**
   * Parses a Google Cloud Storage URI of the form {@code gs://bucket/object}.
   * <p>
   * This is the URI format commonly used by Google Cloud tooling. The method
   * extracts the bucket name from the URI authority and the object name from
   * the URI path, then converts both into the internal map structure used by
   * this provider.
   * </p>
   * <p>
   * For example, the URI below:
   * </p>
   * <pre>
   * gs://my-bucket/config/payload.json
   * </pre>
   * <p>
   * is parsed into a map containing {@code bucket=my-bucket} and
   * {@code object=config/payload.json}.
   * </p>
   *
   * @param location A GCS URI beginning with {@code gs://}. Not null.
   * @return A map containing the parsed bucket and object values.
   */
  static Map<String, String> parseGsUri(String location) {
    URI uri = URI.create(location);
    return createUriLocation(uri.getAuthority(), getObjectPath(uri), location);
  }

  /**
   * Parses a public Cloud Storage URL of the form
   * {@code https://storage.googleapis.com/bucket/object}.
   * <p>
   * This method extracts the bucket and object from the URL path and converts
   * them into the same internal structure used for other supported location
   * formats.
   * </p>
   * <p>
   * For example, the URL below:
   * </p>
   * https://storage.googleapis.com/my-bucket/config/payload.json
   * <p>
   * is parsed into a map containing {@code bucket=my-bucket} and
   * {@code object=config/payload.json}.
   * </p>
   *
   * @param location A Cloud Storage URL beginning with
   * {@code https://storage.googleapis.com/}. Not null.
   * @return A map containing the parsed bucket and object values.
   * @throws IllegalArgumentException If the URL does not include both a bucket
   * and an object path.
   */
  static Map<String, String> parseStorageUrl(String location) {
    URI uri = URI.create(location);
    String[] pathParts = uri.getPath().split("/", 3);
    if (pathParts.length < 3) {
      throw new IllegalArgumentException("GCP Cloud Storage URL must include bucket and object: " + location);
    }
    return createUriLocation(pathParts[1], pathParts[2], location);
  }

  /**
   * Returns the object path portion of a parsed GCS URI.
   * <p>
   * A URI such as {@code gs://my-bucket/folder/config.json} stores the object
   * name in the URI path as {@code /folder/config.json}. This method removes
   * the leading slash and validates that an object path is actually present.
   * </p>
   *
   * @param uri Parsed GCS URI. Not null.
   * @return The object path without a leading slash.
   * @throws IllegalArgumentException If the URI does not contain an object
   * path.
   */
  private static String getObjectPath(URI uri) {
    String path = uri.getPath();
    if (path == null || path.length() <= 1) {
      throw new IllegalArgumentException("GCP Cloud Storage URI must include an object path: " + uri);
    }
    return path.substring(1);
  }

  /**
   * Creates the internal map representation used by this provider for
   * URI-based inputs.
   * <p>
   * Both {@code gs://...} URIs and
   * {@code https://storage.googleapis.com/...} URLs ultimately need to be
   * reduced to the same two logical values: a bucket name and an object name.
   * This method validates those values and stores them in a map using the
   * parameter names recognized by the provider.
   * </p>
   *
   * @param bucket Parsed bucket name. Must not be null or blank.
   * @param object Parsed object name. Must not be null or blank.
   * @param originalLocation Original input string, used only to provide a
   * helpful error message if validation fails.
   * @return A map containing the parsed bucket and object values.
   * @throws IllegalArgumentException If the bucket or object is missing.
   */
  private static Map<String, String> createUriLocation(
      String bucket, String object, String originalLocation) {

    if (bucket == null || bucket.isBlank()) {
      throw new IllegalArgumentException("GCP Cloud Storage URI must include a bucket: " + originalLocation);
    }

    if (object == null || object.isBlank()) {
      throw new IllegalArgumentException("GCP Cloud Storage URI must include an object: " + originalLocation);
    }

    Map<String, String> namedValues = new HashMap<>();
    namedValues.put(BUCKET_PARAMETER, bucket);
    namedValues.put(OBJECT_PARAMETER, object);
    return namedValues;
  }

  /**
   * {@inheritDoc}
   * @return cache of this provider which is used to store configuration
   */
  @Override
  public OracleConfigurationCache getCache() {
    return CACHE;
  }
}
