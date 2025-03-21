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

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.util.Arrays;

final class ParameterSetParserImpl implements ParameterSetParser {

  /** Parameter value parsers mapped to a case insensitive parameter name */
  private final Map<String, ParameterParser> parameterParsers;

  ParameterSetParserImpl(Builder builder) {
    parameterParsers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    parameterParsers.putAll(builder.parameterParsers);
  }

  @Override
  public ParameterSet parseNamedValues(Map<String, String> namedValues) {
    requireNonNull(namedValues, "namedValues is null");

    ParameterSetBuilder builder = ParameterSet.builder();

    // Make a copy of the keyset containing lower case keys, from which we will
    // remove those present in the namedValues map
    Set<String> missingKeys = parameterParsers
        .keySet().stream().map(String::toLowerCase).collect(Collectors.toSet());

    for (Map.Entry<String, String> namedValue : namedValues.entrySet()) {

      String name = namedValue.getKey();
      ParameterParser parameterParser = parameterParsers.get(name);

      // remove key from missing keys
      missingKeys.remove(name.toLowerCase());

      if (parameterParser == null) {
        throw new IllegalArgumentException(format(
          "Unrecognized parameter name: \"%s\". Valid parameter names are: %s",
          name, Arrays.toString(parameterParsers.keySet().toArray())));
      }

      parameterParser.setValue(builder, namedValue.getValue());
    }

    // Set value null to parameters that were not present in namedValues
    for (String missingKey : missingKeys) {
      parameterParsers.get(missingKey).setValue(builder, null);
    }

    return builder.build();
  }

  @Override
  public ParameterSet parseUrl(CharSequence url) {
    Map<String, String> parameters = UriParameters.parse(url);
    return parseNamedValues(parameters);
  }

  static final class Builder implements ParameterSetParser.Builder {

    /** Parameter parsers that have been added to this builder */
    private final Map<String, ParameterParser> parameterParsers =
        new HashMap<>();

    @Override
    public ParameterSetParser build() {
      return new ParameterSetParserImpl(this);
    }

    @Override
    public Builder addParameter(String name, Parameter<String> parameter) {
      addParameterParser(
          name,
          (value, builder) -> builder.add(name, parameter, value));
      return this;
    }

    @Override
    public Builder addParameter(
            String name, Parameter<String> parameter, String defaultValue) {
      addParameterParser(
          name,
          (value, builder) -> builder.add(name, parameter, 
                              value == null ? defaultValue : value));
      return this;
    }

    @Override
    public <T> Builder addParameter(
      String name, Parameter<T> parameter, Function<String, T> valueParser) {
      addParameterParser(
          name,
          (value, builder) ->
              builder.add(name, parameter, valueParser.apply(value)));
      return this;
    }

    @Override
    public <T> Builder addParameter(
      String name, Parameter<T> parameter, T defaultValue,
      Function<String, T> valueParser) {
      addParameterParser(
          name,
          (value, builder) -> {
            // If the value is null set the default value.
            if (value != null)
              builder.add(name, parameter, valueParser.apply(value));
            else
              builder.add(name, parameter, defaultValue);
          });
      return this;
    }

    @Override
    public Builder addParameter(
      String name, BiConsumer<String, ParameterSetBuilder> valueSetter) {
      addParameterParser(name, valueSetter);
      return this;
    }

    private void addParameterParser(
        String name,
        BiConsumer<String, ParameterSetBuilder> valueSetter) {

      ParameterParser parameterParser =
        new ParameterParser(valueSetter);
      parameterParsers.put(name, parameterParser);
    }

  }

  /**
   * This inner class represents the parameter parsing definition expressed
   * by calling an {@code addParameter} method of the {@link Builder}.
   */
  private static final class ParameterParser {

    /** A consumer that parses and sets a value for a parameter */
    private final BiConsumer<String, ParameterSetBuilder> valueSetter;

    private ParameterParser(
        BiConsumer<String, ParameterSetBuilder> valueSetter) {
      this.valueSetter = valueSetter;
    }

    void setValue(ParameterSetBuilder builder, String value) {
      valueSetter.accept(value, builder);
    }

  }

}
