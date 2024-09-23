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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Period;

/**
 * The AllOracleTypes class is a Plain Old Java Object (POJO) that encapsulates
 * various data types corresponding to Oracle database types, including primitive
 * types, wrappers, and complex types like BigInteger, BigDecimal, and Date/Time types.
 * It provides getters and setters for each field and a constructor for full
 * initialization.
 */
public class AllOracleTypes {

  /**
   * A sample integer value.
   */
  private int int_sample;

  /**
   * A sample long value.
   */
  private long long_sample;

  /**
   * A sample BigInteger value, used for handling large integers.
   */
  private BigInteger big_integer_sample;

  /**
   * A sample String value.
   */
  private String string_sample;

  /**
   * A sample character value.
   */
  private char char_sample;

  /**
   * A sample boolean value.
   */
  private boolean boolean_sample;

  /**
   * A sample double-precision floating-point number.
   */
  private double double_sample;

  /**
   * A sample single-precision floating-point number.
   */
  private float float_sample;

  /**
   * A sample BigDecimal value, used for handling precise decimal values.
   */
  private BigDecimal big_decimal_sample;

  /**
   * A sample date value represented by the java.sql.Date class.
   */
  private Date date_sample;

  /**
   * A sample period value, representing a date-based amount of time.
   */
  private Period period_sample;

  /**
   * A sample duration value, representing a time-based amount of time.
   */
  private Duration duration_sample;

  /**
   * A sample LocalDateTime value, representing a date-time without a timezone.
   */
  private LocalDateTime ldt_sample;

  /**
   * A sample OffsetDateTime value, representing a date-time with an offset from UTC/Greenwich.
   */
  private OffsetDateTime odt_sample;

  /**
   * Default no-args constructor.
   */
  public AllOracleTypes() {
    super();
  }

  /**
   * All-args constructor to initialize all fields.
   *
   * @param int_sample an integer sample value
   * @param long_sample a long sample value
   * @param big_integer_sample a BigInteger sample value
   * @param string_sample a String sample value
   * @param char_sample a char sample value
   * @param boolean_sample a boolean sample value
   * @param double_sample a double sample value
   * @param float_sample a float sample value
   * @param big_decimal_sample a BigDecimal sample value
   * @param date_sample a Date sample value
   * @param period_sample a Period sample value
   * @param duration_sample a Duration sample value
   * @param ldt_sample a LocalDateTime sample value
   * @param odt_sample an OffsetDateTime sample value
   */
  public AllOracleTypes(int int_sample, long long_sample, BigInteger big_integer_sample, String string_sample,
                        char char_sample, boolean boolean_sample, double double_sample, float float_sample, BigDecimal big_decimal_sample,
                        Date date_sample, Period period_sample, Duration duration_sample, LocalDateTime ldt_sample,
                        OffsetDateTime odt_sample) {
    super();
    this.int_sample = int_sample;
    this.long_sample = long_sample;
    this.big_integer_sample = big_integer_sample;
    this.string_sample = string_sample;
    this.char_sample = char_sample;
    this.boolean_sample = boolean_sample;
    this.double_sample = double_sample;
    this.float_sample = float_sample;
    this.big_decimal_sample = big_decimal_sample;
    this.date_sample = date_sample;
    this.period_sample = period_sample;
    this.duration_sample = duration_sample;
    this.ldt_sample = ldt_sample;
    this.odt_sample = odt_sample;
  }

  // Getters and Setters for each field

  /**
   * Gets the integer sample.
   *
   * @return the integer sample
   */
  public int getInt_sample() {
    return int_sample;
  }

  /**
   * Sets the integer sample.
   *
   * @param int_sample the integer sample to set
   */
  public void setInt_sample(int int_sample) {
    this.int_sample = int_sample;
  }

  /**
   * Gets the long sample.
   *
   * @return the long sample
   */
  public long getLong_sample() {
    return long_sample;
  }

  /**
   * Sets the long sample.
   *
   * @param long_sample the long sample to set
   */
  public void setLong_sample(long long_sample) {
    this.long_sample = long_sample;
  }

  /**
   * Gets the BigInteger sample.
   *
   * @return the BigInteger sample
   */
  public BigInteger getBig_integer_sample() {
    return big_integer_sample;
  }

  /**
   * Sets the BigInteger sample.
   *
   * @param big_integer_sample the BigInteger sample to set
   */
  public void setBig_integer_sample(BigInteger big_integer_sample) {
    this.big_integer_sample = big_integer_sample;
  }

  /**
   * Gets the String sample.
   *
   * @return the String sample
   */
  public String getString_sample() {
    return string_sample;
  }

  /**
   * Sets the String sample.
   *
   * @param string_sample the String sample to set
   */
  public void setString_sample(String string_sample) {
    this.string_sample = string_sample;
  }

  /**
   * Gets the char sample.
   *
   * @return the char sample
   */
  public char getChar_sample() {
    return char_sample;
  }

  /**
   * Sets the char sample.
   *
   * @param char_sample the char sample to set
   */
  public void setChar_sample(char char_sample) {
    this.char_sample = char_sample;
  }

  /**
   * Gets the boolean sample.
   *
   * @return the boolean sample
   */
  public boolean isBoolean_sample() {
    return boolean_sample;
  }

  /**
   * Sets the boolean sample.
   *
   * @param boolean_sample the boolean sample to set
   */
  public void setBoolean_sample(boolean boolean_sample) {
    this.boolean_sample = boolean_sample;
  }

  /**
   * Gets the double sample.
   *
   * @return the double sample
   */
  public double getDouble_sample() {
    return double_sample;
  }

  /**
   * Sets the double sample.
   *
   * @param double_sample the double sample to set
   */
  public void setDouble_sample(double double_sample) {
    this.double_sample = double_sample;
  }

  /**
   * Gets the float sample.
   *
   * @return the float sample
   */
  public float getFloat_sample() {
    return float_sample;
  }

  /**
   * Sets the float sample.
   *
   * @param float_sample the float sample to set
   */
  public void setFloat_sample(float float_sample) {
    this.float_sample = float_sample;
  }

  /**
   * Gets the BigDecimal sample.
   *
   * @return the BigDecimal sample
   */
  public BigDecimal getBig_decimal_sample() {
    return big_decimal_sample;
  }

  /**
   * Sets the BigDecimal sample.
   *
   * @param big_decimal_sample the BigDecimal sample to set
   */
  public void setBig_decimal_sample(BigDecimal big_decimal_sample) {
    this.big_decimal_sample = big_decimal_sample;
  }

  /**
   * Gets the Date sample.
   *
   * @return the Date sample
   */
  public Date getDate_sample() {
    return date_sample;
  }

  /**
   * Sets the Date sample.
   *
   * @param date_sample the Date sample to set
   */
  public void setDate_sample(Date date_sample) {
    this.date_sample = date_sample;
  }

  /**
   * Gets the Period sample.
   *
   * @return the Period sample
   */
  public Period getPeriod_sample() {
    return period_sample;
  }

  /**
   * Sets the Period sample.
   *
   * @param period_sample the Period sample to set
   */
  public void setPeriod_sample(Period period_sample) {
    this.period_sample = period_sample;
  }

  /**
   * Gets the Duration sample.
   *
   * @return the Duration sample
   */
  public Duration getDuration_sample() {
    return duration_sample;
  }

  /**
   * Sets the Duration sample.
   *
   * @param duration_sample the Duration sample to set
   */
  public void setDuration_sample(Duration duration_sample) {
    this.duration_sample = duration_sample;
  }

  /**
   * Gets the LocalDateTime sample.
   *
   * @return the LocalDateTime sample
   */
  public LocalDateTime getLdt_sample() {
    return ldt_sample;
  }

  /**
   * Sets the LocalDateTime sample.
   *
   * @param ldt_sample the LocalDateTime sample to set
   */
  public void setLdt_sample(LocalDateTime ldt_sample) {
    this.ldt_sample = ldt_sample;
  }

  /**
   * Gets the OffsetDateTime sample.
   *
   * @return the OffsetDateTime sample
   */
  public OffsetDateTime getOdt_sample() {
    return odt_sample;
  }

  /**
   * Sets the OffsetDateTime sample.
   *
   * @param odt_sample the OffsetDateTime sample to set
   */
  public void setOdt_sample(OffsetDateTime odt_sample) {
    this.odt_sample = odt_sample;
  }

  /**
   * Provides a string representation of the object.
   *
   * @return a string representation of the object
   */
  @Override
  public String toString() {
    return "AllOracleTypes \n{\n"
            +"  int_sample=" + int_sample + ", \n"
            +"  long_sample=" + long_sample + ", \n"
            +"  big_integer_sample=" + big_integer_sample + ", \n"
            +"  string_sample=" + string_sample + ", \n"
            +"  char_sample=" + char_sample + ", \n"
            +"  boolean_sample=" + boolean_sample + ", \n"
            +"  double_sample=" + double_sample + ", \n"
            +"  float_sample=" + float_sample + ", \n"
            +"  big_decimal_sample=" + big_decimal_sample + ", \n"
            +"  date_sample=" + date_sample + ", \n"
            +"  period_sample=" + period_sample + ", \n"
            +"  duration_sample=" + duration_sample + ", \n"
            +"  ldt_sample=" + ldt_sample + ", \n"
            +"  odt_sample=" + odt_sample + "\n}";
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this)
      return true;
    if (!(obj instanceof AllOracleTypes))
      return false;
    AllOracleTypes other = (AllOracleTypes)obj;
    if (this.int_sample != other.int_sample) return false;
    if (this.long_sample != other.long_sample) return false;
    if (this.big_integer_sample == null && other.big_integer_sample != null) return false;
    if (!this.big_integer_sample.equals(other.big_integer_sample)) return false;
    if (this.string_sample == null && other.string_sample != null) return false;
    if (!this.string_sample.equals(other.string_sample)) return false;
    if (this.char_sample != other.char_sample) return false;
    if (this.boolean_sample != other.boolean_sample) return false;
    if (this.double_sample != other.double_sample) return false;
    if (this.float_sample != other.float_sample) return false;
    if (this.big_decimal_sample == null && other.big_decimal_sample != null) return false;
    if (!this.big_decimal_sample.equals(other.big_decimal_sample)) return false;
    if (this.date_sample == null && other.date_sample != null) return false;
    if (!this.date_sample.equals(other.date_sample)) return false;
    if (this.period_sample == null && other.period_sample != null) return false;
    if (!this.period_sample.equals(other.period_sample)) return false;
    if (this.duration_sample == null && other.duration_sample != null) return false;
    if (!this.duration_sample.equals(other.duration_sample)) return false;
    if (this.ldt_sample == null && other.ldt_sample != null) return false;
    if (!this.ldt_sample.equals(other.ldt_sample)) return false;
    if (this.odt_sample == null && other.odt_sample != null) return false;
    if (!this.odt_sample.isEqual(other.odt_sample)) return false;
    return true;
  }
}
