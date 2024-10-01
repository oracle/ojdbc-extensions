package oracle.jdbc.provider.oson.model;

import java.util.ArrayList;
import java.util.List;

public class OrganisationInstances {
    static final String[] organizations = {
            "Oracle Corporation",
            "Oracle Financial Services",
            "Oracle Cloud Infrastructure",
            "Oracle Consulting",
            "Oracle Academy",
            "Oracle Labs",
            "Oracle Hospitality",
            "Oracle NetSuite",
            "Oracle Japan",
            "Oracle University"
    };
    static List<Organisation> organisationInstances = new ArrayList<Organisation>();


    public static void buildOrganisationInstances() {
        for (int i = 0; i < 10; i++) {
            int no_of_employees = employeesCount[i];
            String organisationName = organizations[i];
            List<Employee> employees = new ArrayList<>();
            for (int j = 0; j < no_of_employees; j++) {
                employees.add(EmployeeInstances.getEmployee());
            }
            organisationInstances.add(new Organisation(organisationName, employees));
        }
    }

    static final int[] employeesCount = {100000, 100000, 100000, 100000, 100000, 100000, 100000, 100000, 100000, 100000};

    public static List<Organisation> getInstances() {
        if (organisationInstances.isEmpty()) {
            buildOrganisationInstances();
        }
        return organisationInstances;
    }
}
