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
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/** Utility methods for processing "name=value" style parameters of a URI */
public final class UriParameters {

  private UriParameters(){}

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
      .map(entry ->
        encodeQueryComponent(entry.getKey())
          + "="
          + encodeQueryComponent(String.valueOf(entry.getValue())))
      .collect(Collectors.joining("&"));
  }

  /**
   * <p>
   * Returns name=value pairs, delimited by {@code &}, from the
   * query of a URI.
   * </p><p>
   * This method first validates the input CharSequence as conforming to the URI
   * format before parsing its query. Use {@link #parseQuery(String)} to
   * parse application/x-www-form-urlencoded input that does not appear within a
   * URI.
   * </p><p>
   * This method will throw an <code>IllegalArgumentException</code> if the
   * query of the URI contains a duplicated key, as in
   * {@code name=A&name=B}. It also uses case-insensitive
   * matching for names. Use
   * {@link #parseMultiValueQuery(String)} to allow duplicate and
   * case-insensitive keys, both of which are allowed according to
   * <a href="https://url.spec.whatwg.org/?utm_source=chatgpt.com#urlencoded-parsing">
   * section 5.1 of the WHATWG URL Standard
   * </a>.
   * </p>
   *
   * @param uri URI to be parsed. May be null.
   *
   * @return Map of name=value pairs. Not null. May be empty if there are no
   * name=value pairs, or the URI is null.
   *
   * @throws IllegalArgumentException If the input is not a valid URI.
   */
  public static Map<String, String> parseUri(CharSequence uri) {

    if (uri == null) {
      return Collections.emptyMap();
    }

    URI parsedUri = toURI(uri);
    return parseQuery(parsedUri.getRawQuery());
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
   * If {@link #parseUri(CharSequence) were to return an empty map because a
   * "//" is missing, it would not be obvious for a human to understand why their
   * parameters are not being recognized.
   * </p>
   *
   * @implNote There is no such thing as an ojdbc-resource URI scheme. That was
   * an old idea that was abandoned for a better idea. The original doc is
   * left here in case someone wants to know why this code is doing what it
   * does.
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
   * <p>
   * Returns name=value pairs parsed from application/x-www-form-urlencoded
   * input. This format commonly appears in the query of a URI, but may also
   * appear elsewhere, such as in the body of an HTTP POST request.
   * </p><p>
   * This method will throw an <code>IllegalArgumentException</code> if the
   * query of the URI contains a duplicated key, as in
   * {@code name=A&name=B}. It also uses case-insensitive
   * matching for names, mean this is a duplicate key:
   * {@code name=A&NAME=B}. Use {@link #parseMultiValueQuery(String)} to allow
   * duplicate and case-insensitive keys, both of which are allowed according to
   * <a href="https://url.spec.whatwg.org/?utm_source=chatgpt.com#urlencoded-parsing">
   * section 5.1 of the WHATWG URL Standard
   * </a>.
   * </p>
   *
   * @param query application/x-www-form-urlencoded input to be parsed. May be
   * null.
   *
   * @return Map of name=value pairs. Not null. May be empty if there are no
   * name=value pairs, or the URI is null.
   *
   * @throws IllegalArgumentException If the input does not conform to the
   * application/x-www-form-urlencoded format.
   */
  public static Map<String, String> parseQuery(String query) {

    if (query == null || query.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, String> values = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    parseQuery(
      query,
      (name, value) -> {

        if (values.containsKey(name)) {
          throw new IllegalArgumentException(
            "Duplicate parameter name: \"" + name + "\"");
        }

        values.put(name, value);
      });

    return values;
  }

  /**
   * <p>
   * Returns name=value pairs parsed from application/x-www-form-urlencoded
   * input. This format commonly appears in the query of a URI, but may also
   * appear elsewhere, such as in the body of an HTTP POST request.
   * </p><p>
   * This method may collect multiple values for the same key into a
   * <code>List</code>, and it uses case-sensitive matching for names. This
   * behavior is allowed according to
   * <a href="https://url.spec.whatwg.org/?utm_source=chatgpt.com#urlencoded-parsing">
   * section 5.1 of the WHATWG URL Standard
   * </a>.
   * </p>
   *
   * @param query application/x-www-form-urlencoded input to be parsed. May be
   * null.
   *
   * @return Map of name=value pairs. Not null. May be empty if there are no
   * name=value pairs, or the URI is null.
   *
   * @throws IllegalArgumentException If the input does not conform to the
   * application/x-www-form-urlencoded format.
   */
  public static Map<String, List<String>> parseMultiValueQuery(String query) {

    if (query == null || query.isEmpty()) {
      return Collections.emptyMap();
    }

    Map<String, List<String>> map = new HashMap<>();

    parseQuery(
      query,
      (name, value) ->
        map.computeIfAbsent(
          name,
          sameName -> new ArrayList<>(1))
        .add(value));

    return map;

  }

  /**
   * <p>
   * Returns name=value pairs parsed from application/x-www-form-urlencoded
   * input. This format commonly appears in the query of a URI, but may also
   * appear elsewhere, such as in the body of an HTTP POST request.
   * </p><p>
   * Each name=value pair is fed into a <code>pairConsumer</code> function,
   * which may be used to store the entries in a collection, or perform
   * validations. Any RuntimeException thrown from the function will get thrown
   * out of this method.
   * </p>
   *
   * @param query application/x-www-form-urlencoded input to be parsed. May be
   * null.
   *
   * @param pairConsumer Consumes name=value pairs. The first argument to this
   * function is the name which appears on left hand side of the '=' operator.
   * The second argument is the value which appears on the right hand side of
   * the '=' operator. Null values are not input to the function. The function
   * is not called if a '=' operator does not appear in a {@code &} delimited
   * entry. The function cannot be null.
   *
   * @throws IllegalArgumentException If the input does not conform to the
   * application/x-www-form-urlencoded format.
   */
  public static void parseQuery(
    String query,
    BiConsumer<String, String> pairConsumer
  ) {

    if (query == null || query.isEmpty()) {
      return;
    }

    for (String nameEqualsValue : query.split("&", -1)) {

      if (nameEqualsValue.isEmpty()) {
        continue;
      }

      int valueSeparatorIndex = nameEqualsValue.indexOf('=');

      if (valueSeparatorIndex < 0) {
        continue;
      }

      String name = decodeQueryComponent(
        nameEqualsValue.substring(0, valueSeparatorIndex));

      String value = decodeQueryComponent(
        nameEqualsValue.substring(valueSeparatorIndex + 1));

      pairConsumer.accept(name, value);
    }
  }

  /**
   * Decodes a single query name or value after structural parsing has already
   * isolated the component from '&' and '=' separators.
   *
   * @param queryComponent Encoded query component.
   * @return Decoded query component.
   */
  private static String decodeQueryComponent(String queryComponent) {
    return URLDecoder.decode(
      queryComponent.replace("+", "%2B"), UTF_8);
  }

  /**
   * Percent-encodes a single query name or value so reserved characters do not
   * alter query structure when serialized into a URI.
   *
   * @param queryComponent Query component to encode.
   * @return Percent-encoded query component.
   */
  private static String encodeQueryComponent(String queryComponent) {
    StringBuilder encoded = new StringBuilder(queryComponent.length());

    for (byte queryByte : queryComponent.getBytes(UTF_8)) {
      int unsignedByte = queryByte & 0xFF;
      if (isUnreserved(unsignedByte))
        encoded.append((char)unsignedByte);
      else {
        encoded.append('%');
        encoded.append(Character.forDigit((unsignedByte >>> 4) & 0xF, 16));
        encoded.append(Character.forDigit(unsignedByte & 0xF, 16));
      }
    }

    return encoded.toString();
  }

  /**
   * Indicates whether a byte value is an RFC 3986 "unreserved" character that
   * may be emitted directly in a query component without percent-encoding.
   * See <a href="https://datatracker.ietf.org/doc/html/rfc3986#section-2.3">
   * RFC 3986 Section 2.3</a>
   */
  private static boolean isUnreserved(int value) {
    return (value >= 'A' && value <= 'Z')
      || (value >= 'a' && value <= 'z')
      || (value >= '0' && value <= '9')
      || value == '-'
      || value == '.'
      || value == '_'
      || value == '~';
  }
}
