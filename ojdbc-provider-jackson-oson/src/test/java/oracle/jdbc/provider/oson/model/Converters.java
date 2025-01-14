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

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Converters {
    @Converter(autoApply = true)
    public static class BooleanToIntegerConverter implements AttributeConverter<Boolean, Integer> {

        @Override
        public Integer convertToDatabaseColumn(Boolean attribute) {
            return (attribute != null && attribute) ? 1 : 0;
        }

        @Override
        public Boolean convertToEntityAttribute(Integer dbData) {
            return dbData != null && dbData == 1;
        }
    }

    @Converter(autoApply = true)
    public static class LocalDateToStringConverter implements AttributeConverter<LocalDate, String> {

        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        @Override
        public String convertToDatabaseColumn(LocalDate attribute) {
            return (attribute == null) ? null : attribute.format(FORMATTER);
        }

        @Override
        public LocalDate convertToEntityAttribute(String dbData) {
            return (dbData == null || dbData.isEmpty()) ? null : LocalDate.parse(dbData, FORMATTER);
        }
    }

    @Converter
    public static class ListToStringConverter implements AttributeConverter<List<String>, String> {

        @Override
        public String convertToDatabaseColumn(List<String> attribute) {
            return (attribute == null || attribute.isEmpty()) ? null : String.join(",", attribute);
        }

        @Override
        public List<String> convertToEntityAttribute(String dbData) {
            return (dbData == null || dbData.isEmpty()) ? null : Arrays.asList(dbData.split(","));
        }
    }

    @Converter
    public static class BigDecimalToStringConverter implements AttributeConverter<BigDecimal, String> {

        @Override
        public String convertToDatabaseColumn(BigDecimal attribute) {
            return (attribute == null) ? null : attribute.toPlainString();
        }

        @Override
        public BigDecimal convertToEntityAttribute(String dbData) {
            return (dbData == null || dbData.isEmpty()) ? null : new BigDecimal(dbData);
        }
    }

    @Converter
    public static class DateToTimestampConverter implements AttributeConverter<Date, Timestamp> {

        @Override
        public Timestamp convertToDatabaseColumn(Date attribute) {
            return (attribute == null) ? null : new Timestamp(attribute.getTime());
        }

        @Override
        public Date convertToEntityAttribute(Timestamp dbData) {
            return (dbData == null) ? null : new Date(dbData.getTime());
        }
    }

    @Converter
    public static class LocalDateTimeToTimestampConverter implements AttributeConverter<LocalDateTime, Timestamp> {

        @Override
        public Timestamp convertToDatabaseColumn(LocalDateTime attribute) {
            return (attribute == null) ? null : Timestamp.valueOf(attribute);
        }

        @Override
        public LocalDateTime convertToEntityAttribute(Timestamp dbData) {
            return (dbData == null) ? null : dbData.toLocalDateTime();
        }
    }


    @Converter
    public static class LocalTimeToTimeConverter implements AttributeConverter<LocalTime, Time> {

        @Override
        public Time convertToDatabaseColumn(LocalTime attribute) {
            return (attribute == null) ? null : Time.valueOf(attribute);
        }

        @Override
        public LocalTime convertToEntityAttribute(Time dbData) {
            return (dbData == null) ? null : dbData.toLocalTime();
        }
    }

    @Converter
    public static class EnumToStringConverter implements AttributeConverter<Status, String> {

        @Override
        public String convertToDatabaseColumn(Status attribute) {
            return (attribute == null) ? null : attribute.name();
        }

        @Override
        public Status convertToEntityAttribute(String dbData) {
            return (dbData == null || dbData.isEmpty()) ? null : Status.valueOf(dbData);
        }
    }

    public enum Status {
        ACTIVE,
        INACTIVE,
        PENDING,
        SUSPENDED,
        DELETED;

        public String toDisplayName() {
            switch (this) {
                case ACTIVE:
                    return "Active";
                case INACTIVE:
                    return "Inactive";
                case PENDING:
                    return "Pending Approval";
                case SUSPENDED:
                    return "Suspended";
                case DELETED:
                    return "Deleted";
                default:
                    return this.name();
            }
        }
    }

}
