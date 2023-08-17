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

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * <p>
 * A common abstraction for resources that have been requested from an external
 * system, such as a cloud service. This interface is intended to wrap an
 * arbitrary object of type {@code T}, which represents the specific content of
 * a particular of resource. For example, the SDK for an cloud service might
 * represent the content of a resource using a proprietary object of type
 * {@code Foo}. An instance of {@code Resource<Foo>} would then be used to wrap
 * this content.
 * </p><p>
 * This interface is intended to promote code reuse when a resource needs to be
 * processed in someway. Rather than couple a implementation to an object type
 * that is proprietary to one SDK, a common implementation can consume the
 * {@code Resource} interface.
 * </p>
 * @param <T> Type used to represent the specific content of a particular
 * resource.
 */
public interface Resource<T> {

  /**
   * @return An object representation of the content for a specific type of
   * resource. Not null.
   */
  T getContent();

  /**
   * Returns a boolean indicating if this resource contains a security sensitive
   * value, such as a password. This method should be called before outputting
   * the value of the resource in log messages or error messages.
   *
   * @return {@code true} if this resource contains a security sensitive value,
   *    or {@code false} if not.
   */
  boolean isSensitive();

  /**
   * Returns a boolean indicating if this resource has become invalid. This
   * method should be called before reusing a cached value, as resources may
   * expire after some time, or upon some external event.
   * @return {@code true} if this resource is still valid, or {@code false} if
   * not.
   */
  boolean isValid();

  /**
   * Creates a resource that wraps the given {@code content}, and becomes
   * invalid at a specified {@code expireTime}.
   *
   * @param content Object representation of the content for a particular
   * type of resource. Not null.
   * @param expireTime The time at which the resource expires. Not null.
   * @param isSensitive {@code true} if the content contains security sensitive
   * information, or {@code false} if not.
   * @return A resource that wraps the {@code content}. Not null.
   * @param <T> Specific type of object used to represent the content of a
   * resource.
   */
  static <T> Resource<T> createExpiringResource(
      T content, OffsetDateTime expireTime, boolean isSensitive) {
    Objects.requireNonNull(content, "value is null");
    Objects.requireNonNull(expireTime, "expireTime is null");

    return new ExpiringResource<>(content, expireTime, isSensitive);
  }

  /**
   * Creates a resource that wraps a specific {@code value}, and never becomes
   * invalid.
   *
   * @param value Specific object representation of this resource. Not null.
   * @param isSensitive {@code true} if the value contains security sensitive
   * information, or {@code false} if not.
   * @return A resource that wraps the {@code value}. Not null.
   * @param <T> Specific type of object used to represent the content of a
   * resource.
   */
  static <T> Resource<T> createPermanentResource(T value, boolean isSensitive) {
    Objects.requireNonNull(value, "value is null");

    return new PermanentResource<>(value, isSensitive);
  }

}
