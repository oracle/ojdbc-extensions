/*
 ** Copyright (c) 2024 Oracle and/or its affiliates.
 **
 ** The Universal Permissive License (UPL), Version 1.0
 **
 ** Subject to the condition set forth below, permission is hereby granted to any
 ** person obtaining a copy of this software, associated documentation and/or data
 ** (collectively the "Software"), free of charge and under any and all copyright
 ** rights in the Software, and any and all patent rights owned or freely
 ** licensable by each licensor hereunder covering either (i) the unmodified
 ** Software as contributed to or provided by such licensor, or (ii) the Larger
 ** Works (as defined below), to deal in both
 **
 ** (a) the Software, and
 ** (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 ** one is included with the Software (each a "Larger Work" to which the Software
 ** is contributed by such licensors),
 **
 ** without restriction, including without limitation the rights to copy, create
 ** derivative works of, display, perform, and distribute the Software and make,
 ** use, sell, offer for sale, import, export, have made, and have sold the
 ** Software and the Larger Work(s), and to sublicense the foregoing rights on
 ** either these or other terms.
 **
 ** This license is subject to the following condition:
 ** The above copyright notice and either this complete permission notice or at
 ** a minimum a reference to the UPL must be included in all copies or
 ** substantial portions of the Software.
 **
 ** THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 ** IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 ** FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 ** AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 ** LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 ** OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 ** SOFTWARE.
 */
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
                MonthDay.now(), YearMonth.now(), System.currentTimeMillis(), ZonedDateTime.now(ZoneOffset.UTC),(short)101,
                1234567890L, (byte)5, 1500.5f, 75000.0, 'M', Period.of(10,10,0),
                1, true, (byte) 1, 'M', (short) 25, 50000L,
                4.5f, 5000.0, java.util.Date.from(Instant.now())));

        employees.add(new Employee(2, new BigDecimal("60000.99"), new BigInteger("987654321"),
                "Jane", "B", "Smith", Date.valueOf("2016-02-20"), Time.valueOf("08:30:00"),
                Timestamp.valueOf("2023-09-18 09:10:25"), LocalDateTime.now(), OffsetDateTime.now(ZoneOffset.UTC),
                Year.of(2016), Duration.ofDays(365 * 7), new byte[]{4, 5, 6}, "Resume text",
                true, new byte[]{3, 4}, phones2,  LocalDate.now(), Instant.now(),LocalTime.now(),
                MonthDay.now(), YearMonth.now(), System.currentTimeMillis(), ZonedDateTime.now(ZoneOffset.UTC),(short)102,
                1234567891L, (byte)4, 1200.0f, 68000.0, 'F',Period.of(10,10,0),
                2, false, (byte) 2, 'F', (short) 30, 60000L,
                4.0f, 6000.0, java.util.Date.from(Instant.now())));

        employees.add(new Employee(3, new BigDecimal("45000.99"), new BigInteger("456789123"),
                "Alice", "C", "Johnson", Date.valueOf("2017-03-30"), Time.valueOf("07:45:00"),
                Timestamp.valueOf("2023-09-17 08:05:20"), LocalDateTime.now(), OffsetDateTime.now(ZoneOffset.UTC),
                Year.of(2017), Duration.ofDays(365 * 6), new byte[]{7, 8, 9}, "Resume text",
                false, new byte[]{5, 6}, phones3,  LocalDate.now(), Instant.now(),LocalTime.now(),
                MonthDay.now(), YearMonth.now(), System.currentTimeMillis(), ZonedDateTime.now(ZoneOffset.UTC),(short)104,
                1234567893L, (byte)2, 950.0f, 60000.0, 'F',Period.of(10,10,0),3,
                true, (byte) 3, 'M', (short) 35, 70000L, 3.5f, 7000.0, java.util.Date.from(Instant.now())));

        employees.add(new Employee(4, new BigDecimal("70000.99"), new BigInteger("654321987"),
                "Bob", "D", "Williams", Date.valueOf("2018-04-15"), Time.valueOf("10:15:00"),
                Timestamp.valueOf("2023-09-16 07:00:15"), LocalDateTime.now(), OffsetDateTime.now(ZoneOffset.UTC),
                Year.of(2018), Duration.ofDays(365 * 5), new byte[]{10, 11, 12}, "Resume text",
                true, new byte[]{7, 8}, phones4,  LocalDate.now(), Instant.now(),LocalTime.now(),
                MonthDay.now(), YearMonth.now(), System.currentTimeMillis(), ZonedDateTime.now(ZoneOffset.UTC),(short)103,
                1234567892L, (byte)3, 800.0f, 55000.0, 'M',Period.of(10,10,0),4,
                true, (byte) 4, 'F', (short) 40, 80000L, 4.8f, 8000.0, java.util.Date.from(Instant.now())));

        employees.add(new Employee(5, new BigDecimal("55000.99"), new BigInteger("321987654"),
                "Charlie", "E", "Brown", Date.valueOf("2019-05-05"), Time.valueOf("11:00:00"),
                Timestamp.valueOf("2023-09-15 06:55:10"), LocalDateTime.now(), OffsetDateTime.now(ZoneOffset.UTC),
                Year.of(2019), Duration.ofDays(365 * 4), new byte[]{13, 14, 15}, "Resume text",
                false, new byte[]{9, 10}, phones5,  LocalDate.now(), Instant.now(),LocalTime.now(),
                MonthDay.now(), YearMonth.now(), System.currentTimeMillis(), ZonedDateTime.now(ZoneOffset.UTC),(short)105,
                1234567894L, (byte)5, 1400.0f, 72000.0, 'M',Period.of(10,10,0),5,
                false, (byte) 5, 'M', (short) 45, 90000L, 3.8f, 9000.0, java.util.Date.from(Instant.now())));

        employees.add(new Employee(6, new BigDecimal("48000.99"), new BigInteger("159753852"),
                "Eve", "F", "Davis", Date.valueOf("2020-06-10"), Time.valueOf("12:00:00"),
                Timestamp.valueOf("2023-09-14 05:50:05"), LocalDateTime.now(), OffsetDateTime.now(ZoneOffset.UTC),
                Year.of(2020), Duration.ofDays(365 * 3), new byte[]{16, 17, 18}, "Resume text",
                true, new byte[]{11, 12}, phones6,  LocalDate.now(), Instant.now(),LocalTime.now(),
                MonthDay.now(), YearMonth.now(), System.currentTimeMillis(), ZonedDateTime.now(ZoneOffset.UTC),(short)110,
                1234567899L, (byte)5, 1600.0f, 80000.0, 'F',Period.of(10,10,0),6,
                true, (byte) 6, 'F', (short) 50, 100000L, 4.2f, 10000.0,java.util.Date.from(Instant.now())));

        employees.add(new Employee(7, new BigDecimal("62000.99"), new BigInteger("951753852"),
                "Frank", "G", "Evans", Date.valueOf("2021-07-20"), Time.valueOf("08:00:00"),
                Timestamp.valueOf("2023-09-13 04:45:00"), LocalDateTime.now(), OffsetDateTime.now(ZoneOffset.UTC),
                Year.of(2021), Duration.ofDays(365 * 2), new byte[]{19, 20, 21}, "Resume text",
                false, new byte[]{13, 14}, phones7,  LocalDate.now(), Instant.now(),LocalTime.now(),
                MonthDay.now(), YearMonth.now(), System.currentTimeMillis(), ZonedDateTime.now(ZoneOffset.UTC),(short)109,
                1234567898L, (byte)2, 900.0f, 61000.0, 'M',Period.of(10,10,0),7,
                false, (byte) 7, 'M', (short) 55, 110000L, 3.2f, 11000.0,java.util.Date.from(Instant.now())));

        employees.add(new Employee(8, new BigDecimal("53000.99"), new BigInteger("753951456"),
                "Grace", "H", "Green", Date.valueOf("2022-08-30"), Time.valueOf("09:30:00"),
                Timestamp.valueOf("2023-09-12 03:40:55"), LocalDateTime.now(), OffsetDateTime.now(ZoneOffset.UTC),
                Year.of(2022), Duration.ofDays(365), new byte[]{22, 23, 24}, "Resume text",
                true, new byte[]{15, 16}, phones8,  LocalDate.now(), Instant.now(),LocalTime.now(),
                MonthDay.now(), YearMonth.now(), System.currentTimeMillis(), ZonedDateTime.now(ZoneOffset.UTC), (short)106,
                1234567895L, (byte)1, 500.0f, 45000.0, 'F',Period.of(10,10,0),8,
                true, (byte) 8, 'F', (short) 60, 120000L, 4.7f, 12000.0,java.util.Date.from(Instant.now())));

        employees.add(new Employee(9, new BigDecimal("59000.99"), new BigInteger("357159852"),
                "Hank", "I", "Martinez", Date.valueOf("2023-09-01"), Time.valueOf("10:45:00"),
                Timestamp.valueOf("2023-09-11 02:35:50"), LocalDateTime.now(), OffsetDateTime.now(ZoneOffset.UTC),
                Year.of(2023), Duration.ofDays(100), new byte[]{25, 26, 27}, "Resume text",
                false, new byte[]{17, 18}, phones9,  LocalDate.now(), Instant.now(),LocalTime.now(),
                MonthDay.now(), YearMonth.now(), System.currentTimeMillis(), ZonedDateTime.now(ZoneOffset.UTC),(short)107,
                1234567896L, (byte)3, 850.0f, 58000.0, 'M', Period.of(10,10,0),9,
                false, (byte) 9, 'M', (short) 65, 130000L, 3.6f, 13000.0,java.util.Date.from(Instant.now())));

        employees.add(new Employee(10, new BigDecimal("61000.99"), new BigInteger("147258369"),
                "Ivy", "J", "Parker", Date.valueOf("2024-09-01"), Time.valueOf("11:15:00"),
                Timestamp.valueOf("2023-09-10 01:30:45"), LocalDateTime.now(), OffsetDateTime.now(ZoneOffset.UTC),
                Year.of(2024), Duration.ofDays(50), new byte[]{28, 29, 30}, "Resume text",
                true, new byte[]{19, 20}, phones10,  LocalDate.now(), Instant.now(),LocalTime.now(),
                MonthDay.now(), YearMonth.now(), System.currentTimeMillis(), ZonedDateTime.now(ZoneOffset.UTC),(short)106,
                1234567895L, (byte)1, 500.0f, 45000.0, 'F',Period.of(10,10,0),10,
                true, (byte) 10, 'F', (short) 70, 140000L, 4.9f, 14000.0,java.util.Date.from(Instant.now())));
    }
}
