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

package  oracle.jdbc.provider.factory;

import oracle.jdbc.provider.parameter.Parameter;
import oracle.jdbc.provider.parameter.ParameterSet;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concrete implementation of {@link ResourceFactory} that may be used for
 * testing. This factory recognizes an {@link #ID} parameter in the
 * {@link ParameterSet} passed to {@link #request(ParameterSet)}. Tests may call
 * {@link ##addResource(String, Resource)} to map an id to a resource. If a
 * mapping exists for an ID, the resource is returned when
 * {@link #request(ParameterSet)} is called. Otherwise, the {@code request}
 * method throws an {@link IllegalStateException} if no value is mapped to an
 * ID.
 */
public class TestResourceFactory<T> implements ResourceFactory<T> {

  public static final Parameter<String> ID = Parameter.create();

  private final Map<String, Resource<T>> resources = new HashMap<>();

  private final AtomicInteger requestCount = new AtomicInteger(0);

  /**
   * Adds a resource that can be requested from this factory.
   * @param id {@link #ID} of the resource
   * @param resource Resource added to this factory.
   * @return The parameter set that requests the added resource.
   */
  public ParameterSet addResource(String id, Resource<T> resource) {
    resources.put(id, resource);

    return ParameterSet.builder()
      .add("id", ID, id)
      .build();
  }

  public void removeResource(String id) {
    resources.remove(id);
  }


  /**
   * Returns the number calls to {@link #request(ParameterSet)} that have
   * occurred on this factory.
   */
  public int getRequestCount() {
    return requestCount.get();
  }

  @Override
  public Resource<T> request(ParameterSet parameterSet) {
    String id = parameterSet.getRequired(ID);

    requestCount.incrementAndGet();

    Resource<T> resource = resources.get(id);

    if (resource == null)
      throw new IllegalStateException("No resource is mapped to ID: " + id);

    return resource;
  }
}
