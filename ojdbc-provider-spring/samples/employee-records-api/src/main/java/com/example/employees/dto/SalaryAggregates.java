package com.example.employees.dto;

import java.math.BigDecimal;

public class SalaryAggregates {
    private final BigDecimal min;
    private final BigDecimal max;
    private final BigDecimal sum;
    private final Long count;

    public SalaryAggregates(BigDecimal min, BigDecimal max, BigDecimal sum, Long count) {
        this.min = min;
        this.max = max;
        this.sum = sum;
        this.count = count;
    }

    public BigDecimal getMin() {
        return min;
    }

    public BigDecimal getMax() {
        return max;
    }

    public BigDecimal getSum() {
        return sum;
    }

    public Long getCount() {
        return count;
    }
}
