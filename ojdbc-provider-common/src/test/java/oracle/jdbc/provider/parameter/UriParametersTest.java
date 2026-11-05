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

import java.util.List;
import java.util.Locale;
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
    String query = "value=/path%26mode%3Dstrict&tag=dev";

    Map<String, String> parseUriResult =
      UriParameters.parseUri("config-provider-name://config?" + query);
    assertEquals("/path&mode=strict", parseUriResult.get("value"));
    assertEquals("dev", parseUriResult.get("tag"));

    Map<String, String> parseQueryResult =
      UriParameters.parseQuery(query);
    assertEquals("/path&mode=strict", parseQueryResult.get("value"));
    assertEquals("dev", parseQueryResult.get("tag"));

    Map<String, List<String>> parseMultiValueQueryResult =
      UriParameters.parseMultiValueQuery(query);
    assertEquals(
      List.of("/path&mode=strict"),
      parseMultiValueQueryResult.get("value"));
    assertEquals(
      List.of("dev"),
      parseMultiValueQueryResult.get("tag"));
  }

  @Test
  void preservesEqualsWithinValues() {
    String query = "secret=abc=def&tag=dev";

    Map<String, String> parseUriResult =
      UriParameters.parseUri("config-provider-name://config?" + query);
    assertEquals("abc=def", parseUriResult.get("secret"));
    assertEquals("dev", parseUriResult.get("tag"));

    Map<String, String> parseQueryResult =
      UriParameters.parseQuery(query);
    assertEquals("abc=def", parseQueryResult.get("secret"));
    assertEquals("dev", parseQueryResult.get("tag"));

    Map<String, List<String>> parseMultiValueQueryResult =
      UriParameters.parseMultiValueQuery(query);
    assertEquals(
      List.of("abc=def"),
      parseMultiValueQueryResult.get("secret"));
    assertEquals(
      List.of("dev"),
      parseMultiValueQueryResult.get("tag"));
  }

  @Test
  void rejectsDuplicateParameterNamesIgnoringCase() {
    String query = "mode=default&MODE=interactive";

    IllegalArgumentException parseUriException = assertThrows(
      IllegalArgumentException.class,
      () -> UriParameters.parseUri(
        "config-provider-name://config?" + query));
    assertEquals(
      "Duplicate parameter name: \"MODE\"", parseUriException.getMessage());

    IllegalArgumentException parseQueryException = assertThrows(
      IllegalArgumentException.class,
      () -> UriParameters.parseQuery(query));
    assertEquals(
      "Duplicate parameter name: \"MODE\"", parseQueryException.getMessage());

    // parseMultiValueQuery uses case-sensitive matching for the names.
    // into a list.
    assertEquals(
      Map.of(
        "mode", List.of("default"),
        "MODE", List.of("interactive")),
      UriParameters.parseMultiValueQuery(query));

    // parseMultiValueQuery collects the values of duplicate names into a List
    assertEquals(
      Map.of("mode", List.of("default", "interactive")),
      UriParameters.parseMultiValueQuery(query.toLowerCase(Locale.ROOT)));
  }
}
