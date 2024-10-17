package oracle.jdbc.provider.oson.sample.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Emp {

  String name;

  String job;

  BigDecimal salary;

  @JsonProperty("e-mail")
  String email;

  List<Phone> phoneNumbers = new ArrayList<>();

  public Emp() {
  }

  public Emp(String name, String job, BigDecimal salary, String email, List<Phone> phones) {
    this.name = name;
    this.job = job;
    this.salary = salary;
    this.email = email;
    phoneNumbers.addAll(phones);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getJob() {
    return job;
  }

  public void setJob(String job) {
    this.job = job;
  }

  public BigDecimal getSalary() {
    return salary;
  }

  public void setSalary(BigDecimal salary) {
    this.salary = salary;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public List<Phone> getPhoneNumbers() {
    return this.phoneNumbers;
  }

  public void setPhoneNumbers(List<Phone> phones) {
    this.phoneNumbers = phones;
  }

  @Override
  public String toString() {
    return "Emp \n{\n" +
        "  name=" + name + ",\n" +
        "  job=" + job + ",\n" +
        "  salary=" + salary + ",\n" +
        "  email=" + email +  ",\n" +
        "  phoneNumbers=" + phoneNumbers + "\n" +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Emp emp = (Emp) o;
    return Objects.equals(name, emp.name) && Objects.equals(job, emp.job) && Objects.equals(salary, emp.salary) && Objects.equals(email, emp.email) && Objects.equals(phoneNumbers, emp.phoneNumbers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, job, salary, email, phoneNumbers);
  }
}
