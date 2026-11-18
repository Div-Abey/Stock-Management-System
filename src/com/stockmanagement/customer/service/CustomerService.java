package com.stockmanagement.customer.service;

import com.stockmanagement.common.enums.CustomerType;
import com.stockmanagement.common.exception.ValidationException;
import com.stockmanagement.common.util.ValidationUtil;
import com.stockmanagement.customer.dao.CustomerDAO;
import com.stockmanagement.customer.model.Customer;

import java.sql.SQLException;
import java.util.List;

/** Customer accounts (3.1): create / edit customers and their credit limit. */
public class CustomerService {

    private final CustomerDAO customerDAO = new CustomerDAO();

    public void addCustomer(String name, String phone, String email, CustomerType type, double creditLimit) throws SQLException {
        customerDAO.insert(ValidationUtil.requireText(name, "Customer name"), ValidationUtil.optionalText(phone),
                ValidationUtil.optionalText(email), type, ValidationUtil.requireNotNegative(creditLimit, "Credit limit"));
    }

    public void updateCustomer(int customerId, String name, String phone, String email, CustomerType type,
                               double creditLimit) throws SQLException {
        customerDAO.update(customerId, ValidationUtil.requireText(name, "Customer name"), ValidationUtil.optionalText(phone),
                ValidationUtil.optionalText(email), type, ValidationUtil.requireNotNegative(creditLimit, "Credit limit"));
    }

    public List<Customer> getAllCustomers() throws SQLException {
        return customerDAO.findAll();
    }

    public Customer getCustomer(int customerId) throws SQLException {
        Customer c = customerDAO.findById(customerId);
        if (c == null) {
            throw new ValidationException("Customer not found.");
        }
        return c;
    }
}
