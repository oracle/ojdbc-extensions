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

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.*;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Pojo to test Jackson annotations.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "full_name", "dateOfBirth", "email", "age", "isActive", "phones", "address", "job",
        "localDate", "localTime", "localDateTime", "zonedDateTime", "offsetDateTime", "offsetTime", "yearMonth", "monthDay", "instant"})
public class AnnonationTest {

    @JsonProperty("full_name")
    private String name;

    @JsonIgnore
    private int age;

    @JsonProperty("is_active")
    private boolean isActive;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd@HH:mm:ss.SSSZ")
    private Date dateOfBirth;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate localDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
    private LocalTime localTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime localDateTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private ZonedDateTime zonedDateTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime offsetDateTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ssXXX")
    private OffsetTime offsetTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM")
    private YearMonth yearMonth;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM-dd")
    private MonthDay monthDay;

    private Instant instant;


    @JsonAlias({ "emailAddress", "email_id" })
    private String email;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Phone> phones;

    @JsonUnwrapped
    private Address address;

    @JsonManagedReference
    private Job job;


    public AnnonationTest() {}

    public AnnonationTest(String name, int age, boolean isActive, Date dateOfBirth, String email, List<Phone> phones, Address address, Job job,
                          LocalDate localDate, LocalTime localTime, LocalDateTime localDateTime, ZonedDateTime zonedDateTime, OffsetDateTime offsetDateTime,
                          OffsetTime offsetTime, YearMonth yearMonth, MonthDay monthDay, Instant instant) {
        this.name = name;
        this.age = age;
        this.isActive = isActive;
        this.dateOfBirth = dateOfBirth;
        this.email = email;
        this.phones = phones;
        this.address = address;
        this.job = job;
        this.localDate = localDate;
        this.localTime = localTime;
        this.zonedDateTime = zonedDateTime;
        this.offsetDateTime = offsetDateTime;
        this.offsetTime = offsetTime;
        this.yearMonth = yearMonth;
        this.monthDay = monthDay;
        this.instant = instant;
        this.localDateTime = localDateTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd@HH:mm:ss.SSSZ")
    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<Phone> getPhones() {
        return phones;
    }

    public void setPhones(List<Phone> phones) {
        this.phones = phones;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public void setLocalDate(LocalDate localDate) {
        this.localDate = localDate;
    }

    public LocalTime getLocalTime() {
        return localTime;
    }

    public void setLocalTime(LocalTime localTime) {
        this.localTime = localTime;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    public ZonedDateTime getZonedDateTime() {
        return zonedDateTime;
    }

    public void setZonedDateTime(ZonedDateTime zonedDateTime) {
        this.zonedDateTime = zonedDateTime;
    }

    public OffsetDateTime getOffsetDateTime() {
        return offsetDateTime;
    }

    public void setOffsetDateTime(OffsetDateTime offsetDateTime) {
        this.offsetDateTime = offsetDateTime;
    }

    public OffsetTime getOffsetTime() {
        return offsetTime;
    }

    public void setOffsetTime(OffsetTime offsetTime) {
        this.offsetTime = offsetTime;
    }

    public YearMonth getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(YearMonth yearMonth) {
        this.yearMonth = yearMonth;
    }

    public MonthDay getMonthDay() {
        return monthDay;
    }

    public void setMonthDay(MonthDay monthDay) {
        this.monthDay = monthDay;
    }

    public Instant getInstant() {
        return instant;
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }

    @Override
    public String toString() {
        return "AnnonationTest{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", isActive=" + isActive +
                ", dateOfBirth=" + dateOfBirth +
                ", localDate=" + localDate +
                ", localTime=" + localTime +
                ", localDateTime=" + localDateTime +
                ", zonedDateTime=" + zonedDateTime +
                ", offsetDateTime=" + offsetDateTime +
                ", offsetTime=" + offsetTime +
                ", yearMonth=" + yearMonth +
                ", monthDay=" + monthDay +
                ", instant=" + instant +
                ", email='" + email + '\'' +
                ", phones=" + phones +
                ", address=" + address +
                ", job=" + job +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnnonationTest that = (AnnonationTest) o;
        return  isActive == that.isActive && Objects.equals(name, that.name)
                && Objects.equals(dateOfBirth, that.dateOfBirth) && Objects.equals(localDate, that.localDate)
                && Objects.equals(localTime, that.localTime) && Objects.equals(localDateTime, that.localDateTime)
                && Objects.equals(offsetTime, that.offsetTime)
                && Objects.equals(yearMonth, that.yearMonth)
                && Objects.equals(monthDay, that.monthDay)
                && Objects.equals(instant, that.instant)
                && Objects.equals(email, that.email)
                && Objects.equals(phones, that.phones);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age, isActive, dateOfBirth, localDate, localTime, localDateTime, zonedDateTime,
                offsetDateTime, offsetTime, yearMonth, monthDay, instant, email, phones, address, job);
    }
}

class Address {

    @JsonProperty("street")
    private String streetName;

    @JsonProperty("city")
    private String city;

    @JsonProperty("country")
    private String country;

    public Address() {}

    public Address(String streetName, String city, String country) {
        this.streetName = streetName;
        this.city = city;
        this.country = country;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}


class Job {

    private String title;
    private double salary;

    @JsonBackReference  // to prevent recursion??
    private AnnonationTest person;

    public Job() {}

    public Job(String title, double salary) {
        this.title = title;
        this.salary = salary;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public AnnonationTest getPerson() {
        return person;
    }

    public void setPerson(AnnonationTest person) {
        this.person = person;
    }
}

