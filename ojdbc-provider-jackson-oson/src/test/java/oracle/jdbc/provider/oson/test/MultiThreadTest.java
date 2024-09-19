package oracle.jdbc.provider.oson.test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import oracle.jdbc.provider.oson.JacksonOsonConverter;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonGenerator;
import oracle.sql.json.OracleJsonParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiThreadTest {

    public static void main(String[] args) throws IOException, InterruptedException {

          Person[]  persons = {
                new Person("John Doe", "Manager", "New York"),
                new Person("Jane Smith", "Developer", "California"),
                new Person("Alice Johnson", "Designer", "Texas"),
                new Person("Bob Brown", "Analyst", "Florida"),
                new Person("Charlie White", "HR", "Chicago"),
                new Person("David Green", "Support", "Seattle"),
                new Person("Emma Taylor", "Marketing", "Boston"),
                new Person("Frank Black", "Consultant", "Washington"),
                new Person("Grace King", "Engineer", "Denver"),
                new Person("Henry Adams", "Technician", "Arizona")
        };

        ExecutorService executorService = Executors.newFixedThreadPool(10);


        CountDownLatch latch = new CountDownLatch(1);

        for (int i = 0; i <   persons.length; i++) {
            int index = i;
            executorService.execute(() -> {
                try {
                    latch.await();
                    Person origPerson = persons[index];

                    JacksonOsonConverter conv = new JacksonOsonConverter();
                    OracleJsonFactory jsonFactory = new OracleJsonFactory();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();

                    OracleJsonGenerator generator = jsonFactory.createJsonBinaryGenerator(out);
                    conv.serialize(generator, persons[index]);
                    generator.close();


                    OracleJsonParser oParser = jsonFactory.createJsonBinaryParser(new ByteArrayInputStream(out.toByteArray()));
                    Person deserPerson = (Person) conv.deserialize(oParser, Person.class);
                    System.out.println("Comparing the two instances:" + origPerson.equals(deserPerson));


                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        Thread.sleep(2000);
        latch.countDown();
        // Shut down the executor service
        executorService.shutdown();
    }
}

class Person {

    private String name;
    private String designation;
    private String address;

    public Person() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return Objects.equals(name, person.name) && Objects.equals(designation, person.designation) && Objects.equals(address, person.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, designation, address);
    }

    public Person(String name, String designation, String address) {
        this.name = name;
        this.designation = designation;
        this.address = address;
    }

    @JsonProperty
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty
    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    @JsonProperty
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "  Person{" +
                "name='" + name + '\'' +
                ", designation='" + designation + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}

