package com.stockmanagement.supplier.dao;

import com.stockmanagement.common.enums.ChequeStatus;
import com.stockmanagement.common.enums.ChequeType;
import com.stockmanagement.common.util.DateUtil;
import com.stockmanagement.database.DatabaseConnection;
import com.stockmanagement.supplier.model.Cheque;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** SQL for the cheques table (received from customers and issued to suppliers). */
public class ChequeDAO {

    private static final String SELECT =
            "SELECT c.cheque_id, c.cheque_number, c.bank_name, c.amount, c.issue_date, c.realization_date, " +
                    "       c.cheque_type, c.customer_id, c.supplier_id, c.status, c.reminder_sent, " +
                    "       COALESCE(cu.customer_name, s.supplier_name, '') AS party_name " +
                    "FROM cheques c " +
                    "LEFT JOIN customers cu ON cu.customer_id = c.customer_id " +
                    "LEFT JOIN suppliers s ON s.supplier_id = c.supplier_id ";

    /** Returns the new cheque_id. */
    public int insert(Connection conn, String chequeNumber, String bankName, double amount, LocalDate issueDate,
                      LocalDate realizationDate, ChequeType type, Integer customerId, Integer supplierId) throws SQLException {
        String sql = "INSERT INTO cheques(cheque_number, bank_name, amount, issue_date, realization_date, " +
                "cheque_type, customer_id, supplier_id, status) VALUES(?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, chequeNumber);
            ps.setString(2, bankName);
            ps.setDouble(3, amount);
            ps.setString(4, DateUtil.format(issueDate));
            ps.setString(5, DateUtil.format(realizationDate));
            ps.setString(6, type.name());
            setNullableInt(ps, 7, customerId);
            setNullableInt(ps, 8, supplierId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Could not save the cheque.");
    }

    public List<Cheque> findAll() throws SQLException {
        return query(SELECT + "ORDER BY c.realization_date ASC, c.cheque_id ASC");
    }

    /** Pending cheques whose realization date is on or before the given date. */
    public List<Cheque> findPendingUpTo(LocalDate date) throws SQLException {
        return query(SELECT + "WHERE c.status = 'PENDING' AND c.realization_date <= '" + DateUtil.format(date) +
                "' ORDER BY c.realization_date ASC, c.cheque_id ASC");
    }

    public Cheque findById(Connection conn, int chequeId) throws SQLException {
        String sql = SELECT + "WHERE c.cheque_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, chequeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void updateStatus(Connection conn, int chequeId, ChequeStatus status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE cheques SET status = ? WHERE cheque_id = ?")) {
            ps.setString(1, status.name());
            ps.setInt(2, chequeId);
            ps.executeUpdate();
        }
    }

    public void markReminderSent(Connection conn, int chequeId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE cheques SET reminder_sent = 1 WHERE cheque_id = ?")) {
            ps.setInt(1, chequeId);
            ps.executeUpdate();
        }
    }

    /** Writes a dashboard notification (notification_logs) so reminders leave a trace. */
    public void logNotification(Connection conn, String message) throws SQLException {
        String sql = "INSERT INTO notification_logs(channel, recipient, message, status) VALUES('DASHBOARD', 'ALL', ?, 'SENT')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, message);
            ps.executeUpdate();
        }
    }

    private List<Cheque> query(String sql) throws SQLException {
        List<Cheque> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    private Cheque map(ResultSet rs) throws SQLException {
        int cust = rs.getInt("customer_id");
        boolean custNull = rs.wasNull();
        int sup = rs.getInt("supplier_id");
        boolean supNull = rs.wasNull();
        return new Cheque(rs.getInt("cheque_id"), rs.getString("cheque_number"), rs.getString("bank_name"),
                rs.getDouble("amount"), DateUtil.fromDb(rs.getString("issue_date")),
                DateUtil.fromDb(rs.getString("realization_date")), ChequeType.valueOf(rs.getString("cheque_type")),
                custNull ? null : cust, supNull ? null : sup, rs.getString("party_name"),
                ChequeStatus.valueOf(rs.getString("status")), rs.getInt("reminder_sent") == 1);
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }
}
