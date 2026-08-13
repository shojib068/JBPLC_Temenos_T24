package com.temenos.t24;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.records.aajbllockerdetails.AaJblLockerDetailsRecord;
import com.temenos.t24.api.system.DataAccess;
/**
 * Routine: Input.Routine
 * Version attached to: AA.JBL.LOCKER.ACCT,INPUT
 * Business Logic:
 * 1. Retrieve Locker ID from the current record.
 * 
 * 2. Validation: Empty Locker ID
 *    - If Locker ID is null or empty:
 *        → Raise error: "Locker ID cannot be empty"
 * 
 * 3. Validation: Locker Existence
 *    - Check if Locker ID exists in AA.JBL.LOCKER.DETAILS
 *    - If not found:
 *        → Raise error: "Invalid Locker ID"
 * 
 * 4. Validation: Unauthorised Usage (NAU Check)
 *    - Check AA.JBL.LOCKER.ACCT$NAU for any records using the same Locker ID
 *    - If another unauthorised record exists:
 *        → Raise error: "This locker is already taken and unauthorised"
 * 
 * 5. Retrieve Locker Status and Linked Account from Locker Details record
 * 
 * 6. Validation: Locker Already Used
 *    - If Locker Status = "USED"
 *    - And Locker is linked to another account:
 *        → Raise error:
 *          "Cannot allocate locker. Locker <LockerId> is already used."
 * 
 * 7. Validation: Damaged or Maintenance Locker
 *    - If Locker Status = "DAMAGED" or "MAINTENANCE"
 *    - And Locker is linked to another account:
 *        → Trigger override:
 *          "Locker Id <LockerId> is in <Status> stage"
 * 
 * 8. If all validations pass:
 *    - Allow the transaction to proceed
 * 
 * @author kawsar
 */
public class GbInAaJblLockerActLockId extends RecordLifecycle {

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {

        DataAccess da = new DataAccess(this);

        AaJblLockerAcctRecord lockerAccRec = null;
        AaJblLockerDetailsRecord locDetRec = null;

        String lockerId = null;
        String lockerStatus = null;
        String lockerAcc = null;

        List<String> nauIds = null;

        /* -------------------------------
         * Read Locker ID safely
         * ------------------------------- */
        try{
            lockerAccRec = new AaJblLockerAcctRecord(currentRecord);
        if (lockerAccRec.getLockerId() != null) {
            lockerId = lockerAccRec.getLockerId().getValue();
        }
            } catch (Exception e ){
                
            }
        

        /* -------------------------------
         * Validation: Empty Locker ID
         * ------------------------------- */
        if (lockerId == null || lockerId.trim().isEmpty()) {
            if (lockerAccRec.getLockerId() != null) {
                lockerAccRec.getLockerId().setError("Locker ID cannot be empty");
            }
            return lockerAccRec.getValidationResponse();
        }

        /* -------------------------------
         * Validation: Locker exists
         * ------------------------------- */
        try {
            locDetRec = new AaJblLockerDetailsRecord(
                    da.getRecord("AA.JBL.LOCKER.DETAILS", lockerId));
        } catch (Exception e) {
            lockerAccRec.getLockerId().setError("Invalid Locker ID");
            return lockerAccRec.getValidationResponse();
        }

        /* -------------------------------
         * Check UNAUTH (NAU)
         * ------------------------------- */
        try {
            nauIds = da.selectRecords("", "AA.JBL.LOCKER.ACCT$NAU", "",
                    "WITH LOCKER.ID EQ " + lockerId);

            if (nauIds != null) {
                for (String id : nauIds) {
                    if (!id.equals(currentRecordId)) {
                        lockerAccRec.getLockerId()
                                .setError("This locker is already taken and unauthorised");
                        return lockerAccRec.getValidationResponse();
                    }
                }
            }
        } catch (Exception e) {
            // Optional: log error
        }

        /* -------------------------------
         * Read status & account safely
         * ------------------------------- */
        if (locDetRec.getStatus() != null) {
            lockerStatus = locDetRec.getStatus().getValue();
        }

        if (locDetRec.getLockerAcct() != null) {
            lockerAcc = locDetRec.getLockerAcct().getValue();
        }

        /* -------------------------------
         * Validation: Already used
         * ------------------------------- */
        if ("USED".equalsIgnoreCase(lockerStatus)
                && lockerAcc != null
                && !lockerAcc.equals(currentRecordId)) {

            lockerAccRec.getLockerId().setError(
                    "Cannot allocate locker. Locker " + lockerId + " is already used.");
            return lockerAccRec.getValidationResponse();
        }

        /* -------------------------------
         * Validation: Damaged / Maintenance
         * ------------------------------- */
        if (("DAMAGED".equalsIgnoreCase(lockerStatus)
                || "MAINTENANCE".equalsIgnoreCase(lockerStatus))
                && lockerAcc != null
                && !lockerAcc.equals(currentRecordId)) {

            lockerAccRec.getLockerId().setOverride(
                    "Locker Id " + lockerId + " is in " + lockerStatus + " stage");
            return lockerAccRec.getValidationResponse();
        }

        /* -------------------------------
         * Success
         * ------------------------------- */
        return lockerAccRec.getValidationResponse();
    }
}