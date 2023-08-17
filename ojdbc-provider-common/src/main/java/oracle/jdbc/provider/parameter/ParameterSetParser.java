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

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <p>
 * A parser that creates {@link ParameterSet} objects populated with parameter
 * values that appear a text format. Instances of this class are created by
 * configuring by a {@link Builder}. The {@code addParameter} methods of the
 * builder specify the names of parameters to be parsed, and may specify
 * functions that parse the parameter value from a {@code String} into a more
 * descriptive object type.
 * </p><p>
 * The parser uses case insensitive matching to identify parameter names
 * appearing in the text format.
 * </p><p>
 * This class is intended to support providers that process text representations
 * of parameters, such as the query section of a URL, or the fields of a JSON
 * object.
 * </p>
 */
public interface ParameterSetParser {

  /**
   * Parses a {@code ParameterSet} from a mapping of names to values.
   *
   * @param namedValues Mapping of names to values. Not null.
   * @return A {@code ParameterSet} populated with parameters parsed from the
   *   name=value mappings. Not null.
   * @throws IllegalArgumentException If the query includes an unrecognized
   *   parameter name, or if a parameter value can not be parsed.
   */
  ParameterSet parseNamedValues(Map<String, String> namedValues);

  /**
   * Parses a {@code ParameterSet} from the query section of a URI. The query
   * section is parsed as a sequence of name=value pairs, separated with
   * ampersand characters.
   *
   * @param url URL to parse. Not null.
   * @return A {@code ParameterSet} populated with parameters parsed from the
   *   name=value mappings. Not null.
   * @throws IllegalArgumentException If the query includes an unrecognized
   *   parameter name, or if a parameter value can not be parsed.
   */
  ParameterSet parseUrl(CharSequence url);

  /**
   * @return A builder for configuring a {@link ParameterSetParser}. Not null.
   */
  static Builder builder() {
    return new ParameterSetParserImpl.Builder();
  }

  /**
   * <p>
   * A builder for configuring a {@link ParameterSetParserImpl}. Each call to an
   * {@code addParameter} method defines a parameter that will be parsed. The
   * {@link #addParameter(String, Parameter)} method adds the minimal
   * definition, in which only the name used to identify a {@link Parameter} is
   * specified. Overloaded forms of {@code addParameter} may specify the default
   * value of a parameter, along with a custom function to parse the parameter
   * value.
   * </p>
   */
  interface Builder {

    /**
     * Builds a {@link ParameterSetParserImpl} that recognizes the parameter
     * definitions added to this builder.
     *
     * @return A parser configured by {@code addParameter} calls on this
     *   builder. Not null.
     */
    ParameterSetParser build();

    /**
     * Adds a {@code parameter} that a parser identifies by a given
     * {@code name}, and assigns to a {@code String} value exactly as it appears
     * in text format.
     *
     * @param name Name of the parsed parameter. Not null.
     * @param parameter Parameter identified by the {@code name}. Not null.
     * @return This builder.
     */
    Builder addParameter(
      String name, Parameter<String> parameter);

    /**
     * Adds a {@code parameter} that a parser identifies by a given
     * {@code name}, and assigns to a {@code defaultValue} if the name is not
     * present, or assigns to a {@code String} value exactly as it appears in
     * text format.
     *
     * @param name Name of the parsed parameter. Not null.
     * @param defaultValue Default value of the parameter. Not null.
     * @param parameter Parameter identified by the {@code name}. Not null.
     * @return This builder.
     */
    Builder addParameter(
      String name, Parameter<String> parameter, String defaultValue);

    /**
     * Adds a {@code parameter} that a parser identifies by a given
     * {@code name}, and assigns a value output by a {@code valueParser}. The
     * input to the {@code valueParser} is the value as it appears in text form.
     * If the name passed to this method is "x", and the parsed input includes an
     * assignment such as "x=0", then "0" is input to the {@code valueParser}.
     *
     * @param <T> The type of value that is assigned to the parameter.
     * @param name Name of the parsed parameter. Not null.
     * @param parameter Parameter identified by the {@code name}. Not null.
     * @param valueParser Parses the value of the {@code parameter} from text
     *   input. Not null.
     * @return This builder.
     */
    <T> Builder addParameter(
      String name, Parameter<T> parameter, Function<String, T> valueParser);

    /**
     * Adds a {@code parameter} that a parser identifies by a given
     * {@code name}, and assigns to a {@code defaultValue} if the name is not
     * present, or assigns to a value output by a {@code valueParser} if the
     * name is present. The input to the {@code valueParser} is the value as it
     * appears in text form. If the name passed to this method is "x", and the parsed
     * input includes an assignment such as "x=0", then "0" is input to the
     * {@code valueParser}.
     *
     * @param <T> The type of value that is assigned to the parameter.
     * @param name Name of the parsed parameter. Not null.
     * @param defaultValue Default value of the parameter. Not null.
     * @param parameter Parameter identified by the {@code name}. Not null.
     * @param valueParser Parses the value of the {@code parameter} from text
     *   input. Not null.
     * @return This builder.
     */
    <T> Builder addParameter(
      String name, Parameter<T> parameter, T defaultValue,
      Function<String, T> valueParser);

    /**
     * <p>
     * Adds a parameter that a parser identifies by a given
     * {@code name}, and invokes a {@code valueSetter} function for assigning
     * the value. The inputs to the {@code valueSetter} function are the value
     * as it appears in text form, and a {@link ParameterSetBuilder} that builds
     * the parsed {@link ParameterSet}. If the name passed to this method is "x", and
     * the parsed input includes an assignment such as "x=0", then "0" is input
     * to the {@code valueSetter}.
     * </p><p>
     * This method is designed for cases where a single parameter in text format
     * may map to multiple {@link Parameter} objects. The {@code valueSetter}
     * function can perform multiple calls to set each parameter, as in this
     * example:
     * </p>
     * <pre>{@code
     * builder.addParameter("coordinate", (value, parameterSetBuilder) -> {
     *   // Split "x,y,z" formatted value
     *   String[] xyz = value.split(",");
     *   parameterSetBuilder.add("coordinate", X, xyz[0]);
     *   parameterSetBuilder.add("coordinate", Y, xyz[1]);
     *   parameterSetBuilder.add("coordinate", Z, xyz[2]);
     * });
     * }</pre>
     *
     * @param name Name of the parsed parameter. Not null.
     * @param valueSetter Parses and sets the value of parameter(s) from text
     *        input. Not null.
     * @return This builder.
     */
    Builder addParameter(
      String name, BiConsumer<String, ParameterSetBuilder> valueSetter);

    /**
     * <p>
     * Adds a parameter that a parser identifies by a given
     * {@code name}, and invokes a {@code defaultValueSetter} to assign a value
     * if the name is not present, or invokes a {@code valueSetter} function for
     * assigning the value if the name is present. The input to the
     * {@code defaultValueSetter} function is a {@link ParameterSetBuilder} that
     * builds the parsed {@link ParameterSet}. The input to the
     * {@code valueSetter} is the value as it appears in text form, and a
     * {@link ParameterSetBuilder} that builds the parsed {@link ParameterSet}. If
     * the name passed to this method is "x", and the parsed input includes an
     * assignment such as "x=0", then "0" is input to the {@code valueSetter}.
     * </p><p>
     * This method is designed for cases where a single parameter in text format
     * may map to multiple {@link Parameter} objects. The
     * {@code defaultValueSetter} and {@code valueSetter} functions can perform
     * multiple calls to set each parameter, as in this example:
     * </p>
     * <pre>{@code
     * builder.addParameter(
     *   "coordinate",
     *   (parameterSetBuilder) -> {
     *     // Assign the default value of 0 to X, Y, and Z
     *     parameterSetBuilder.add("coordinate", X, 0);
     *     parameterSetBuilder.add("coordinate", Y, 0);
     *     parameterSetBuilder.add("coordinate", Z, 0);
     *   },
     *   (value, parameterSetBuilder) -> {
     *     // Split "x,y,z" formatted value
     *     String[] xyz = value.split(",");
     *     parameterSetBuilder.add("coordinate", X, xyz[0]);
     *     parameterSetBuilder.add("coordinate", Y, xyz[1]);
     *     parameterSetBuilder.add("coordinate", Z, xyz[2]);
     * });
     * }</pre>
     *
     * @param name Name of the parsed parameter. Not null.
     * @param defaultValueSetter Parses and sets the default value of
     *        parameter(s). Not null.
     * @param valueSetter Parses and sets the value of parameter(s) from text
     *        input. Not null.
     * @return This builder
     */
    Builder addParameter(
      String name,
      Consumer<ParameterSetBuilder> defaultValueSetter,
      BiConsumer<String, ParameterSetBuilder> valueSetter);
  }
}
