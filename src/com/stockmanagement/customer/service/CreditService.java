package com.stockmanagement.customer.service;

import com.stockmanagement.customer.model.Customer;

/** Credit-limit arithmetic for a customer account. */
public class CreditService {

    public double getAvailableCredit(Customer customer) {
        return customer.getAvailableCredit();
    }

    public boolean canBuyOnCredit(Customer customer, double amount) {
        return customer.getCurrentBalance() + amount <= customer.getCreditLimit() + 0.005;
    }
}
