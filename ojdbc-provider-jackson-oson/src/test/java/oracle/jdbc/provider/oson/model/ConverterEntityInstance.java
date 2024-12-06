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

package oracle.jdbc.provider.oson.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class ConverterEntityInstance {
    public static final List<ConverterEntity> instances = new ArrayList<ConverterEntity>();

    static {
        instances.add(new ConverterEntity(
                1L,
                true,
                LocalDate.of(2024, 1, 15),
                Arrays.asList("tag1", "tag2", "tag3"),
                new BigDecimal("12345.67"),
                LocalDateTime.of(2024, 1, 20, 10, 30),
                new Date(),
                LocalTime.of(14, 15, 30),
                Converters.Status.ACTIVE
        ));

        instances.add(new ConverterEntity(
                2L,
                false,
                LocalDate.of(2023, 12, 25),
                Arrays.asList("tagA", "tagB"),
                new BigDecimal("54321.99"),
                LocalDateTime.of(2024, 1, 1, 9, 0),
                new Date(),
                LocalTime.of(16, 45, 0),
                Converters.Status.PENDING
        ));
        instances.add(new ConverterEntity(
                3L,
                true,
                LocalDate.of(2024, 2, 10),
                Arrays.asList("finance", "tax"),
                new BigDecimal("10000.50"),
                LocalDateTime.of(2024, 2, 15, 11, 45),
                new Date(),
                LocalTime.of(12, 0, 0),
                Converters.Status.INACTIVE
        ));
        instances.add(new ConverterEntity(
                4L,
                true,
                LocalDate.of(2024, 3, 5),
                Arrays.asList("dev", "ops"),
                new BigDecimal("75890.45"),
                LocalDateTime.of(2024, 3, 10, 8, 20),
                new Date(),
                LocalTime.of(9, 30, 0),
                Converters.Status.SUSPENDED));
        instances.add(new ConverterEntity(
                5L,
                false,
                LocalDate.of(2023, 11, 10),
                Arrays.asList("urgent", "review"),
                new BigDecimal("5000.00"),
                LocalDateTime.of(2024, 1, 5, 17, 15),
                new Date(),
                LocalTime.of(20, 0, 0),
                Converters.Status.DELETED
        ));

    }

    public static ConverterEntity getRandomEntity() {
        Random random = new Random();
        int index = random.nextInt(instances.size());
        return instances.get(index);
    }
}
