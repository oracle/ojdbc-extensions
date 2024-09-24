package oracle.jdbc.provider.oson.model;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.time.*;

public class AnnotationTestInstances {
    private static List<AnnonationTest> testList = new ArrayList<AnnonationTest>();

    static List<Phone> phones1 = Arrays.asList(new Phone("123-456-7890", Phone.Type.MOBILE), new Phone("098-765-4321", Phone.Type.HOME));
    static List<Phone> phones2 = Arrays.asList(new Phone("321-654-9870", Phone.Type.WORK));
    static List<Phone> phones3 = Arrays.asList(new Phone("111-222-3333", Phone.Type.MOBILE));
    static List<Phone> phones4 = Arrays.asList(new Phone("444-555-6666", Phone.Type.HOME), new Phone("777-888-9999", Phone.Type.MOBILE));
    static List<Phone> phones5 = Arrays.asList(new Phone("999-888-7777", Phone.Type.MOBILE));
    static List<Phone> phones6 = Arrays.asList(new Phone("555-444-3333", Phone.Type.WORK), new Phone("666-777-8888", Phone.Type.HOME));
    static List<Phone> phones7 = Arrays.asList(new Phone("123-123-1234", Phone.Type.HOME));
    static List<Phone> phones8 = Arrays.asList(new Phone("987-654-3210", Phone.Type.WORK));
    static List<Phone> phones9 = Arrays.asList(new Phone("555-666-7777", Phone.Type.MOBILE), new Phone("888-999-0000", Phone.Type.HOME));
    static List<Phone> phones10 = Arrays.asList(new Phone("000-111-2222", Phone.Type.WORK));

    static List<List<Phone>> phonesList = new ArrayList<>();

    public static List<AnnonationTest> getTestList() {
        if (testList.isEmpty())
            instantiateInstances();
        return testList;
    }

    public static AnnonationTest getRandomInstances () {
        if (testList.isEmpty())
            instantiateInstances();
        return testList.get(new Random().nextInt(testList.size()));
    }

    private static void instantiateInstances() {
        phonesList.add(phones1);
        phonesList.add(phones2);
        phonesList.add(phones3);
        phonesList.add(phones4);
        phonesList.add(phones5);
        phonesList.add(phones6);
        phonesList.add(phones7);
        phonesList.add(phones8);
        phonesList.add(phones9);
        phonesList.add(phones10);
        List<AnnonationTest> instances = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {

            Address address = new Address("Street " + i, "City " + i, "Country " + i);

            Job job = new Job("Job Title " + i, 50000 + (i * 1000));

            LocalDate localDate = LocalDate.of(2000 + i, i, i % 28 + 1); // example LocalDate
            LocalTime localTime = LocalTime.of(i % 24, i % 60, i % 60); // example LocalTime
            LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime); // example LocalDateTime
            ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.systemDefault()); // ZonedDateTime
            OffsetDateTime offsetDateTime = OffsetDateTime.of(localDateTime, ZoneOffset.ofHours(i % 12 - 6)); // OffsetDateTime
            OffsetTime offsetTime = OffsetTime.of(localTime, ZoneOffset.ofHours(i % 12 - 6)); // OffsetTime
            YearMonth yearMonth = YearMonth.of(2000 + i, i); // YearMonth
            MonthDay monthDay = MonthDay.of(i % 12 + 1, i % 28 + 1); // MonthDay
            Instant instant = Instant.now().plusSeconds(i * 1000);

            AnnonationTest person = new AnnonationTest(
                    "Full Name " + i,
                    20 + i,
                    i % 2 == 0,
                    Date.valueOf("2015-01-10"),
                    "email" + i + "@example.com",
                    phonesList.get(new Random().nextInt(phonesList.size())),
                    address,
                    job,
                    localDate,
                    localTime,
                    localDateTime,
                    zonedDateTime,
                    offsetDateTime,
                    offsetTime,
                    yearMonth,
                    monthDay,
                    instant
            );

            job.setPerson(person);

            testList.add(person);
        }
    }
}
