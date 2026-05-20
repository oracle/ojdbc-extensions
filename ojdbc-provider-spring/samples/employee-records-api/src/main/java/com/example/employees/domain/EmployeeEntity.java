package com.example.employees.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "EMPLOYEES", schema = "HR")
public class EmployeeEntity {

    @Id
    @Column(name = "EMPLOYEE_ID")
    private Long id;

    @Column(name = "FIRST_NAME")
    private String firstName;

    @Column(name = "LAST_NAME")
    private String lastName;

    @Column(name = "SALARY")
    private BigDecimal salary;

    @Column(name = "PHONE")
    private String phone;

    public EmployeeEntity() {
    }

    public Long getId() {
        return id;
    }

    public EmployeeEntity setId(Long id) {
        this.id = id;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public EmployeeEntity setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public EmployeeEntity setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public EmployeeEntity setSalary(BigDecimal salary) {
        this.salary = salary;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public EmployeeEntity setPhone(String phone) {
        this.phone = phone;
        return this;
    }
}
