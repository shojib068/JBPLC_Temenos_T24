package com.temenos.t24;

import com.temenos.api.TField;
import com.temenos.api.TStructure;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebaajbllockeraccess.EbAaJblLockerAccessRecord;
import com.temenos.t24.api.records.ebaajbllockeracct.EbAaJblLockerAcctRecord;
import com.temenos.t24.api.system.DataAccess;

/**
 * TODO: 
 *
 * @author kawsar
 *
 */
public class GbEbAaJblLockerAccessCustId extends RecordLifecycle{

    @Override
    public void defaultFieldValues(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
     
        String lockerId = "";
        String customerId = "";
        
        DataAccess da = new DataAccess(this);
        EbAaJblLockerAccessRecord lockerRec = null;
        EbAaJblLockerAcctRecord lockerAcctRec = null;
        
        lockerRec = new EbAaJblLockerAccessRecord(currentRecord);
        lockerId = lockerRec.getLockerId().getValue();
        
        lockerAcctRec = new EbAaJblLockerAcctRecord(da.getRecord("EB.AA.JBL.LOCKER.ACCT",lockerId ));
        customerId = lockerAcctRec.getCustomer().getValue();
        
        lockerRec.setCustomer(customerId);
        currentRecord.set(lockerRec.toStructure());
        
    }
    

}
