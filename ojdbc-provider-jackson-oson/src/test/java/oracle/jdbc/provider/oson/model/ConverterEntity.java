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

import jakarta.persistence.Convert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.Objects;

import static oracle.jdbc.provider.oson.model.Converters.*;

public class ConverterEntity {

    private long id;

    @Convert(converter = BooleanToIntegerConverter.class)
    private Boolean active;

    @Convert(converter = LocalDateToStringConverter.class)
    private LocalDate createdDate;

    @Convert(converter = ListToStringConverter.class)
    private List<String> tags;

    @Convert(converter = BigDecimalToStringConverter.class)
    private BigDecimal amount;

    @Convert(converter = DateToTimestampConverter.class)
    private Date transactionDate;

    @Convert(converter = LocalDateTimeToTimestampConverter.class)
    private LocalDateTime lastUpdated;

    @Convert(converter = LocalTimeToTimeConverter.class)
    private LocalTime eventTime;

    @Convert(converter = EnumToStringConverter.class)
    private Status status;


    public ConverterEntity() {}

    public ConverterEntity(long id, Boolean active, LocalDate createdDate,
                           List<String> tags, BigDecimal amount, LocalDateTime lastUpdated,
                           Date transactionDate, LocalTime eventTime, Status status) {
        this.id = id;
        this.active = active;
        this.createdDate = createdDate;
        this.tags = tags;
        this.amount = amount;
        this.lastUpdated = lastUpdated;
        this.transactionDate = transactionDate;
        this.eventTime = eventTime;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public LocalTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalTime eventTime) {
        this.eventTime = eventTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConverterEntity entity = (ConverterEntity) o;
        return id == entity.id && Objects.equals(active, entity.active) && Objects.equals(createdDate, entity.createdDate) && Objects.equals(tags, entity.tags) && Objects.equals(amount, entity.amount) && Objects.equals(transactionDate, entity.transactionDate) && Objects.equals(lastUpdated, entity.lastUpdated) && Objects.equals(eventTime, entity.eventTime) && status == entity.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, active, createdDate, tags, amount, transactionDate, lastUpdated, eventTime, status);
    }
}



