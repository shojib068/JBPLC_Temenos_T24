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
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbInAaJblLokcerAcctLockId extends RecordLifecycle{

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        // TODO Auto-generated method stub
        DataAccess da = new DataAccess(this);
        AaJblLockerAcctRecord lockerAccRec = null;
        AaJblLockerDetailsRecord locDetRec = null;
        
        String lockerId = null;
        String lockerStatus = null;
        String lockerAcc = null;
        
        List<String> nauIds = null;
        
        lockerId = lockerAccRec.getLockerId().getValue();
        
        /* -------------------------------------------------
         * Locker ID cannot be empty
         * ------------------------------------------------- */
        if (lockerId == null || lockerId.trim().isEmpty()) {
            lockerAccRec.getLockerId().setError("Locker ID cannot be empty");
            return lockerAccRec.getValidationResponse();
        }
        /* -------------------------------------------------
         * Locker ID must exist in LOCKER DETAILS
         * ------------------------------------------------- */
        try {
            locDetRec = new AaJblLockerDetailsRecord(
                    da.getRecord("AA.JBL.LOCKER.DETAILS", lockerId));
        } catch (Exception e) {
            lockerAccRec.getLockerId().setError("Invalid Locker ID");
            return lockerAccRec.getValidationResponse();
        }
        /* -------------------------------------------------
         * Check UNAUTH file (NAU)
         * ------------------------------------------------- */
        try {
            nauIds = da.selectRecords("","AA.JBL.LOCKER.ACCT$NAU","","WITH LOCKER.ID EQ " + lockerId);
            if (nauIds != null) {
                for (String id : nauIds) {
                    if (!id.equals(currentRecordId)) {
                        lockerAccRec.getLockerId().setError("This locker is already taken and unauth");
                        return lockerAccRec.getValidationResponse();
                    }
                }
            }

        } catch (Exception e) {
           
        }
        /* -------------------------------------------------
         * Check LIVE locker usage
         * ------------------------------------------------- */
        lockerStatus = locDetRec.getStatus().getValue();
        lockerAcc    = locDetRec.getLockerAcct().getValue();

        if ("USED".equalsIgnoreCase(lockerStatus) && lockerAcc != null && !lockerAcc.equals(currentRecordId)) {

            lockerAccRec.getLockerId().setError("Cannot allocate locker. Locker " + lockerId + " is already used.");
            return lockerAccRec.getValidationResponse();
        }
        if (("DAMAGED".equalsIgnoreCase(lockerStatus) || "MAINTENANCE".equalsIgnoreCase(lockerStatus))  && lockerAcc != null && !lockerAcc.equals(currentRecordId)) {

            lockerAccRec.getLockerId().setOverride("Locker Id " + lockerId + " is in "+ lockerStatus +" stage");
            return lockerAccRec.getValidationResponse();
        }

        return null;
    }

}
