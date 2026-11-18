package com.stockmanagement.customer.model;

import com.stockmanagement.common.enums.CustomerType;

/** A row of the customers table. current_balance = what the customer owes us. */
public class Customer {
    private final int customerId;
    private final String customerName;
    private final String phone;
    private final String email;
    private final CustomerType customerType;
    private final double creditLimit;
    private final double currentBalance;

    public Customer(int customerId, String customerName, String phone, String email,
                    CustomerType customerType, double creditLimit, double currentBalance) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.phone = phone;
        this.email = email;
        this.customerType = customerType;
        this.creditLimit = creditLimit;
        this.currentBalance = currentBalance;
    }

    public int getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getPhone() { return phone == null ? "" : phone; }
    public String getEmail() { return email == null ? "" : email; }
    public CustomerType getCustomerType() { return customerType; }
    public double getCreditLimit() { return creditLimit; }
    public double getCurrentBalance() { return currentBalance; }
    public double getAvailableCredit() { return Math.max(0, creditLimit - currentBalance); }

    @Override
    public String toString() { return customerName; }
}
