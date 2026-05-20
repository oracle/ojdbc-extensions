package com.example.employees.dto;

import java.math.BigDecimal;

public class SalarySummaryDto {
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private BigDecimal averageSalary;
    private long employeeCount;

    public SalarySummaryDto() {
    }

    public SalarySummaryDto(BigDecimal minSalary, BigDecimal maxSalary, BigDecimal averageSalary, long employeeCount) {
        this.minSalary = minSalary;
        this.maxSalary = maxSalary;
        this.averageSalary = averageSalary;
        this.employeeCount = employeeCount;
    }

    public BigDecimal getMinSalary() {
        return minSalary;
    }

    public SalarySummaryDto setMinSalary(BigDecimal minSalary) {
        this.minSalary = minSalary;
        return this;
    }

    public BigDecimal getMaxSalary() {
        return maxSalary;
    }

    public SalarySummaryDto setMaxSalary(BigDecimal maxSalary) {
        this.maxSalary = maxSalary;
        return this;
    }

    public BigDecimal getAverageSalary() {
        return averageSalary;
    }

    public SalarySummaryDto setAverageSalary(BigDecimal averageSalary) {
        this.averageSalary = averageSalary;
        return this;
    }

    public long getEmployeeCount() {
        return employeeCount;
    }

    public SalarySummaryDto setEmployeeCount(long employeeCount) {
        this.employeeCount = employeeCount;
        return this;
    }
}
