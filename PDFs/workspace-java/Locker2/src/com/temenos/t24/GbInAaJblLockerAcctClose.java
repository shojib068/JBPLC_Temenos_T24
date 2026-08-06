package com.temenos.t24;

import java.util.List;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.aajbllockeracct.AaJblLockerAcctRecord;
import com.temenos.t24.api.system.DataAccess;
import com.temenos.t24.api.system.Session;

/**
 * TODO: Document me!
 *
 * @author kawsar
 *
 */
public class GbInAaJblLockerAcctClose extends RecordLifecycle{

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        
        AaJblLockerAcctRecord locAcctRec = new AaJblLockerAcctRecord(currentRecord);
        DataAccess da = new DataAccess (this);
        Session ss = new Session(this);
        String coCode = ss.getCompanyId();
        String selectStmt = " WITH CO.CODE EQ "+coCode+ " AND LOCKER.ACCT.ID EQ "+currentRecordId+" AND STATUS EQ 'Due'";
        List<String> chargeIds = da.selectRecords("BNK", "AA.JBL.LOCKER.CHARGE", "", selectStmt);
        if(chargeIds != null && !chargeIds.isEmpty() ){
            locAcctRec.getAcctNo().setError("Due Records Exist");
        }
        return locAcctRec.getValidationResponse();
    }

}
