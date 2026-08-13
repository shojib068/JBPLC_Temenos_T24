package com.temenos.t24;
import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebaajbllockeraccess.EbAaJblLockerAccessRecord;
import com.temenos.t24.api.records.ebaajbllockeracct.EbAaJblLockerAcctRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: if the locker status is !Active-> give an override message after commit
 *
 * @author kawsar
 *
 */
public class GbEbAaJblLockerAccessLockId extends RecordLifecycle {

    @Override
    public TValidationResponse validateRecord(
            String application,
            String currentRecordId,
            TStructure currentRecord,
            TStructure unauthorisedRecord,
            TStructure liveRecord,
            TransactionContext transactionContext) {

        DataAccess da = new DataAccess(this);
        EbAaJblLockerAccessRecord rec =
                new EbAaJblLockerAccessRecord(currentRecord);

        String lockerId;
        String status;

        /* -----------------------------
         * 1. Read Locker ID
         * ----------------------------- */
        lockerId = rec.getLockerId().getValue();

        if (lockerId == null || lockerId.trim().isEmpty()) {
            rec.getLockerId().setError("Locker ID cannot be empty");
            return rec.getValidationResponse();   
        }

        /* -----------------------------
         * 2. Read Locker Account
         * ----------------------------- */
        EbAaJblLockerAcctRecord accRec;
        try {
            accRec = new EbAaJblLockerAcctRecord(
                    da.getRecord("EB.AA.JBL.LOCKER.ACCT", lockerId));
        } catch (Exception e) {
            rec.getLockerId()
                    .setError("Invalid Locker ID or locker account not found");
            return rec.getValidationResponse();  
        }

        /* -----------------------------
         * 3. Read Status
         * ----------------------------- */
        status = accRec.getStatus().getValue();

        if (status == null || status.trim().isEmpty()) {
            rec.getLockerId()
                    .setError("Locker status is not maintained");
            return rec.getValidationResponse();  
        }

        status = status.trim().toUpperCase();

        /* -----------------------------
         * 4. Business Rule → OVERRIDE
         * ----------------------------- */
        if (!"ACTIVE".equals(status)) {
            rec.getLockerId().setOverride(
                    "Locker access is not allowed for status "
                            + status + ". Override required.");
            return rec.getValidationResponse(); 
        }

        return null; 
    }
}

