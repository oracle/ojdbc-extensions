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

import java.util.*;
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

    public static AnnonationTest getRandomInstance() {
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
                    new Date(),
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
