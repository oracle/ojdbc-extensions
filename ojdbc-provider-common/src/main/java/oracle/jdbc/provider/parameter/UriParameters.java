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

package  oracle.jdbc.provider.parameter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Utility methods for processing "name=value" style parameters of a URI */
public final class UriParameters {

  private UriParameters(){}

  /**
   * Returns name=value pairs parsed from a URI.
   *
   * @param uri URI to be parsed
   * @return name=value pairs expressed as {@code name=value[&name=value...]}
   */
  public static Map<String, String> parse(CharSequence uri) {
    if (uri == null)
      return Collections.emptyMap();

    return parseQuery(toURI(uri));
  }

  /**
   * Returns a URI query in the form of {@code name=value[&name=value...]} for
   * the given {@code namedValues}.
   * <em>The query is returned without the leading "?"</em>
   *
   * @param namedValues name=value pairs to be added to this URI query
   * @return A String representing the URI query in the form of
   *         {@code name=value[&name=value...]} for the given
   *         {@code namedValues}
   */
  public static String toString(Map<String, String> namedValues) {
    if (namedValues == null)
      return "";

    return namedValues.entrySet().stream()
      .map(entry -> entry.getKey() + "=" + entry.getValue())
      .collect(Collectors.joining("&"));
  }

  /**
   * <p>
   * Converts an ojdbc-resource URI to a URI object.
   * </p><p>
   * This method handles the case where an "opaque" URI is given:
   * <pre>
   *   ojdbc-resource:system.value?x=0
   * </pre>
   * This method will convert the opaque URI to a "hierarchical" URI by
   * prepending a "//" to the scheme specific part:
   * <pre>
   *   ojdbc-resource://system.value?x=0
   * </pre>
   * This is done to usability, as humans do not typically retain the rules of
   * a URI syntax parser in their brain. A human may omit the "//" for brevity.
   * If {@link #parse(CharSequence) were to return an empty map because a "//"
   * is missing, it would not be obvious for a human to understand why their
   * parameters are not being recognized.
   * </p>
   */
  private static URI toURI(CharSequence uriCharSequence) {
    URI uri = URI.create(uriCharSequence.toString());

    if (!uri.isOpaque())
      return uri;

    try {
      return new URI(
        uri.getScheme(),
        "//" + uri.getSchemeSpecificPart(),
        uri.getFragment());
    }
    catch (URISyntaxException uriSyntaxException) {
      throw new IllegalArgumentException(uriSyntaxException);
    }
  }

  /**
   * Returns name=value pairs expressed as "name=value[&name=value...]",
   * parsed from the query section of a given {@code uri}.
   */
  private static Map<String, String> parseQuery(URI uri) {
    Map<String, String> values = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    String query = uri.getQuery();

    if (query == null)
      return Collections.emptyMap();

    for (String nameEqualsValue : query.split("&")) {
      String[] nameValue = nameEqualsValue.split("=");

      final int expectedLength = 2;

      // Not storing a value if query contains "name=" without a value
      if (nameValue.length < expectedLength)
        continue;

      values.put(nameValue[0], nameValue[1]);
    }

    return values;
  }
}
