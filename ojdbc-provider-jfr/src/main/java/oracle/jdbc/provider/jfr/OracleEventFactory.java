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
package oracle.jdbc.provider.jfr;

import java.util.ArrayList;
import java.util.List;

import jdk.jfr.AnnotationElement;
import jdk.jfr.Event;
import jdk.jfr.EventFactory;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Category;
import jdk.jfr.ValueDescriptor;
import oracle.jdbc.DatabaseFunction;

/**
 * Factory class for creating Java Flight Recorder events.
 */
public class OracleEventFactory {

  /**
   * Creates an event for a given database operation.
   * @param databaseFunction The database function originating the round trip.
   * @return returns a Java Flight Recorder Event containing the following 
   * fields:
   * <ul>
   *  <li>ConnectionID</li>
   *  <li>DatabaseOperation</li>
   *  <li>OriginalSqlText</li>
   *  <li>ActualSqlText</li>
   *  <li>User</li>
   * </ul>
   */
  public static Event createEvent(DatabaseFunction databaseFunction) {
    String eventName = "oracle.jdbc.provider.jfr.roundtrip." + databaseFunction.toString();
    List<AnnotationElement> eventAnnotations = new ArrayList<AnnotationElement>();
    eventAnnotations
        .add(new AnnotationElement(Name.class, eventName));
    eventAnnotations.add(new AnnotationElement(Label.class, databaseFunction.getDescription()));
    eventAnnotations.add(new AnnotationElement(Category.class,  new String[] { "Oracle JDBC", "Round trips" }));

    List<ValueDescriptor> fields = new ArrayList<ValueDescriptor>();
    fields.add(new ValueDescriptor(String.class, "Connection ID"));
    fields.add(new ValueDescriptor(String.class, "Database operation"));
    fields.add(new ValueDescriptor(String.class, "Original SQL text"));
    fields.add(new ValueDescriptor(String.class, "Actual SQL text"));
    fields.add(new ValueDescriptor(String.class, "Database user"));
    fields.add(new ValueDescriptor(String.class, "Database tenant"));
    fields.add(new ValueDescriptor(String.class, "SQL ID"));

    EventFactory f = EventFactory.create(eventAnnotations, fields);
    return f.newEvent();
  }

  private static String toCamelCase(String value) {
    String[] parts = value.split("\\s+");
    StringBuilder camelCase = new StringBuilder();
    for (String part : parts) {
      if (part != null && part.length() >= 1) {
        camelCase.append(part.substring(0, 1).toUpperCase());
        camelCase.append(part.substring(1).toLowerCase());
      }
    }
    return camelCase.toString();
  }
}
