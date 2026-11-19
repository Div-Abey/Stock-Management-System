package com.stockmanagement.supplier.model;

import com.stockmanagement.common.enums.ChequeStatus;
import com.stockmanagement.common.enums.ChequeType;

import java.time.LocalDate;

/** A post-dated cheque received from a customer or issued to a supplier. */
public class Cheque {
    private final int chequeId;
    private final String chequeNumber;
    private final String bankName;
    private final double amount;
    private final LocalDate issueDate;
    private final LocalDate realizationDate;
    private final ChequeType chequeType;
    private final Integer customerId;
    private final Integer supplierId;
    private final String partyName;
    private final ChequeStatus status;
    private final boolean reminderSent;

    public Cheque(int chequeId, String chequeNumber, String bankName, double amount, LocalDate issueDate,
                  LocalDate realizationDate, ChequeType chequeType, Integer customerId, Integer supplierId,
                  String partyName, ChequeStatus status, boolean reminderSent) {
        this.chequeId = chequeId;
        this.chequeNumber = chequeNumber;
        this.bankName = bankName;
        this.amount = amount;
        this.issueDate = issueDate;
        this.realizationDate = realizationDate;
        this.chequeType = chequeType;
        this.customerId = customerId;
        this.supplierId = supplierId;
        this.partyName = partyName == null ? "" : partyName;
        this.status = status;
        this.reminderSent = reminderSent;
    }

    public int getChequeId() { return chequeId; }
    public String getChequeNumber() { return chequeNumber; }
    public String getBankName() { return bankName; }
    public double getAmount() { return amount; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getRealizationDate() { return realizationDate; }
    public ChequeType getChequeType() { return chequeType; }
    public Integer getCustomerId() { return customerId; }
    public Integer getSupplierId() { return supplierId; }
    public String getPartyName() { return partyName; }
    public ChequeStatus getStatus() { return status; }
    public boolean isReminderSent() { return reminderSent; }

    /** Pending and the realization date is today or already passed. */
    public boolean isDue(LocalDate today) {
        return status == ChequeStatus.PENDING && realizationDate != null && !realizationDate.isAfter(today);
    }

    /** What to do on the realization date. */
    public String getAction() {
        return chequeType == ChequeType.RECEIVED ? "Bank this cheque" : "Make sure funds are available";
    }
}
