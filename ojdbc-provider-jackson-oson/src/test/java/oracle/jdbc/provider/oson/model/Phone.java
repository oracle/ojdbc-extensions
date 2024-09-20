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

import java.util.Objects;

/**
 * The {@code Phone} class represents a phone with a phone number and a type.
 * The type can be one of {@code MOBILE}, {@code HOME}, or {@code WORK}.
 */
public class Phone {

  /**
   * Enumeration representing the type of phone.
   * It can be {@code MOBILE}, {@code HOME}, or {@code WORK}.
   */
  public enum Type {MOBILE, HOME, WORK}

  // The phone number in string format.
  String number;

  // The type of phone (MOBILE, HOME, WORK).
  Type type;

  /**
   * Default constructor that initializes an empty {@code Phone} object.
   */
  public Phone() {
  }

  /**
   * Constructs a {@code Phone} object with the specified phone number and type.
   *
   * @param number the phone number in string format.
   * @param type   the type of phone (MOBILE, HOME, WORK).
   */
  public Phone(String number, Type type) {
    this.number = number;
    this.type = type;
  }

  /**
   * Retrieves the phone number.
   *
   * @return the phone number as a string.
   */
  public String getNumber() {
    return number;
  }

  /**
   * Sets the phone number.
   *
   * @param number the phone number to set.
   */
  public void setNumber(String number) {
    this.number = number;
  }

  /**
   * Retrieves the type of phone.
   *
   * @return the phone type (MOBILE, HOME, WORK).
   */
  public Type getType() {
    return type;
  }

  /**
   * Sets the type of phone.
   *
   * @param type the type of phone (MOBILE, HOME, WORK).
   */
  public void setType(Type type) {
    this.type = type;
  }

  /**
   * Returns a string representation of the {@code Phone} object.
   *
   * @return a string in the format {@code Phone {number='xxx', type=xxx}}.
   */
  @Override
  public String toString() {
    return "Phone {" +
            "number='" + number + '\'' +
            ", type=" + type +
            '}';
  }

  /**
   * Compares this phone to another object.
   * The result is {@code true} if and only if the argument is not {@code null},
   * is a {@code Phone} object, and both the phone number and type are equal.
   *
   * @param o the object to compare with.
   * @return {@code true} if the objects are equal, otherwise {@code false}.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Phone phone = (Phone) o;
    return Objects.equals(number, phone.number) && type == phone.type;
  }

  /**
   * Returns the hash code value for this {@code Phone} object.
   * It is calculated based on the phone number and type.
   *
   * @return the hash code value.
   */
  @Override
  public int hashCode() {
    return Objects.hash(number, type);
  }
}
