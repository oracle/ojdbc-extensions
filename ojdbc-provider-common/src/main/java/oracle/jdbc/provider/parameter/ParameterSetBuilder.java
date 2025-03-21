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

/**
 * A builder that builds an instance of {@link ParameterSetImpl}.
 */
public interface ParameterSetBuilder {

  /**
   * Adds a parameter with a given name and value. If the value is null, the 
   * ParameterSet will behave as if no value has been set for the parameter. 
   * Calling this method with a null value allows the ParameterSet to create 
   * error messages that identify the {@code name} of missing parameters.
   *
   * @param <T> The type of value that is assigned to the parameter
   * @param name The parameter name. Not null.
   * @param parameter The parameter that is assigned with the {@code name} and
   *                  {@code value} in this set. Not null.
   * @param value The value, or null if the parameter is not configured with a 
   *              value.
   * @return A reference to this object
   */
  <T> ParameterSetBuilder add(String name, Parameter<T> parameter, T value);

  /**
   * Returns a {@code ParameterSet} populated with all parameter values added
   * to this builder.
   *
   * @return A {@code ParameterSet} populated with all parameter values added
   *         to this builder
   */
  ParameterSet build();
}
