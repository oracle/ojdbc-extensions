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

import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.util.NameTransformer;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hibernate.annotations.JdbcTypeCode;


public class AnnotationIntrospector extends JacksonAnnotationIntrospector {
    @Override
    public PropertyName findNameForSerialization(Annotated a) {
        Column column = _findAnnotation(a, Column.class);
        if (column != null && !column.name().isEmpty()) {
            return PropertyName.construct(column.name());
        }
        return super.findNameForSerialization(a);
    }

    @Override
    public PropertyName findNameForDeserialization(Annotated a) {
        Column column = _findAnnotation(a, Column.class);
        if (column != null && !column.name().isEmpty()) {
            return PropertyName.construct(column.name());
        }
        return super.findNameForDeserialization(a);
    }

    @Override
    public NameTransformer findUnwrappingNameTransformer(AnnotatedMember member) {
        if(member instanceof AnnotatedField) {
            Embeddable embeddable = member.getType().getRawClass().getAnnotation(Embeddable.class);
            if (embeddable != null) {
                JdbcTypeCode jdbcTypeCode = _findAnnotation(member, JdbcTypeCode.class);
                if(jdbcTypeCode == null ) {
                    return NameTransformer.simpleTransformer("", "");
                }
            }
        }
        return super.findUnwrappingNameTransformer(member);
    }


}
