package com.example.employees.api;

import com.example.employees.dto.EmployeeDto;
import com.example.employees.dto.SalarySummaryDto;
import com.example.employees.service.EmployeeService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/employees", produces = MediaType.APPLICATION_JSON_VALUE)
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @GetMapping
    public List<EmployeeDto> listEmployees() {
        return service.listEmployees();
    }

    @GetMapping("/salary-summary")
    public SalarySummaryDto getSalarySummary() {
        return service.getSalarySummary();
    }
}
