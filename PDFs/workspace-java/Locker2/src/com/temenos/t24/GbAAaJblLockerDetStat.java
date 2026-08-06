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
 * Version Attached: AA.JBL.LOCKER.ACCT,INPUT
 * Business Logic:
 * 
 * 1. Retrieve Locker ID and Status from the updated locker account record.
 * 
 * 2. Using the Locker ID, read the corresponding Locker Details record.
 * 
 * 3. Based on the account status, update the locker details as follows:
 *    //Now it is not working
 *    CASE 1: If Account Status = "CLOSED"
 *      - Set Locker Details status to "Free"
 *      - Remove the linked locker account reference
 *      - This indicates the locker is now available for reassignment
 * 
 *    CASE 2: If Account Status = "ACTIVE", "FREEZE", "DECEASED", or "UNCLAIMED"
 *      AND
 *      - Locker status is currently "FREE"
 *      - No account is linked to the locker
 * 
 *      Then:
 *      - Set Locker Details status to "Used"
 *      - Link the locker to the current account (store currentRecordId)
 *      - This indicates the locker is now occupied
 * 
 * 4. No action is taken if:
 *      - Locker ID or Status is null
 *      - Locker is already assigned or not in FREE state
 * 
 * @author kawsar
 */
public class GbAAaJblLockerDetStat extends RecordLifecycle {

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {

        DataAccess da = new DataAccess(this);

        AaJblLockerAcctRecord locAcctRec = new AaJblLockerAcctRecord(currentRecord);

        String lockerId = "";
        String locAcctStatus = "";
        String locDetStatus = "";
        String linkedAcct = "";

        try {
            lockerId = locAcctRec.getLockerId().getValue();    
        } catch (Exception e) {}
        try{
            locAcctStatus = locAcctRec.getStatus().getValue();
        }catch(Exception e){}
        
        AaJblLockerDetailsRecord locDetailsRec = null;
        try {
            locDetailsRec = new AaJblLockerDetailsRecord(
                    da.getRecord("AA.JBL.LOCKER.DETAILS", lockerId));
        } catch (Exception e) {}

        if(locDetailsRec!= null){
            try {
                locDetStatus = locDetailsRec.getStatus().getValue();        
            } catch (Exception e) {}
            try{
                linkedAcct = locDetailsRec.getLockerAcct().getValue();  
            }catch(Exception e){}
        }


        AaJblLockerDetailsTable locDetTable = new AaJblLockerDetailsTable(this);

        // ✅ MAIN CONDITION
        if ("FREE".equalsIgnoreCase(locDetStatus)
                && (linkedAcct == null || linkedAcct.isEmpty())
                && locAcctStatus != null
                && ("ACTIVE".equalsIgnoreCase(locAcctStatus)
                    || "FREEZE".equalsIgnoreCase(locAcctStatus)
                    || "DECEASED".equalsIgnoreCase(locAcctStatus)
                    || "UNCLAIMED".equalsIgnoreCase(locAcctStatus))) {

            locDetailsRec.setStatus("Used");
            locDetailsRec.setLockerAcct(currentRecordId);
            try {
                locDetTable.write(lockerId, locDetailsRec);
            } catch (T24IOException e) {}
        }
    }
}
