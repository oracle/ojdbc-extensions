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

package oracle.jdbc.provider.oson.deser;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers.BooleanDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import oracle.jdbc.provider.oson.OsonParser;

import java.io.IOException;
/**
 * Custom deserializer for Boolean values that supports additional formats for parsing.
 * This class is designed to handle specific input values such as "Y" and "N" and map them
 * to `Boolean.TRUE` and `Boolean.FALSE` respectively. For other values, it delegates
 * deserialization to a wrapped Boolean deserializer.
 *
 * <p>This implementation is especially useful when working with JSON input that uses
 * unconventional representations of Boolean values.</p>
 *
 *
 * @see StdScalarDeserializer
 */
public class OsonBooleanDeserializer extends StdScalarDeserializer<Boolean> {
  /**
   * Singleton instance of the deserializer for direct usage.
   */
  public static final OsonBooleanDeserializer INSTANCE = new OsonBooleanDeserializer();

  /**
   * Wrapped Boolean deserializer instance for handling standard deserialization cases.
   */
  public static final BooleanDeserializer WRAPPER_INSTANCE = new BooleanDeserializer(Boolean.class,null);

  /**
   * Protected constructor to enforce singleton pattern.
   * Initializes the deserializer with the `Boolean` type.
   */
  protected OsonBooleanDeserializer() {
    super(Boolean.class);
  }

  /**
   * Deserializes a JSON token into a Boolean value.
   *
   * <p>If the token is a string and its value matches "Y" (case-insensitive), it returns
   * `Boolean.TRUE`. If the value matches "N" (case-insensitive), it returns `Boolean.FALSE`.
   * For all other cases, the method delegates the deserialization to `WRAPPER_INSTAMCE`.</p>
   *
   * @param p     the JSON parser
   * @param ctxt  the deserialization context
   * @return the deserialized Boolean value
   * @throws IOException        if a low-level I/O problem occurs
   * @throws JacksonException   if there is a problem with deserialization
   */
  @Override
  public Boolean deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
    if (p instanceof OsonParser) {
      switch (p.getCurrentToken()) {
        case VALUE_STRING:
        {
          String str = p.getText();
          if (str.equalsIgnoreCase("Y")) {
            return Boolean.TRUE;
          } else if (str.equalsIgnoreCase("N")) {
            return Boolean.FALSE;
          }
        }
        default:
          return WRAPPER_INSTANCE.deserialize(p, ctxt);
      }
    }
    return WRAPPER_INSTANCE.deserialize(p, ctxt);
  }
}
