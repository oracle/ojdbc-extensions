package com.example.employees.dto;

import java.math.BigDecimal;

public class EmployeeDto {
    private Long id;
    private String name;
    private BigDecimal salary;
    private String phone;

    public EmployeeDto() {
    }

    public EmployeeDto(Long id, String name, BigDecimal salary, String phone) {
        this.id = id;
        this.name = name;
        this.salary = salary;
        this.phone = phone;
    }

    public Long getId() {
        return id;
    }

    public EmployeeDto setId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public EmployeeDto setName(String name) {
        this.name = name;
        return this;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public EmployeeDto setSalary(BigDecimal salary) {
        this.salary = salary;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public EmployeeDto setPhone(String phone) {
        this.phone = phone;
        return this;
    }
}
