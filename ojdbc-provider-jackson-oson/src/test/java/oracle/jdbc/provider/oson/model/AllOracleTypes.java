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

public class AllOracleTypes {

	private int int_sample;
	private long long_sample;
	private BigInteger big_integer_sample;
	private String string_sample;
	private char char_sample;
	private boolean boolean_sample;
	private double double_sample;
	private float float_sample;
	private BigDecimal big_decimal_sample;
	private Date date_sample;
	private Period period_sample;
	private Duration duration_sample;
	private LocalDateTime ldt_sample;
	private OffsetDateTime odt_sample;
	
	public AllOracleTypes() {
		super();
	}

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

	public int getInt_sample() {
		return int_sample;
	}

	public void setInt_sample(int int_sample) {
		this.int_sample = int_sample;
	}

	public long getLong_sample() {
		return long_sample;
	}

	public void setLong_sample(long long_sample) {
		this.long_sample = long_sample;
	}

	public BigInteger getBig_integer_sample() {
		return big_integer_sample;
	}

	public void setBig_integer_sample(BigInteger big_integer_sample) {
		this.big_integer_sample = big_integer_sample;
	}

	public String getString_sample() {
		return string_sample;
	}

	public void setString_sample(String string_sample) {
		this.string_sample = string_sample;
	}

	public char getChar_sample() {
		return char_sample;
	}

	public void setChar_sample(char char_sample) {
		this.char_sample = char_sample;
	}

	public boolean isBoolean_sample() {
		return boolean_sample;
	}

	public void setBoolean_sample(boolean boolean_sample) {
		this.boolean_sample = boolean_sample;
	}

	public double getDouble_sample() {
		return double_sample;
	}

	public void setDouble_sample(double double_sample) {
		this.double_sample = double_sample;
	}

	public float getFloat_sample() {
		return float_sample;
	}

	public void setFloat_sample(float float_sample) {
		this.float_sample = float_sample;
	}

	public BigDecimal getBig_decimal_sample() {
		return big_decimal_sample;
	}

	public void setBig_decimal_sample(BigDecimal big_decimal_sample) {
		this.big_decimal_sample = big_decimal_sample;
	}

	public Date getDate_sample() {
		return date_sample;
	}

	public void setDate_sample(Date date_sample) {
		this.date_sample = date_sample;
	}

	public Period getPeriod_sample() {
		return period_sample;
	}

	public void setPeriod_sample(Period period_sample) {
		this.period_sample = period_sample;
	}

	public Duration getDuration_sample() {
		return duration_sample;
	}

	public void setDuration_sample(Duration duration_sample) {
		this.duration_sample = duration_sample;
	}

	public LocalDateTime getLdt_sample() {
		return ldt_sample;
	}

	public void setLdt_sample(LocalDateTime ldt_sample) {
		this.ldt_sample = ldt_sample;
	}

	public OffsetDateTime getOdt_sample() {
		return odt_sample;
	}

	public void setOdt_sample(OffsetDateTime odt_sample) {
		this.odt_sample = odt_sample;
	}

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
	
}
