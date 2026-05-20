package com.example.employees.repository;

import com.example.employees.domain.EmployeeEntity;
import com.example.employees.dto.SalaryAggregates;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long> {

    @Query("select new com.example.employees.dto.SalaryAggregates(min(e.salary), max(e.salary), sum(e.salary), count(e)) from EmployeeEntity e")
    SalaryAggregates getSalaryAggregates();
}
