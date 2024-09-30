package oracle.jdbc.provider.oson.model;

import java.util.List;
import java.util.Objects;

public class Organisation {
    private String organisationName;
    private List<Employee> employees;

    public String getOrganisationName() {
        return organisationName;
    }

    public void setOrganisationName(String organisationName) {
        this.organisationName = organisationName;
    }

    public Organisation(String organisationName, List<Employee> employees) {
        this.organisationName = organisationName;
        this.employees = employees;
    }

    public Organisation() {
    }

    public List<Employee> getEmployees() {
        return employees;
    }

    public void setEmployees(List<Employee> employees) {
        this.employees = employees;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Organisation that = (Organisation) o;
        return Objects.equals(organisationName, that.organisationName) && Objects.equals(employees, that.employees);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organisationName, employees);
    }
}
