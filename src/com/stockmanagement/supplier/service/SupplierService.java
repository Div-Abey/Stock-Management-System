package com.stockmanagement.supplier.service;

import com.stockmanagement.common.exception.ValidationException;
import com.stockmanagement.common.util.ValidationUtil;
import com.stockmanagement.supplier.dao.SupplierDAO;
import com.stockmanagement.supplier.model.Supplier;

import java.sql.SQLException;
import java.util.List;

public class SupplierService {

    private final SupplierDAO supplierDAO = new SupplierDAO();

    public void addSupplier(String name, String phone, String email, String address) throws SQLException {
        supplierDAO.insert(ValidationUtil.requireText(name, "Supplier name"), ValidationUtil.optionalText(phone),
                ValidationUtil.optionalText(email), ValidationUtil.optionalText(address));
    }

    public void updateSupplier(int id, String name, String phone, String email, String address) throws SQLException {
        supplierDAO.update(id, ValidationUtil.requireText(name, "Supplier name"), ValidationUtil.optionalText(phone),
                ValidationUtil.optionalText(email), ValidationUtil.optionalText(address));
    }

    public List<Supplier> getAllSuppliers() throws SQLException {
        return supplierDAO.findAll();
    }

    public Supplier getSupplier(int id) throws SQLException {
        Supplier s = supplierDAO.findById(id);
        if (s == null) {
            throw new ValidationException("Supplier not found.");
        }
        return s;
    }
}
