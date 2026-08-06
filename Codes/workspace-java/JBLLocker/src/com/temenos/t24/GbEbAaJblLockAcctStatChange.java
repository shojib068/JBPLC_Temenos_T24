package com.temenos.t24;

import com.temenos.api.TStructure;
import com.temenos.api.TValidationResponse;
import com.temenos.t24.api.complex.eb.templatehook.TransactionContext;
import com.temenos.t24.api.hook.system.RecordLifecycle;
import com.temenos.t24.api.records.ebaajbllockeracct.EbAaJblLockerAcctRecord;

/**
 * VERSION: EB.AA.JBL.LOCKER.ACCT,STATUS
 * FUNCTIONALITY
 * Change the status of locker
 * Status cannot be closed. For closing the locker go to EB.AA.JBL.LOCKER.ACCT,CLOSE
 * TYPE: ID.RTN
 * @author kawsar
 *
 */
public class GbEbAaJblLockAcctStatChange extends RecordLifecycle{

    @Override
    public TValidationResponse validateRecord(String application, String currentRecordId, TStructure currentRecord,
            TStructure unauthorisedRecord, TStructure liveRecord, TransactionContext transactionContext) {
        
        EbAaJblLockerAcctRecord locAccRec = new EbAaJblLockerAcctRecord(currentRecord);
        String status = locAccRec.getStatus().getValue();
        try{
            if(status.equalsIgnoreCase("CLOSED") || status.isEmpty()){
                locAccRec.getStatus().setError("Status cannot be " + status);
                return locAccRec.getValidationResponse();
            }
        }catch(Exception e){
            throw e;
        }
       
        return null;
    }

}
