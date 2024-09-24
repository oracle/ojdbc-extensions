package oracle.jdbc.provider.oson.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "full_name", "dateOfBirth", "email", "age", "isActive", "phones", "address", "job" })
public class AnnonationTest {

    @JsonProperty("full_name")
    private String name;

    @JsonIgnore
    private int age;

    @JsonProperty("is_active")
    private boolean isActive;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date dateOfBirth;

    @JsonAlias({ "emailAddress", "email_id" })
    private String email;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Phone> phones;

    @JsonUnwrapped
    private Address address;

    @JsonManagedReference
    private Job job;


    public AnnonationTest() {}

    public AnnonationTest(String name, int age, boolean isActive, Date dateOfBirth, String email, List<Phone> phones, Address address, Job job) {
        this.name = name;
        this.age = age;
        this.isActive = isActive;
        this.dateOfBirth = dateOfBirth;
        this.email = email;
        this.phones = phones;
        this.address = address;
        this.job = job;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<Phone> getPhones() {
        return phones;
    }

    public void setPhones(List<Phone> phones) {
        this.phones = phones;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }
}

class Address {

    @JsonProperty("street")
    private String streetName;

    @JsonProperty("city")
    private String city;

    @JsonProperty("country")
    private String country;

    public Address() {}

    public Address(String streetName, String city, String country) {
        this.streetName = streetName;
        this.city = city;
        this.country = country;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}


class Job {

    private String title;
    private double salary;

    @JsonBackReference  // to prevent recursion??
    private AnnonationTest person;

    public Job() {}

    public Job(String title, double salary) {
        this.title = title;
        this.salary = salary;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public AnnonationTest getPerson() {
        return person;
    }

    public void setPerson(AnnonationTest person) {
        this.person = person;
    }
}

