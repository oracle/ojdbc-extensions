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
package oracle.jdbc.provider.resource;

import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSetBuilder;
import oracle.jdbc.provider.parameter.ParameterSetParser;
import oracle.jdbc.spi.OracleResourceProvider;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An {@link OracleResourceProvider.Parameter} that can be parsed as a
 * common provider {@link Parameter}.
 */
public final class ResourceParameter
  implements OracleResourceProvider.Parameter {

  /** Name of the parameter. */
  private final String name;

  /** Default value of the parameter, or {@code null} if there is none. */
  private final String defaultValue;

  /**
   * {@code true} if the value of the parameter is security sensitive, or
   * {@code false} if not.
   */
  private final boolean isSensitive;

  /**
   * {@code true} if it is required to configure the parameter, or {@code false}
   * if not.
   */
  private final boolean isRequired;

  /**
   * Configures a {@code ParameterSetBuilder} with a default value, if any.
   */
  private final Consumer<ParameterSetBuilder> defaultValueSetter;

  /**
   * Configures a {@code ParameterSetBuilder} with a value parsed from a
   * {@code String}.
   */
  private final BiConsumer<String, ParameterSetBuilder> parser;

  /**
   * Constructs a new {@link OracleResourceProvider.Parameter} which is mapped
   * to a common {@link Parameter}. The value of the parameter may be any
   * string, and there is no default value.
   *
   * @param name Name of the parameter. Not null.
   * @param providerParameter Common provider parameter that this parameter maps
   * to. Not null.
   */
  public ResourceParameter(String name, Parameter<String> providerParameter) {
    this(name, providerParameter, null);
  }

  /**
   * Constructs a new {@link OracleResourceProvider.Parameter} which is mapped
   * to a common {@link Parameter}. The value of the parameter may be any
   * string, and has a {@code defaultValue}.
   *
   * @param name Name of the parameter. Not null.
   * @param providerParameter Common provider parameter that this parameter maps
   * to. Not null.
   * @param defaultValue Default value of the parameter, or null if there is
   * none.
   */
  public ResourceParameter(
    String name, Parameter<String> providerParameter, String defaultValue) {
    this(name, defaultValue,
      providerParameter.isRequired(),
      providerParameter.isSensitive(),
      defaultValue == null
        ? builder -> { }
        : builder -> builder.add(name, providerParameter, defaultValue),
      (value, builder) -> builder.add(name, providerParameter, value));
  }

  /**
   * Constructs a new {@link OracleResourceProvider.Parameter} which is mapped
   * to a common {@link Parameter}. The value of the parameter is parsed from a
   * string representation, and has a {@code defaultValue}.
   *
   * @param name Name of the parameter. Not null.
   * @param providerParameter Common provider parameter that this parameter maps
   * to. Not null.
   * @param defaultValue Default value of the parameter, or null if there is
   * none.
   * @param parser Parses the value of the parameter. Not null.
   * @param <T> The type of object that represents values of the parameter.
   */
  public <T> ResourceParameter(
    String name, Parameter<T> providerParameter, String defaultValue,
    Function<String, T> parser) {
    this(name, defaultValue,
      providerParameter.isRequired(),
      providerParameter.isSensitive(),
      defaultValue == null
        ? builder -> { }
        : builder ->
             builder.add(name, providerParameter, parser.apply(defaultValue)),
      (value, builder) -> {
        if (value != null) 
          builder.add(name, providerParameter, parser.apply(value));
        else
          if (defaultValue != null) 
            builder.add(name, providerParameter, parser.apply(defaultValue));
          else 
          builder.add(name, providerParameter, null);
      });
  }

  /**
   * Constructs a new {@link OracleResourceProvider.Parameter} which is mapped
   * to a common {@link Parameter}. The value of the parameter is parsed from a
   * string representation, has a default value configured by the
   * {@code defaultValueSetter}, and may be required or sensitive.
   *
   * @param name Name of the parameter. Not null.
   * @param defaultValue Default value of the parameter, or null if there is
   * none.
   * @param isRequired {@code true} if it is required to configure the
   * parameter, or {@code false} if not.
   * @param isSensitive {@code true} if value of the parameter is security
   * sensitive, or {@code false} if not.
   * @param defaultValueSetter Configures the default value of the parameter,
   * if any.
   * @param parser Parses the value of the parameter. Not null.
   */
  public ResourceParameter(
    String name, String defaultValue, boolean isRequired,
    boolean isSensitive,
    Consumer<ParameterSetBuilder> defaultValueSetter,
    BiConsumer<String, ParameterSetBuilder> parser) {
    this.name = name;
    this.defaultValue = defaultValue;
    this.isRequired = isRequired;
    this.isSensitive = isSensitive;
    this.defaultValueSetter = defaultValueSetter;
    this.parser = parser;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public boolean isSensitive() {
    return isSensitive;
  }

  @Override
  public boolean isRequired() {
    return isRequired;
  }

  @Override
  public CharSequence defaultValue() {
    return defaultValue;
  }

  /**
   * Configures a parser to parse this parameter.
   * @param builder Builder to configure. Not null.
   */
  void configureParser(ParameterSetParser.Builder builder) {
    builder.addParameter(
      name,
      defaultValueSetter,
      parser);
  }

}
