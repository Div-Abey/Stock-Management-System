package com.stockmanagement.supplier.model;

/** A supplier. currentBalance = what we owe the supplier (accounts payable). */
public class Supplier {
    private final int supplierId;
    private final String name;
    private final String phone;
    private final String email;
    private final String address;
    private final double currentBalance;

    public Supplier(int supplierId, String name, String phone, String email, String address, double currentBalance) {
        this.supplierId = supplierId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.currentBalance = currentBalance;
    }

    public int getSupplierId() { return supplierId; }
    public String getName() { return name; }
    public String getPhone() { return phone == null ? "" : phone; }
    public String getEmail() { return email == null ? "" : email; }
    public String getAddress() { return address == null ? "" : address; }
    public double getCurrentBalance() { return currentBalance; }

    @Override
    public String toString() { return name; }
}
