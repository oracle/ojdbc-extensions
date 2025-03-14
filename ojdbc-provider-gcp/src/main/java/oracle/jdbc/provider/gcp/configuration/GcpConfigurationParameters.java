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

import static oracle.jdbc.provider.gcp.secrets.GcpSecretManagerFactory.SECRET_VERSION_NAME;

import oracle.jdbc.provider.gcp.objectstorage.GcpCloudStorageFactory;
import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSetParser;;

public class GcpConfigurationParameters {
  /**
   * <p>
   * A parser that recognizes parameters of key/value pairs received by
   * {@link GcpCloudStorageConfigurationProvider}, and resource names received
   * by {@link GcpSecretManagerConfigurationProvider}.
   * </p>
   */
  private static final ParameterSetParser PARAMETER_SET_PARSER = ParameterSetParser.builder()
      .addParameter("value", SECRET_VERSION_NAME)
      .addParameter("secretVersionName", SECRET_VERSION_NAME)
      .addParameter(
          GcpCloudStorageConfigurationProvider.PROJECT_PARAMETER, GcpCloudStorageFactory.PROJECT)
      .addParameter(
          GcpCloudStorageConfigurationProvider.BUCKET_PARAMETER, GcpCloudStorageFactory.BUCKET)
      .addParameter(
          GcpCloudStorageConfigurationProvider.OBJECT_PARAMETER, GcpCloudStorageFactory.OBJECT)
      .addParameter("type", Parameter.create())
      .build();

  /**
   * @return A parser that recognizes parameters of URIs received by
   *         {@link GcpCloudStorageConfigurationProvider}, and JSON objects
   *         received by
   *         {@link GcpSecretManagerConfigurationProvider}.
   */
  public static ParameterSetParser getParser() {
    return PARAMETER_SET_PARSER;
  }
}
