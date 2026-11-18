package com.stockmanagement.inventory.service;

import com.stockmanagement.common.exception.ValidationException;
import com.stockmanagement.inventory.dao.ItemDAO;
import com.stockmanagement.inventory.dao.UnitDAO;
import com.stockmanagement.inventory.model.Item;
import com.stockmanagement.inventory.model.Unit;
import com.stockmanagement.inventory.model.UnitConversion;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Bulk unit (Carton/Box) <-> retail unit (Piece/Kg/Gram) maths.
 * Rules made for one item win over default rules; a rule can be used in reverse (1 Box = 12 Piece also gives Piece -> Box).
 * Stock is always stored in the item's BASE unit.
 */
public class UnitConversionService {

    private final UnitDAO unitDAO = new UnitDAO();
    private final ItemDAO itemDAO = new ItemDAO();

    // ------------------------------------------------------------ units

    public List<Unit> getUnits() throws SQLException {
        return unitDAO.findAll();
    }

    public void addUnit(String name, boolean baseUnit) throws ValidationException, SQLException {
        if (name == null || name.isBlank()) throw new ValidationException("Unit name is required.");
        name = name.trim();
        if (unitDAO.nameExists(name)) throw new ValidationException("Unit \"" + name + "\" already exists.");
        unitDAO.insert(name, baseUnit);
    }

    public void deleteUnit(Unit unit) throws ValidationException, SQLException {
        if (unitDAO.countUsage(unit.getUnitId()) > 0) {
            throw new ValidationException("Unit \"" + unit.getUnitName() + "\" is used by items or conversion rules and cannot be deleted.");
        }
        unitDAO.delete(unit.getUnitId());
    }

    // ------------------------------------------------------ conversions

    public List<UnitConversion> getConversions() throws SQLException {
        return unitDAO.findAllConversions();
    }

    /** itemId == null (or 0) = default rule for every item. */
    public void addConversion(Integer itemId, Unit from, Unit to, double ratio) throws ValidationException, SQLException {
        if (itemId != null && itemId == 0) itemId = null;
        if (from == null || to == null) throw new ValidationException("Select both units.");
        if (from.getUnitId() == to.getUnitId()) throw new ValidationException("The two units must be different.");
        if (ratio <= 0) throw new ValidationException("Ratio must be greater than zero.");
        if (unitDAO.conversionExists(itemId, from.getUnitId(), to.getUnitId())
                || unitDAO.conversionExists(itemId, to.getUnitId(), from.getUnitId())) {
            throw new ValidationException("A rule between " + from + " and " + to + " already exists for this scope. Delete it first to change it.");
        }
        unitDAO.insertConversion(itemId, from.getUnitId(), to.getUnitId(), ratio);
    }

    public void deleteConversion(UnitConversion c) throws SQLException {
        unitDAO.deleteConversion(c.getConversionId());
    }

    /** How many {@code to} units are in ONE {@code from} unit for this item. */
    public double getRatio(int itemId, int fromUnitId, int toUnitId) throws ValidationException, SQLException {
        if (fromUnitId == toUnitId) return 1.0;
        Double r = lookup(itemId, fromUnitId, toUnitId);
        if (r == null) r = lookup(null, fromUnitId, toUnitId);
        if (r == null) {
            Unit f = unitDAO.findById(fromUnitId);
            Unit t = unitDAO.findById(toUnitId);
            throw new ValidationException("No conversion is defined from " + f + " to " + t
                    + ". Add one under Units & Conversions.");
        }
        return r;
    }

    private Double lookup(Integer itemId, int from, int to) throws SQLException {
        Double direct = unitDAO.findDirectRatio(itemId, from, to);
        if (direct != null && direct > 0) return direct;
        Double inverse = unitDAO.findDirectRatio(itemId, to, from);
        if (inverse != null && inverse > 0) return 1.0 / inverse;
        return null;
    }

    /**
     * Converts a quantity in any purchase/sale unit to the item's BASE unit, e.g. 3 Box -> 36 Piece.
     * This is the exact figure that must be deducted from (or added to) stock.
     * The POS calls this when an item is sold in a bulk unit.
     */
    public int toBaseUnits(int itemId, int unitId, int quantity) throws ValidationException, SQLException {
        Item item = itemDAO.findById(itemId);
        if (item == null) throw new ValidationException("Item not found.");
        double ratio = getRatio(itemId, unitId, item.getBaseUnitId());
        double result = quantity * ratio;
        long whole = Math.round(result);
        if (Math.abs(result - whole) > 1e-6) {
            throw new ValidationException("That quantity is not a whole number of " + item.getBaseUnitName()
                    + " (" + result + "). Please adjust the quantity or the ratio.");
        }
        return (int) whole;
    }

    /** Units this item can be bought/sold in: its base unit plus every unit that has a usable conversion to it. */
    public List<Unit> getUnitsFor(Item item) throws SQLException {
        List<Unit> usable = new ArrayList<>();
        for (Unit u : unitDAO.findAll()) {
            if (u.getUnitId() == item.getBaseUnitId()) {
                usable.add(0, u);
                continue;
            }
            try {
                getRatio(item.getItemId(), u.getUnitId(), item.getBaseUnitId());
                usable.add(u);
            } catch (ValidationException ignored) {
                // no conversion -> not offered
            }
        }
        return usable;
    }

    /** Text such as "1 Box = 12 Piece" for the UI. */
    public String describe(Item item, Unit unit) throws SQLException {
        if (unit.getUnitId() == item.getBaseUnitId()) return "Base unit";
        try {
            double r = getRatio(item.getItemId(), unit.getUnitId(), item.getBaseUnitId());
            return "1 " + unit + " = " + UnitConversion.trim(r) + " " + item.getBaseUnitName();
        } catch (ValidationException e) {
            return e.getMessage();
        }
    }
}
