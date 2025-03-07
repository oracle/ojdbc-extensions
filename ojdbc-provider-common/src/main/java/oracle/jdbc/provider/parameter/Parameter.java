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

import java.util.Collection;

/**
 * <p>
 * A parameter that can be assigned to a value. Instances of
 * {@link ParameterSet} use {@code Parameter} as a key for retrieving values.
 * A {@link ParameterSetParser} maps a name to a
 * {@code Parameter}, and is used by {@link ParameterSetParser} to recognize
 * parameters in "name=value" style text representations.
 *
 * @param <T> The type of value that may be assigned to this parameter
 */
public interface Parameter<T> {

  /**
   * Returns a collection that describes the attributes of this parameter,
   * such as its security sensitivity.
   *
   * @return The attributes of this parameter. Not null. May be empty.
   */
  Collection<? extends Attribute> getAttributes();

  /**
   * @return {@code true} if the value of this parameter is a security
   * sensitive value, or {@code false} if not.
   */
  default boolean isSensitive() {
    return getAttributes()
      .contains(CommonAttribute.SENSITIVE);
  }

  /**
   * @return {@code true} if it is required to configure a value of this
   * parameter, or {@code false} if not.
   */
  default boolean isRequired() {
    return getAttributes()
      .contains(CommonAttribute.REQUIRED);
  }

  /**
   * Checks if the parameter supports fallback to environment variables.
   *
   * @return {@code true} if the parameter can be configured using environment
   * variables, {@code false} otherwise.
   */
  default boolean isEnvAllowed() {
    return getAttributes().contains(CommonAttribute.ALLOW_ENV);
  }

  /**
   * Checks if the parameter supports fallback to system properties.
   *
   * @return {@code true} if the parameter can be configured using system
   * properties, {@code false} otherwise.
   */
  default boolean isSystemPropertyAllowed() {
    return getAttributes().contains(CommonAttribute.ALLOW_SYSTEM_PROPERTY);
  }

  /**
   * Creates a new parameter with the specified attributes. The parameter is
   * security insensitive unless {@link CommonAttribute#SENSITIVE} is specified,
   * and is optional unless {@link CommonAttribute#REQUIRED} is specified.
   *
   * @param attributes of the parameter, if any.
   * @param <T> The type of object that represents values of the parameter.
   * @return A parameter with the given {@code attributes}
   */
  static <T> Parameter<T> create(Attribute... attributes) {
    return new ParameterImpl<>(attributes);
  }

  /** 
   * An attribute of a parameter. An attribute describes the way in which a
   * parameter should be processed. For example, an attribute can indicate
   * that the value of a parameter is security sensitive information. The
   * {@link CommonAttribute} enum defines attributes that are broadly 
   * applicable to all parameters used by all providers. Additional subclasses
   * of {@code Attribute} may be defined for attributes that are applicable
   * within a more specific domain.
   */
  interface Attribute { }

  /** 
   * An enumeration of attributes that are broadly applicable to all parameters
   * of all providers.
   */
  enum CommonAttribute implements Attribute {

    /**
     * Attribute of a parameter that may be configured with security sensitive
     * value, such as a password. If a parameter does not have this attribute,
     * then a value of that parameter is not considered security sensitive.
     */
    SENSITIVE,

    /**
     * Attribute of a parameter that must be configured with a value. If a
     * parameter does not have this attribute, then configuring a value of the
     * parameter is considered optional.
     */
    REQUIRED,

    /**
     * Allows a parameter to retrieve its value from an environment variable
     * as a fallback mechanism if not explicitly set.
     */
    ALLOW_ENV,

    /**
     * Allows a parameter to retrieve its value from a system property
     * as a fallback mechanism if not explicitly set.
     */
    ALLOW_SYSTEM_PROPERTY
  }

}
