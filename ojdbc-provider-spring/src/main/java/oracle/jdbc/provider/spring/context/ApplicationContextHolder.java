/*
 *  Copyright (c) 2026 Oracle and/or its affiliates.
 *
 *  The Universal Permissive License (UPL), Version 1.0
 *
 *  Subject to the condition set forth below, permission is hereby granted to any
 *  person obtaining a copy of this software, associated documentation and/or data
 *  (collectively the "Software"), free of charge and under any and all copyright
 *  rights in the Software, and any and all patent rights owned or freely
 *  licensable by each licensor hereunder covering either (i) the unmodified
 *  Software as contributed to or provided by such licensor, or (ii) the Larger
 *  Works (as defined below), to deal in both
 *
 *  (a) the Software, and
 *  (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 *  one is included with the Software (each a "Larger Work" to which the Software
 *  is contributed by such licensors),
 *
 *  without restriction, including without limitation the rights to copy, create
 *  derivative works of, display, perform, and distribute the Software and make,
 *  use, sell, offer for sale, import, export, have made, and have sold the
 *  Software and the Larger Work(s), and to sublicense the foregoing rights on
 *  either these or other terms.
 *
 *  This license is subject to the following condition:
 *  The above copyright notice and either this complete permission notice or at
 *  a minimum a reference to the UPL must be included in all copies or
 *  substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package oracle.jdbc.provider.spring.context;

import oracle.jdbc.provider.factory.Resource;
import oracle.jdbc.provider.parameter.ParameterSet;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.env.Environment;

/**
 * An ApplicationListener that is registered with Spring Framework to receive
 * events when the application context is refreshed. Providers in the
 * ojdbc-provider-spring module may use the {@link ApplicationContext} to
 * read configuration from a Spring configuration source via
 * {@linkplain ApplicationContext#getEnvironment()}
 */
public final class ApplicationContextHolder
  implements ApplicationListener<ContextRefreshedEvent> {

  /**
   * Application context obtained from the most recent context refresh event.
   * <i>No method calls should occur on this field</i> because it may be
   * written to at any time by another thread calling
   * {@link #onApplicationEvent(ContextRefreshedEvent)}.
   */
  private static volatile ApplicationContext applicationContext;

  /**
   * Public no-args constructor intended to be called by the Spring Framework
   * only
   */
  public ApplicationContextHolder() { }

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    applicationContext = event.getApplicationContext();
  }

  /**
   * <p>
   * Returns the current ApplicationContext, or null if this listener has not
   * received an {@link ContextRefreshedEvent}. The ApplicationContext returned
   * by this method may change over time.
   * </p><p><i>
   * Any {@link ParameterSet} that is
   * configured with values read from the {@link Environment} of this context
   * MUST include the {@link ApplicationContext#getId()} as a parameter. This
   * ensures that a {@link Resource} from one ApplicationContext will not
   * become available in a different ApplicationContext.
   * </i></p>
   * @return The current application context, or null.
   */
  public static ApplicationContext get() {
    return applicationContext;
  }

}
