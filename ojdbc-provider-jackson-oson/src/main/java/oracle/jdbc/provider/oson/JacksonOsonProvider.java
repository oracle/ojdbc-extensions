/*
 ** Copyright (c) 2024 Oracle and/or its affiliates.
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

package oracle.jdbc.provider.oson;

import oracle.jdbc.spi.JsonProvider;
import oracle.jdbc.spi.OsonConverter;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Provider class for integrating Jackson with the Oson library.
 * This class implements the {@link JsonProvider} interface to supply Jackson-based JSON conversion
 * capabilities for Oson's serialization and deserialization processes.
 * <p>
 * It provides the implementation of {@link OsonConverter} that uses Jackson
 * for handling JSON data. The provider ensures that the Jackson-based converter can be accessed
 * and used within the Oson library.
 * </p>
 *
 * @see JsonProvider
 * @see OsonConverter
 * @see JacksonOsonConverter
 */
public class JacksonOsonProvider implements JsonProvider{

  /**
   * The name of the Jackson JSON provider.
   */
  public static final String PROVIDER_NAME = "jackson-json-provider";

  private final Logger logger = Logger.getLogger(JacksonOsonProvider.class.getName());

  /**
   * Default constructor.
   */
  public JacksonOsonProvider () {}

  /**
   * Returns the name of this JSON provider.
   *
   * @return the name of the provider
   */
  @Override
  public String getName() {
    return PROVIDER_NAME;
  }

  /**
   * Provides an instance of {@link OsonConverter} that uses Jackson for JSON processing.
   *
   * @param parameterValues a map of parameters and their values for the converter (can be null)
   * @return an instance of {@link JacksonOsonConverter}
   */
  @Override
  public OsonConverter getOsonConverter(Map<Parameter, CharSequence> parameterValues) {
    logger.fine("JacksonOsonProvider getOsonConverter");
    return new JacksonOsonConverter();
  }

}
