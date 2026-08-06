package com.temenos.t24;

import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aajbllockeraccess.AaJblLockerAccessRecord;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * Routine: Default.Routine
 * Version Attached: AA.JBL.LOCKER.ACCESS,TIME.IN  
 * Business Logic:
 * 
 * Processing Steps:
 * 
 * 1. Retrieve the Locker ID from the current locker access record.
 * 
 * 2. If the Locker ID is available:
 *      - Fetch the corresponding Locker Account record
 *        (AA.JBL.LOCKER.ACCT) using the Locker ID.
 * 
 * 3. From the Locker Account record:
 *      - Extract the Customer ID associated with the locker.
 * 
 * 4. Set the retrieved Customer ID into the current Locker Access record.
 * 
 * 5. If any step fails (e.g., Locker ID not present, record not found,
 *    or field missing), the Customer field is set as empty to avoid errors.
 *    
 * @author kawsar
 */
public class GbDAaJblLockerAccessCustId extends RecordLifecycle {

    @Override
    public void defaultFieldValues(String application, String currentRecordId,
            TStructure currentRecord, TStructure unauthorisedRecord,
            TStructure liveRecord, TransactionContext transactionContext) {

        DataAccess da = new DataAccess(this);

        String lockerId = "";
        String customerId = "";

        AaJblLockerAccessRecord lockerAccessRec = null;
        AaJblLockerAcctRecord lockerAcctRec = null;

        // -------------------------
        // GET LOCKER ID
        // -------------------------
        try {
            lockerAccessRec = new AaJblLockerAccessRecord(currentRecord);
            lockerId = lockerAccessRec.getLockerId().getValue();
        } catch (Exception e) {
            lockerId = "";
        }

        // -------------------------
        // FETCH LOCKER ACCOUNT
        // -------------------------
        try {
            if (lockerId != null && !lockerId.trim().isEmpty()) {

                TStructure accStruct = da.getRecord("AA.JBL.LOCKER.ACCT", lockerId);

                if (accStruct != null) {
                    lockerAcctRec = new AaJblLockerAcctRecord(accStruct);

                    try {
                        customerId = lockerAcctRec.getCustomer().getValue();
                    } catch (Exception e) {
                        customerId = "";
                    }
                }
            }
        } catch (Exception e) {
            customerId = "";
        }

        // -------------------------
        // SET CUSTOMER (SAFE)
        // -------------------------
        try {
            if (lockerAccessRec != null) {
                lockerAccessRec.setCustomer(customerId != null ? customerId : "");
                currentRecord.set(lockerAccessRec.toStructure());
            }
        } catch (Exception e) {
            // ignore safely
        }
    }
}
