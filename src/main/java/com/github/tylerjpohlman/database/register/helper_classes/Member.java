package com.github.tylerjpohlman.database.register.helper_classes;

public class Member {
    private long accountNumber;
    private String firstName;
    private String lastName;

    public Member(long accountNumber, String firstName, String lastName) {
        this.accountNumber = accountNumber;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public long getAccountNumber() {
        return accountNumber;
    }
    public String getFirstName() {
        return firstName;
    }
    public String getLastName() {
        return lastName;
    }


    @Override
    public String toString() {
        String accountNumberString = String.valueOf(accountNumber);
        return firstName + ' ' + lastName + '\n' +
                "Account Number: " + "*****" + accountNumberString.substring(accountNumberString.length() - 4 );

    }
}
