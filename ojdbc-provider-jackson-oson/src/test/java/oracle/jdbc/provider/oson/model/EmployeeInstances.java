package oracle.jdbc.provider.oson.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * A utility class for managing and instantiating a list of Employee objects.
 */
public class EmployeeInstances {
    private static List<Employee> employees = new ArrayList<Employee>();

    /**
     * Retrieves the list of employees. If the list is empty, it instantiates employee objects.
     *
     * @return a list of Employee objects.
     */
    public static List<Employee> getEmployees() {
        if (employees.isEmpty()) {
            instantiateEmployees();
        }
        return employees;
    }

    /**
     * Retrieves a random employee from the list. If the list is empty, it instantiates employee objects.
     *
     * @return a randomly selected Employee object from the list.
     */
    public static Employee getEmployee() {
        if (employees.isEmpty()) {
            instantiateEmployees();
        }
        Random random = new Random();
        int index = random.nextInt(employees.size());
        return employees.get(index);
    }

    /**
     * Populates the list of employees with predefined Employee objects.
     * This method is private and is called only when the list of employees is empty.
     */
    private static void instantiateEmployees() {
        List<Phone> phones1 = Arrays.asList(new Phone("123-456-7890", Phone.Type.MOBILE), new Phone("098-765-4321", Phone.Type.HOME));
        List<Phone> phones2 = Arrays.asList(new Phone("321-654-9870", Phone.Type.WORK));
        List<Phone> phones3 = Arrays.asList(new Phone("111-222-3333", Phone.Type.MOBILE));
        List<Phone> phones4 = Arrays.asList(new Phone("444-555-6666", Phone.Type.HOME), new Phone("777-888-9999", Phone.Type.MOBILE));
        List<Phone> phones5 = Arrays.asList(new Phone("999-888-7777", Phone.Type.MOBILE));
        List<Phone> phones6 = Arrays.asList(new Phone("555-444-3333", Phone.Type.WORK), new Phone("666-777-8888", Phone.Type.HOME));
        List<Phone> phones7 = Arrays.asList(new Phone("123-123-1234", Phone.Type.HOME));
        List<Phone> phones8 = Arrays.asList(new Phone("987-654-3210", Phone.Type.WORK));
        List<Phone> phones9 = Arrays.asList(new Phone("555-666-7777", Phone.Type.MOBILE), new Phone("888-999-0000", Phone.Type.HOME));
        List<Phone> phones10 = Arrays.asList(new Phone("000-111-2222", Phone.Type.WORK));

        // Creating Employee instances
        employees.add(new Employee(1, new BigDecimal("50000.99"), new BigInteger("123456789"),
                "John", "A", "Doe", Date.valueOf("2015-01-10"), Time.valueOf("09:00:00"),
                Timestamp.valueOf("2023-09-19 10:15:30"), LocalDateTime.now(), OffsetDateTime.now(ZoneOffset.UTC),
                Year.of(2015), Duration.ofDays(365 * 8), new byte[]{1, 2, 3}, "Resume text",
                true, new byte[]{1, 2}, phones1,  LocalDate.now(), Instant.now(),LocalTime.now(),
                MonthDay.now(), YearMonth.now(), System.currentTimeMillis(), ZonedDateTime.now(ZoneOffset.UTC)));

        employees.add(new Employee(2, new BigDecimal("60000.99"), new BigInteger("987654321"),
                "Jane", "B", "Smith", Date.valueOf("2016-02-20"), Time.valueOf("08:30:00"),
                Timestamp.valueOf("2023-09-18 09:10:25"), LocalDateTime.now(), OffsetDateTime.now(ZoneOffset.UTC),
                Year.of(2016), Duration.ofDays(365 * 7), new byte[]{4, 5, 6}, "Resume text",
                true, new byte[]{3, 4}, phones2,  LocalDate.now(), Instant.now(),LocalTime.now(),
                MonthDay.now(), YearMonth.now(), System.currentTimeMillis(), ZonedDateTime.now(ZoneOffset.UTC)));

        employees.add(new Employee(3, new BigDecimal("45000.99"), new BigInteger("456789123"),
                "Alice", "C", "Johnson", Date.valueOf("2017-03-30"), Time.valueOf("07:45:00"),
                Timestamp.valueOf("2023-09-17 08:05:20"), LocalDateTime.now(), OffsetDateTime.now(ZoneOffset.UTC),
                Year.of(2017), Duration.ofDays(365 * 6), new byte[]{7, 8, 9}, "Resume text",
                false, new byte[]{5, 6}, phones3,  LocalDate.now(), Instant.now(),LocalTime.now(),
                MonthDay.now(), YearMonth.now(), System.currentTimeMillis(), ZonedDateTime.now(ZoneOffset.UTC)));

        employees.add(new Employee(4, new BigDecimal("70000.99"), new BigInteger("654321987"),
                "Bob", "D", "Williams", Date.valueOf("2018-04-15"), Time.valueOf("10:15:00"),
                Timestamp.valueOf("2023-09-16 07:00:15"), LocalDateTime.now(), OffsetDateTime.now(ZoneOffset.UTC),
                Year.of(2018), Duration.ofDays(365 * 5), new byte[]{10, 11, 12}, "Resume text",
                true, new byte[]{7, 8}, phones4,  LocalDate.now(), Instant.now(),LocalTime.now(),
                MonthDay.now(), YearMonth.now(), System.currentTimeMillis(), ZonedDateTime.now(ZoneOffset.UTC)));

        employees.add(new Employee(5, new BigDecimal("55000.99"), new BigInteger("321987654"),
                "Charlie", "E", "Brown", Date.valueOf("2019-05-05"), Time.valueOf("11:00:00"),
                Timestamp.valueOf("2023-09-15 06:55:10"), LocalDateTime.now(), OffsetDateTime.now(ZoneOffset.UTC),
                Year.of(2019), Duration.ofDays(365 * 4), new byte[]{13, 14, 15}, "Resume text",
                false, new byte[]{9, 10}, phones5,  LocalDate.now(), Instant.now(),LocalTime.now(),
                MonthDay.now(), YearMonth.now(), System.currentTimeMillis(), ZonedDateTime.now(ZoneOffset.UTC)));

        employees.add(new Employee(6, new BigDecimal("48000.99"), new BigInteger("159753852"),
                "Eve", "F", "Davis", Date.valueOf("2020-06-10"), Time.valueOf("12:00:00"),
                Timestamp.valueOf("2023-09-14 05:50:05"), LocalDateTime.now(), OffsetDateTime.now(ZoneOffset.UTC),
                Year.of(2020), Duration.ofDays(365 * 3), new byte[]{16, 17, 18}, "Resume text",
                true, new byte[]{11, 12}, phones6,  LocalDate.now(), Instant.now(),LocalTime.now(),
                MonthDay.now(), YearMonth.now(), System.currentTimeMillis(), ZonedDateTime.now(ZoneOffset.UTC)));

        employees.add(new Employee(7, new BigDecimal("62000.99"), new BigInteger("951753852"),
                "Frank", "G", "Evans", Date.valueOf("2021-07-20"), Time.valueOf("08:00:00"),
                Timestamp.valueOf("2023-09-13 04:45:00"), LocalDateTime.now(), OffsetDateTime.now(ZoneOffset.UTC),
                Year.of(2021), Duration.ofDays(365 * 2), new byte[]{19, 20, 21}, "Resume text",
                false, new byte[]{13, 14}, phones7,  LocalDate.now(), Instant.now(),LocalTime.now(),
                MonthDay.now(), YearMonth.now(), System.currentTimeMillis(), ZonedDateTime.now(ZoneOffset.UTC)));

        employees.add(new Employee(8, new BigDecimal("53000.99"), new BigInteger("753951456"),
                "Grace", "H", "Green", Date.valueOf("2022-08-30"), Time.valueOf("09:30:00"),
                Timestamp.valueOf("2023-09-12 03:40:55"), LocalDateTime.now(), OffsetDateTime.now(ZoneOffset.UTC),
                Year.of(2022), Duration.ofDays(365), new byte[]{22, 23, 24}, "Resume text",
                true, new byte[]{15, 16}, phones8,  LocalDate.now(), Instant.now(),LocalTime.now(),
                MonthDay.now(), YearMonth.now(), System.currentTimeMillis(), ZonedDateTime.now(ZoneOffset.UTC)));

        employees.add(new Employee(9, new BigDecimal("59000.99"), new BigInteger("357159852"),
                "Hank", "I", "Martinez", Date.valueOf("2023-09-01"), Time.valueOf("10:45:00"),
                Timestamp.valueOf("2023-09-11 02:35:50"), LocalDateTime.now(), OffsetDateTime.now(ZoneOffset.UTC),
                Year.of(2023), Duration.ofDays(100), new byte[]{25, 26, 27}, "Resume text",
                false, new byte[]{17, 18}, phones9,  LocalDate.now(), Instant.now(),LocalTime.now(),
                MonthDay.now(), YearMonth.now(), System.currentTimeMillis(), ZonedDateTime.now(ZoneOffset.UTC)));

        employees.add(new Employee(10, new BigDecimal("61000.99"), new BigInteger("147258369"),
                "Ivy", "J", "Parker", Date.valueOf("2024-09-01"), Time.valueOf("11:15:00"),
                Timestamp.valueOf("2023-09-10 01:30:45"), LocalDateTime.now(), OffsetDateTime.now(ZoneOffset.UTC),
                Year.of(2024), Duration.ofDays(50), new byte[]{28, 29, 30}, "Resume text",
                true, new byte[]{19, 20}, phones10,  LocalDate.now(), Instant.now(),LocalTime.now(),
                MonthDay.now(), YearMonth.now(), System.currentTimeMillis(), ZonedDateTime.now(ZoneOffset.UTC)));
    }
}
