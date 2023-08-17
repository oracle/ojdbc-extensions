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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

final class ParameterSetImpl implements ParameterSet {

  /**
   * The values of each parameter. It is assumed that values in this map have
   * the generic type, {@code T}, of the {@code Parameter<T>} they are mapped
   * to.
   */
  private final Map<Parameter<?>, Object> parameterValues;

  /**
   * The names of each parameter. Names are defined when a parameter is added to
   * a {@link ParameterSetBuilder}.
   */
  private final Map<Parameter<?>, String> parameterNames;

  /**
   * Constructs a set of parameters having the given {@code parameterValues} and
   * {@code parameterNames}.
   *
   * @param parameterValues Parameters mapped to their value. Not null. The Map
   * is copied, not retained.
   * @param parameterNames Parameters mapped to their name. Not null. The Map is
   * copied, not retained.
   */
  ParameterSetImpl(
    Map<Parameter<?>, Object> parameterValues,
    Map<Parameter<?>, String> parameterNames) {

    HashMap<Parameter<?>, Object> parameterValuesCopy =
      new HashMap<>(parameterValues);
    this.parameterValues = Collections.unmodifiableMap(parameterValuesCopy);

    HashMap<Parameter<?>, String> parameterNamesCopy =
      new HashMap<>(parameterNames);
    this.parameterNames = Collections.unmodifiableMap(parameterNamesCopy);
  }

  @Override
  public boolean contains(Parameter<?> parameter) {
    return parameterValues.containsKey(parameter);
  }

  @Override
  public <T> T getOptional(Parameter<T> parameter) {
    @SuppressWarnings("unchecked")
    T value = (T) parameterValues.get(parameter);
    return value;
  }

  @Override
  public String getName(Parameter<?> parameter) {
    return parameterNames.get(parameter);
  }

  @Override
  public ParameterSetBuilder copyBuilder() {
    ParameterSetBuilder builder = ParameterSet.builder();

    for (Map.Entry<Parameter<?>, Object> parameterValue
      : parameterValues.entrySet()) {
      @SuppressWarnings("unchecked")
      Parameter<Object> parameter = (Parameter<Object>) parameterValue.getKey();
      Object value = parameterValue.getValue();
      String parameterName = parameterNames.get(parameter);
      builder.add(parameterName, parameter, value);
    }

    return builder;
  }

  @Override
  public int hashCode() {
    return parameterValues.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ParameterSetImpl
      && ((ParameterSetImpl)other).parameterValues.equals(parameterValues);
  }

  /**
   * Returns a key=value style text representation of this set, with parameter
   * names as keys, and the result of calling {@code toString()} on parameter
   * values as values.
   */
  @Override
  public String toString() {
    return parameterNames.entrySet()
      .stream()
      .map(entry ->
        String.format("%s=%s",
          entry.getValue(),
          entry.getKey().isSensitive()
            ? "[OMITTED]"
            : parameterValues.get(entry.getKey())))
      .collect(Collectors.joining(", "));
  }
}
