package ru.ifmo.rain.menshutin.rmi;

public class RemotePerson implements Person {
    private int id;
    private String firstName;
    private String lastName;

    @Override
    public int getPassportId() {
        return id;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    public RemotePerson(int id, String firstName, String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
