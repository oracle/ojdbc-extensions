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

package  oracle.jdbc.provider.resource;

import oracle.jdbc.provider.factory.ResourceFactory;
import oracle.jdbc.provider.parameter.ParameterSet;
import oracle.jdbc.provider.parameter.ParameterSetParser;
import oracle.jdbc.spi.OracleResourceProvider;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Super class of {@code OracleResourceProvider} implementations in this
 * project. The super class exposes the name and parameters of a provider
 * implementation.
 */
public abstract class AbstractResourceProvider
  implements OracleResourceProvider {

  /** The system this provider integrates with to provide values */
  private final String system;

  /** The type of values this provider provides */
  private final String valueType;

  /** The set of named parameters recognized by this provider */
  private final ParameterSetParser parameterSetParser;

  private final Collection<ResourceParameter> parameters;

  /**
   * <p>
   * Constructs a provider that provides values from a named {@code system},
   * and of a named {@code valueType}. The {@code providedType} argument
   * specifies that class of object that values are provided as.
   * </p><p>
   * The constructed provider recognizes itself as a provider for
   * "{@code ojdbc-resource:}" URIs that identify the given {@code service} and
   * {@code valueType}:</p><pre>
   * ojdbc-resource:service.valueType[?parameters]
   * </pre>
   *
   * @param system System values are provided from
   * @param valueType Type of values provided
   * @param parameters parameters that are specific to the subclass provider.
   *   Not null.
   */
  protected AbstractResourceProvider(
    String system, String valueType,
    ResourceParameter... parameters) {
    this.system = system;
    this.valueType = valueType;
    this.parameters =
      Collections.unmodifiableList(Arrays.asList(parameters.clone()));

    ParameterSetParser.Builder parserBuilder = ParameterSetParser.builder();
    for (ResourceParameter parameter : parameters) {
      parameter.configureParser(parserBuilder);
    }
    this.parameterSetParser = parserBuilder.build();
  }

  @Override
  public final String getName() {
    return "ojdbc-provider-" + system + "-" + valueType;
  }

  @Override
  public final Collection<? extends Parameter> getParameters() {
    return parameters;
  }

  /**
   * Returns parameters parsed from text values. This method recognizes all
   * parameters passed to the constructor as {@link ResourceParameter} objects.
   *
   * @param parameterValues Parameter values represented as text. Not null.
   * @return Parameter values represented as a {@code ParameterSet}. Not null.
   * @throws IllegalArgumentException If {@code parameterValues} includes an
   *   unrecognized parameter or a value that can not be parsed.
   */
  protected final ParameterSet parseParameterValues(
    Map<Parameter, CharSequence> parameterValues) {

    return parameterSetParser.parseNamedValues(
      parameterValues.entrySet()
        .stream()
        .collect(Collectors.toMap(
          entry -> entry.getKey().name(),
          entry -> entry.getValue().toString())));
  }

  /**
   * Requests a resource from a factory using the given parameterValues. This
   * method implements the common operations of parsing parameterValues,
   * requesting a resource from a factory, and then extracting the content from
   * the resource object. Concrete implementations of AbstractResourceProvider
   * should use this method whenever possible to avoid duplications of the same
   * code pattern.
   *
   * @param factory Factory to request resources from. Not null.
   *
   * @param parameterValues Parameters for the request. Not null.
   *
   * @return The content of the requested resource.
   *
   * @param <T> The type of resource content.
   *
   * @throws IllegalArgumentException If {@code parameterValues} includes an
   *   unrecognized parameter or a value that can not be parsed. Or if the
   *   {@code parameterValues} does not include a required parameter, or does
   *   not represent a valid configuration.
   *
   * @throws IllegalStateException If the request fails to return a resource.
   */
  protected <T> T getResource(
          ResourceFactory<T> factory, Map<Parameter, CharSequence> parameterValues) {

    ParameterSet parameterSet = parseParameterValues(parameterValues);

    return factory
            .request(parameterSet)
            .getContent();
  }

}
