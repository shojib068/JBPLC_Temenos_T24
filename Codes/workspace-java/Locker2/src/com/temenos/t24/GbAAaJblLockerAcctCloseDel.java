package com.temenos.t24;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24IOException;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.records.aajbllockerdetails.AaJblLockerDetailsRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.tables.aajbllockerdetails.AaJblLockerDetailsTable;
/**
 * Routine: Auth.Routine
 * Version Attached: AA.JBL.LOCKER.ACCT,CLOSE
 * Processing Steps:
 * 
 * 1. Retrieve the Locker ID from the current Locker Account record.
 * 
 * 2. Fetch the corresponding Locker Details record
 *    (AA.JBL.LOCKER.DETAILS) using the Locker ID.
 * 
 * 3. Update the Locker Details as follows:
 *      - Set Status = "Free"
 *      - Remove linked Locker Account reference
 * 
 * 4. Write the updated Locker Details record back to the database.
 * 
 * @author kawsar
 */
public class GbAAaJblLockerAcctCloseDel extends RecordLifecycle {

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        
        AaJblLockerAcctRecord lockerAcctRec = null;
        AaJblLockerDetailsRecord lockerDetRec = null;
        AaJblLockerDetailsTable lockerDetTable = null;
        DataAccess da = null;
        String lockerId = null;
        
        
        
        try {
            lockerAcctRec = new AaJblLockerAcctRecord(currentRecord);
            da = new DataAccess(this);
            try {
                lockerId = lockerAcctRec.getLockerId().getValue();
            } catch (Exception e) {}

            if (lockerId == null || lockerId.isEmpty()) {
                return;
            }


            // Fetch locker details safely
            try {
                lockerDetRec = new AaJblLockerDetailsRecord(
                        da.getRecord("AA.JBL.LOCKER.DETAILS", lockerId));
            } catch (Exception e) {}

            // Update locker details
            lockerDetRec.setStatus("Free");
            lockerDetRec.setLockerAcct("");

            lockerDetTable = new AaJblLockerDetailsTable(this);

            try {
                lockerDetTable.write(lockerId, lockerDetRec);
            } catch (T24IOException e) {}

        } catch (Exception e) {}
    }
}