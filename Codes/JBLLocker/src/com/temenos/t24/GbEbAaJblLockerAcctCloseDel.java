package com.temenos.t24;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.api.exceptions.T24IOException;
import com.temenos.t24.api.complex.eb.servicehook.TransactionData;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebaajbllockeracct.EbAaJblLockerAcctRecord;
import com.temenos.t24.api.records.ebaajbllockerdetails.EbAaJblLockerDetailsRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.tables.ebaajbllockerdetails.EbAaJblLockerDetailsTable;

/**
 *ATTACHED TO: EB.AA.JBL.LOCKER.ACCT,CLOSE
 * FUNCTIONALITY:
 * After successful authorization of a Locker Account closure:
 *      • The corresponding Locker Details record is updated
 *      • Locker status is changed from "USED" to "Free"
 *      • The linked Locker Account field is cleared (set to empty)
 *
 *TYPE: AUTH.RTN
 * @author kawsar
 *
 */
public class GbEbAaJblLockerAcctCloseDel extends RecordLifecycle{

    @Override
    public void postUpdateRequest(String application, String currentRecordId, TStructure currentRecord,
            List<TransactionData> transactionData, List<TStructure> currentRecords,
            TransactionContext transactionContext) {
        EbAaJblLockerAcctRecord acctRec = new EbAaJblLockerAcctRecord(currentRecord);
        DataAccess da = new DataAccess(this);
        
        // ----------------------------------------------------
        // Step 1: Get Locker ID from Locker Account record
        // ----------------------------------------------------
        
        String lockerId = acctRec.getLockerId().getValue();
        EbAaJblLockerDetailsRecord locDetRec = new EbAaJblLockerDetailsRecord(da.getRecord("BNK", "EB.AA.JBL.LOCKER.DETAILS", "", lockerId));
        // ----------------------------------------------------
        // Step 2: Update Locker status and clear Locker Account
        // ----------------------------------------------------
        
        locDetRec.setStatus("Free");
        locDetRec.setLockerAcct(""); 
        
        // ----------------------------------------------------
        // Step 3: Write updated Locker Details record
        // ----------------------------------------------------
        EbAaJblLockerDetailsTable detTable = new EbAaJblLockerDetailsTable(this);
        try {
            detTable.write(lockerId, locDetRec);
        } catch (T24IOException e) {}
        }
    }


