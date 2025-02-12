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
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The Employee class represents an employee with various personal and employment details.
 * It includes fields that covers some of the oracle types.
 */
public class Employee {

  /**
   * Employee ID as an integer.
   */
  private int employeeId;

  /**
   * Logout Time.
   */
  private long logoutTime;

  /**
   * Flag indicating whether the employee is currently active.
   */
  private boolean active;

  /**
   * Represents the department ID.
   */
  private short deptId;

  /**
   * Represents the employee code.
   */
  private long employeeCode;

  /**
   * Represents the grade.
   */
  private byte grade;

  /**
   * Represents the bonus.
   */
  private float bonus;

  /**
   * Represents the previous salary.
   */
  private double prevSalary;

  /**
   * Represents the gender (M/F)
   */
  private char gender;

  /**
   * Employee's salary as a BigDecimal.
   */
  private BigDecimal salary;

  /**
   * A large number representing additional information (e.g., unique identifier).
   */
  private BigInteger largeNumber;

  /**
   * Employee's first name.
   */
  private String firstName;

  /**
   * Employee's middle initial.
   */
  private String middleInitial;

  /**
   * Employee's last name.
   */
  private String lastName;

  /**
   * The date the employee was hired.
   */
  private Date hireDate;

  /**
   * The date the employee was hired in java.util.Date.
   */
  private java.util.Date hireUtilDate;

  /**
   * The LocalDate the employee was hired.
   */
  private LocalDate localHireDate;

  /**
   * The login instant
   */
  private Instant loginInstant;

  /**
   * Login Local Time.
   */
  private LocalTime loginLocalTime;

  /**
   * SalaryDay Month
   */
  private MonthDay salaryMonthDay;

  /**
   * Incremnent year month
   */
  private YearMonth incrementYearmonth;

  /**
   * The time the employee starts work.
   */
  private Time startTime;

  /**
   * The last time the employee's record was updated.
   */
  private Timestamp lastUpdated;

  /**
   * LocalDateTime representing when the employee started working.
   */
  private LocalDateTime localDateTime;

  /**
   * OffsetDateTime capturing the date and time with a timezone offset.
   */
  private OffsetDateTime offsetDateTime;

  /**
   * The year the employee joined the company.
   */
  private Year yearJoined;

  /**
   * The duration of the employee's employment.
   */
  private Duration durationOfEmployment;

  /**
   * The employee's profile picture as a byte array.
   */
  private byte[] picture;

  /**
   * Employee's resume as a String.
   */
  private String resume;

  /**
   * Raw data associated with the employee (e.g., binary data).
   */
  private byte[] rawData;

  /**
   * A list of Phone objects associated with the employee's contact information.
   */
  private List<Phone> phones;

  /**
   * Employee's vacation Zoned DateTime;
   */
  private ZonedDateTime vacationZonedDateTime;

  /**
   * Employee's total working period
   */
  private Period totalPeriod;

  /**
   * Boxed primitive types
   */
  private Integer idBoxed;
  private Boolean isActiveBoxed;
  private Byte rankBoxed;
  private Character genderBoxed;
  private Short ageBoxed;
  private Long salaryBoxed;
  private Float performanceScoreBoxed;
  private Double bonusBoxed;

  /**
   * Default constructor for the Employee class.
   */
  public Employee() {}

  /**
   * Parameterized constructor to initialize an Employee object with all fields.
   *
   * @param employeeId the employee's ID
   * @param salary the employee's salary
   * @param largeNumber a large number representing an identifier
   * @param firstName the employee's first name
   * @param middleInitial the employee's middle initial
   * @param lastName the employee's last name
   * @param hireDate the date the employee was hired
   * @param startTime the time the employee starts work
   * @param lastUpdated the last updated timestamp for the employee's record
   * @param localDateTime the LocalDateTime of employee's joining
   * @param offsetDateTime the OffsetDateTime for employee's joining with timezone
   * @param yearJoined the year the employee joined
   * @param durationOfEmployment the duration of employee's employment
   * @param picture the employee's profile picture
   * @param resume the employee's resume
   * @param active whether the employee is currently active
   * @param rawData raw binary data associated with the employee
   * @param phones the list of phone contact information for the employee
   */
  public Employee(int employeeId, BigDecimal salary, BigInteger largeNumber, String firstName, String middleInitial,
                  String lastName, Date hireDate, Time startTime, Timestamp lastUpdated, LocalDateTime localDateTime,
                  OffsetDateTime offsetDateTime, Year yearJoined, Duration durationOfEmployment, byte[] picture,
                  String resume, boolean active, byte[] rawData, List<Phone> phones, LocalDate localHireDate,
                  Instant loginInstant, LocalTime loginLocalTime, MonthDay salaryMonthDay, YearMonth incrementYearmonth,
                  long logoutTime, ZonedDateTime vacationZonedDateTime, short deptId, long employeeCode, byte grade,
                  float bonus, double prevSalary, char gender, Period totalPeriod, Integer idBoxed, Boolean isActiveBoxed,
                  Byte rankBoxed, Character genderBoxed, Short ageBoxed, Long salaryBoxed, Float performanceScoreBoxed,
                  Double bonusBoxed, java.util.Date hireUtilDate) {
    this.employeeId = employeeId;
    this.salary = salary;
    this.largeNumber = largeNumber;
    this.firstName = firstName;
    this.middleInitial = middleInitial;
    this.lastName = lastName;
    this.hireDate = hireDate;
    this.startTime = startTime;
    this.lastUpdated = lastUpdated;
    this.localDateTime = localDateTime;
    this.offsetDateTime = offsetDateTime;
    this.yearJoined = yearJoined;
    this.durationOfEmployment = durationOfEmployment;
    this.picture = picture;
    this.resume = resume;
    this.active = active;
    this.rawData = rawData;
    this.phones = phones;
    this.localHireDate = localHireDate;
    this.loginInstant = loginInstant;
    this.loginLocalTime = loginLocalTime;
    this.salaryMonthDay = salaryMonthDay;
    this.incrementYearmonth = incrementYearmonth;
    this.logoutTime = logoutTime;
    this.vacationZonedDateTime = vacationZonedDateTime;
    this.deptId = deptId;
    this.employeeCode = employeeCode;
    this.grade = grade;
    this.bonus = bonus;
    this.prevSalary = prevSalary;
    this.gender = gender;
    this.totalPeriod = totalPeriod;
    this.idBoxed = idBoxed;
    this.isActiveBoxed = isActiveBoxed;
    this.rankBoxed = rankBoxed;
    this.genderBoxed = genderBoxed;
    this.ageBoxed = ageBoxed;
    this.salaryBoxed = salaryBoxed;
    this.performanceScoreBoxed = performanceScoreBoxed;
    this.bonusBoxed = bonusBoxed;
    this.hireUtilDate = hireUtilDate;
  }

// Getters and Setters

  /**
   * Get Login Instant.
   * @return loginInstant
   */
  public Instant getLoginInstant() {
    return loginInstant;
  }

  /**
   * Sets the login instant of the employee.
   *
   * @param loginInstant the {@link Instant} representing the exact moment the employee logged in.
   */
  public void setLoginInstant(Instant loginInstant) {
    this.loginInstant = loginInstant;
  }

  /**
   * Gets the login time of the employee.
   *
   * @return the {@link LocalTime} representing the time of day when the employee logged in.
   */
  public LocalTime getLoginLocalTime() {
    return loginLocalTime;
  }

  /**
   * Sets the login time of the employee.
   *
   * @param loginLocalTime the {@link LocalTime} representing the time of day when the employee logged in.
   */
  public void setLoginLocalTime(LocalTime loginLocalTime) {
    this.loginLocalTime = loginLocalTime;
  }

  /**
   * Gets the salary month and day for the employee.
   *
   * @return the {@link MonthDay} representing the month and day when the employee receives salary.
   */
  public MonthDay getSalaryMonthDay() {
    return salaryMonthDay;
  }

  /**
   * Sets the salary month and day for the employee.
   *
   * @param salaryMonthDay the {@link MonthDay} representing the month and day when the employee receives salary.
   */
  public void setSalaryMonthDay(MonthDay salaryMonthDay) {
    this.salaryMonthDay = salaryMonthDay;
  }

  /**
   * Gets the increment year and month for the employee.
   *
   * @return the {@link YearMonth} representing the year and month when the employee receives an increment.
   */
  public YearMonth getIncrementYearmonth() {
    return incrementYearmonth;
  }

  /**
   * Sets the increment year and month for the employee.
   *
   * @param incrementYearmonth the {@link YearMonth} representing the year and month when the employee receives an
   * increment
   */
  public void setIncrementYearmonth(YearMonth incrementYearmonth) {
    this.incrementYearmonth = incrementYearmonth;
  }

  /**
   * Gets the logout time of the employee.
   *
   * @return the {@code long} value representing the time of day in milliseconds since the epoch when the employee
   * logged out
   */
  public long getLogoutTime() {
    return logoutTime;
  }

  /**
   * Sets the logout time of the employee.
   *
   * @param logoutTime the {@code long} value representing the time of day in milliseconds since the epoch when
   * the employee logged out
   */
  public void setLogoutTime(long logoutTime) {
    this.logoutTime = logoutTime;
  }

  /**
   * Gets the employee ID.
   *
   * @return the employee ID
   */
  public int getEmployeeId() {
    return employeeId;
  }


  /**
   * Gets the local hire date of the employee.
   *
   * @return the {@link LocalDate} representing the date when the employee was hired
   */
  public LocalDate getLocalHireDate() {
    return localHireDate;
  }

  /**
   * Sets the local hire date of the employee.
   *
   * @param localHireDate the {@link LocalDate} representing the date when the employee was hired
   */
  public void setLocalHireDate(LocalDate localHireDate) {
    this.localHireDate = localHireDate;
  }

  /**
   * Sets the employee ID.
   *
   * @param employeeId the new employee ID
   */
  public void setEmployeeId(int employeeId) {
    this.employeeId = employeeId;
  }

  /**
   * Gets the employee's salary.
   *
   * @return the salary
   */
  public BigDecimal getSalary() {
    return salary;
  }

  /**
   * Sets the employee's salary.
   *
   * @param salary the new salary
   */
  public void setSalary(BigDecimal salary) {
    this.salary = salary;
  }

  /**
   * Gets the large number (e.g., an identifier).
   *
   * @return the large number
   */
  public BigInteger getLargeNumber() {
    return largeNumber;
  }

  /**
   * Get the First Name.
   * @return
   */
  public String getFirstName() {
    return firstName;
  }

  /**
   * Set First Name
   * @param firstName
   */
  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  /**
   * Gets the employee's middle initial.
   *
   * @return the middle initial of the employee.
   */
  public String getMiddleInitial() {
    return middleInitial;
  }

  /**
   * Sets the employee's middle initial.
   *
   * @param middleInitial the new middle initial of the employee.
   */
  public void setMiddleInitial(String middleInitial) {
    this.middleInitial = middleInitial;
  }

  /**
   * Gets the employee's last name.
   *
   * @return the last name of the employee.
   */
  public String getLastName() {
    return lastName;
  }

  /**
   * Sets the employee's last name.
   *
   * @param lastName the new last name of the employee.
   */
  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  /**
   * Gets the employee's start time.
   *
   * @return the start time of the employee.
   */
  public Time getStartTime() {
    return startTime;
  }

  /**
   * Sets the employee's start time.
   *
   * @param startTime the new start time of the employee.
   */
  public void setStartTime(Time startTime) {
    this.startTime = startTime;
  }

  /**
   * Gets the date the employee was hired.
   *
   * @return the hire date of the employee.
   */
  public Date getHireDate() {
    return hireDate;
  }

  /**
   * Sets the date the employee was hired.
   *
   * @param hireDate the new hire date of the employee.
   */
  public void setHireDate(Date hireDate) {
    this.hireDate = hireDate;
  }

  /**
   * Gets the timestamp of the last update to the employee's record.
   *
   * @return the last updated timestamp of the employee's record.
   */
  public Timestamp getLastUpdated() {
    return lastUpdated;
  }

  /**
   * Sets the timestamp of the last update to the employee's record.
   *
   * @param lastUpdated the new last updated timestamp.
   */
  public void setLastUpdated(Timestamp lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  /**
   * Gets the local date and time of when the employee joined.
   *
   * @return the LocalDateTime of the employee's joining.
   */
  public LocalDateTime getLocalDateTime() {
    return localDateTime;
  }

  /**
   * Sets the local date and time of when the employee joined.
   *
   * @param localDateTime the new LocalDateTime of the employee's joining.
   */
  public void setLocalDateTime(LocalDateTime localDateTime) {
    this.localDateTime = localDateTime;
  }

  /**
   * Gets the date and time with the offset from UTC when the employee joined.
   *
   * @return the OffsetDateTime of the employee's joining.
   */
  public OffsetDateTime getOffsetDateTime() {
    return offsetDateTime;
  }

  /**
   * Sets the date and time with the offset from UTC when the employee joined.
   *
   * @param offsetDateTime the new OffsetDateTime of the employee's joining.
   */
  public void setOffsetDateTime(OffsetDateTime offsetDateTime) {
    this.offsetDateTime = offsetDateTime;
  }

  /**
   * Gets the year the employee joined the company.
   *
   * @return the year the employee joined the company.
   */
  public Year getYearJoined() {
    return yearJoined;
  }

  /**
   * Sets the year the employee joined the company.
   *
   * @param yearJoined the new year the employee joined the company.
   */
  public void setYearJoined(Year yearJoined) {
    this.yearJoined = yearJoined;
  }

  /**
   * Gets the duration of the employee's employment.
   *
   * @return the duration of the employee's employment.
   */
  public Duration getDurationOfEmployment() {
    return durationOfEmployment;
  }

  /**
   * Sets the duration of the employee's employment.
   *
   * @param durationOfEmployment the new duration of employment.
   */
  public void setDurationOfEmployment(Duration durationOfEmployment) {
    this.durationOfEmployment = durationOfEmployment;
  }

  /**
   * Gets the employee's profile picture.
   *
   * @return the profile picture as a byte array.
   */
  public byte[] getPicture() {
    return picture;
  }

  /**
   * Sets the employee's profile picture.
   *
   * @param picture the new profile picture as a byte array.
   */
  public void setPicture(byte[] picture) {
    this.picture = picture;
  }

  /**
   * Gets the employee's resume.
   *
   * @return the resume of the employee.
   */
  public String getResume() {
    return resume;
  }

  /**
   * Sets the employee's resume.
   *
   * @param resume the new resume of the employee.
   */
  public void setResume(String resume) {
    this.resume = resume;
  }

  /**
   * Gets the employee's active status.
   *
   * @return {@code true} if the employee is active; {@code false} otherwise.
   */
  public boolean isActive() {
    return active;
  }

  /**
   * Sets the employee's active status.
   *
   * @param active the new active status.
   */
  public void setActive(boolean active) {
    this.active = active;
  }

  /**
   * Gets the raw data associated with the employee.
   *
   * @return the raw data as a byte array.
   */
  public byte[] getRawData() {
    return rawData;
  }

  /**
   * Sets the raw data associated with the employee.
   *
   * @param rawData the new raw data as a byte array.
   */
  public void setRawData(byte[] rawData) {
    this.rawData = rawData;
  }

  /**
   * Gets the list of phone numbers associated with the employee.
   *
   * @return the list of phone numbers.
   */
  public List<Phone> getPhones() {
    return phones;
  }

  /**
   * Sets the list of phone numbers associated with the employee.
   *
   * @param phones the new list of phone numbers.
   */
  public void setPhones(List<Phone> phones) {
    this.phones = phones;
  }


  /**
   * Sets the large number.
   *
   * @param largeNumber the new large number
   */
  public void setLargeNumber(BigInteger largeNumber) {
    this.largeNumber = largeNumber;
  }

  /**
   * Gets the vacation start date and time of the employee with timezone information.
   *
   * @return the {@link ZonedDateTime} representing the date, time, and time zone
   * when the employee's vacation starts
   */
  public ZonedDateTime getVacationZonedDateTime() {
    return vacationZonedDateTime;
  }

  /**
   * Sets the vacation start date and time of the employee with timezone information.
   *
   * @param vacationZonedDateTime the {@link ZonedDateTime} representing the date,
   * time, and time zone when the employee's vacation starts
   */
  public void setVacationZonedDateTime(ZonedDateTime vacationZonedDateTime) {
    this.vacationZonedDateTime = vacationZonedDateTime;
  }

  /**
   * Returns the department ID.
   *
   * @return the department ID.
   */
  public short getDeptId() {
    return deptId;
  }

  /**
   * Sets the department ID.
   *
   * @param deptId the department ID to set.
   */
  public void setDeptId(short deptId) {
    this.deptId = deptId;
  }

  /**
   * Returns the employee code.
   *
   * @return the employee code.
   */
  public long getEmployeeCode() {
    return employeeCode;
  }

  /**
   * Sets the employee code.
   *
   * @param employeeCode the employee code to set.
   */
  public void setEmployeeCode(long employeeCode) {
    this.employeeCode = employeeCode;
  }

  /**
   * Returns the grade.
   *
   * @return the grade.
   */
  public byte getGrade() {
    return grade;
  }

  /**
   * Sets the grade.
   *
   * @param grade the grade to set.
   */
  public void setGrade(byte grade) {
    this.grade = grade;
  }


  /**
   * Returns the bonus.
   *
   * @return the bonus.
   */
  public float getBonus() {
    return bonus;
  }

  /**
   * Sets the bonus.
   *
   * @param bonus the bonus to set.
   */
  public void setBonus(float bonus) {
    this.bonus = bonus;
  }
  /**
   * Returns the gender.
   *
   * @return the gender.
   */
  public char getGender() {
    return gender;
  }

  /**
   * Sets the gender.
   *
   * @param gender the gender to set.
   */
  public void setGender(char gender) {
    this.gender = gender;
  }

  /**
   * Returns the previous salary.
   *
   * @return the previous salary.
   */
  public double getPrevSalary() {
    return prevSalary;
  }

  /**
   * Sets the previous salary.
   *
   * @param prevSalary the previous salary to set.
   */
  public void setPrevSalary(double prevSalary) {
    this.prevSalary = prevSalary;
  }

  public Period getTotalPeriod() {
    return totalPeriod;
  }

  public void setTotalPeriod(Period totalPeriod) {
    this.totalPeriod = totalPeriod;
  }

  public Integer getIdBoxed() {
    return idBoxed;
  }

  public void setIdBoxed(Integer idBoxed) {
    this.idBoxed = idBoxed;
  }

  public Boolean getIsActiveBoxed() {
    return isActiveBoxed;
  }

  public void setIsActiveBoxed(Boolean isActiveBoxed) {
    this.isActiveBoxed = isActiveBoxed;
  }

  public Byte getRankBoxed() {
    return rankBoxed;
  }

  public void setRankBoxed(Byte rankBoxed) {
    this.rankBoxed = rankBoxed;
  }

  public Character getGenderBoxed() {
    return genderBoxed;
  }

  public void setGenderBoxed(Character genderBoxed) {
    this.genderBoxed = genderBoxed;
  }

  public Short getAgeBoxed() {
    return ageBoxed;
  }

  public void setAgeBoxed(Short ageBoxed) {
    this.ageBoxed = ageBoxed;
  }

  public Long getSalaryBoxed() {
    return salaryBoxed;
  }

  public void setSalaryBoxed(Long salaryBoxed) {
    this.salaryBoxed = salaryBoxed;
  }

  public Float getPerformanceScoreBoxed() {
    return performanceScoreBoxed;
  }

  public void setPerformanceScoreBoxed(Float performanceScoreBoxed) {
    this.performanceScoreBoxed = performanceScoreBoxed;
  }

  public Double getBonusBoxed() {
    return bonusBoxed;
  }

  public void setBonusBoxed(Double bonusBoxed) {
    this.bonusBoxed = bonusBoxed;
  }

  public java.util.Date getHireUtilDate() {
    return hireUtilDate;
  }

  public void setHireUtilDate(java.util.Date hireUtilDate) {
    this.hireUtilDate = hireUtilDate;
  }




  /**
   * Provides a string representation of the Employee object.
   *
   * @return a string representation of the Employee object
   */
  @Override
  public String toString() {
    return "Employee{" +
            "employeeId=" + employeeId +
            ", logoutTime=" + logoutTime +
            ", active=" + active +
            ", deptId=" + deptId +
            ", employeeCode=" + employeeCode +
            ", grade=" + grade +
            ", bonus=" + bonus +
            ", prevSalary=" + prevSalary +
            ", gender=" + gender +
            ", salary=" + salary +
            ", largeNumber=" + largeNumber +
            ", firstName='" + firstName + '\'' +
            ", middleInitial='" + middleInitial + '\'' +
            ", lastName='" + lastName + '\'' +
            ", hireDate=" + hireDate +
            ", hireUtilDate=" + hireUtilDate +
            ", localHireDate=" + localHireDate +
            ", loginInstant=" + loginInstant +
            ", loginLocalTime=" + loginLocalTime +
            ", salaryMonthDay=" + salaryMonthDay +
            ", incrementYearmonth=" + incrementYearmonth +
            ", startTime=" + startTime +
            ", lastUpdated=" + lastUpdated +
            ", localDateTime=" + localDateTime +
            ", offsetDateTime=" + offsetDateTime +
            ", yearJoined=" + yearJoined +
            ", durationOfEmployment=" + durationOfEmployment +
            ", picture=" + Arrays.toString(picture) +
            ", resume='" + resume + '\'' +
            ", rawData=" + Arrays.toString(rawData) +
            ", phones=" + phones +
            ", vacationZonedDateTime=" + vacationZonedDateTime +
            ", totalPeriod=" + totalPeriod +
            ", idBoxed=" + idBoxed +
            ", isActiveBoxed=" + isActiveBoxed +
            ", rankBoxed=" + rankBoxed +
            ", genderBoxed=" + genderBoxed +
            ", ageBoxed=" + ageBoxed +
            ", salaryBoxed=" + salaryBoxed +
            ", performanceScoreBoxed=" + performanceScoreBoxed +
            ", bonusBoxed=" + bonusBoxed +
            '}';
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) return true;
    if (object == null || getClass() != object.getClass()) return false;
    Employee employee = (Employee) object;
    return employeeId == employee.employeeId && logoutTime == employee.logoutTime
            && active == employee.active && deptId == employee.deptId
            && employeeCode == employee.employeeCode && grade == employee.grade
            && Float.compare(bonus, employee.bonus) == 0
            && Double.compare(prevSalary, employee.prevSalary) == 0
            && gender == employee.gender && Objects.equals(salary, employee.salary)
            && Objects.equals(largeNumber, employee.largeNumber)
            && Objects.equals(firstName, employee.firstName)
            && Objects.equals(middleInitial, employee.middleInitial)
            && Objects.equals(lastName, employee.lastName)
            && Objects.equals(hireDate, employee.hireDate)
            && Objects.equals(hireUtilDate, employee.hireUtilDate)
            && Objects.equals(localHireDate, employee.localHireDate)
            && Objects.equals(loginInstant, employee.loginInstant)
            && Objects.equals(loginLocalTime, employee.loginLocalTime)
            && Objects.equals(salaryMonthDay, employee.salaryMonthDay)
            && Objects.equals(incrementYearmonth, employee.incrementYearmonth)
            && Objects.equals(startTime, employee.startTime)
            && Objects.equals(lastUpdated, employee.lastUpdated)
            && Objects.equals(localDateTime, employee.localDateTime)
            && Objects.equals(offsetDateTime, employee.offsetDateTime)
            && Objects.equals(yearJoined, employee.yearJoined)
            && Objects.equals(durationOfEmployment, employee.durationOfEmployment)
            && Objects.deepEquals(picture, employee.picture)
            && Objects.equals(resume, employee.resume)
            && Objects.deepEquals(rawData, employee.rawData)
            && Objects.equals(phones, employee.phones)
            && Objects.equals(vacationZonedDateTime, employee.vacationZonedDateTime)
            && Objects.equals(totalPeriod, employee.totalPeriod)
            && Objects.equals(idBoxed, employee.idBoxed)
            && Objects.equals(isActiveBoxed, employee.isActiveBoxed)
            && Objects.equals(rankBoxed, employee.rankBoxed)
            && Objects.equals(genderBoxed, employee.genderBoxed)
            && Objects.equals(ageBoxed, employee.ageBoxed)
            && Objects.equals(salaryBoxed, employee.salaryBoxed)
            && Objects.equals(performanceScoreBoxed, employee.performanceScoreBoxed)
            && Objects.equals(bonusBoxed, employee.bonusBoxed);
  }

  @Override
  public int hashCode() {
    return Objects.hash(employeeId, logoutTime, active, deptId, employeeCode, grade,
            bonus, prevSalary, gender, salary, largeNumber, firstName, middleInitial,
            lastName, hireDate, hireUtilDate, localHireDate, loginInstant, loginLocalTime,
            salaryMonthDay, incrementYearmonth, startTime, lastUpdated, localDateTime,
            offsetDateTime, yearJoined, durationOfEmployment,
            Arrays.hashCode(picture), resume, Arrays.hashCode(rawData), phones,
            vacationZonedDateTime, totalPeriod, idBoxed, isActiveBoxed,
            rankBoxed, genderBoxed, ageBoxed, salaryBoxed, performanceScoreBoxed, bonusBoxed);
  }
}
