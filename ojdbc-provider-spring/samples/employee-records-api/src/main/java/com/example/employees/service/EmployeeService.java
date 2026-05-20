/*
 *  Copyright (c) 2026 Oracle and/or its affiliates.
 *
 *  The Universal Permissive License (UPL), Version 1.0
 *
 *  Subject to the condition set forth below, permission is hereby granted to any
 *  person obtaining a copy of this software, associated documentation and/or data
 *  (collectively the "Software"), free of charge and under any and all copyright
 *  rights in the Software, and any and all patent rights owned or freely
 *  licensable by each licensor hereunder covering either (i) the unmodified
 *  Software as contributed to or provided by such licensor, or (ii) the Larger
 *  Works (as defined below), to deal in both
 *
 *  (a) the Software, and
 *  (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 *  one is included with the Software (each a "Larger Work" to which the Software
 *  is contributed by such licensors),
 *
 *  without restriction, including without limitation the rights to copy, create
 *  derivative works of, display, perform, and distribute the Software and make,
 *  use, sell, offer for sale, import, export, have made, and have sold the
 *  Software and the Larger Work(s), and to sublicense the foregoing rights on
 *  either these or other terms.
 *
 *  This license is subject to the following condition:
 *  The above copyright notice and either this complete permission notice or at
 *  a minimum a reference to the UPL must be included in all copies or
 *  substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

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
