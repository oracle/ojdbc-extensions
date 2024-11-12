package oracle.jdbc.provider.oson.sample.model;

import java.util.Objects;

public class Phone {

  public enum Type {MOBILE, HOME, WORK}

  String number;

  Type type;

  public Phone() {
  }

  public Phone(String number, Type type) {
    this.number = number;
    this.type = type;
  }

  public String getNumber() {
    return number;
  }

  public void setNumber(String number) {
    this.number = number;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return "Phone {" +
        "number='" + number + '\'' +
        ", type=" + type +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Phone phone = (Phone) o;
    return Objects.equals(number, phone.number) && type == phone.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(number, type);
  }
}
