package com.example.employees.service;

import com.example.employees.domain.EmployeeEntity;
import com.example.employees.dto.EmployeeDto;
import com.example.employees.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import com.example.employees.dto.SalarySummaryDto;
import com.example.employees.dto.SalaryAggregates;
import com.example.annotation.RunWithDataRoles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class EmployeeService {

    private final EmployeeRepository repo;

    public EmployeeService(EmployeeRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<EmployeeDto> listEmployees() {
        return repo.findAll().stream()
                .sorted(Comparator.comparing(EmployeeEntity::getId))
                .map(this::toDto)
                .toList();
    }

    @RunWithDataRoles(
            dataRoles = {"COMPENSATION_ANALYST"}
    )
    @Transactional(readOnly = true)
    public SalarySummaryDto getSalarySummary() {

        SalaryAggregates aggs = repo.getSalaryAggregates();

        BigDecimal min = aggs != null ? aggs.getMin() : null;
        BigDecimal max = aggs != null ? aggs.getMax() : null;
        BigDecimal sum = aggs != null ? aggs.getSum() : null;
        long count = (aggs != null && aggs.getCount() != null) ? aggs.getCount() : 0L;

        BigDecimal avg = null;
        if (sum != null && count > 0) {
            avg = sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        }

        return new SalarySummaryDto()
                .setMinSalary(min)
                .setMaxSalary(max)
                .setAverageSalary(avg)
                .setEmployeeCount(count);
    }

    private EmployeeDto toDto(EmployeeEntity e) {
        String name = String.format("%s %s",
                e.getFirstName() != null ? e.getFirstName() : "",
                e.getLastName() != null ? e.getLastName() : "").trim().replaceAll("\\s{2,}", " ");
        return new EmployeeDto()
                .setId(e.getId())
                .setName(name)
                .setSalary(e.getSalary())
                .setPhone(e.getPhone());
    }
}
