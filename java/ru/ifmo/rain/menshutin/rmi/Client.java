package ru.ifmo.rain.menshutin.rmi;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class Client {
    public static void main(final String... args) throws RemoteException {
        final Bank bank;
        String firstName;
        String lastName;
        String accountId;
        int passportId;
        int change;

        try {
//            bank = (Bank) Naming.lookup("//localhost/bank");
            Registry registry = LocateRegistry.getRegistry(8889);
            bank = (Bank) registry.lookup("bank");
        } catch (NotBoundException e) {
            error(e, "bank is not bound");
            return;
        }

        if (args.length != 5) {
            System.out.println("Expected five arguments, found " + args.length);
            return;
        }

        for (var arg : args) {
            if (arg == null) {
                System.out.println("Expected non-null arguments");
                return;
            }
        }

        try {
            firstName = args[0];
            lastName = args[1];
            passportId = Integer.parseInt(args[2]);
            accountId = args[3];
            change = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            error(e, "An error occurred while parsing args");
            return;
        }

        Person person = bank.getRemotePerson(passportId);
        if (person == null) {
            System.out.println("Creating new person by id " + passportId);
            bank.createPerson(passportId, firstName, lastName);
            person = bank.getRemotePerson(passportId);
        }

        if (!bank.checkPerson(passportId, firstName, lastName)) {
            System.out.println("Incorrect person data");
            return;
        }

        Account account = bank.getAccount(person, accountId);
        System.out.println("Account id: " + account.getId());
        System.out.println("Money: " + account.getAmount());
        System.out.println("Changing amount...");
        account.setAmount(account.getAmount() + change);
        System.out.println("Money: " + account.getAmount());
    }

    private static void error(Exception e, String message) {
        System.err.println(message);
        System.err.println("Exception message: " + e.getMessage());
    }
}
