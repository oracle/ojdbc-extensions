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

import oracle.jdbc.provider.factory.ResourceFactory;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;

/**
 * <p>
 * A set of parameters that configure the request for a resource by a
 * {@link oracle.jdbc.provider.factory.ResourceFactory}. Instances of this class
 * may be created using a {@link #builder()}, or using a
 * {@link ParameterSetParser}.
 * </p><p>
 * Instances of {@code ParameterSet} are immutable, as they are intended to be
 * used as a key for cached resource look ups. Assigning a mutable object as the
 * value of a parameter should be avoided when possible, as this creates the
 * potential for cache look ups to break when the object is mutated.
 * </p><p>
 * Instances of {@code ParameterSet} are considered to be equal if both
 * instances contain the same values for the same parameters. The names used to
 * identify parameters may be different. This is to allow different providers,
 * using different parameter names, to share a common cache of resources that
 * are keyed to a {@code ParameterSet}.
 * </p><p>
 * The intent of this class is to provide a common abstraction for different
 * representations of parameters used by different providers. An instance of
 * this class might represent the parameters of a URL query, or the fields of a
 * JSON object, or something else that assigns values to names. Using a
 * {@code ParameterSet} to representation, different providers with different
 * parameter names and different parameter encodings (URL, JSON, etc) may share
 * a common {@link ResourceFactory}.
 * </p>
 */
public interface ParameterSet {

  /**
   * Creates an empty set of parameters.
   *
   * @return An empty set of parameters
   */
  static ParameterSet empty() {
    return new ParameterSetImpl(emptyMap(), emptyMap());
  }

  /**
   * Creates a builder for building a set of parameters.
   *
   * @return A builder for building a set of parameters
   */
  static ParameterSetBuilder builder() {
    return new ParameterSetBuilderImpl();
  }

  /**
   * Returns {@code true} if a {@code parameter} is contained in this set.
   *
   * @param parameter Parameter whose presence in this set is to be tested
   * @return {@code true} if a {@code parameter} is contained in this set
   */
  boolean contains(Parameter<?> parameter);

  /**
   * Returns the value of a {@code parameter}, or {@code null} if the
   * {@code parameter} is not contained in this set
   *
   * @param <T> The type of value that is assigned to the parameter
   * @param parameter Parameter to be retrieved the value from
   * @return The value of a {@code parameter}, or {@code null} if the
   *         {@code parameter} is not contained in this set
   */
  <T> T getOptional(Parameter<T> parameter);

  /**
   * Returns the name of a {@code parameter}, or {@code null} if the
   * {@code parameter} is not contained in this set.
   *
   * @param parameter Parameter to be retrieved the name from
   * @return The name of a {@code parameter}, or {@code null} if the
   *         {@code parameter} is not contained in this set
   */
  String getName(Parameter<?> parameter);

  /**
   * Returns the value of a {@code parameter}, or throws
   * {@code IllegalStateException} if the {@code parameter} is not contained in
   * this set.
   *
   * @param <T> The type of value that is assigned to the parameter
   * @param parameter Parameter to be retrieved the value from
   * @return The value of a {@code parameter}, or throws
   *         {@code IllegalStateException} if the {@code parameter} is not
   *         contained in this set
   */
  default <T> T getRequired(Parameter<T> parameter)
    throws IllegalStateException {

    T value = getOptional(parameter);

    if (value != null)
      return value;

    String name = getName(parameter);
    throw new IllegalStateException(format(
      "No value defined for parameter \"%s\"",
      name != null ? name : parameter.toString()));
  }

  /**
   * <p>
   * Returns a builder that is pre-configured with the same parameter values as
   * this {@code ParameterSet}. The builder returned by this method may be used
   * to create a new {@code ParameterSet} having additional parameter values.
   * </p><p>
   * This {@code ParameterSet}, upon which {@code copyBuilder()} is called, does
   * not change when new parameter values are added to the builder. Instances of
   * {@code ParameterSet} are always unmodifiable.
   * </p>
   * @return A builder pre-configured with the same parameter values as this
   * {@code ParameterSet}. Not null.
   */
  ParameterSetBuilder copyBuilder();

}