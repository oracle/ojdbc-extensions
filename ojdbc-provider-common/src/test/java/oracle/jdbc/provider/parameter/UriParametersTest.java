/*
 ** Copyright (c) 2026 Oracle and/or its affiliates.
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

package oracle.jdbc.provider.parameter;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies {@link UriParameters} with regression coverage for query parsing
 * edge cases that are security-relevant for provider URL handling.
 */
class UriParametersTest {

  @Test
  void preservesEncodedSeparatorsWithinValues() {
    Map<String, String> values = UriParameters.parse(
      "config-provider-name://config?value=/path%26mode%3Dstrict&tag=dev");

    assertEquals("/path&mode=strict", values.get("value"));
    assertEquals("dev", values.get("tag"));
  }

  @Test
  void preservesEqualsWithinValues() {
    Map<String, String> values = UriParameters.parse(
      "config-provider-name://config?secret=abc=def&tag=dev");

    assertEquals("abc=def", values.get("secret"));
    assertEquals("dev", values.get("tag"));
  }

  @Test
  void rejectsDuplicateParameterNamesIgnoringCase() {
    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> UriParameters.parse(
        "config-provider-name://config?mode=default&MODE=interactive"));

    assertEquals("Duplicate parameter name: \"MODE\"", exception.getMessage());
  }
}
