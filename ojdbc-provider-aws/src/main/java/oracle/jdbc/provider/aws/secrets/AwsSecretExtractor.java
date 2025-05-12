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

import oracle.sql.json.OracleJsonException;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for extracting secret values from AWS Secrets Manager secrets.
 */
public class AwsSecretExtractor {

  private static final OracleJsonFactory JSON_FACTORY = new OracleJsonFactory();

  /**
   * Extracts a secret value from a secret string, which may be a plain string
   * or a JSON object.
   *
   * <p><b>Behavior:</b></p>
   * <ul>
   *   <li>If the string is valid JSON and {@code fieldName} is provided:
   *     <ul>
   *       <li>Returns the value for that field if found.</li>
   *       <li>Throws an exception if the field is not found.</li>
   *     </ul>
   *   </li>
   *   <li>If the string is valid JSON and {@code fieldName} is not provided:
   *     <ul>
   *       <li>Returns the sole value if there is exactly one key-value pair.</li>
   *       <li>Throws an exception if multiple keys exist and no field name is specified.</li>
   *     </ul>
   *   </li>
   *   <li>If the string is not valid JSON, returns the original string as-is.</li>
   * </ul>
   *
   * @param secretString the raw secret string from AWS Secrets Manager
   * @param fieldName the key to extract from the JSON object (optional)
   * @return the extracted secret value
   * @throws IllegalStateException if the JSON is valid but ambiguous or the field is missing
   */
  public static String extractSecret(String secretString, String fieldName) {
    try {
      OracleJsonObject jsonObject = JSON_FACTORY.createJsonTextValue(
        new ByteArrayInputStream(secretString.getBytes(StandardCharsets.UTF_8))
      ).asJsonObject();

      if (fieldName != null) {
        if (!jsonObject.containsKey(fieldName)) {
          throw new IllegalStateException("Field '" + fieldName + "' not found in secret JSON.");
        }
        return jsonObject.get(fieldName).asJsonString().getString();
      } else if (jsonObject.size() == 1) {
        return jsonObject.values().iterator().next().asJsonString().getString();
      } else {
        throw new IllegalStateException("FIELD_NAME is required when multiple keys exist in the secret JSON");
      }
    } catch (OracleJsonException e) {
      // Fallback to plain text if not a JSON object
      return secretString;
    }
  }
}
