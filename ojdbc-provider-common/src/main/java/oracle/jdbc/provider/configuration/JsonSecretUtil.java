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

package  oracle.jdbc.provider.configuration;

import oracle.sql.json.OracleJsonObject;
import oracle.sql.json.OracleJsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods that implement common functions for implementations of
 * {@link oracle.jdbc.spi.OracleConfigurationJsonSecretProvider} in
 * the {@code ojdbc-azure-json-secret} and {@code ojdbc-oci-json-secret}
 * modules.
 */
public final class JsonSecretUtil {

  private JsonSecretUtil(){}

  /**
   * Returns the name-value pairs of "value" field and "authentication" object
   * within a {@code secretJsonObject}.
   * <pre>
   *   {
   *     ...,
   *     'value' : OCID or URL of the target Secret
   *     'authentication' : {
   *       ... Name-value pairs in here are returned ...
   *     },
   *     ...
   *   }
   * </pre>
   * If an attribute named 'method' is present, the value is keyed to the
   * name 'AUTHENTICATION' in the returned map. This is done to match the
   * "AUTHENTICATION" parameter name used by the {@code ojdbc-azure-common}
   * module.
   * @param secretJsonObject JSON that may contain a 'value' field or/and an
   *                           'authentication' object.
   * @return The fields of the 'authentication' object, or an empty map if
   * all the attributes mentioned above are not present.
   */
  public static Map<String, String> toNamedValues(
    OracleJsonObject secretJsonObject) {

    final String valueFieldName = "value";
    Map<String, String> options = new HashMap<>();

    if (secretJsonObject.containsKey(valueFieldName)) {
      OracleJsonValue secretUri = secretJsonObject.get(valueFieldName);
      if (secretUri.getOracleJsonType()
        .equals(OracleJsonValue.OracleJsonType.STRING)) {
        options.put(valueFieldName, secretUri.asJsonString().getString());
      } else {
        options.put(valueFieldName, secretUri.toString());
      }
    }

    if (secretJsonObject.containsKey("authentication")) {
      OracleJsonObject authenticationJsonObject = secretJsonObject
        .get("authentication")
        .asJsonObject();

      // Rename "method" to "AUTHENTICATION" to match the parameter names
      if (authenticationJsonObject.containsKey("method")) {
        OracleJsonValue authentication = authenticationJsonObject.get("method");
        authenticationJsonObject.remove("method");
        authenticationJsonObject.put("AUTHENTICATION", authentication);
      }

      authenticationJsonObject.forEach((key, value) -> {
        if (value.getOracleJsonType()
          .equals(OracleJsonValue.OracleJsonType.STRING)) {
          options.put(key, value.asJsonString().getString());
        } else {
          options.put(key, value.toString());
        }
      });
    }

    return options;
  }
}
